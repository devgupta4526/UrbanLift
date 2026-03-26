import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PassengerBottomNav } from '@/components/PassengerBottomNav';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput } from '@/components/UiField';
import {
  authApi,
  bookingApi,
  paymentApi,
  ApiError,
  type BookingDetailDto,
  type FareEstimateDto,
} from '@/lib/api';
import { SESSION_LAST_BOOKING_ID } from '@/lib/config';
import { CAR_CLASS_LABELS, RIDE_AREA_LABEL, RIDE_PLACES, getPlace } from '@/lib/places';
import { rideHeadline, rideSubline } from '@/lib/ride-copy';
import { getStoredPassengerId, setStoredPassengerIdentity } from '@/lib/storage';
import {
  bookingStatusUpper,
  BOOKING_STATUS_ORDER,
  formatBookingPerson,
  isActiveTripStatus,
  isTerminalTripStatus,
} from '@/lib/booking-flow';
import {
  passengerIdFormSchema,
  fareEstimateFormSchema,
  createBookingFormSchema,
  paymentInitiateSchema,
  paymentConfirmSchema,
  type PassengerIdFormValues,
  type FareEstimateFormValues,
  type CreateBookingFormValues,
  type PaymentInitiateValues,
} from '@/lib/validation/schemas';

const CAR_TYPES = ['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL'] as const;

type HubView = 'home' | 'plan' | 'price' | 'ride' | 'activity' | 'account' | 'pay';

function money(n: unknown): string {
  if (n == null) return '—';
  const x = typeof n === 'number' ? n : Number(n);
  if (!Number.isFinite(x)) return String(n);
  return x.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

function readSessionLastBookingId(): number | undefined {
  try {
    const s = sessionStorage.getItem(SESSION_LAST_BOOKING_ID);
    if (!s) return undefined;
    const n = Number(s);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : undefined;
  } catch {
    return undefined;
  }
}

function ScreenBar({
  title,
  onBack,
  right,
}: {
  title?: string;
  onBack?: () => void;
  right?: ReactNode;
}) {
  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-white/[0.06] bg-black/80 px-4 py-3 backdrop-blur-md">
      <div className="flex min-w-0 flex-1 items-center gap-2">
        {onBack ? (
          <button
            type="button"
            onClick={onBack}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-300 transition hover:bg-white/10 hover:text-white"
            aria-label="Back"
          >
            ←
          </button>
        ) : (
          <span className="w-10" />
        )}
        {title ? <h1 className="truncate text-base font-semibold text-white">{title}</h1> : null}
      </div>
      <div className="shrink-0">{right}</div>
    </header>
  );
}

function MapHero({ label }: { label: string }) {
  return (
    <div className="relative h-44 overflow-hidden bg-gradient-to-b from-zinc-800 via-zinc-900 to-black">
      <div
        className="absolute inset-0 opacity-30"
        style={{
          backgroundImage: `radial-gradient(circle at 2px 2px, rgba(255,255,255,0.07) 1px, transparent 0)`,
          backgroundSize: '24px 24px',
        }}
      />
      <div className="absolute bottom-4 left-4 right-4 rounded-2xl border border-white/10 bg-black/40 px-4 py-2 backdrop-blur-sm">
        <p className="text-[11px] font-medium uppercase tracking-wider text-zinc-500">{label}</p>
        <p className="text-sm text-zinc-200">Pickup near you · {RIDE_AREA_LABEL}</p>
      </div>
    </div>
  );
}

export function PassengerRidePage() {
  const lastBookingInit = useMemo(() => readSessionLastBookingId(), []);
  const storedId = useMemo(() => getStoredPassengerId(), []);

  const [view, setView] = useState<HubView>('home');
  const [pickupPlaceId, setPickupPlaceId] = useState<string>('cp');
  const [pickupCustom, setPickupCustom] = useState<{ lat: number; lng: number } | null>(null);
  const [dropPlaceId, setDropPlaceId] = useState<string>('cyber');
  const [geoWorking, setGeoWorking] = useState(false);

  const [bookings, setBookings] = useState<BookingDetailDto[] | null>(null);
  const [tripsLoading, setTripsLoading] = useState(false);
  const [fareResult, setFareResult] = useState<FareEstimateDto | null>(null);
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [globalSuccess, setGlobalSuccess] = useState<string | null>(null);
  const [liveDetail, setLiveDetail] = useState<BookingDetailDto | null>(null);
  const [payWorking, setPayWorking] = useState(false);
  const [requestingRide, setRequestingRide] = useState(false);

  const [trackedBookingId, setTrackedBookingId] = useState<number | null>(() => lastBookingInit ?? null);

  const idForm = useForm<PassengerIdFormValues>({
    resolver: zodResolver(passengerIdFormSchema),
    mode: 'onBlur',
    defaultValues: { passengerId: storedId ?? undefined },
  });

  const fareForm = useForm<FareEstimateFormValues>({
    resolver: zodResolver(fareEstimateFormSchema),
    mode: 'onBlur',
    defaultValues: {
      startLat: 28.6315,
      startLng: 77.2167,
      endLat: 28.4959,
      endLng: 77.0887,
      carType: 'SEDAN',
    },
  });

  const bookForm = useForm<CreateBookingFormValues>({
    resolver: zodResolver(createBookingFormSchema),
    mode: 'onBlur',
    defaultValues: {
      passengerId: storedId ?? undefined,
      startLat: 28.6315,
      startLng: 77.2167,
      endLat: 28.4959,
      endLng: 77.0887,
    },
  });

  const payForm = useForm<PaymentInitiateValues>({
    resolver: zodResolver(paymentInitiateSchema),
    mode: 'onBlur',
    defaultValues: { bookingId: lastBookingInit, amount: undefined as unknown as number },
  });

  useEffect(() => {
    if (storedId) {
      idForm.setValue('passengerId', storedId);
      bookForm.setValue('passengerId', storedId);
    }
  }, [storedId, idForm, bookForm]);

  /** If a rider JWT cookie exists, sync passenger id into forms and localStorage (matches sign-in response). */
  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const v = await authApi.validate();
        if (cancelled || v.passengerId == null) return;
        setStoredPassengerIdentity(v.passengerId, v.email);
        idForm.setValue('passengerId', v.passengerId);
        bookForm.setValue('passengerId', v.passengerId);
      } catch {
        /* guest */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [idForm, bookForm]);

  useEffect(() => {
    const from = pickupCoords();
    const to = getPlace(dropPlaceId);
    if (from && to) {
      fareForm.setValue('startLat', from.lat);
      fareForm.setValue('startLng', from.lng);
      fareForm.setValue('endLat', to.lat);
      fareForm.setValue('endLng', to.lng);
      bookForm.setValue('startLat', from.lat);
      bookForm.setValue('startLng', from.lng);
      bookForm.setValue('endLat', to.lat);
      bookForm.setValue('endLng', to.lng);
    }
  }, [pickupPlaceId, pickupCustom, dropPlaceId, fareForm, bookForm]);

  function pickupCoords(): { lat: number; lng: number } | null {
    if (pickupCustom) return pickupCustom;
    const pl = getPlace(pickupPlaceId);
    return pl ? { lat: pl.lat, lng: pl.lng } : null;
  }

  function pickupLabel(): string {
    if (pickupCustom) return 'Current location';
    return getPlace(pickupPlaceId)?.label ?? 'Pickup';
  }

  function dropLabel(): string {
    return getPlace(dropPlaceId)?.label ?? 'Where to';
  }

  function savePassengerId(v: PassengerIdFormValues) {
    setStoredPassengerIdentity(v.passengerId);
    bookForm.setValue('passengerId', v.passengerId);
    setGlobalError(null);
    setGlobalSuccess('Profile saved.');
    setView('home');
  }

  function applyTrackId(id: number) {
    sessionStorage.setItem(SESSION_LAST_BOOKING_ID, String(id));
    payForm.setValue('bookingId', id);
    setTrackedBookingId(id);
  }

  function resolvedPassengerIdForActions(): number | null {
    const raw = bookForm.getValues('passengerId') ?? getStoredPassengerId();
    const parsed = passengerIdFormSchema.safeParse({ passengerId: raw });
    if (!parsed.success) return null;
    return parsed.data.passengerId;
  }

  function requestCurrentLocation() {
    if (!navigator.geolocation) {
      setGlobalError('Location isn’t available in this browser.');
      return;
    }
    setGeoWorking(true);
    setGlobalError(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPickupCustom({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setPickupPlaceId('');
        setGeoWorking(false);
      },
      () => {
        setGeoWorking(false);
        setGlobalError('Could not read your location. Choose a pickup spot instead.');
      },
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 60_000 }
    );
  }

  async function computeFareAndGo() {
    const pid = resolvedPassengerIdForActions();
    if (pid == null) {
      setGlobalError('Set up your rider profile first.');
      setView('account');
      return;
    }
    const from = pickupCoords();
    const to = getPlace(dropPlaceId);
    if (!from || !to) {
      setGlobalError('Choose a destination.');
      return;
    }
    const carType = fareForm.getValues('carType');
    setFareResult(null);
    setGlobalError(null);
    try {
      const res = await paymentApi.estimateFare({
        startLat: from.lat,
        startLng: from.lng,
        endLat: to.lat,
        endLng: to.lng,
        carType,
      });
      setFareResult(res);
      bookForm.setValue('startLat', from.lat);
      bookForm.setValue('startLng', from.lng);
      bookForm.setValue('endLat', to.lat);
      bookForm.setValue('endLng', to.lng);
      bookForm.setValue('passengerId', pid);
      if (res.totalFare != null && Number.isFinite(Number(res.totalFare))) {
        payForm.setValue('amount', Number(res.totalFare));
      }
      setView('price');
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Could not get a price for this route.');
    }
  }

  async function requestRide(values: CreateBookingFormValues) {
    setGlobalError(null);
    setGlobalSuccess(null);
    try {
      const idempotencyKey =
        typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
          ? crypto.randomUUID()
          : `${values.passengerId}-${Date.now()}`;
      const res = await bookingApi.create({
        passengerId: values.passengerId,
        startLocation: { latitude: values.startLat, longitude: values.startLng },
        endLocation: { latitude: values.endLat, longitude: values.endLng },
      }, idempotencyKey);
      if (res?.bookingId != null) {
        applyTrackId(res.bookingId);
        try {
          setLiveDetail(await bookingApi.get(res.bookingId));
        } catch {
          /* ignore */
        }
      }
      setView('ride');
      await refreshTrips(values.passengerId);
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Couldn’t request this ride.');
    }
  }

  async function confirmRequestRide() {
    const pid = resolvedPassengerIdForActions();
    const from = pickupCoords();
    const to = getPlace(dropPlaceId);
    if (pid == null || !from || !to) {
      setGlobalError('Choose pickup, destination, and profile.');
      return;
    }
    const parsed = createBookingFormSchema.safeParse({
      passengerId: pid,
      startLat: from.lat,
      startLng: from.lng,
      endLat: to.lat,
      endLng: to.lng,
    });
    if (!parsed.success) {
      const first = parsed.error.flatten().fieldErrors;
      setGlobalError(
        first.passengerId?.[0] ?? first.startLat?.[0] ?? first.endLat?.[0] ?? 'Check your trip details.'
      );
      return;
    }
    setRequestingRide(true);
    try {
      await requestRide(parsed.data);
    } finally {
      setRequestingRide(false);
    }
  }

  async function refreshTrips(passengerId?: number) {
    const pid = passengerId ?? getStoredPassengerId() ?? bookForm.getValues('passengerId');
    if (pid == null || !Number.isFinite(pid)) {
      return;
    }
    setTripsLoading(true);
    try {
      const list = await bookingApi.listByPassenger(pid);
      setBookings(list);
    } catch {
      setBookings([]);
    } finally {
      setTripsLoading(false);
    }
  }

  async function cancelBooking(bookingId: number) {
    const passengerId = resolvedPassengerIdForActions();
    if (passengerId == null) {
      setGlobalError('Sign in and save your profile first.');
      setView('account');
      return;
    }
    setGlobalSuccess(null);
    try {
      await bookingApi.cancel(bookingId);
      await refreshTrips(passengerId);
      if (liveDetail?.id === bookingId) {
        try {
          setLiveDetail(await bookingApi.get(bookingId));
        } catch {
          setLiveDetail(null);
        }
      }
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : 'Could not cancel.');
    }
  }

  async function loadLiveOnce(bookingId: number) {
    try {
      const d = await bookingApi.get(bookingId);
      setLiveDetail(d);
    } catch {
      setLiveDetail(null);
    }
  }

  useEffect(() => {
    if (view !== 'ride' || trackedBookingId == null) return;
    const bookingId = trackedBookingId;
    let cancelled = false;
    async function tick() {
      try {
        const d = await bookingApi.get(bookingId);
        if (!cancelled) setLiveDetail(d);
      } catch {
        if (!cancelled) setLiveDetail(null);
      }
    }
    void tick();
    const iv = window.setInterval(() => void tick(), 5000);
    return () => {
      cancelled = true;
      window.clearInterval(iv);
    };
  }, [view, trackedBookingId]);

  async function payForRide(values: PaymentInitiateValues) {
    setPayWorking(true);
    setGlobalError(null);
    setGlobalSuccess(null);
    try {
      const init = await paymentApi.initiate({
        bookingId: values.bookingId,
        amount: values.amount,
      });
      if (init.paymentId == null) {
        throw new Error('Payment could not be started.');
      }
      const confirmParsed = paymentConfirmSchema.safeParse({
        paymentId: init.paymentId,
      });
      if (!confirmParsed.success) throw new Error('Invalid payment reference.');
      await paymentApi.confirm({ paymentId: confirmParsed.data.paymentId });
      setGlobalSuccess('Payment successful. Thanks for riding with UrbanLift.');
      setView('activity');
    } catch (e) {
      setGlobalError(e instanceof ApiError ? e.message : e instanceof Error ? e.message : 'Payment failed.');
    } finally {
      setPayWorking(false);
    }
  }

  const showBottomNav = view === 'home' || view === 'activity' || view === 'account';
  const navActive =
    view === 'home' ? 'home' : view === 'activity' ? 'activity' : view === 'account' ? 'account' : 'home';

  const statusNow = bookingStatusUpper(liveDetail?.bookingStatus);
  const driverName = formatBookingPerson(liveDetail?.driver);
  const cancelledTrip = statusNow === 'CANCELLED';
  const stepIndex = cancelledTrip
    ? -1
    : (BOOKING_STATUS_ORDER as readonly string[]).indexOf(statusNow);

  const ie = idForm.formState.errors;
  const pfe = payForm.formState.errors;

  return (
    <div className="min-h-screen bg-black pb-24 text-zinc-100">
      {showBottomNav ? (
        <PassengerBottomNav
          active={navActive}
          onNavigate={(v) => {
            setGlobalError(null);
            if (v === 'home') setView('home');
            if (v === 'activity') {
              void refreshTrips();
              setView('activity');
            }
            if (v === 'account') setView('account');
          }}
        />
      ) : null}

      {globalError ? (
        <div className="fixed left-4 right-4 top-[env(safe-area-inset-top,12px)] z-50 mt-3 sm:left-[calc(50%-10rem)] sm:right-auto sm:w-96">
          <Alert variant="error">{globalError}</Alert>
        </div>
      ) : null}
      {globalSuccess ? (
        <div className="fixed left-4 right-4 top-[env(safe-area-inset-top,12px)] z-50 mt-3 sm:left-[calc(50%-10rem)] sm:right-auto sm:w-96">
          <Alert variant="success">{globalSuccess}</Alert>
        </div>
      ) : null}

      {/* —— Home —— */}
      {view === 'home' && (
        <div className="mx-auto max-w-lg">
          <MapHero label="UrbanLift" />
          <div className="relative z-10 -mt-8 px-4">
            <div className="rounded-2xl border border-white/[0.08] bg-zinc-900/90 p-4 shadow-card backdrop-blur-xl">
              <button
                type="button"
                onClick={() => {
                  setGlobalError(null);
                  setView('plan');
                }}
                className="flex w-full items-center gap-4 rounded-xl bg-zinc-950 px-4 py-4 text-left ring-1 ring-white/10 transition hover:ring-white/20"
              >
                <span className="text-2xl text-signal" aria-hidden>
                  ◎
                </span>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">Where to?</p>
                  <p className="text-lg font-semibold text-white">{dropLabel()}</p>
                  <p className="text-sm text-zinc-500">
                    From {pickupLabel()} · {CAR_CLASS_LABELS[fareForm.watch('carType')] ?? fareForm.watch('carType')}
                  </p>
                </div>
              </button>
            </div>
            {resolvedPassengerIdForActions() == null ? (
              <button
                type="button"
                onClick={() => setView('account')}
                className="mt-4 w-full rounded-xl border border-amber-500/25 bg-amber-500/10 px-4 py-3 text-left text-sm text-amber-100/90"
              >
                Finish your rider profile to book a trip →
              </button>
            ) : null}
            <div className="mt-8 flex items-center justify-between px-1">
              <p className="text-sm font-medium text-zinc-400">Suggestions</p>
              <Link to="/passenger" className="text-sm text-signal hover:underline">
                Sign in
              </Link>
            </div>
            <div className="mt-3 space-y-2">
              {RIDE_PLACES.slice(0, 4).map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => {
                    setDropPlaceId(p.id);
                    setView('plan');
                  }}
                  className="flex w-full items-center gap-3 rounded-xl border border-white/[0.06] bg-zinc-900/50 px-4 py-3 text-left transition hover:bg-zinc-800/80"
                >
                  <span className="text-zinc-600">📍</span>
                  <div>
                    <p className="font-medium text-zinc-100">{p.label}</p>
                    <p className="text-xs text-zinc-500">{p.area}</p>
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* —— Plan —— */}
      {view === 'plan' && (
        <div className="mx-auto min-h-screen max-w-lg bg-black">
          <ScreenBar title="Plan your ride" onBack={() => setView('home')} />
          <div className="space-y-6 px-4 py-6">
            <section>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-zinc-500">Pickup</p>
              <button
                type="button"
                onClick={requestCurrentLocation}
                disabled={geoWorking}
                className="mb-3 flex w-full items-center gap-3 rounded-xl border border-signal/30 bg-signal/10 px-4 py-3 text-left transition hover:bg-signal/15 disabled:opacity-50"
              >
                <span className="text-xl">⌖</span>
                <div>
                  <p className="font-semibold text-white">{geoWorking ? 'Locating you…' : 'Current location'}</p>
                  <p className="text-xs text-zinc-400">Best for door-to-door pickup</p>
                </div>
              </button>
              <div className="space-y-2">
                {RIDE_PLACES.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => {
                      setPickupCustom(null);
                      setPickupPlaceId(p.id);
                    }}
                    className={`flex w-full items-center gap-3 rounded-xl border px-4 py-3 text-left transition ${
                      !pickupCustom && pickupPlaceId === p.id
                        ? 'border-white/25 bg-white/[0.08]'
                        : 'border-white/[0.06] bg-zinc-900/40 hover:bg-zinc-800/60'
                    }`}
                  >
                    <span className="text-zinc-600">○</span>
                    <div>
                      <p className="font-medium text-zinc-100">{p.label}</p>
                      <p className="text-xs text-zinc-500">{p.area}</p>
                    </div>
                  </button>
                ))}
              </div>
            </section>

            <section>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-zinc-500">Dropoff</p>
              <div className="space-y-2">
                {RIDE_PLACES.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => setDropPlaceId(p.id)}
                    className={`flex w-full items-center gap-3 rounded-xl border px-4 py-3 text-left transition ${
                      dropPlaceId === p.id
                        ? 'border-signal/40 bg-signal/10'
                        : 'border-white/[0.06] bg-zinc-900/40 hover:bg-zinc-800/60'
                    }`}
                  >
                    <span className="text-signal/80">◆</span>
                    <div>
                      <p className="font-medium text-zinc-100">{p.label}</p>
                      <p className="text-xs text-zinc-500">{p.area}</p>
                    </div>
                  </button>
                ))}
              </div>
            </section>

            <section>
              <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-zinc-500">Ride options</p>
              <div className="flex gap-2 overflow-x-auto pb-1">
                {CAR_TYPES.map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => fareForm.setValue('carType', c)}
                    className={`shrink-0 rounded-2xl px-4 py-3 text-left ring-1 transition ${
                      fareForm.watch('carType') === c
                        ? 'bg-white text-black ring-white'
                        : 'bg-zinc-900/80 text-zinc-300 ring-white/10 hover:ring-white/20'
                    }`}
                  >
                    <p className="text-sm font-semibold">{CAR_CLASS_LABELS[c]}</p>
                    <p className="text-[10px] uppercase text-zinc-500">{c.replace('_', ' ')}</p>
                  </button>
                ))}
              </div>
            </section>

            <UiButton type="button" className="w-full !py-4 text-base" onClick={() => void computeFareAndGo()}>
              See price
            </UiButton>
          </div>
        </div>
      )}

      {/* —— Price —— */}
      {view === 'price' && (
        <div className="mx-auto min-h-screen max-w-lg">
          <ScreenBar title="Choose your ride" onBack={() => setView('plan')} />
          <div className="px-4 py-8">
            <div className="rounded-3xl border border-white/[0.08] bg-zinc-900/80 p-6 backdrop-blur-xl">
              <p className="text-sm text-zinc-400">{CAR_CLASS_LABELS[fareForm.watch('carType')]}</p>
              <p className="mt-1 font-display text-4xl font-bold text-white">
                ₹{money(fareResult?.totalFare)}
              </p>
              <p className="mt-2 text-sm text-zinc-500">
                {pickupLabel()} → {dropLabel()}
              </p>
              <ul className="mt-6 space-y-2 border-t border-white/[0.06] pt-4 text-sm text-zinc-400">
                <li className="flex justify-between">
                  <span>Base fare</span>
                  <span>₹{money(fareResult?.baseFare)}</span>
                </li>
                <li className="flex justify-between">
                  <span>Distance & time</span>
                  <span>₹{money((Number(fareResult?.distanceFare) || 0) + (Number(fareResult?.timeFare) || 0))}</span>
                </li>
              </ul>
            </div>
            <UiButton
              type="button"
              className="mt-6 w-full !py-4 text-base"
              loading={requestingRide}
              onClick={() => void confirmRequestRide()}
            >
              Request {CAR_CLASS_LABELS[fareForm.watch('carType')]}
            </UiButton>
          </div>
        </div>
      )}

      {/* —— Active ride —— */}
      {view === 'ride' && (
        <div className="mx-auto min-h-screen max-w-lg bg-black">
          <ScreenBar
            title="Your trip"
            onBack={() => setView('home')}
            right={
              <Link to="/passenger" className="text-sm text-zinc-400 hover:text-white">
                Help
              </Link>
            }
          />
          <div className="h-40 bg-gradient-to-b from-emerald-900/30 to-black" />
          <div className="-mt-12 space-y-4 px-4 pb-10">
            <div className="rounded-3xl border border-white/[0.08] bg-zinc-900/90 p-6 shadow-card backdrop-blur-xl">
              <p className="text-sm font-medium text-signal">{rideHeadline(liveDetail?.bookingStatus)}</p>
              <p className="mt-2 text-lg font-semibold leading-snug text-white">
                {rideSubline(liveDetail?.bookingStatus, driverName)}
              </p>
              {driverName ? (
                <div className="mt-4 flex items-center gap-3 rounded-2xl bg-black/40 px-4 py-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-700 text-lg font-semibold text-white">
                    {driverName.charAt(0)}
                  </div>
                  <div>
                    <p className="font-medium text-white">{driverName}</p>
                    <p className="text-xs text-zinc-500">Your driver</p>
                  </div>
                </div>
              ) : null}
              {liveDetail?.startLocation ? (
                <p className="mt-4 text-xs text-zinc-500">
                  {pickupLabel()} → {dropLabel()}
                </p>
              ) : null}
              <p className="mt-4 text-center text-[11px] text-zinc-600">
                Trip reference · #{trackedBookingId ?? liveDetail?.id ?? '—'}
              </p>
            </div>

            {!cancelledTrip && !isTerminalTripStatus(liveDetail?.bookingStatus) ? (
              <div className="flex flex-wrap gap-2 px-1">
                {BOOKING_STATUS_ORDER.map((st, i) => {
                  const current = st === statusNow;
                  const past = stepIndex >= 0 && i < stepIndex;
                  return (
                    <span
                      key={st}
                      className={`rounded-full px-2.5 py-1 text-[10px] font-medium ${
                        current ? 'bg-white text-black' : past ? 'bg-zinc-800 text-zinc-500' : 'text-zinc-700'
                      }`}
                    >
                      {st.replace(/_/g, ' ')}
                    </span>
                  );
                })}
              </div>
            ) : null}

            {liveDetail?.bookingStatus &&
            isActiveTripStatus(liveDetail.bookingStatus) &&
            !['CANCELLED', 'COMPLETED'].includes(liveDetail.bookingStatus.toUpperCase()) ? (
              <UiButton
                type="button"
                variant="ghost"
                className="w-full border border-white/15"
                onClick={() => liveDetail?.id && void cancelBooking(liveDetail.id)}
              >
                Cancel trip
              </UiButton>
            ) : null}

            {isTerminalTripStatus(liveDetail?.bookingStatus) && statusNow === 'COMPLETED' ? (
              <UiButton
                type="button"
                className="w-full !py-4 text-base"
                onClick={() => {
                  if (liveDetail?.id) payForm.setValue('bookingId', liveDetail.id);
                  if (fareResult?.totalFare != null) payForm.setValue('amount', Number(fareResult.totalFare));
                  setView('pay');
                }}
              >
                Pay for this trip
              </UiButton>
            ) : null}
          </div>
        </div>
      )}

      {/* —— Activity —— */}
      {view === 'activity' && (
        <div className="mx-auto min-h-screen max-w-lg">
          <ScreenBar
            title="Activity"
            right={
              <button
                type="button"
                onClick={() => void refreshTrips()}
                className="text-sm text-signal hover:underline"
              >
                Refresh
              </button>
            }
          />
          <div className="px-4 py-4">
            {!bookings?.length && !tripsLoading ? (
              <p className="py-16 text-center text-sm text-zinc-500">No trips yet. Book a ride from Home.</p>
            ) : null}
            <ul className="space-y-4">
              {bookings?.map((b) => {
                const dname = formatBookingPerson(b.driver);
                const active = isActiveTripStatus(b.bookingStatus);
                const completed = b.bookingStatus?.toUpperCase() === 'COMPLETED';
                const cancelled = b.bookingStatus?.toUpperCase() === 'CANCELLED';
                return (
                  <li
                    key={b.id}
                    className="rounded-2xl border border-white/[0.06] bg-zinc-900/50 p-4 transition hover:border-white/12"
                  >
                    <button
                      type="button"
                      onClick={() => {
                        applyTrackId(b.id);
                        if (active) {
                          void loadLiveOnce(b.id);
                          setView('ride');
                        }
                      }}
                      className="flex w-full flex-col gap-1 text-left"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="font-semibold text-white">{rideHeadline(b.bookingStatus)}</p>
                          <p className="text-xs text-zinc-500">
                            {active ? 'In progress' : b.bookingStatus}
                            {dname ? ` · ${dname}` : ''}
                          </p>
                        </div>
                        <span className="text-xs text-zinc-600">#{b.id}</span>
                      </div>
                      {active ? <p className="text-sm font-medium text-signal">Live trip</p> : null}
                    </button>
                    {completed && !cancelled ? (
                      <UiButton
                        type="button"
                        variant="ghost"
                        className="mt-3 w-full !min-h-10 border border-white/10 text-sm"
                        onClick={() => {
                          payForm.setValue('bookingId', b.id);
                          setView('pay');
                        }}
                      >
                        Pay for this trip
                      </UiButton>
                    ) : null}
                    {active && b.bookingStatus && !['CANCELLED', 'COMPLETED'].includes(b.bookingStatus.toUpperCase()) ? (
                      <UiButton
                        type="button"
                        variant="ghost"
                        className="mt-3 w-full !min-h-10 border border-white/10 text-sm"
                        onClick={() => void cancelBooking(b.id)}
                      >
                        Cancel trip
                      </UiButton>
                    ) : null}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      )}

      {/* —— Account —— */}
      {view === 'account' && (
        <div className="mx-auto min-h-screen max-w-lg">
          <ScreenBar title="Account" onBack={() => setView('home')} />
          <div className="px-4 py-8">
            <p className="text-sm text-zinc-400">
              UrbanLift uses your rider ID to request trips. If you signed up on this device, it may already be saved.
            </p>
            <form onSubmit={idForm.handleSubmit(savePassengerId)} className="mt-6 space-y-4" noValidate>
              <UiField label="Rider ID" id="pid" hint="From your welcome email or signup screen" error={ie.passengerId?.message}>
                <UiInput id="pid" type="number" step={1} min={1} {...idForm.register('passengerId')} />
              </UiField>
              <UiButton type="submit">Save profile</UiButton>
            </form>
            <Link
              to="/passenger"
              className="mt-8 block w-full rounded-2xl border border-white/10 bg-zinc-900/50 py-4 text-center text-sm font-medium text-white hover:bg-zinc-800/80"
            >
              Sign in to UrbanLift
            </Link>
          </div>
        </div>
      )}

      {/* —— Pay —— */}
      {view === 'pay' && (
        <div className="mx-auto min-h-screen max-w-lg">
          <ScreenBar title="Payment" onBack={() => setView('activity')} />
          <div className="px-4 py-8">
            <p className="text-sm text-zinc-400">Secure checkout — your card is processed by our payments partner.</p>
            <form onSubmit={payForm.handleSubmit(payForRide)} className="mt-6 space-y-5" noValidate>
              <UiField label="Trip" id="paybid" error={pfe.bookingId?.message}>
                <UiInput id="paybid" type="number" step={1} min={1} {...payForm.register('bookingId')} />
              </UiField>
              <UiField label="Amount (₹)" id="payamt" error={pfe.amount?.message}>
                <UiInput id="payamt" type="number" step="any" inputMode="decimal" {...payForm.register('amount')} />
              </UiField>
              <UiButton type="submit" className="w-full !py-4 text-base" loading={payWorking}>
                Pay now
              </UiButton>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
