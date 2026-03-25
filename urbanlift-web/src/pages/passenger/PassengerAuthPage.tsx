import { useState, type FormEvent } from 'react';
import { NavBack } from '@/components/NavBack';
import { PhaseBadge } from '@/components/PhaseBadge';
import { Alert } from '@/components/Alert';
import { UiButton } from '@/components/UiButton';
import { UiField, UiInput } from '@/components/UiField';
import { authApi, ApiError } from '@/lib/api';

type Tab = 'signup' | 'signin' | 'session';

export function PassengerAuthPage() {
  const [tab, setTab] = useState<Tab>('signup');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(
    null
  );

  const [signup, setSignup] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    address: '',
  });
  const [signin, setSignin] = useState({ email: '', password: '' });

  async function onSignup(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    try {
      const res = await authApi.signup({
        firstName: signup.firstName.trim(),
        lastName: signup.lastName.trim(),
        email: signup.email.trim(),
        phoneNumber: signup.phoneNumber.trim(),
        password: signup.password,
        address: signup.address.trim() || undefined,
      });
      setMessage({
        type: 'success',
        text: `Account created — id ${res.id}, email ${res.email}. You can sign in now.`,
      });
      setTab('signin');
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
      await authApi.signin({
        email: signin.email.trim(),
        password: signin.password,
      });
      setMessage({
        type: 'success',
        text: 'Signed in — httpOnly JWT_TOKEN cookie set for this origin. Try “Check session”.',
      });
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
      const res = await authApi.validate();
      setMessage({
        type: 'success',
        text: res.success ? 'Session valid — JWT is accepted by Auth Service.' : 'Unexpected response',
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

  const tabs: { id: Tab; label: string }[] = [
    { id: 'signup', label: 'Create account' },
    { id: 'signin', label: 'Sign in' },
    { id: 'session', label: 'Check session' },
  ];

  return (
    <div className="mx-auto max-w-lg px-4 py-10 sm:py-14">
      <NavBack />
      <div className="mt-8">
        <PhaseBadge />
        <h1 className="mt-4 font-display text-3xl font-bold text-zinc-50">Passenger access</h1>
        <p className="mt-2 text-sm text-zinc-500">
          Backend: <code className="text-zinc-400">{authApi.base}</code>
          <span className="text-zinc-600"> → </span>
          <code className="text-zinc-400">/api/v1/auth/…</code>
        </p>
      </div>

      <div className="mt-8 flex gap-1 rounded-xl bg-night-900/80 p-1 ring-1 ring-white/10">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => {
              setTab(t.id);
              setMessage(null);
            }}
            className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition ${
              tab === t.id
                ? 'bg-white/10 text-signal shadow-sm'
                : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            {t.label}
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

        {tab === 'signup' && (
          <form onSubmit={onSignup} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <UiField label="First name" id="pf">
                <UiInput
                  id="pf"
                  required
                  value={signup.firstName}
                  onChange={(e) => setSignup({ ...signup, firstName: e.target.value })}
                  autoComplete="given-name"
                />
              </UiField>
              <UiField label="Last name" id="pl">
                <UiInput
                  id="pl"
                  required
                  value={signup.lastName}
                  onChange={(e) => setSignup({ ...signup, lastName: e.target.value })}
                  autoComplete="family-name"
                />
              </UiField>
            </div>
            <UiField label="Email" id="pe">
              <UiInput
                id="pe"
                type="email"
                required
                value={signup.email}
                onChange={(e) => setSignup({ ...signup, email: e.target.value })}
                autoComplete="email"
              />
            </UiField>
            <UiField label="Phone" id="pp" hint="Include country code if you use one">
              <UiInput
                id="pp"
                required
                value={signup.phoneNumber}
                onChange={(e) => setSignup({ ...signup, phoneNumber: e.target.value })}
                autoComplete="tel"
              />
            </UiField>
            <UiField label="Password" id="ppw" hint="Minimum 8 characters">
              <UiInput
                id="ppw"
                type="password"
                required
                minLength={8}
                value={signup.password}
                onChange={(e) => setSignup({ ...signup, password: e.target.value })}
                autoComplete="new-password"
              />
            </UiField>
            <UiField label="Address (optional)" id="pa">
              <UiInput
                id="pa"
                value={signup.address}
                onChange={(e) => setSignup({ ...signup, address: e.target.value })}
                autoComplete="street-address"
              />
            </UiField>
            <UiButton type="submit" className="w-full" loading={loading}>
              Create passenger account
            </UiButton>
          </form>
        )}

        {tab === 'signin' && (
          <form onSubmit={onSignin} className="space-y-4">
            <UiField label="Email" id="se">
              <UiInput
                id="se"
                type="email"
                required
                value={signin.email}
                onChange={(e) => setSignin({ ...signin, email: e.target.value })}
                autoComplete="email"
              />
            </UiField>
            <UiField label="Password" id="sp">
              <UiInput
                id="sp"
                type="password"
                required
                value={signin.password}
                onChange={(e) => setSignin({ ...signin, password: e.target.value })}
                autoComplete="current-password"
              />
            </UiField>
            <UiButton type="submit" className="w-full" loading={loading}>
              Sign in
            </UiButton>
          </form>
        )}

        {tab === 'session' && (
          <div className="space-y-4">
            <p className="text-sm text-zinc-400">
              Calls <code className="text-aqua">GET /api/v1/auth/validate</code> with cookies. Sign in
              first on this same origin (Vite proxy keeps cookies on <code>localhost:5173</code>).
            </p>
            <UiButton type="button" variant="ghost" className="w-full" onClick={onValidate} loading={loading}>
              Validate JWT cookie
            </UiButton>
          </div>
        )}
      </div>
    </div>
  );
}
