import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { NavBack } from '@/components/NavBack';
import { PhaseBadge } from '@/components/PhaseBadge';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput, UiSelect } from '@/components/UiField';
import { driverApi, ApiError } from '@/lib/api';
import {
  driverFullSignupSchema,
  signinSchema,
  type SigninValues,
} from '@/lib/validation/schemas';
import { z } from 'zod';

type CarType = 'SEDAN' | 'HATCHBACK' | 'SUV' | 'COMPACT_SUV' | 'XL';

export type DriverSignupFormValues = z.infer<typeof driverFullSignupSchema>;

type MainTab = 'signup' | 'signin' | 'session';
type SignupStep = 1 | 2 | 3;

const CAR_TYPES: CarType[] = ['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL'];

const step1Fields: (keyof DriverSignupFormValues)[] = [
  'firstName',
  'lastName',
  'email',
  'phoneNumber',
  'password',
  'address',
];
const step2Fields: (keyof DriverSignupFormValues)[] = ['licenseNumber', 'aadharNumber', 'activeCity'];

export function DriverAuthPage() {
  const [main, setMain] = useState<MainTab>('signup');
  const [step, setStep] = useState<SignupStep>(1);
  const [signupBanner, setSignupBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [sessionMsg, setSessionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [sessionLoading, setSessionLoading] = useState(false);

  const signupForm = useForm<DriverSignupFormValues>({
    resolver: zodResolver(driverFullSignupSchema),
    mode: 'onBlur',
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      password: '',
      address: '',
      licenseNumber: '',
      aadharNumber: '',
      activeCity: '',
      car: {
        plateNumber: '',
        colorName: '',
        brand: '',
        model: '',
        carType: 'SEDAN',
      },
    },
  });

  const signinForm = useForm<SigninValues>({
    resolver: zodResolver(signinSchema),
    mode: 'onBlur',
    defaultValues: { email: '', password: '' },
  });

  const [signinBanner, setSigninBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null
  );

  useEffect(() => {
    setSignupBanner(null);
    setSigninBanner(null);
  }, [main]);

  async function goNext() {
    if (step === 1) {
      const ok = await signupForm.trigger(step1Fields);
      if (ok) setStep(2);
      return;
    }
    if (step === 2) {
      const ok = await signupForm.trigger(step2Fields);
      if (ok) setStep(3);
    }
  }

  function goBack() {
    if (step > 1) setStep((s) => (s - 1) as SignupStep);
  }

  async function onSignupSubmit(values: DriverSignupFormValues) {
    setSignupBanner(null);
    try {
      const res = await driverApi.signup({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        phoneNumber: values.phoneNumber.trim(),
        password: values.password,
        address: values.address?.trim() || undefined,
        licenseNumber: values.licenseNumber.trim(),
        aadharNumber: values.aadharNumber.trim(),
        activeCity: values.activeCity?.trim() || undefined,
        car: {
          plateNumber: values.car.plateNumber.trim(),
          colorName: values.car.colorName.trim(),
          brand: values.car.brand.trim(),
          model: values.car.model.trim(),
          carType: values.car.carType,
        },
      });
      setSignupBanner({
        type: 'success',
        text: `Registered — id ${res.id}, status ${res.driverApprovalStatus ?? 'PENDING'}. Approve in DB before sign-in.`,
      });
      signupForm.reset();
      setStep(1);
      setMain('signin');
      signinForm.setValue('email', values.email.trim());
    } catch (err) {
      setSignupBanner({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Signup failed',
      });
    }
  }

  async function onSignin(values: SigninValues) {
    setSigninBanner(null);
    try {
      const res = await driverApi.signin({ email: values.email.trim(), password: values.password });
      if (res.success) {
        setSigninBanner({
          type: 'success',
          text: 'Signed in. DRIVER_JWT set. Open the driver console for trips & availability.',
        });
      } else {
        setSigninBanner({ type: 'error', text: 'Sign in rejected (credentials or not APPROVED).' });
      }
    } catch (err) {
      setSigninBanner({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Sign in failed',
      });
    }
  }

  async function onValidate() {
    setSessionLoading(true);
    setSessionMsg(null);
    try {
      const res = await driverApi.validate();
      setSessionMsg({
        type: 'success',
        text: res.success ? 'Driver session valid.' : 'Unexpected',
      });
    } catch (err) {
      setSessionMsg({ type: 'error', text: err instanceof ApiError ? err.message : 'Failed' });
    } finally {
      setSessionLoading(false);
    }
  }

  const err = signupForm.formState.errors;

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:py-14">
      <NavBack />
      <div className="mt-8 flex flex-wrap items-center gap-4">
        <PhaseBadge phase={1} />
        <Link to="/driver/app" className="text-sm font-medium text-aqua hover:underline">
          Driver console →
        </Link>
      </div>
      <h1 className="mt-4 font-display text-3xl font-bold text-zinc-50">Driver access</h1>
      <p className="mt-2 text-sm text-zinc-500">
        API <code className="text-zinc-400">{driverApi.base}</code>
      </p>

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
            onClick={() => setMain(id)}
            className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition ${
              main === id ? 'bg-white/10 text-aqua shadow-sm' : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="glass-panel shadow-card mt-8 p-6 sm:p-8">
        {main === 'signup' && (
          <>
            <div className="mb-6 flex gap-2">
              {([1, 2, 3] as const).map((s) => (
                <div key={s} className={`h-1 flex-1 rounded-full ${s <= step ? 'bg-aqua' : 'bg-white/10'}`} />
              ))}
            </div>
            <p className="mb-4 text-xs font-mono uppercase tracking-wider text-zinc-500">Step {step} of 3</p>

            {signupBanner && (
              <div className="mb-6">
                <Alert variant={signupBanner.type === 'success' ? 'success' : 'error'}>{signupBanner.text}</Alert>
              </div>
            )}

            <form onSubmit={signupForm.handleSubmit(onSignupSubmit)} className="space-y-4" noValidate>
              {step === 1 && (
                <>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <UiField label="First name" id="df" error={err.firstName?.message}>
                      <UiInput id="df" {...signupForm.register('firstName')} />
                    </UiField>
                    <UiField label="Last name" id="dl" error={err.lastName?.message}>
                      <UiInput id="dl" {...signupForm.register('lastName')} />
                    </UiField>
                  </div>
                  <UiField label="Email" id="de" error={err.email?.message}>
                    <UiInput id="de" type="email" {...signupForm.register('email')} />
                  </UiField>
                  <UiField label="Phone" id="dp" error={err.phoneNumber?.message}>
                    <UiInput id="dp" {...signupForm.register('phoneNumber')} />
                  </UiField>
                  <UiField label="Password" id="dpw" error={err.password?.message}>
                    <UiInput id="dpw" type="password" {...signupForm.register('password')} />
                  </UiField>
                  <UiField label="Address (optional)" id="da" error={err.address?.message}>
                    <UiInput id="da" {...signupForm.register('address')} />
                  </UiField>
                </>
              )}

              {step === 2 && (
                <>
                  <UiField label="License number" id="dlic" error={err.licenseNumber?.message}>
                    <UiInput id="dlic" {...signupForm.register('licenseNumber')} />
                  </UiField>
                  <UiField label="Aadhar number" id="daad" error={err.aadharNumber?.message}>
                    <UiInput id="daad" {...signupForm.register('aadharNumber')} />
                  </UiField>
                  <UiField label="Active city (optional)" id="dcity" error={err.activeCity?.message}>
                    <UiInput id="dcity" {...signupForm.register('activeCity')} />
                  </UiField>
                </>
              )}

              {step === 3 && (
                <>
                  <UiField label="Plate number" id="dplate" error={err.car?.plateNumber?.message}>
                    <UiInput id="dplate" {...signupForm.register('car.plateNumber')} />
                  </UiField>
                  <UiField label="Color" id="dcolor" error={err.car?.colorName?.message}>
                    <UiInput id="dcolor" {...signupForm.register('car.colorName')} />
                  </UiField>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <UiField label="Brand" id="dbrand" error={err.car?.brand?.message}>
                      <UiInput id="dbrand" {...signupForm.register('car.brand')} />
                    </UiField>
                    <UiField label="Model" id="dmodel" error={err.car?.model?.message}>
                      <UiInput id="dmodel" {...signupForm.register('car.model')} />
                    </UiField>
                  </div>
                  <UiField label="Car type" id="dtype" error={err.car?.carType?.message}>
                    <UiSelect id="dtype" {...signupForm.register('car.carType')}>
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
                  <UiButton type="button" variant="ghost" onClick={goBack}>
                    Back
                  </UiButton>
                )}
                {step < 3 ? (
                  <UiButton type="button" className="sm:ml-auto" onClick={() => void goNext()}>
                    Continue
                  </UiButton>
                ) : (
                  <UiButton type="submit" className="w-full sm:w-auto sm:ml-auto" loading={signupForm.formState.isSubmitting}>
                    Submit registration
                  </UiButton>
                )}
              </div>
            </form>
          </>
        )}

        {main === 'signin' && (
          <form onSubmit={signinForm.handleSubmit(onSignin)} className="space-y-4" noValidate>
            <Alert variant="info">
              Drivers must be <strong>APPROVED</strong> in the database before sign-in succeeds.
            </Alert>
            {signinBanner && (
              <Alert variant={signinBanner.type === 'success' ? 'success' : 'error'}>{signinBanner.text}</Alert>
            )}
            <UiField label="Email" id="dse" error={signinForm.formState.errors.email?.message}>
              <UiInput id="dse" type="email" {...signinForm.register('email')} />
            </UiField>
            <UiField label="Password" id="dsp" error={signinForm.formState.errors.password?.message}>
              <UiInput id="dsp" type="password" {...signinForm.register('password')} />
            </UiField>
            <UiButton type="submit" className="w-full" loading={signinForm.formState.isSubmitting}>
              Sign in as driver
            </UiButton>
          </form>
        )}

        {main === 'session' && (
          <div className="space-y-4">
            {sessionMsg && (
              <Alert variant={sessionMsg.type === 'success' ? 'success' : 'error'}>{sessionMsg.text}</Alert>
            )}
            <p className="text-sm text-zinc-400">
              Validates <code className="text-signal">DRIVER_JWT</code> via cookie on this origin.
            </p>
            <UiButton type="button" variant="ghost" className="w-full" onClick={() => void onValidate()} loading={sessionLoading}>
              Validate driver session
            </UiButton>
          </div>
        )}
      </div>
    </div>
  );
}
