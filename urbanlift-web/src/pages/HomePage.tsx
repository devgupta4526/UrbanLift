import { Link } from 'react-router-dom';
import { PhaseBadge } from '@/components/PhaseBadge';

export function HomePage() {
  return (
    <div className="relative mx-auto flex min-h-screen max-w-6xl flex-col px-4 pb-16 pt-10 sm:px-6 lg:px-8 lg:pt-16">
      <header className="mb-16 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <PhaseBadge phase={2} />
          <h1 className="mt-6 font-display text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl">
            <span className="text-gradient-signal">UrbanLift</span>
            <span className="text-zinc-100"> moves cities.</span>
          </h1>
          <p className="mt-4 max-w-xl text-lg text-zinc-400">
            <strong className="text-zinc-200">Phase 1</strong> — passenger & driver authentication with full form
            validation. <strong className="text-zinc-200">Phase 2</strong> — fare quotes, bookings, trip list, and a
            driver console (GPS, availability, assigned rides). All calls use the Vite dev proxy and httpOnly cookies
            where the APIs expect them.
          </p>
        </div>
        <div className="hidden shrink-0 lg:block">
          <div className="glass-panel shadow-card h-40 w-40 rotate-3 rounded-3xl border-signal/20 bg-gradient-to-br from-signal/10 to-transparent p-1">
            <div className="flex h-full w-full items-center justify-center rounded-[1.15rem] bg-night-900/90 font-mono text-xs text-zinc-500">
              Production UX
            </div>
          </div>
        </div>
      </header>

      <div className="mb-10 grid gap-4 md:grid-cols-2">
        <Link
          to="/passenger/app"
          className="glass-panel shadow-card group rounded-3xl border-signal/20 p-6 transition hover:border-signal/40 hover:shadow-lift"
        >
          <p className="font-mono text-xs uppercase tracking-widest text-signal">Passenger</p>
          <h2 className="mt-2 font-display text-xl font-bold text-zinc-50">Ride hub</h2>
          <p className="mt-2 text-sm text-zinc-500">
            Saved passenger ID, fare estimate, book ride, my trips — validated forms (Zod + RHF).
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-signal">
            Open app →
          </span>
        </Link>
        <Link
          to="/driver/app"
          className="glass-panel shadow-card group rounded-3xl border-aqua/20 p-6 transition hover:border-aqua/40 hover:shadow-[0_0_40px_-10px_rgba(45,212,191,0.25)]"
        >
          <p className="font-mono text-xs uppercase tracking-widest text-aqua">Driver</p>
          <h2 className="mt-2 font-display text-xl font-bold text-zinc-50">Operations</h2>
          <p className="mt-2 text-sm text-zinc-500">
            Profile, location ping, availability, assigned bookings — requires driver sign-in.
          </p>
          <span className="mt-4 inline-flex text-sm font-semibold text-aqua">
            Open console →
          </span>
        </Link>
      </div>

      <div className="grid flex-1 gap-6 md:grid-cols-2">
        <Link
          to="/passenger"
          className="group glass-panel shadow-card relative overflow-hidden rounded-3xl p-8 transition hover:border-signal/30 hover:shadow-lift"
        >
          <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-signal/5 blur-2xl transition group-hover:bg-signal/10" />
          <p className="font-mono text-xs uppercase tracking-widest text-signal">Passengers</p>
          <h2 className="mt-3 font-display text-2xl font-bold text-zinc-50">Account & session</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Sign up, sign in, validate <code className="text-aqua">JWT_TOKEN</code> — with live field validation.
          </p>
          <span className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-signal">
            Passenger auth
            <span className="transition group-hover:translate-x-1">→</span>
          </span>
        </Link>

        <Link
          to="/driver"
          className="group glass-panel shadow-card relative overflow-hidden rounded-3xl p-8 transition hover:border-aqua/30"
        >
          <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-aqua/5 blur-2xl transition group-hover:bg-aqua/10" />
          <p className="font-mono text-xs uppercase tracking-widest text-aqua">Drivers</p>
          <h2 className="mt-3 font-display text-2xl font-bold text-zinc-50">Onboarding & login</h2>
          <p className="mt-2 text-sm text-zinc-400">
            Step-by-step registration with Zod, <code className="text-signal">DRIVER_JWT</code> validation.
          </p>
          <span className="mt-8 inline-flex items-center gap-2 text-sm font-semibold text-aqua">
            Driver auth
            <span className="transition group-hover:translate-x-1">→</span>
          </span>
        </Link>
      </div>

      <footer className="mt-auto border-t border-white/5 pt-10 text-center text-xs text-zinc-600">
        Auth :7475 · Driver :8081 · Booking :8001 · Payment :8082 · UI :5173
      </footer>
    </div>
  );
}
