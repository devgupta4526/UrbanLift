type Phase = 1 | 2 | 3;

const labels: Record<Phase, string> = {
  1: 'Phase 1 — Authentication',
  2: 'Phase 2 — Rides & driver ops',
  3: 'Phase 3 — Full journey & payment',
};

export function PhaseBadge({ phase = 3 }: { phase?: Phase }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-xs font-medium text-zinc-400">
      <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-aqua" aria-hidden />
      {labels[phase]}
    </div>
  );
}
