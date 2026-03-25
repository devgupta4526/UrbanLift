import type { ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'ghost' | 'danger';

const variants: Record<Variant, string> = {
  primary:
    'bg-gradient-to-r from-signal to-signal-dim text-night-950 font-semibold shadow-lift hover:brightness-110 active:scale-[0.98]',
  ghost: 'border border-white/15 bg-white/[0.04] text-zinc-200 hover:bg-white/[0.08]',
  danger: 'border border-red-500/30 bg-red-500/10 text-red-200 hover:bg-red-500/20',
};

export function UiButton({
  children,
  variant = 'primary',
  className = '',
  loading,
  type = 'button',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode;
  variant?: Variant;
  loading?: boolean;
}) {
  return (
    <button
      type={type}
      disabled={loading || props.disabled}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 py-2.5 text-sm transition disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {loading && (
        <span
          className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
          aria-hidden
        />
      )}
      {children}
    </button>
  );
}
