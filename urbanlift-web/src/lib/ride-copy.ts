import { bookingStatusUpper } from '@/lib/booking-flow';

/** Passenger-facing status — no enum jargon. */
export function rideHeadline(status: string | undefined): string {
  const u = bookingStatusUpper(status);
  switch (u) {
    case 'ASSIGNING_DRIVER':
      return 'Finding a ride for you';
    case 'SCHEDULED':
      return 'Driver on the way';
    case 'CAB_ARRIVED':
      return 'Your driver has arrived';
    case 'IN_RIDE':
      return 'Enjoy your trip';
    case 'COMPLETED':
      return 'You’ve arrived';
    case 'CANCELLED':
      return 'Trip cancelled';
    default:
      return 'Trip update';
  }
}

/** Driver dashboard — short stage label. */
export function driverTripStageLabel(status: string | undefined): string {
  const u = bookingStatusUpper(status);
  switch (u) {
    case 'ASSIGNING_DRIVER':
      return 'New request';
    case 'SCHEDULED':
      return 'En route to pickup';
    case 'CAB_ARRIVED':
      return 'At pickup';
    case 'IN_RIDE':
      return 'On trip';
    case 'COMPLETED':
      return 'Completed';
    case 'CANCELLED':
      return 'Cancelled';
    default:
      return 'Trip';
  }
}

export function rideSubline(status: string | undefined, driverName?: string): string {
  const u = bookingStatusUpper(status);
  if (u === 'ASSIGNING_DRIVER') return 'We’re matching you with a nearby driver. This usually takes a moment.';
  if (u === 'SCHEDULED')
    return driverName
      ? `${driverName} is heading to your pickup point.`
      : 'Head to your pickup spot — your driver is en route.';
  if (u === 'CAB_ARRIVED') return 'Meet your driver at the pickup location.';
  if (u === 'IN_RIDE') return 'Sit back and relax — we’ll drop you off soon.';
  if (u === 'COMPLETED') return 'Thanks for riding with UrbanLift.';
  if (u === 'CANCELLED') return 'You won’t be charged for this trip.';
  return '';
}
