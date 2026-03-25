import { Link } from 'react-router-dom';

export function NavBack({ to = '/', label = 'Home' }: { to?: string; label?: string }) {
  return (
    <Link
      to={to}
      className="group inline-flex items-center gap-2 text-sm text-zinc-500 transition hover:text-zinc-200"
    >
      <span
        className="inline-block transition-transform group-hover:-translate-x-0.5"
        aria-hidden
      >
        ←
      </span>
      {label}
    </Link>
  );
}
