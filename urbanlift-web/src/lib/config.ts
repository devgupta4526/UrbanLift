/**
 * Default: Vite dev proxy prefixes (see vite.config.ts).
 * Override in `.env`: VITE_AUTH_API_BASE=http://localhost:7475
 */
export const AUTH_API_BASE =
  import.meta.env.VITE_AUTH_API_BASE?.replace(/\/$/, '') ?? '/__auth';

export const DRIVER_API_BASE =
  import.meta.env.VITE_DRIVER_API_BASE?.replace(/\/$/, '') ?? '/__driver';
