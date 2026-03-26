import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput } from '@/components/UiField';
import { bookingApi, driverApi, socketApi, ApiError, type BookingDetailDto, type DriverDto } from '@/lib/api';
import { RIDE_AREA_LABEL } from '@/lib/places';
import { driverTripStageLabel } from '@/lib/ride-copy';
import { driverStatusAction, formatBookingPerson } from '@/lib/booking-flow';
import {
  driverLocationSchema,
  driverAvailabilityFormSchema,
  driverAcceptBookingSchema,
  type DriverLocationValues,
} from '@/lib/validation/schemas';
import type { z } from 'zod';

type AvailabilityFormValues = z.infer<typeof driverAvailabilityFormSchema>;
type AcceptBookingValues = z.infer<typeof driverAcceptBookingSchema>;

export function DriverConsolePage() {
  const [profile, setProfile] = useState<DriverDto | null>(null);
  const [bookings, setBookings] = useState<BookingDetailDto[] | null>(null);
  const [banner, setBanner] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [loadingTrips, setLoadingTrips] = useState(false);
  const [busyBookingId, setBusyBookingId] = useState<number | null>(null);

  const locForm = useForm<DriverLocationValues>({
    resolver: zodResolver(driverLocationSchema),
    mode: 'onBlur',
    defaultValues: { latitude: 28.6315, longitude: 77.2167 },
  });

  const availForm = useForm<AvailabilityFormValues>({
    resolver: zodResolver(driverAvailabilityFormSchema),
    mode: 'onBlur',
    defaultValues: { available: true, lat: '', lng: '' },
  });

  const acceptForm = useForm<AcceptBookingValues>({
    resolver: zodResolver(driverAcceptBookingSchema),
    mode: 'onBlur',
    defaultValues: { bookingId: undefined },
  });

  async function loadProfile() {
    setLoadingProfile(true);
    setBanner(null);
    try {
      const p = await driverApi.profile();
      setProfile(p);
      availForm.setValue('available', Boolean(p.isAvailable));
    } catch (e) {
      setProfile(null);
      setBanner({
        type: 'error',
        text:
          e instanceof ApiError
            ? e.message
            : 'Sign in with your driver account to go online.',
      });
    } finally {
      setLoadingProfile(false);
    }
  }

  async function loadTrips(driverId: number) {
    setLoadingTrips(true);
    try {
      const list = await bookingApi.listByDriver(driverId);
      setBookings(list);
    } catch (e) {
      setBookings([]);
      setBanner({
        type: 'error',
        text: e instanceof ApiError ? e.message : 'Couldn’t load your trips.',
      });
    } finally {
      setLoadingTrips(false);
    }
  }

  useEffect(() => {
    void loadProfile();
  }, []);

  useEffect(() => {
    if (profile?.id != null) void loadTrips(profile.id);
  }, [profile?.id]);

  async function submitLocation(values: DriverLocationValues) {
    setBanner(null);
    try {
      await driverApi.updateLocation({ latitude: values.latitude, longitude: values.longitude });
      const activeBooking = bookings?.find((b) => {
        const s = (b.bookingStatus ?? '').toUpperCase();
        return ['SCHEDULED', 'CAB_ARRIVED', 'IN_RIDE'].includes(s);
      });
      if (activeBooking?.id && profile?.id) {
        await socketApi.publishLocation({
          bookingId: activeBooking.id,
          driverId: profile.id,
          latitude: values.latitude,
          longitude: values.longitude,
          timestamp: Date.now(),
        });
      }
      setBanner({ type: 'success', text: 'Location updated. Riders can find you nearby.' });
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Location update failed.' });
    }
  }

  async function submitAvailability(data: AvailabilityFormValues) {
    setBanner(null);
    const la = data.lat?.trim() ? Number(data.lat) : undefined;
    const ln = data.lng?.trim() ? Number(data.lng) : undefined;
    try {
      await driverApi.setAvailability(
        { available: data.available },
        la !== undefined && ln !== undefined ? la : undefined,
        la !== undefined && ln !== undefined ? ln : undefined
      );
      setBanner({
        type: 'success',
        text: data.available ? 'You are online and can receive trips.' : 'You are offline.',
      });
      await loadProfile();
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Could not update status.' });
    }
  }

  async function submitAcceptRide(values: AcceptBookingValues) {
    setBanner(null);
    if (profile?.id == null) {
      setBanner({ type: 'error', text: 'Load your profile first.' });
      return;
    }
    try {
      await bookingApi.update(values.bookingId, {
        status: 'SCHEDULED',
        driverId: profile.id,
      });
      setBanner({ type: 'success', text: 'Trip added to your queue.' });
      acceptForm.reset({ bookingId: undefined });
      await loadTrips(profile.id);
    } catch (e) {
      setBanner({
        type: 'error',
        text: e instanceof ApiError ? e.message : 'Could not add this trip. Check the code with your rider.',
      });
    }
  }

  async function advanceTrip(bookingId: number, nextStatus: string) {
    if (profile?.id == null) return;
    setBusyBookingId(bookingId);
    setBanner(null);
    try {
      await bookingApi.setStatus(bookingId, nextStatus);
      await loadTrips(profile.id);
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Could not update trip.' });
    } finally {
      setBusyBookingId(null);
    }
  }

  const locErr = locForm.formState.errors;
  const availErr = availForm.formState.errors;
  const acceptErr = acceptForm.formState.errors;
  const isOnline = availForm.watch('available');
  const completedTrips = bookings?.filter((b) => (b.bookingStatus ?? '').toUpperCase() === 'COMPLETED') ?? [];
  const estimatedEarnings = completedTrips.reduce((sum, b) => {
    const km = Math.max(Number(b.totalDistance ?? 0), 1);
    return sum + (50 + km * 12);
  }, 0);

  async function ratePassenger(bookingId: number) {
    if (profile?.id == null) return;
    const scoreRaw = window.prompt('Rate rider (1-5):', '5');
    if (!scoreRaw) return;
    const score = Number(scoreRaw);
    if (!Number.isFinite(score) || score < 1 || score > 5) {
      setBanner({ type: 'error', text: 'Rating must be between 1 and 5.' });
      return;
    }
    const comment = window.prompt('Comment (optional):', '') ?? undefined;
    try {
      await bookingApi.ratePassenger(bookingId, {
        actorId: profile.id,
        score: Math.round(score),
        comment: comment?.trim() || undefined,
      });
      setBanner({ type: 'success', text: 'Rider rating submitted.' });
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Could not submit rating.' });
    }
  }

  return (
    <div className="min-h-screen bg-black pb-32 text-zinc-100">
      <div className="relative h-36 overflow-hidden bg-gradient-to-br from-emerald-900/40 via-black to-black">
        <div className="absolute inset-0 opacity-20" style={{ backgroundSize: '20px 20px', backgroundImage: 'radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px)' }} />
        <div className="relative flex h-full flex-col justify-end px-4 pb-4">
          <Link to="/" className="absolute left-4 top-4 text-sm text-zinc-400 hover:text-white">
            ← Exit
          </Link>
          <Link to="/driver" className="absolute right-4 top-4 text-sm text-zinc-400 hover:text-white">
            Account
          </Link>
          <p className="text-xs font-medium uppercase tracking-widest text-zinc-500">UrbanLift Driver</p>
          <p className="font-display text-2xl font-bold text-white">
            {loadingProfile ? '…' : profile ? `Hi, ${profile.firstName ?? 'partner'}` : 'Welcome'}
          </p>
          <p className="text-sm text-zinc-500">{RIDE_AREA_LABEL}</p>
        </div>
      </div>

      <div className="mx-auto max-w-lg px-4">
        {banner && (
          <div className="mt-4">
            <Alert variant={banner.type === 'success' ? 'success' : banner.type === 'info' ? 'info' : 'error'}>
              {banner.text}
            </Alert>
          </div>
        )}

        <section className="mt-6 rounded-3xl border border-white/[0.08] bg-zinc-900/70 p-5 backdrop-blur-xl">
          <form onSubmit={availForm.handleSubmit(submitAvailability)} className="space-y-4" noValidate>
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-white">{isOnline ? 'You are online' : 'You are offline'}</p>
                <p className="text-xs text-zinc-500">
                  {isOnline ? 'Riders can match with you.' : 'Go online to receive trips.'}
                </p>
              </div>
              <label className="relative inline-block h-11 w-[4.5rem] shrink-0 cursor-pointer">
                <input type="checkbox" className="peer sr-only" {...availForm.register('available')} />
                <span className="absolute inset-0 rounded-full bg-zinc-700 transition peer-checked:bg-emerald-500" />
                <span className="absolute left-1 top-1 h-9 w-9 rounded-full bg-white shadow transition peer-checked:translate-x-[2.15rem]" />
              </label>
            </div>
            <p className="text-[11px] text-zinc-600">
              Optional: set both coordinates when you go online to pin your position on the map.
            </p>
            <div className="grid grid-cols-2 gap-2">
              <UiField label="Lat" id="alat" error={availErr.lat?.message}>
                <UiInput id="alat" step="any" className="!py-2 !text-sm" {...availForm.register('lat')} />
              </UiField>
              <UiField label="Lng" id="alng" error={availErr.lng?.message}>
                <UiInput id="alng" step="any" className="!py-2 !text-sm" {...availForm.register('lng')} />
              </UiField>
            </div>
            <UiButton type="submit" loading={availForm.formState.isSubmitting} className="w-full">
              Apply
            </UiButton>
          </form>
        </section>

        <section className="mt-6 rounded-3xl border border-white/[0.08] bg-zinc-900/50 p-5">
          <p className="text-sm font-semibold text-white">Enter trip code</p>
          <p className="mt-1 text-xs text-zinc-500">Your rider shares this after booking — same as their trip reference.</p>
          <form onSubmit={acceptForm.handleSubmit(submitAcceptRide)} className="mt-4 flex gap-2" noValidate>
            <UiInput
              id="tripcode"
              type="number"
              step={1}
              min={1}
              placeholder="Code"
              className="!py-3"
              {...acceptForm.register('bookingId')}
            />
            <UiButton type="submit" className="shrink-0 !px-6">
              Add
            </UiButton>
          </form>
          {acceptErr.bookingId?.message ? (
            <p className="mt-2 text-xs text-red-400">{acceptErr.bookingId.message}</p>
          ) : null}
        </section>

        <section className="mt-6 rounded-3xl border border-emerald-500/20 bg-emerald-500/10 p-5">
          <p className="text-sm font-semibold text-emerald-200">Today</p>
          <div className="mt-2 flex items-end justify-between">
            <div>
              <p className="text-3xl font-bold text-white">₹{Math.round(estimatedEarnings).toLocaleString()}</p>
              <p className="text-xs text-zinc-400">Estimated earnings from completed rides</p>
            </div>
            <p className="text-sm text-zinc-300">{completedTrips.length} completed</p>
          </div>
        </section>

        <section className="mt-6 rounded-3xl border border-white/[0.08] bg-zinc-900/50 p-5">
          <p className="text-sm font-semibold text-white">Your location</p>
          <form onSubmit={locForm.handleSubmit(submitLocation)} className="mt-4 grid grid-cols-2 gap-3" noValidate>
            <UiField label="Latitude" id="dlat" error={locErr.latitude?.message}>
              <UiInput id="dlat" step="any" {...locForm.register('latitude')} />
            </UiField>
            <UiField label="Longitude" id="dlng" error={locErr.longitude?.message}>
              <UiInput id="dlng" step="any" {...locForm.register('longitude')} />
            </UiField>
            <div className="col-span-2">
              <UiButton type="submit" variant="ghost" className="w-full border border-white/10">
                Update on map
              </UiButton>
            </div>
          </form>
        </section>

        <section className="mt-8 pb-10">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Your trips</h2>
            <button
              type="button"
              onClick={() => profile?.id != null && void loadTrips(profile.id)}
              className="text-sm text-emerald-400 hover:underline disabled:opacity-40"
              disabled={profile?.id == null || loadingTrips}
            >
              Refresh
            </button>
          </div>
          {!bookings?.length && !loadingTrips ? (
            <p className="rounded-2xl border border-dashed border-white/10 py-12 text-center text-sm text-zinc-500">
              No active trips. Go online or add a trip code.
            </p>
          ) : null}
          <ul className="space-y-4">
            {bookings?.map((b) => {
              const action = driverStatusAction(b.bookingStatus);
              const pax = formatBookingPerson(b.passenger);
              return (
                <li key={b.id} className="overflow-hidden rounded-2xl border border-white/[0.08] bg-zinc-900/80">
                  <div className="border-b border-white/[0.06] bg-black/30 px-4 py-3">
                    <p className="text-xs font-medium uppercase tracking-wide text-emerald-500/90">
                      {driverTripStageLabel(b.bookingStatus)}
                    </p>
                    <p className="text-lg font-semibold text-white">{pax || 'Rider'}</p>
                    <p className="text-[11px] text-zinc-600">Trip #{b.id}</p>
                  </div>
                  {b.startLocation && (
                    <p className="px-4 py-3 text-xs leading-relaxed text-zinc-400">
                      Pickup near {b.startLocation.latitude?.toFixed(3)} · Drop {b.endLocation?.latitude?.toFixed(3)}
                    </p>
                  )}
                  {action ? (
                    <div className="p-4 pt-0">
                      <UiButton
                        type="button"
                        className="w-full !py-4 text-base"
                        loading={busyBookingId === b.id}
                        disabled={busyBookingId != null && busyBookingId !== b.id}
                        onClick={() => void advanceTrip(b.id, action.nextStatus)}
                      >
                        {action.label}
                      </UiButton>
                    </div>
                  ) : null}
                  {!action && (b.bookingStatus ?? '').toUpperCase() === 'COMPLETED' ? (
                    <div className="p-4 pt-0">
                      <UiButton type="button" variant="ghost" className="w-full border border-white/10" onClick={() => void ratePassenger(b.id)}>
                        Rate rider
                      </UiButton>
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ul>
        </section>
      </div>
    </div>
  );
}
