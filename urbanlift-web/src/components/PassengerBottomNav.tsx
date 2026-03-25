const items = [
  { view: 'home' as const, label: 'Home', icon: '◎' },
  { view: 'activity' as const, label: 'Activity', icon: '☰' },
  { view: 'account' as const, label: 'Account', icon: '◉' },
];

export function PassengerBottomNav({
  active,
  onNavigate,
}: {
  active: 'home' | 'activity' | 'account';
  onNavigate: (view: 'home' | 'activity' | 'account') => void;
}) {
  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 border-t border-white/[0.08] bg-night-950/95 backdrop-blur-xl pb-[env(safe-area-inset-bottom,0)]"
      aria-label="Main navigation"
    >
      <div className="mx-auto flex max-w-lg items-stretch justify-around px-2 pt-1">
        {items.map(({ label, icon, view }) => {
          const isActive = active === view;
          return (
            <button
              key={view}
              type="button"
              onClick={() => onNavigate(view)}
              className={`flex flex-1 flex-col items-center gap-0.5 py-3 text-[11px] font-medium transition ${
                isActive ? 'text-white' : 'text-zinc-500 hover:text-zinc-300'
              }`}
            >
              <span className="text-lg leading-none opacity-90" aria-hidden>
                {icon}
              </span>
              {label}
            </button>
          );
        })}
      </div>
    </nav>
  );
}
