import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PhaseBadge } from '@/components/PhaseBadge';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput, UiSelect } from '@/components/UiField';
import { FormSection } from '@/components/FormSection';
import {
  bookingApi,
  paymentApi,
  ApiError,
  type BookingDetailDto,
  type FareEstimateDto,
} from '@/lib/api';
import { getStoredPassengerId, setStoredPassengerIdentity } from '@/lib/storage';
import {
  passengerIdFormSchema,
  fareEstimateFormSchema,
  createBookingFormSchema,
  type PassengerIdFormValues,
  type FareEstimateFormValues,
  type CreateBookingFormValues,
} from '@/lib/validation/schemas';

const CAR_TYPES = ['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL'] as const;

type HubTab = 'identity' | 'estimate' | 'book' | 'trips';

function money(n: unknown): string {
  if (n == null) return '—';
  const x = typeof n === 'number' ? n : Number(n);
  if (!Number.isFinite(x)) return String(n);
  return x.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

export function PassengerRidePage() {
  const [tab, setTab] = useState<HubTab>('identity');
  const [bookings, setBookings] = useState<BookingDetailDto[] | null>(null);
  const [tripsLoading, setTripsLoading] = useState(false);
  const [fareResult, setFareResult] = useState<FareEstimateDto | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const storedId = useMemo(() => getStoredPassengerId(), []);

  const idForm = useForm<PassengerIdFormValues>({
    resolver: zodResolver(passengerIdFormSchema),
    mode: 'onBlur',
    defaultValues: { passengerId: storedId ?? undefined },
  });

  useEffect(() => {
    if (storedId) idForm.setValue('passengerId', storedId);
  }, [storedId, idForm]);

  const fareForm = useForm<FareEstimateFormValues>({
    resolver: zodResolver(fareEstimateFormSchema),
    mode: 'onBlur',
    defaultValues: {
      startLat: 28.6139,
      startLng: 77.209,
      endLat: 28.5355,
      endLng: 77.391,
      carType: 'SEDAN',
    },
  });

  const bookForm = useForm<CreateBookingFormValues>({
    resolver: zodResolver(createBookingFormSchema),
    mode: 'onBlur',
    defaultValues: {
      passengerId: storedId ?? undefined,
      startLat: 28.6139,
      startLng: 77.209,
      endLat: 28.5355,
      endLng: 77.391,
    },
  });

  useEffect(() => {
    const id = getStoredPassengerId();
    if (id != null) {
      bookForm.setValue('passengerId', id);
    }
  }, [bookForm]);

  function savePassengerId(v: PassengerIdFormValues) {
    setStoredPassengerIdentity(v.passengerId);
    bookForm.setValue('passengerId', v.passengerId);
    setGlobalError(null);
    setTab('estimate');
  }

  async function runFareEstimate(values: FareEstimateFormValues) {
    setFareResult(null);
    setGlobalError(null);
    try {
      const res = await paymentApi.estimateFare({
        startLat: values.startLat,
        startLng: values.startLng,
        endLat: values.endLat,
        endLng: values.endLng,
        carType: values.carType,
      });
      setFareResult(res);
      bookForm.setValue('startLat', values.startLat);
      bookForm.setValue('startLng', values.startLng);
      bookForm.setValue('endLat', values.endLat);
      bookForm.setValue('endLng', values.endLng);
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Fare estimate failed');
    }
  }

  async function createBooking(values: CreateBookingFormValues) {
    setGlobalError(null);
    try {
      await bookingApi.create({
        passengerId: values.passengerId,
        startLocation: { latitude: values.startLat, longitude: values.startLng },
        endLocation: { latitude: values.endLat, longitude: values.endLng },
      });
      setTab('trips');
      await refreshTrips(values.passengerId);
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Booking failed');
    }
  }

  async function refreshTrips(passengerId?: number) {
    const pid = passengerId ?? getStoredPassengerId() ?? bookForm.getValues('passengerId');
    if (pid == null || !Number.isFinite(pid)) {
      setGlobalError('Set a passenger ID first.');
      return;
    }
    setTripsLoading(true);
    setGlobalError(null);
    try {
      const list = await bookingApi.listByPassenger(pid);
      setBookings(list);
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Could not load trips');
      setBookings([]);
    } finally {
      setTripsLoading(false);
    }
  }

  function resolvedPassengerIdForActions(): number | null {
    const raw = bookForm.getValues('passengerId') ?? getStoredPassengerId();
    const parsed = passengerIdFormSchema.safeParse({ passengerId: raw });
    if (!parsed.success) return null;
    return parsed.data.passengerId;
  }

  async function cancelBooking(bookingId: number) {
    const passengerId = resolvedPassengerIdForActions();
    if (passengerId == null) {
      setGlobalError('Set a valid passenger ID (Identity tab or Book ride) before cancelling.');
      return;
    }
    try {
      await bookingApi.cancel(bookingId);
      await refreshTrips(passengerId);
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Cancel failed');
    }
  }

  const fe = fareForm.formState.errors;
  const be = bookForm.formState.errors;
  const ie = idForm.formState.errors;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:py-14">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <Link to="/" className="text-sm text-zinc-500 hover:text-zinc-200">
          ← Home
        </Link>
        <Link to="/passenger" className="text-sm font-medium text-signal hover:underline">
          Auth settings
        </Link>
      </div>
      <PhaseBadge phase={2} />
      <h1 className="mt-4 font-display text-3xl font-bold text-zinc-50">Passenger · Rides</h1>
      <p className="mt-2 max-w-2xl text-sm text-zinc-500">
        Fare estimates use Payment Service; bookings use Booking Service. Ensure{' '}
        <code className="text-zinc-400">{bookingApi.base}</code> and{' '}
        <code className="text-zinc-400">{paymentApi.base}</code> are running (proxied on :5173 by default).
      </p>

      {globalError && (
        <div className="mt-6">
          <Alert variant="error">{globalError}</Alert>
        </div>
      )}

      <div className="mt-8 flex flex-wrap gap-2">
        {(
          [
            ['identity', '1 · Identity'],
            ['estimate', '2 · Fare quote'],
            ['book', '3 · Book'],
            ['trips', '4 · My trips'],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`rounded-xl px-4 py-2 text-sm font-medium ring-1 transition ${
              tab === id
                ? 'bg-white/10 text-signal ring-signal/30'
                : 'bg-night-900/50 text-zinc-400 ring-white/10 hover:text-zinc-200'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="glass-panel shadow-card mt-8 space-y-10 p-6 sm:p-8">
        {tab === 'identity' && (
          <FormSection
            title="Who is riding?"
            description="The sign-in API does not return your numeric ID. We store it on this device after signup, or you can enter it here (e.g. from your welcome screen)."
          >
            <form onSubmit={idForm.handleSubmit(savePassengerId)} className="max-w-md space-y-4" noValidate>
              <UiField label="Passenger ID" id="pid" hint="Positive integer" error={ie.passengerId?.message}>
                <UiInput id="pid" type="number" step={1} min={1} {...idForm.register('passengerId')} />
              </UiField>
              <UiButton type="submit">Save & continue</UiButton>
            </form>
          </FormSection>
        )}

        {tab === 'estimate' && (
          <FormSection
            title="Fare estimate"
            description="Uses coordinates in decimal degrees (WGS84). Same validation rules as the backend."
          >
            <form onSubmit={fareForm.handleSubmit(runFareEstimate)} className="space-y-4" noValidate>
              <div className="grid gap-4 sm:grid-cols-2">
                <UiField label="Pickup latitude" id="slat" error={fe.startLat?.message}>
                  <UiInput id="slat" step="any" inputMode="decimal" {...fareForm.register('startLat')} />
                </UiField>
                <UiField label="Pickup longitude" id="slng" error={fe.startLng?.message}>
                  <UiInput id="slng" step="any" inputMode="decimal" {...fareForm.register('startLng')} />
                </UiField>
                <UiField label="Drop-off latitude" id="elat" error={fe.endLat?.message}>
                  <UiInput id="elat" step="any" inputMode="decimal" {...fareForm.register('endLat')} />
                </UiField>
                <UiField label="Drop-off longitude" id="elng" error={fe.endLng?.message}>
                  <UiInput id="elng" step="any" inputMode="decimal" {...fareForm.register('endLng')} />
                </UiField>
              </div>
              <UiField label="Vehicle class" id="ctype" error={fe.carType?.message}>
                <UiSelect id="ctype" {...fareForm.register('carType')}>
                  {CAR_TYPES.map((c) => (
                    <option key={c} value={c}>
                      {c.replace('_', ' ')}
                    </option>
                  ))}
                </UiSelect>
              </UiField>
              <UiButton type="submit" loading={fareForm.formState.isSubmitting}>
                Get estimate
              </UiButton>
            </form>
            {fareResult && (
              <div className="mt-6 rounded-xl border border-signal/20 bg-signal/5 p-4 text-sm">
                <p className="font-semibold text-signal">Estimated total: {money(fareResult.totalFare)}</p>
                <ul className="mt-2 grid gap-1 text-zinc-400 sm:grid-cols-2">
                  <li>Base: {money(fareResult.baseFare)}</li>
                  <li>Distance: {money(fareResult.distanceFare)}</li>
                  <li>Time: {money(fareResult.timeFare)}</li>
                  <li>Surge: {money(fareResult.surgeMultiplier)}×</li>
                </ul>
                <UiButton type="button" variant="ghost" className="mt-4" onClick={() => setTab('book')}>
                  Use these coordinates to book →
                </UiButton>
              </div>
            )}
          </FormSection>
        )}

        {tab === 'book' && (
          <FormSection
            title="Request a ride"
            description="Creates a booking in ASSIGNING_DRIVER state. Backend will try to notify nearby drivers."
          >
            <form onSubmit={bookForm.handleSubmit(createBooking)} className="space-y-4" noValidate>
              <UiField label="Passenger ID" id="bpid" error={be.passengerId?.message}>
                <UiInput id="bpid" type="number" step={1} min={1} {...bookForm.register('passengerId')} />
              </UiField>
              <div className="grid gap-4 sm:grid-cols-2">
                <UiField label="Pickup latitude" id="blat" error={be.startLat?.message}>
                  <UiInput id="blat" step="any" {...bookForm.register('startLat')} />
                </UiField>
                <UiField label="Pickup longitude" id="blng" error={be.startLng?.message}>
                  <UiInput id="blng" step="any" {...bookForm.register('startLng')} />
                </UiField>
                <UiField label="Drop-off latitude" id="belat" error={be.endLat?.message}>
                  <UiInput id="belat" step="any" {...bookForm.register('endLat')} />
                </UiField>
                <UiField label="Drop-off longitude" id="belng" error={be.endLng?.message}>
                  <UiInput id="belng" step="any" {...bookForm.register('endLng')} />
                </UiField>
              </div>
              <UiButton type="submit" loading={bookForm.formState.isSubmitting}>
                Book ride
              </UiButton>
            </form>
          </FormSection>
        )}

        {tab === 'trips' && (
          <FormSection title="My trips" description="Latest bookings for this passenger ID.">
            <div className="mb-4 flex flex-wrap gap-2">
              <UiButton
                type="button"
                variant="ghost"
                loading={tripsLoading}
                onClick={() => void refreshTrips()}
              >
                Refresh list
              </UiButton>
            </div>
            {!bookings?.length && !tripsLoading && (
              <p className="text-sm text-zinc-500">No bookings loaded yet. Pull refresh or complete a booking.</p>
            )}
            <ul className="space-y-3">
              {bookings?.map((b) => {
                const canCancel =
                  b.bookingStatus &&
                  !['CANCELLED', 'COMPLETED'].includes(b.bookingStatus.toUpperCase());
                return (
                  <li
                    key={b.id}
                    className="flex flex-col gap-2 rounded-xl border border-white/10 bg-night-900/40 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <span className="font-mono text-signal">#{b.id}</span>{' '}
                      <span className="text-zinc-300">{b.bookingStatus}</span>
                      {b.startLocation && (
                        <p className="text-xs text-zinc-500">
                          From {b.startLocation.latitude?.toFixed(4)},{b.startLocation.longitude?.toFixed(4)} →{' '}
                          {b.endLocation?.latitude?.toFixed(4)},{b.endLocation?.longitude?.toFixed(4)}
                        </p>
                      )}
                    </div>
                    {canCancel && (
                      <UiButton
                        type="button"
                        variant="danger"
                        onClick={() => void cancelBooking(b.id)}
                      >
                        Cancel
                      </UiButton>
                    )}
                  </li>
                );
              })}
            </ul>
          </FormSection>
        )}
      </div>
    </div>
  );
}
