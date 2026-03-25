import { useState, type FormEvent } from 'react';
import { NavBack } from '@/components/NavBack';
import { PhaseBadge } from '@/components/PhaseBadge';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput, UiSelect } from '@/components/UiField';
import { driverApi, ApiError } from '@/lib/api';

const CAR_TYPES = ['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL'] as const;

type MainTab = 'signup' | 'signin' | 'session';
type SignupStep = 1 | 2 | 3;

const initialSignup = {
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  password: '',
  address: '',
  licenseNumber: '',
  aadharNumber: '',
  activeCity: '',
  plateNumber: '',
  colorName: '',
  brand: '',
  model: '',
  carType: 'SEDAN' as (typeof CAR_TYPES)[number],
};

export function DriverAuthPage() {
  const [main, setMain] = useState<MainTab>('signup');
  const [step, setStep] = useState<SignupStep>(1);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(
    null
  );
  const [signup, setSignup] = useState(initialSignup);
  const [signin, setSignin] = useState({ email: '', password: '' });

  function buildPayload() {
    return {
      firstName: signup.firstName.trim(),
      lastName: signup.lastName.trim(),
      email: signup.email.trim(),
      phoneNumber: signup.phoneNumber.trim(),
      password: signup.password,
      address: signup.address.trim() || undefined,
      licenseNumber: signup.licenseNumber.trim(),
      aadharNumber: signup.aadharNumber.trim(),
      activeCity: signup.activeCity.trim() || undefined,
      car: {
        plateNumber: signup.plateNumber.trim(),
        colorName: signup.colorName.trim(),
        brand: signup.brand.trim(),
        model: signup.model.trim(),
        carType: signup.carType,
      },
    };
  }

  async function onSignup(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    try {
      const res = await driverApi.signup(buildPayload());
      setMessage({
        type: 'success',
        text: `Driver registered — id ${res.id}. Status: ${res.driverApprovalStatus ?? 'PENDING'}. Approve in DB before sign-in.`,
      });
      setSignup(initialSignup);
      setStep(1);
      setMain('signin');
      setSignin((s) => ({ ...s, email: signup.email.trim() }));
    } catch (err) {
      setMessage({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Signup failed',
      });
    } finally {
      setLoading(false);
    }
  }

  async function onSignin(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    try {
      const res = await driverApi.signin({
        email: signin.email.trim(),
        password: signin.password,
      });
      if (res.success) {
        setMessage({
          type: 'success',
          text: 'Signed in — DRIVER_JWT cookie set. Use “Check session” to verify.',
        });
      } else {
        setMessage({ type: 'error', text: 'Sign in rejected (wrong credentials or not APPROVED).' });
      }
    } catch (err) {
      setMessage({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Sign in failed',
      });
    } finally {
      setLoading(false);
    }
  }

  async function onValidate() {
    setLoading(true);
    setMessage(null);
    try {
      const res = await driverApi.validate();
      setMessage({
        type: 'success',
        text: res.success ? 'Driver session valid.' : 'Unexpected response',
      });
    } catch (err) {
      setMessage({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Validation failed',
      });
    } finally {
      setLoading(false);
    }
  }

  const stepValid =
    step === 1
      ? signup.firstName && signup.lastName && signup.email && signup.phoneNumber && signup.password.length >= 8
      : step === 2
        ? signup.licenseNumber && signup.aadharNumber
        : signup.plateNumber && signup.colorName && signup.brand && signup.model && signup.carType;

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:py-14">
      <NavBack />
      <div className="mt-8">
        <PhaseBadge />
        <h1 className="mt-4 font-display text-3xl font-bold text-zinc-50">Driver access</h1>
        <p className="mt-2 text-sm text-zinc-500">
          Backend: <code className="text-zinc-400">{driverApi.base}</code>
        </p>
      </div>

      <div className="mt-8 flex gap-1 rounded-xl bg-night-900/80 p-1 ring-1 ring-white/10">
        {(
          [
            ['signup', 'Register'],
            ['signin', 'Sign in'],
            ['session', 'Check session'],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            onClick={() => {
              setMain(id);
              setMessage(null);
            }}
            className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition ${
              main === id ? 'bg-white/10 text-aqua shadow-sm' : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="glass-panel shadow-card mt-8 p-6 sm:p-8">
        {message && (
          <div className="mb-6">
            <Alert variant={message.type === 'success' ? 'success' : message.type === 'info' ? 'info' : 'error'}>
              {message.text}
            </Alert>
          </div>
        )}

        {main === 'signup' && (
          <>
            <div className="mb-6 flex gap-2">
              {([1, 2, 3] as const).map((s) => (
                <div
                  key={s}
                  className={`h-1 flex-1 rounded-full transition ${
                    s <= step ? 'bg-aqua' : 'bg-white/10'
                  }`}
                />
              ))}
            </div>
            <p className="mb-4 text-xs font-mono uppercase tracking-wider text-zinc-500">
              Step {step} of 3
            </p>

            <form onSubmit={onSignup} className="space-y-4">
              {step === 1 && (
                <>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <UiField label="First name" id="df">
                      <UiInput
                        id="df"
                        required
                        value={signup.firstName}
                        onChange={(e) => setSignup({ ...signup, firstName: e.target.value })}
                      />
                    </UiField>
                    <UiField label="Last name" id="dl">
                      <UiInput
                        id="dl"
                        required
                        value={signup.lastName}
                        onChange={(e) => setSignup({ ...signup, lastName: e.target.value })}
                      />
                    </UiField>
                  </div>
                  <UiField label="Email" id="de">
                    <UiInput
                      id="de"
                      type="email"
                      required
                      value={signup.email}
                      onChange={(e) => setSignup({ ...signup, email: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Phone" id="dp">
                    <UiInput
                      id="dp"
                      required
                      value={signup.phoneNumber}
                      onChange={(e) => setSignup({ ...signup, phoneNumber: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Password" id="dpw" hint="Min 8 characters">
                    <UiInput
                      id="dpw"
                      type="password"
                      required
                      minLength={8}
                      value={signup.password}
                      onChange={(e) => setSignup({ ...signup, password: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Address (optional)" id="da">
                    <UiInput
                      id="da"
                      value={signup.address}
                      onChange={(e) => setSignup({ ...signup, address: e.target.value })}
                    />
                  </UiField>
                </>
              )}

              {step === 2 && (
                <>
                  <UiField label="License number" id="dlic">
                    <UiInput
                      id="dlic"
                      required
                      value={signup.licenseNumber}
                      onChange={(e) => setSignup({ ...signup, licenseNumber: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Aadhar number" id="daad">
                    <UiInput
                      id="daad"
                      required
                      value={signup.aadharNumber}
                      onChange={(e) => setSignup({ ...signup, aadharNumber: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Active city (optional)" id="dcity">
                    <UiInput
                      id="dcity"
                      value={signup.activeCity}
                      onChange={(e) => setSignup({ ...signup, activeCity: e.target.value })}
                    />
                  </UiField>
                </>
              )}

              {step === 3 && (
                <>
                  <UiField label="Plate number" id="dplate">
                    <UiInput
                      id="dplate"
                      required
                      value={signup.plateNumber}
                      onChange={(e) => setSignup({ ...signup, plateNumber: e.target.value })}
                    />
                  </UiField>
                  <UiField label="Color" id="dcolor">
                    <UiInput
                      id="dcolor"
                      required
                      value={signup.colorName}
                      onChange={(e) => setSignup({ ...signup, colorName: e.target.value })}
                    />
                  </UiField>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <UiField label="Brand" id="dbrand">
                      <UiInput
                        id="dbrand"
                        required
                        value={signup.brand}
                        onChange={(e) => setSignup({ ...signup, brand: e.target.value })}
                      />
                    </UiField>
                    <UiField label="Model" id="dmodel">
                      <UiInput
                        id="dmodel"
                        required
                        value={signup.model}
                        onChange={(e) => setSignup({ ...signup, model: e.target.value })}
                      />
                    </UiField>
                  </div>
                  <UiField label="Car type" id="dtype">
                    <UiSelect
                      id="dtype"
                      value={signup.carType}
                      onChange={(e) =>
                        setSignup({ ...signup, carType: e.target.value as (typeof CAR_TYPES)[number] })
                      }
                    >
                      {CAR_TYPES.map((c) => (
                        <option key={c} value={c}>
                          {c.replace('_', ' ')}
                        </option>
                      ))}
                    </UiSelect>
                  </UiField>
                </>
              )}

              <div className="flex flex-col gap-3 pt-2 sm:flex-row sm:justify-between">
                {step > 1 && (
                  <UiButton type="button" variant="ghost" onClick={() => setStep((s) => (s - 1) as SignupStep)}>
                    Back
                  </UiButton>
                )}
                {step < 3 ? (
                  <UiButton
                    type="button"
                    className="sm:ml-auto"
                    disabled={!stepValid}
                    onClick={() => stepValid && setStep((s) => (s + 1) as SignupStep)}
                  >
                    Continue
                  </UiButton>
                ) : (
                  <UiButton type="submit" className="w-full sm:ml-auto sm:w-auto" loading={loading} disabled={!stepValid}>
                    Submit registration
                  </UiButton>
                )}
              </div>
            </form>
          </>
        )}

        {main === 'signin' && (
          <form onSubmit={onSignin} className="space-y-4">
            <Alert variant="info">
              Drivers must be <strong>APPROVED</strong> in the database before sign-in succeeds.
            </Alert>
            <UiField label="Email" id="dse">
              <UiInput
                id="dse"
                type="email"
                required
                value={signin.email}
                onChange={(e) => setSignin({ ...signin, email: e.target.value })}
              />
            </UiField>
            <UiField label="Password" id="dsp">
              <UiInput
                id="dsp"
                type="password"
                required
                value={signin.password}
                onChange={(e) => setSignin({ ...signin, password: e.target.value })}
              />
            </UiField>
            <UiButton type="submit" className="w-full" loading={loading}>
              Sign in as driver
            </UiButton>
          </form>
        )}

        {main === 'session' && (
          <div className="space-y-4">
            <p className="text-sm text-zinc-400">
              GET <code className="text-signal">/api/v1/driver/auth/validate</code> — sends{' '}
              <code className="text-zinc-300">DRIVER_JWT</code> cookie when you are on the same dev origin.
            </p>
            <UiButton type="button" variant="ghost" className="w-full" onClick={onValidate} loading={loading}>
              Validate driver session
            </UiButton>
          </div>
        )}
      </div>
    </div>
  );
}
