/**
 * Default: Vite dev proxy prefixes (see vite.config.ts).
 */
export const AUTH_API_BASE =
  import.meta.env.VITE_AUTH_API_BASE?.replace(/\/$/, '') ?? '/__auth';

export const DRIVER_API_BASE =
  import.meta.env.VITE_DRIVER_API_BASE?.replace(/\/$/, '') ?? '/__driver';

export const BOOKING_API_BASE =
  import.meta.env.VITE_BOOKING_API_BASE?.replace(/\/$/, '') ?? '/__booking';

export const PAYMENT_API_BASE =
  import.meta.env.VITE_PAYMENT_API_BASE?.replace(/\/$/, '') ?? '/__payment';
export const SOCKET_API_BASE =
  import.meta.env.VITE_SOCKET_API_BASE?.replace(/\/$/, '') ?? '/__socket';

/** localStorage key for passenger numeric id (sign-in does not return id from API). */
export const LS_PASSENGER_ID = 'urbanlift.passengerId';
export const LS_PASSENGER_EMAIL = 'urbanlift.passengerEmail';

