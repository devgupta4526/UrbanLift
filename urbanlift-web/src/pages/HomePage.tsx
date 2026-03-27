import { Link } from 'react-router-dom';

export function HomePage() {
  return (
    <div className="relative min-h-screen bg-black text-zinc-100">
      <div
        className="absolute inset-0 opacity-40"
        style={{
          background:
            'radial-gradient(ellipse 120% 80% at 50% -20%, rgba(16,185,129,0.15), transparent), radial-gradient(circle at 80% 60%, rgba(244,185,66,0.08), transparent)',
        }}
      />
      <div className="relative mx-auto flex min-h-screen max-w-lg flex-col px-6 pb-16 pt-14 sm:max-w-4xl sm:px-10 sm:pt-20">
        <header className="flex flex-1 flex-col justify-center sm:max-w-xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-400/90">UrbanLift</p>
          <h1 className="mt-4 font-display text-4xl font-extrabold leading-tight tracking-tight text-white sm:text-6xl">
            Go anywhere.
            <span className="block text-zinc-500 sm:mt-1">Get there your way.</span>
          </h1>
          <p className="mt-6 max-w-md text-lg leading-relaxed text-zinc-400">
            Request a ride in a few taps. Clear prices, live trip updates, and a driver app that stays in sync with your
            journey.
          </p>
          <div className="mt-10 flex flex-col gap-3 sm:flex-row sm:gap-4">
            <Link
              to="/passenger/app"
              className="inline-flex min-h-14 flex-1 items-center justify-center rounded-2xl bg-white px-6 text-base font-semibold text-black transition hover:bg-zinc-200"
            >
              Ride with UrbanLift
            </Link>
            <Link
              to="/driver/app"
              className="inline-flex min-h-14 flex-1 items-center justify-center rounded-2xl border border-white/20 bg-white/[0.04] px-6 text-base font-semibold text-white transition hover:bg-white/[0.08]"
            >
              Drive with us
            </Link>
          </div>
          <div className="mt-8 flex flex-wrap gap-6 text-sm">
            <Link to="/passenger" className="text-zinc-500 underline-offset-4 hover:text-white hover:underline">
              Rider sign in
            </Link>
            <Link to="/driver" className="text-zinc-500 underline-offset-4 hover:text-white hover:underline">
              Driver sign in
            </Link>
            <Link to="/qa" className="text-emerald-500/90 underline-offset-4 hover:text-emerald-300 hover:underline">
              Automated QA hub
            </Link>
          </div>
        </header>
        <footer className="mt-auto border-t border-white/[0.06] pt-8 text-center text-xs text-zinc-600">
          UrbanLift · Demo experience
        </footer>
      </div>
    </div>
  );
}
