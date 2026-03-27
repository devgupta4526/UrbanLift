import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { authApi, bookingApi, driverApi, paymentApi, socketApi, ApiError } from '@/lib/api';
import { getPlace, RIDE_PLACES } from '@/lib/places';
import { isActiveTripStatus } from '@/lib/booking-flow';
import { setStoredPassengerIdentity } from '@/lib/storage';

type StepState = 'pending' | 'running' | 'pass' | 'fail' | 'skip';

type QaStep = {
  id: string;
  label: string;
  state: StepState;
  detail?: string;
};

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

function stepIcon(s: StepState) {
  switch (s) {
    case 'running':
      return '…';
    case 'pass':
      return '✓';
    case 'fail':
      return '✕';
    case 'skip':
      return '⊘';
    default:
      return '○';
  }
}

function errMessage(e: unknown): string {
  if (e instanceof ApiError) return `${e.message} (HTTP ${e.status})`;
  if (e instanceof Error) return e.message;
  return String(e);
}

export function QaHubPage() {
  const [running, setRunning] = useState(false);
  const [resetFirst, setResetFirst] = useState(true);
  const [includePayment, setIncludePayment] = useState(true);
  const [includeRating, setIncludeRating] = useState(true);
  const [steps, setSteps] = useState<QaStep[]>([]);
  const [banner, setBanner] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  const initialSteps: QaStep[] = useMemo(
    () => [
      { id: 'p-validate', label: 'Passenger session (GET auth /validate)', state: 'pending' },
      { id: 'd-profile', label: 'Driver session (GET driver /profile) — optional for full trip', state: 'pending' },
      { id: 'reset', label: 'Cancel active passenger trips (cleanup)', state: 'pending' },
      { id: 'fare', label: 'Payment: fare estimate', state: 'pending' },
      { id: 'book', label: 'Booking: create trip', state: 'pending' },
      { id: 'poll', label: 'Booking: GET trip detail', state: 'pending' },
      { id: 'accept', label: 'Booking: driver accept (POST …/booking/{id} SCHEDULED)', state: 'pending' },
      { id: 'advance', label: 'Booking: CAB_ARRIVED → IN_RIDE → COMPLETED', state: 'pending' },
      { id: 'socket', label: 'Socket: POST /api/socket/location', state: 'pending' },
      { id: 'pay', label: 'Payment: initiate + confirm', state: 'pending' },
      { id: 'rate', label: 'Booking: rate driver (POST)', state: 'pending' },
      { id: 'list', label: 'Booking: list passenger trips', state: 'pending' },
    ],
    []
  );

  const runFullSuite = async () => {
    setBanner(null);
    setRunning(true);
    const st: QaStep[] = initialSteps.map(
      (s): QaStep => ({ ...s, state: 'pending', detail: undefined })
    );
    const update = (id: string, partial: Partial<QaStep>) => {
      const i = st.findIndex((x) => x.id === id);
      if (i >= 0) st[i] = { ...st[i], ...partial } as QaStep;
      setSteps([...st]);
    };

    const pickup = getPlace('cp') ?? RIDE_PLACES[0];
    const drop = getPlace('cyber') ?? RIDE_PLACES[1];
    if (!pickup || !drop || pickup.id === drop.id) {
      setBanner({ type: 'error', text: 'Place config error (cp / cyber).' });
      setRunning(false);
      return;
    }

    let passengerId: number | null = null;
    let driverId: number | null = null;
    let bookingId: number | null = null;
    let fareAmount = 120;
    let driverOk = false;
    let failed = false;

    const runOne = async (id: string, fn: () => Promise<void>) => {
      if (failed) return;
      update(id, { state: 'running', detail: undefined });
      try {
        await fn();
        update(id, { state: 'pass' });
      } catch (e) {
        failed = true;
        update(id, { state: 'fail', detail: errMessage(e) });
      }
    };

    const skipOne = (id: string, reason: string) => {
      update(id, { state: 'skip', detail: reason });
    };

    await runOne('p-validate', async () => {
      const v = await authApi.validate();
      if (!v.success || v.passengerId == null) {
        throw new Error('No passenger session. Sign in at /passenger first.');
      }
      passengerId = v.passengerId;
      setStoredPassengerIdentity(v.passengerId, v.email);
    });

    update('d-profile', { state: 'running', detail: undefined });
    try {
      const d = await driverApi.profile();
      if (d.id == null) throw new Error('No driver id on profile');
      driverId = d.id;
      driverOk = true;
      update('d-profile', { state: 'pass' });
    } catch (e) {
      driverOk = false;
      update('d-profile', {
        state: 'skip',
        detail: `${errMessage(e)} — Sign in at /driver to enable accept / advance / socket / pay / rating.`,
      });
    }

    if (failed) {
      setRunning(false);
      setSteps([...st]);
      setBanner({ type: 'error', text: 'Passenger session required. Sign in and re-run.' });
      return;
    }

    if (resetFirst && passengerId != null) {
      update('reset', { state: 'running' });
      try {
        const list = await bookingApi.listByPassenger(passengerId);
        const active = list.filter((b) => isActiveTripStatus(b.bookingStatus));
        for (const b of active) {
          await bookingApi.cancel(b.id);
          await sleep(200);
        }
        update('reset', {
          state: active.length ? 'pass' : 'skip',
          detail: active.length ? undefined : 'No active trips.',
        });
      } catch (e) {
        failed = true;
        update('reset', { state: 'fail', detail: errMessage(e) });
      }
    } else {
      skipOne('reset', 'Skipped — toggle off.');
    }

    if (!failed) {
      await runOne('fare', async () => {
        const res = await paymentApi.estimateFare({
          startLat: pickup.lat,
          startLng: pickup.lng,
          endLat: drop.lat,
          endLng: drop.lng,
          carType: 'SEDAN',
        });
        if (res.totalFare != null && Number.isFinite(Number(res.totalFare))) {
          fareAmount = Math.max(1, Math.round(Number(res.totalFare)));
        }
      });
    }

    if (!failed) {
      await runOne('book', async () => {
        const key = typeof crypto !== 'undefined' && crypto.randomUUID ? `qa-${crypto.randomUUID()}` : `qa-${Date.now()}`;
        const res = await bookingApi.create(
          {
            passengerId: passengerId!,
            startLocation: { latitude: pickup.lat, longitude: pickup.lng },
            endLocation: { latitude: drop.lat, longitude: drop.lng },
          },
          key
        );
        bookingId = res.bookingId;
        if (bookingId == null) throw new Error('No bookingId in response');
      });
    }

    if (!failed && bookingId != null) {
      await runOne('poll', async () => {
        const d = await bookingApi.get(bookingId!);
        if (!d.bookingStatus) throw new Error('Missing bookingStatus');
      });
    } else if (!failed) {
      skipOne('poll', 'No booking id.');
    }

    if (!failed && driverOk && driverId != null && bookingId != null) {
      await runOne('accept', async () => {
        await bookingApi.update(bookingId!, { status: 'SCHEDULED', driverId });
        await sleep(350);
      });
    } else {
      skipOne('accept', !driverOk ? 'No driver session.' : !bookingId ? 'No booking.' : 'No driver id.');
    }

    if (!failed && driverOk && bookingId != null) {
      await runOne('advance', async () => {
        await bookingApi.setStatus(bookingId!, 'CAB_ARRIVED');
        await sleep(350);
        await bookingApi.setStatus(bookingId!, 'IN_RIDE');
        await sleep(350);
        await bookingApi.setStatus(bookingId!, 'COMPLETED');
        await sleep(350);
      });
    } else {
      skipOne('advance', !driverOk ? 'No driver session.' : !bookingId ? 'No booking.' : 'Cannot advance.');
    }

    if (!failed && driverOk && bookingId != null && driverId != null) {
      update('socket', { state: 'running' });
      try {
        await socketApi.publishLocation({
          bookingId,
          driverId,
          latitude: pickup.lat,
          longitude: pickup.lng,
          timestamp: Date.now(),
        });
        update('socket', { state: 'pass' });
      } catch (e) {
        update('socket', { state: 'skip', detail: errMessage(e) });
      }
    } else {
      skipOne('socket', 'Needs completed flow with driver (or socket service down).');
    }

    if (!failed && includePayment && bookingId != null) {
      await runOne('pay', async () => {
        const snap = await bookingApi.get(bookingId!);
        if ((snap.bookingStatus ?? '').toUpperCase() !== 'COMPLETED') {
          throw new Error(
            `Trip must be COMPLETED before payment in this suite (current: ${snap.bookingStatus ?? 'unknown'}). Sign in as driver and re-run.`
          );
        }
        const init = await paymentApi.initiate({ bookingId: bookingId!, amount: fareAmount });
        if (init.paymentId == null) throw new Error('No paymentId');
        await paymentApi.confirm({ paymentId: init.paymentId });
      });
    } else {
      skipOne(
        'pay',
        !includePayment ? 'Skipped — toggle off.' : failed ? 'Earlier failure.' : 'No booking id.'
      );
    }

    if (!failed && includeRating && bookingId != null && passengerId != null) {
      if (!driverOk) {
        skipOne('rate', 'Trip not completed (need driver session to finish lifecycle).');
      } else {
        await runOne('rate', async () => {
          await bookingApi.rateDriver(bookingId!, {
            actorId: passengerId!,
            score: 5,
            comment: 'QA automated run',
          });
        });
      }
    } else {
      skipOne('rate', !includeRating ? 'Skipped — toggle off.' : 'Missing booking or passenger.');
    }

    if (!failed && passengerId != null) {
      await runOne('list', async () => {
        const list = await bookingApi.listByPassenger(passengerId!);
        if (!Array.isArray(list)) throw new Error('Expected array');
      });
    } else {
      skipOne('list', failed ? 'Earlier failure.' : 'No passenger id.');
    }

    setSteps([...st]);
    setRunning(false);

    if (failed) {
      setBanner({ type: 'error', text: 'Stopped on first blocking failure — read the ✕ row.' });
    } else {
      setBanner({
        type: 'success',
        text: driverOk
          ? 'Suite finished. ⊘ rows are optional skips or non-blocking (e.g. socket).'
          : 'Passenger + booking checks passed. Sign in as driver and re-run for full lifecycle, payment, and rating.',
      });
    }
  };

  const displaySteps = steps.length ? steps : initialSteps;

  return (
    <div className="min-h-screen bg-black px-4 pb-16 pt-10 text-zinc-100">
      <div className="mx-auto max-w-2xl">
        <Link to="/" className="text-sm text-zinc-500 hover:text-white">
          ← Home
        </Link>
        <h1 className="mt-6 font-display text-3xl font-bold text-white">QA test hub</h1>
        <p className="mt-2 text-sm leading-relaxed text-zinc-400">
          One-click API regression: fare → book → poll → (driver) accept &amp; complete → socket ping → pay → rate → list. Sign in on{' '}
          <Link className="text-emerald-400 underline-offset-2 hover:underline" to="/passenger">
            /passenger
          </Link>{' '}
          and{' '}
          <Link className="text-emerald-400 underline-offset-2 hover:underline" to="/driver">
            /driver
          </Link>{' '}
          so both cookies are present.
        </p>

        {banner ? (
          <div className="mt-6">
            <Alert variant={banner.type}>{banner.text}</Alert>
          </div>
        ) : null}

        <div className="mt-8 space-y-3 rounded-2xl border border-white/[0.08] bg-zinc-900/50 p-5">
          <label className="flex cursor-pointer items-center gap-3 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={resetFirst}
              onChange={(e) => setResetFirst(e.target.checked)}
              className="rounded border-white/20 bg-black"
            />
            Cancel active passenger trips before creating a new one
          </label>
          <label className="flex cursor-pointer items-center gap-3 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={includePayment}
              onChange={(e) => setIncludePayment(e.target.checked)}
              className="rounded border-white/20 bg-black"
            />
            Run payment (initiate + confirm) after trip steps
          </label>
          <label className="flex cursor-pointer items-center gap-3 text-sm text-zinc-300">
            <input
              type="checkbox"
              checked={includeRating}
              onChange={(e) => setIncludeRating(e.target.checked)}
              className="rounded border-white/20 bg-black"
            />
            Submit driver rating (needs completed trip)
          </label>
        </div>

        <UiButton type="button" className="mt-6 w-full !py-4 text-base" loading={running} onClick={() => void runFullSuite()}>
          Run automated suite
        </UiButton>

        <ul className="mt-10 space-y-2">
          {displaySteps.map((s) => (
            <li
              key={s.id}
              className={`rounded-xl border px-4 py-3 text-sm transition ${
                s.state === 'fail'
                  ? 'border-red-500/30 bg-red-500/10 text-red-100'
                  : s.state === 'pass'
                    ? 'border-emerald-500/20 bg-emerald-500/5 text-zinc-200'
                    : s.state === 'skip'
                      ? 'border-zinc-600/40 bg-zinc-900/60 text-zinc-400'
                      : s.state === 'running'
                        ? 'border-amber-500/30 bg-amber-500/10 text-amber-100'
                        : 'border-white/[0.06] bg-zinc-900/40 text-zinc-500'
              }`}
            >
              <div className="flex gap-3">
                <span className="w-5 shrink-0 font-mono text-base leading-relaxed">{stepIcon(s.state)}</span>
                <div className="min-w-0 flex-1">
                  <p className="font-medium leading-snug">{s.label}</p>
                  {s.detail ? <p className="mt-1.5 break-words text-xs opacity-95">{s.detail}</p> : null}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
