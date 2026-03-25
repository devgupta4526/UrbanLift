import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';

export function UiField({
  label,
  hint,
  error,
  id,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  id: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-zinc-300">
        {label}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-zinc-500">{hint}</p>}
      {error && <p className="text-xs text-red-400">{error}</p>}
    </div>
  );
}

export function UiInput({
  id,
  error,
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { id: string; error?: string }) {
  return (
    <input
      id={id}
      className={`w-full rounded-xl border bg-night-900/80 px-4 py-3 text-sm text-zinc-100 placeholder:text-zinc-600 transition focus:border-signal/50 ${
        error ? 'border-red-500/40' : 'border-white/10'
      } ${className}`}
      {...props}
    />
  );
}

export function UiSelect({
  id,
  error,
  className = '',
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { id: string; error?: string }) {
  return (
    <select
      id={id}
      className={`w-full rounded-xl border bg-night-900/80 px-4 py-3 text-sm text-zinc-100 transition focus:border-signal/50 ${
        error ? 'border-red-500/40' : 'border-white/10'
      } ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}
