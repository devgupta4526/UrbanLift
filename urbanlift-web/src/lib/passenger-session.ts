import { authApi, ApiError } from '@/lib/api';
import { getStoredPassengerId, setStoredPassengerIdentity } from '@/lib/storage';

export type PassengerSessionSource = 'jwt' | 'merged' | 'storage-only';

export type ResolvedPassengerSession = {
  passengerId: number;
  email?: string;
  source: PassengerSessionSource;
  /** Human-readable note for QA / debugging */
  note?: string;
};

/**
 * Resolves the current rider id for API calls:
 * - Prefer JWT validate() when it succeeds (cookie session).
 * - If the token validates but omits `passengerId` (older tokens), fall back to localStorage.
 * - If validate returns 401 but this device has a saved id, allow continuing (booking APIs are open) with a clear note.
 */
export async function resolvePassengerSession(): Promise<ResolvedPassengerSession> {
  const stored = getStoredPassengerId();

  try {
    const v = await authApi.validate();
    if (!v.success) {
      throw new Error('Session check returned success=false.');
    }
    const pid = v.passengerId ?? stored;
    if (pid == null) {
      throw new Error('No passenger id in JWT or saved profile. Sign in at /passenger.');
    }
    if (v.passengerId != null) {
      setStoredPassengerIdentity(v.passengerId, v.email);
      return { passengerId: v.passengerId, email: v.email, source: 'jwt' };
    }
    setStoredPassengerIdentity(pid, v.email);
    return {
      passengerId: pid,
      email: v.email,
      source: 'merged',
      note: 'JWT did not include passengerId; using id saved on this device from sign-in.',
    };
  } catch (e) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403) && stored != null) {
      return {
        passengerId: stored,
        source: 'storage-only',
        note: `Auth cookie missing or rejected (${e.message}). Using saved passenger id for booking calls — open /passenger and sign in again to refresh the session.`,
      };
    }
    throw e;
  }
}
