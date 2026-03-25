import type { BookingPersonDto } from '@/lib/api';

/** Matches com.example.Uber_EntityService.Models.BookingStatus */
export const BOOKING_STATUS_ORDER = [
  'ASSIGNING_DRIVER',
  'SCHEDULED',
  'CAB_ARRIVED',
  'IN_RIDE',
  'COMPLETED',
  'CANCELLED',
] as const;

export function bookingStatusUpper(s: string | undefined): string {
  return (s ?? '').trim().toUpperCase();
}

export function isActiveTripStatus(status: string | undefined): boolean {
  const u = bookingStatusUpper(status);
  return ['ASSIGNING_DRIVER', 'SCHEDULED', 'CAB_ARRIVED', 'IN_RIDE'].includes(u);
}

export function isTerminalTripStatus(status: string | undefined): boolean {
  const u = bookingStatusUpper(status);
  return u === 'COMPLETED' || u === 'CANCELLED';
}

export function formatBookingPerson(p?: BookingPersonDto | null): string {
  if (!p) return '';
  const name = [p.firstName, p.lastName].filter(Boolean).join(' ').trim();
  if (name) return name;
  if (p.email) return p.email;
  if (p.id != null) return `ID ${p.id}`;
  return '';
}

export type DriverTripAction =
  | { label: string; nextStatus: string }
  | null;

/** Driver-visible next step via PUT .../status (after ride is assigned to this driver). */
export function driverStatusAction(status: string | undefined): DriverTripAction {
  const u = bookingStatusUpper(status);
  if (u === 'SCHEDULED') return { label: 'Mark arrived at pickup', nextStatus: 'CAB_ARRIVED' };
  if (u === 'CAB_ARRIVED') return { label: 'Start trip', nextStatus: 'IN_RIDE' };
  if (u === 'IN_RIDE') return { label: 'Complete trip', nextStatus: 'COMPLETED' };
  return null;
}
