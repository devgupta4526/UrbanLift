import type { ReactNode } from 'react';

export function Alert({
  variant,
  children,
}: {
  variant: 'success' | 'error' | 'info';
  children: ReactNode;
}) {
  const styles = {
    success: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200',
    error: 'border-red-500/30 bg-red-500/10 text-red-200',
    info: 'border-aqua/30 bg-aqua/10 text-aqua',
  }[variant];
  return (
    <div
      role="status"
      className={`rounded-xl border px-4 py-3 text-sm ${styles}`}
    >
      {children}
    </div>
  );
}
