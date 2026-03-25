import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PhaseBadge } from '@/components/PhaseBadge';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput } from '@/components/UiField';
import { FormSection } from '@/components/FormSection';
import { bookingApi, driverApi, ApiError, type BookingDetailDto, type DriverDto } from '@/lib/api';
import {
  driverLocationSchema,
  driverAvailabilityFormSchema,
  type DriverLocationValues,
} from '@/lib/validation/schemas';
import type { z } from 'zod';

type AvailabilityFormValues = z.infer<typeof driverAvailabilityFormSchema>;

export function DriverConsolePage() {
  const [profile, setProfile] = useState<DriverDto | null>(null);
  const [bookings, setBookings] = useState<BookingDetailDto[] | null>(null);
  const [banner, setBanner] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);

  const locForm = useForm<DriverLocationValues>({
    resolver: zodResolver(driverLocationSchema),
    mode: 'onBlur',
    defaultValues: { latitude: 19.076, longitude: 72.8777 },
  });

  const availForm = useForm<AvailabilityFormValues>({
    resolver: zodResolver(driverAvailabilityFormSchema),
    mode: 'onBlur',
    defaultValues: { available: true, lat: '', lng: '' },
  });

  async function loadProfile() {
    setLoadingProfile(true);
    setBanner(null);
    try {
      const p = await driverApi.profile();
      setProfile(p);
      setBanner(null);
    } catch (e) {
      setProfile(null);
      setBanner({
        type: 'error',
        text:
          e instanceof ApiError
            ? e.message
            : 'Could not load profile — sign in on the driver auth page first.',
      });
    } finally {
      setLoadingProfile(false);
    }
  }

  async function loadTrips(driverId: number) {
    try {
      const list = await bookingApi.listByDriver(driverId);
      setBookings(list);
    } catch (e) {
      setBookings([]);
      setBanner({
        type: 'error',
        text: e instanceof ApiError ? e.message : 'Could not load trips',
      });
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
      setBanner({ type: 'success', text: 'Location synced to Location Service (Redis GEO).' });
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Location update failed' });
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
        text: data.available ? 'You are marked available.' : 'You are marked offline.',
      });
      await loadProfile();
    } catch (e) {
      setBanner({ type: 'error', text: e instanceof ApiError ? e.message : 'Availability update failed' });
    }
  }

  const locErr = locForm.formState.errors;
  const availErr = availForm.formState.errors;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:py-14">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <Link to="/" className="text-sm text-zinc-500 hover:text-zinc-200">
          ← Home
        </Link>
        <Link to="/driver" className="text-sm font-medium text-aqua hover:underline">
          Driver auth
        </Link>
      </div>
      <PhaseBadge phase={2} />
      <h1 className="mt-4 font-display text-3xl font-bold text-zinc-50">Driver console</h1>
      <p className="mt-2 text-sm text-zinc-500">
        Profile & booking reads use your <code className="text-zinc-400">DRIVER_JWT</code> cookie.
      </p>

      {banner && (
        <div className="mt-6">
          <Alert variant={banner.type === 'success' ? 'success' : banner.type === 'info' ? 'info' : 'error'}>
            {banner.text}
          </Alert>
        </div>
      )}

      <div className="mt-8 space-y-8">
        <section className="glass-panel shadow-card p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="font-display text-lg font-semibold text-zinc-100">Profile</h2>
              {loadingProfile && <p className="mt-2 text-sm text-zinc-500">Loading…</p>}
              {profile && (
                <ul className="mt-3 space-y-1 text-sm text-zinc-400">
                  <li>
                    <span className="text-zinc-500">ID</span> {profile.id}
                  </li>
                  <li>
                    <span className="text-zinc-500">Name</span>{' '}
                    {profile.firstName} {profile.lastName}
                  </li>
                  <li>
                    <span className="text-zinc-500">Email</span> {profile.email}
                  </li>
                  <li>
                    <span className="text-zinc-500">Status</span> {profile.driverApprovalStatus}
                  </li>
                  <li>
                    <span className="text-zinc-500">Available</span>{' '}
                    {profile.isAvailable ? 'Yes' : 'No'}
                  </li>
                </ul>
              )}
            </div>
            <UiButton type="button" variant="ghost" onClick={() => void loadProfile()} loading={loadingProfile}>
              Reload profile
            </UiButton>
          </div>
        </section>

        <FormSection
          title="GPS ping"
          description="Pushes coordinates to the Location Service so you can receive ride offers near this point."
        >
          <form onSubmit={locForm.handleSubmit(submitLocation)} className="max-w-lg space-y-4" noValidate>
            <div className="grid gap-4 sm:grid-cols-2">
              <UiField label="Latitude" id="dlat" error={locErr.latitude?.message}>
                <UiInput id="dlat" step="any" {...locForm.register('latitude')} />
              </UiField>
              <UiField label="Longitude" id="dlng" error={locErr.longitude?.message}>
                <UiInput id="dlng" step="any" {...locForm.register('longitude')} />
              </UiField>
            </div>
            <UiButton type="submit" loading={locForm.formState.isSubmitting}>
              Update location
            </UiButton>
          </form>
        </FormSection>

        <FormSection
          title="Availability"
          description="Optional lat/lng registers you on the map when you go online."
        >
          <form onSubmit={availForm.handleSubmit(submitAvailability)} className="max-w-lg space-y-4" noValidate>
            <label className="flex cursor-pointer items-center gap-3 text-sm text-zinc-300">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-white/20 bg-night-900"
                {...availForm.register('available')}
              />
              I am available for rides
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <UiField label="Latitude (optional)" id="alat" error={availErr.lat?.message}>
                <UiInput id="alat" step="any" {...availForm.register('lat')} />
              </UiField>
              <UiField label="Longitude (optional)" id="alng" error={availErr.lng?.message}>
                <UiInput id="alng" step="any" {...availForm.register('lng')} />
              </UiField>
            </div>
            <UiButton type="submit" loading={availForm.formState.isSubmitting}>
              Save availability
            </UiButton>
          </form>
        </FormSection>

        <FormSection title="Assigned bookings" description="Bookings where you are the assigned driver.">
          {!bookings?.length && (
            <p className="text-sm text-zinc-500">No bookings yet — accept a ride from the socket flow or wait for assignments.</p>
          )}
          <ul className="space-y-3">
            {bookings?.map((b) => (
              <li
                key={b.id}
                className="rounded-xl border border-white/10 bg-night-900/40 px-4 py-3 text-sm text-zinc-300"
              >
                <span className="font-mono text-aqua">#{b.id}</span> {b.bookingStatus}
              </li>
            ))}
          </ul>
        </FormSection>
      </div>
    </div>
  );
}
