import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { NavBack } from '@/components/NavBack';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput } from '@/components/UiField';
import { authApi, ApiError } from '@/lib/api';
import { setStoredPassengerIdentity } from '@/lib/storage';
import { passengerSignupSchema, signinSchema, type PassengerSignupValues, type SigninValues } from '@/lib/validation/schemas';

type Tab = 'signup' | 'signin' | 'session';

export function PassengerAuthPage() {
  const [tab, setTab] = useState<Tab>('signup');
  const [sessionMsg, setSessionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [sessionLoading, setSessionLoading] = useState(false);

  const signupForm = useForm<PassengerSignupValues>({
    resolver: zodResolver(passengerSignupSchema),
    mode: 'onBlur',
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      password: '',
      address: '',
    },
  });

  const signinForm = useForm<SigninValues>({
    resolver: zodResolver(signinSchema),
    mode: 'onBlur',
    defaultValues: { email: '', password: '' },
  });

  const [signupBanner, setSignupBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [signinBanner, setSigninBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    setSignupBanner(null);
    setSigninBanner(null);
  }, [tab]);

  async function onSignup(values: PassengerSignupValues) {
    setSignupBanner(null);
    try {
      const res = await authApi.signup({
        ...values,
        address: values.address?.trim() || undefined,
      });
      setStoredPassengerIdentity(res.id, res.email);
      setSignupBanner({
        type: 'success',
        text: `Account created. Passenger ID ${res.id} saved on this device for bookings.`,
      });
      signinForm.setValue('email', values.email.trim());
      setTab('signin');
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
      await authApi.signin({ email: values.email.trim(), password: values.password });
      setSigninBanner({
        type: 'success',
        text: 'Signed in. Open the rider app to book a trip.',
      });
    } catch (err) {
      setSigninBanner({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Sign in failed',
      });
    }
  }

  async function onValidateSession() {
    setSessionLoading(true);
    setSessionMsg(null);
    try {
      const res = await authApi.validate();
      setSessionMsg({
        type: 'success',
        text: res.success ? 'You are signed in.' : 'Something went wrong.',
      });
    } catch (err) {
      setSessionMsg({
        type: 'error',
        text: err instanceof ApiError ? err.message : 'Validation failed',
      });
    } finally {
      setSessionLoading(false);
    }
  }

  const tabs: { id: Tab; label: string }[] = [
    { id: 'signup', label: 'Create account' },
    { id: 'signin', label: 'Sign in' },
    { id: 'session', label: 'Check session' },
  ];

  return (
    <div className="mx-auto max-w-lg px-4 py-10 sm:py-14">
      <NavBack />
      <div className="mt-8">
        <Link to="/passenger/app" className="text-sm font-medium text-emerald-400 hover:underline">
          Continue to the rider app
        </Link>
      </div>
      <h1 className="mt-6 font-display text-3xl font-bold text-white">Rider account</h1>
      <p className="mt-2 text-sm text-zinc-500">Create an account or sign in to book rides with UrbanLift.</p>

      <div className="mt-8 flex gap-1 rounded-xl bg-night-900/80 p-1 ring-1 ring-white/10">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition ${
              tab === t.id ? 'bg-white/10 text-signal shadow-sm' : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="glass-panel shadow-card mt-8 p-6 sm:p-8">
        {tab === 'signup' && (
          <form onSubmit={signupForm.handleSubmit(onSignup)} className="space-y-4" noValidate>
            {signupBanner && (
              <Alert variant={signupBanner.type === 'success' ? 'success' : 'error'}>{signupBanner.text}</Alert>
            )}
            <div className="grid gap-4 sm:grid-cols-2">
              <UiField label="First name" id="pf" error={signupForm.formState.errors.firstName?.message}>
                <UiInput id="pf" autoComplete="given-name" {...signupForm.register('firstName')} />
              </UiField>
              <UiField label="Last name" id="pl" error={signupForm.formState.errors.lastName?.message}>
                <UiInput id="pl" autoComplete="family-name" {...signupForm.register('lastName')} />
              </UiField>
            </div>
            <UiField label="Email" id="pe" error={signupForm.formState.errors.email?.message}>
              <UiInput id="pe" type="email" autoComplete="email" {...signupForm.register('email')} />
            </UiField>
            <UiField
              label="Phone"
              id="pp"
              hint="Include country code if needed"
              error={signupForm.formState.errors.phoneNumber?.message}
            >
              <UiInput id="pp" autoComplete="tel" {...signupForm.register('phoneNumber')} />
            </UiField>
            <UiField label="Password" id="ppw" hint="Min 8 characters" error={signupForm.formState.errors.password?.message}>
              <UiInput id="ppw" type="password" autoComplete="new-password" {...signupForm.register('password')} />
            </UiField>
            <UiField label="Address (optional)" id="pa" error={signupForm.formState.errors.address?.message}>
              <UiInput id="pa" autoComplete="street-address" {...signupForm.register('address')} />

            </UiField>
            <UiButton type="submit" className="w-full" loading={signupForm.formState.isSubmitting}>
              Create passenger account
            </UiButton>
          </form>
        )}

        {tab === 'signin' && (
          <form onSubmit={signinForm.handleSubmit(onSignin)} className="space-y-4" noValidate>
            {signinBanner && (
              <Alert variant={signinBanner.type === 'success' ? 'success' : 'error'}>{signinBanner.text}</Alert>
            )}
            <UiField label="Email" id="se" error={signinForm.formState.errors.email?.message}>
              <UiInput id="se" type="email" autoComplete="email" {...signinForm.register('email')} />
            </UiField>
            <UiField label="Password" id="sp" error={signinForm.formState.errors.password?.message}>
              <UiInput id="sp" type="password" autoComplete="current-password" {...signinForm.register('password')} />
            </UiField>
            <UiButton type="submit" className="w-full" loading={signinForm.formState.isSubmitting}>
              Sign in
            </UiButton>
          </form>
        )}

        {tab === 'session' && (
          <div className="space-y-4">
            {sessionMsg && (
              <Alert variant={sessionMsg.type === 'success' ? 'success' : 'error'}>{sessionMsg.text}</Alert>
            )}
            <p className="text-sm text-zinc-400">Check whether you are still signed in on this device.</p>
            <UiButton type="button" variant="ghost" className="w-full" onClick={onValidateSession} loading={sessionLoading}>
              Verify session
            </UiButton>
          </div>
        )}
      </div>
    </div>
  );
}
