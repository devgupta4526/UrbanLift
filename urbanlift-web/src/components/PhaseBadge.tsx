export function PhaseBadge() {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-xs font-medium text-zinc-400">
      <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-aqua" aria-hidden />
      Phase 1 — Authentication
    </div>
  );
}
