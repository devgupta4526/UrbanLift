import { LS_PASSENGER_EMAIL, LS_PASSENGER_ID } from './config';

export function getStoredPassengerId(): number | null {
  const raw = localStorage.getItem(LS_PASSENGER_ID);
  if (raw == null || raw === '') return null;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export function setStoredPassengerIdentity(id: number, email?: string) {
  localStorage.setItem(LS_PASSENGER_ID, String(id));
  if (email) localStorage.setItem(LS_PASSENGER_EMAIL, email.trim().toLowerCase());
}

export function clearStoredPassengerIdentity() {
  localStorage.removeItem(LS_PASSENGER_ID);
  localStorage.removeItem(LS_PASSENGER_EMAIL);
}
