import { Link } from 'react-router-dom';
import { PhaseBadge } from '@/components/PhaseBadge';

export function HomePage() {
  return (
    <div className="relative mx-auto flex min-h-screen max-w-6xl flex-col px-4 pb-16 pt-10 sm:px-6 lg:px-8 lg:pt-16">
      <header className="mb-16 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <PhaseBadge />
          <h1 className="mt-6 font-display text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl">
            <span className="text-gradient-signal">UrbanLift</span>
            <span className="text-zinc-100"> moves cities.</span>
          </h1>
          <p className="mt-4 max-w-xl text-lg text-zinc-400">
            Phase 1 focuses on rock-solid <strong className="text-zinc-300">passenger</strong> and{' '}
            <strong className="text-zinc-300">driver</strong> authentication — cookies, validation,
            and clear errors so you can debug each service in isolation.
          </p>
        </div>
        <div className="hidden shrink-0 lg:block">
          <div className="glass-panel shadow-card h-40 w-40 rotate-3 rounded-3xl border-signal/20 bg-gradient-to-br from-signal/10 to-transparent p-1">
            <div className="flex h-full w-full items-center justify-center rounded-[1.15rem] bg-night-900/90 font-mono text-xs text-zinc-500">
              API-ready
            </div>
          </div>
        </div>
      </header>

      <div className="grid flex-1 gap-6 md:grid-cols-2">
        <Link
          to="/passenger"
          className="group glass-panel shadow-card relative overflow-hidden rounded-3xl p-8 transition hover:border-signal/30 hover:shadow-lift"
        >
          <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-signal/5 blur-2xl transition group-hover:bg-signal/10" />
          <p className="font-mono text-xs uppercase tracking-widest text-signal">Passengers</p>
          <h2 className="mt-3 font-display text-2xl font-bold text-zinc-50">Ride with confidence</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Sign up, sign in, and verify your <code className="text-aqua">JWT_TOKEN</code> session
            against the Auth Service.
          </p>
          <span className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-signal">
            Open passenger auth
            <span className="transition group-hover:translate-x-1">→</span>
          </span>
        </Link>

        <Link
          to="/driver"
          className="group glass-panel shadow-card relative overflow-hidden rounded-3xl p-8 transition hover:border-aqua/30 hover:shadow-[0_0_40px_-10px_rgba(45,212,191,0.25)]"
        >
          <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-aqua/5 blur-2xl transition group-hover:bg-aqua/10" />
          <p className="font-mono text-xs uppercase tracking-widest text-aqua">Drivers</p>
          <h2 className="mt-3 font-display text-2xl font-bold text-zinc-50">Earn on your schedule</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Multi-step onboarding, vehicle details, and{' '}
            <code className="text-signal">DRIVER_JWT</code> validation (approval required to sign in).
          </p>
          <span className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-aqua">
            Open driver auth
            <span className="transition group-hover:translate-x-1">→</span>
          </span>
        </Link>
      </div>

      <footer className="mt-auto border-t border-white/5 pt-10 text-center text-xs text-zinc-600">
        Auth:7475 · Driver:8081 · Dev UI :5173 — use Vite proxy or CORS (both configured).
      </footer>
    </div>
  );
}
