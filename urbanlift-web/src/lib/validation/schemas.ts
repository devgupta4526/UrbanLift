import { z } from 'zod';

const emailSchema = z
  .string()
  .trim()
  .min(1, 'Email is required')
  .email('Enter a valid email address')
  .max(255, 'Email is too long');

const passwordSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .max(128, 'Password is too long');

export const passengerSignupSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required').max(100),
  lastName: z.string().trim().min(1, 'Last name is required').max(100),
  email: emailSchema,
  phoneNumber: z.string().trim().min(1, 'Phone is required').max(32),
  password: passwordSchema,
  address: z.string().trim().max(500).optional(),
});

export const signinSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Password is required'),
});

export const latitudeSchema = z.coerce
  .number({ invalid_type_error: 'Enter a valid latitude' })
  .min(-90, 'Latitude must be ≥ -90')
  .max(90, 'Latitude must be ≤ 90');

export const longitudeSchema = z.coerce
  .number({ invalid_type_error: 'Enter a valid longitude' })
  .min(-180, 'Longitude must be ≥ -180')
  .max(180, 'Longitude must be ≤ 180');

export const passengerIdFormSchema = z.object({
  passengerId: z.coerce
    .number({ invalid_type_error: 'Passenger ID must be a number' })
    .int('Must be a whole number')
    .positive('Must be a positive ID'),
});

export const fareEstimateFormSchema = z.object({
  startLat: latitudeSchema,
  startLng: longitudeSchema,
  endLat: latitudeSchema,
  endLng: longitudeSchema,
  carType: z.enum(['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL'], {
    errorMap: () => ({ message: 'Choose a vehicle class' }),
  }),
});

export const createBookingFormSchema = z
  .object({
    passengerId: z.coerce
      .number({ invalid_type_error: 'Passenger ID is required' })
      .int()
      .positive(),
    startLat: latitudeSchema,
    startLng: longitudeSchema,
    endLat: latitudeSchema,
    endLng: longitudeSchema,
  })
  .refine(
    (d) =>
      !(d.startLat === d.endLat && d.startLng === d.endLng),
    { message: 'Pickup and drop-off cannot be identical', path: ['endLat'] }
  );

export const driverLocationSchema = z.object({
  latitude: latitudeSchema,
  longitude: longitudeSchema,
});

/** Optional GPS when toggling availability (both or neither). */
export const driverAvailabilityFormSchema = z
  .object({
    available: z.coerce.boolean(),
    lat: z.string().optional(),
    lng: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    const latS = data.lat?.trim() ?? '';
    const lngS = data.lng?.trim() ?? '';
    if (latS === '' && lngS === '') return;
    if (latS === '' || lngS === '') {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Provide both latitude and longitude for location sync',
        path: latS === '' ? ['lat'] : ['lng'],
      });
      return;
    }
    const la = Number(latS);
    const ln = Number(lngS);
    if (!Number.isFinite(la) || la < -90 || la > 90) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Latitude must be between -90 and 90', path: ['lat'] });
    }
    if (!Number.isFinite(ln) || ln < -180 || ln > 180) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Longitude must be between -180 and 180', path: ['lng'] });
    }
  });

export const driverSignupStep1Schema = z.object({
  firstName: z.string().trim().min(1, 'Required').max(100),
  lastName: z.string().trim().min(1, 'Required').max(100),
  email: emailSchema,
  phoneNumber: z.string().trim().min(1, 'Required').max(32),
  password: passwordSchema,
  address: z.string().trim().max(500).optional(),
});

export const driverSignupStep2Schema = z.object({
  licenseNumber: z.string().trim().min(1, 'License number is required').max(64),
  aadharNumber: z.string().trim().min(1, 'Aadhar is required').max(32),
  activeCity: z.string().trim().max(100).optional(),
});

export const driverSignupStep3Schema = z.object({
  plateNumber: z.string().trim().min(1, 'Plate number is required').max(32),
  colorName: z.string().trim().min(1, 'Color is required').max(64),
  brand: z.string().trim().min(1, 'Brand is required').max(64),
  model: z.string().trim().min(1, 'Model is required').max(64),
  carType: z.enum(['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL']),
});

/** Full payload for driver signup API. */
export const driverFullSignupSchema = z.object({
  firstName: z.string().trim().min(1, 'Required').max(100),
  lastName: z.string().trim().min(1, 'Required').max(100),
  email: emailSchema,
  phoneNumber: z.string().trim().min(1, 'Required').max(32),
  password: passwordSchema,
  address: z.string().trim().max(500).optional(),
  licenseNumber: z.string().trim().min(1, 'Required').max(64),
  aadharNumber: z.string().trim().min(1, 'Required').max(32),
  activeCity: z.string().trim().max(100).optional(),
  car: z.object({
    plateNumber: z.string().trim().min(1, 'Required').max(32),
    colorName: z.string().trim().min(1, 'Required').max(64),
    brand: z.string().trim().min(1, 'Required').max(64),
    model: z.string().trim().min(1, 'Required').max(64),
    carType: z.enum(['SEDAN', 'HATCHBACK', 'SUV', 'COMPACT_SUV', 'XL']),
  }),
});

export type PassengerSignupValues = z.infer<typeof passengerSignupSchema>;
export type SigninValues = z.infer<typeof signinSchema>;
export type PassengerIdFormValues = z.infer<typeof passengerIdFormSchema>;
export type FareEstimateFormValues = z.infer<typeof fareEstimateFormSchema>;
export type CreateBookingFormValues = z.infer<typeof createBookingFormSchema>;
export type DriverLocationValues = z.infer<typeof driverLocationSchema>;
