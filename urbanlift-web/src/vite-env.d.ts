/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_AUTH_API_BASE: string;
  readonly VITE_DRIVER_API_BASE: string;
  readonly VITE_BOOKING_API_BASE: string;
  readonly VITE_PAYMENT_API_BASE: string;
  readonly VITE_SOCKET_API_BASE: string;
  readonly VITE_USE_PROXY?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
