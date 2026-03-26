import { AUTH_API_BASE, BOOKING_API_BASE, DRIVER_API_BASE, PAYMENT_API_BASE } from './config';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function parseErrorBody(res: Response): Promise<string> {
  const ct = res.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) {
    try {
      const j = (await res.json()) as { message?: string; error?: string };
      if (typeof j.message === 'string' && j.message.trim()) return j.message;
      if (typeof j.error === 'string' && j.error.trim()) return j.error;
      return JSON.stringify(j);
    } catch {
      return res.statusText;
    }
  }
  const t = await res.text();
  return t || res.statusText;
}

export async function apiFetch(
  base: string,
  path: string,
  init?: RequestInit
): Promise<Response> {
  const url = `${base}${path.startsWith('/') ? path : `/${path}`}`;
  const res = await fetch(url, {
    ...init,
    credentials: 'include',
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(init?.headers ?? {}),
    },
  });
  return res;
}

export async function apiJson<T>(
  base: string,
  path: string,
  init?: RequestInit
): Promise<T> {
  const res = await apiFetch(base, path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    const msg = await parseErrorBody(res);
    throw new ApiError(msg, res.status);
  }
  if (res.status === 204) return undefined as T;
  const ct = res.headers.get('content-type') ?? '';
  if (!ct.includes('application/json')) {
    const text = await res.text();
    return text as unknown as T;
  }
  return res.json() as Promise<T>;
}

// —— Types (defensive; backend may serialize Optionals oddly)

export interface ExactLocationDto {
  latitude?: number;
  longitude?: number;
}

/** Nested entity shapes from Booking Service JSON (may include extra fields). */
export interface BookingPersonDto {
  id?: number;
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface BookingDetailDto {
  id: number;
  bookingStatus: string;
  bookingDate?: string;
  startTime?: string;
  endTime?: string;
  totalDistance?: number;
  passenger?: BookingPersonDto | null;
  driver?: BookingPersonDto | null;
  startLocation?: ExactLocationDto;
  endLocation?: ExactLocationDto;
}

export interface CreateBookingResponseDto {
  bookingId: number;
  bookingStatus: string;
}

export interface FareEstimateDto {
  estimatedFare?: number;
  baseFare?: number;
  distanceFare?: number;
  timeFare?: number;
  surgeMultiplier?: number;
  totalFare?: number;
}

export interface UpdateBookingResponseDto {
  bookingId?: number;
  status?: string;
  driver?: BookingPersonDto | null;
}

export interface DriverDto {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  driverApprovalStatus?: string;
  isAvailable?: boolean;
  car?: { plateNumber?: string; carType?: string };
}

export const authApi = {
  base: AUTH_API_BASE,
  signup: (body: Record<string, unknown>) =>
    apiJson<{ id: number; email: string; firstName?: string; lastName?: string }>(
      AUTH_API_BASE,
      '/api/v1/auth/signup/passenger',
      { method: 'POST', body: JSON.stringify(body) }
    ),
  signin: (body: { email: string; password: string }) =>
    apiJson<{ success: boolean; passengerId?: number; email?: string }>(AUTH_API_BASE, '/api/v1/auth/signin/passenger', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  validate: () =>
    apiJson<{ success: boolean; passengerId?: number; email?: string }>(AUTH_API_BASE, '/api/v1/auth/validate', {
      method: 'GET',
    }),
};

export const driverApi = {
  base: DRIVER_API_BASE,
  signup: (body: Record<string, unknown>) =>
    apiJson<{
      id: number;
      email: string;
      driverApprovalStatus?: string;
    }>(DRIVER_API_BASE, '/api/v1/driver/auth/signup', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  signin: (body: { email: string; password: string }) =>
    apiJson<{ success: boolean }>(DRIVER_API_BASE, '/api/v1/driver/auth/signin', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  validate: () =>
    apiJson<{ success: boolean }>(DRIVER_API_BASE, '/api/v1/driver/auth/validate', {
      method: 'GET',
    }),
  profile: () => apiJson<DriverDto>(DRIVER_API_BASE, '/api/v1/driver/profile', { method: 'GET' }),
  updateLocation: (body: { latitude: number; longitude: number }) =>
    apiJson<void>(DRIVER_API_BASE, '/api/v1/driver/location', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  setAvailability: (body: { available: boolean }, lat?: number, lng?: number) => {
    const q =
      lat != null && lng != null ? `?lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}` : '';
    return apiJson<void>(DRIVER_API_BASE, `/api/v1/driver/availability${q}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },
};

export const bookingApi = {
  base: BOOKING_API_BASE,
  create: (body: {
    passengerId: number;
    startLocation: { latitude: number; longitude: number };
    endLocation: { latitude: number; longitude: number };
  }, idempotencyKey?: string) =>
    apiJson<CreateBookingResponseDto>(BOOKING_API_BASE, '/api/v1/booking', {
      method: 'POST',
      body: JSON.stringify(body),
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
    }),
  listByPassenger: (passengerId: number) =>
    apiJson<BookingDetailDto[]>(BOOKING_API_BASE, `/api/v1/booking/passenger/${passengerId}`, {
      method: 'GET',
    }),
  listByDriver: (driverId: number) =>
    apiJson<BookingDetailDto[]>(BOOKING_API_BASE, `/api/v1/booking/driver/${driverId}`, {
      method: 'GET',
    }),
  get: (bookingId: number) =>
    apiJson<BookingDetailDto>(BOOKING_API_BASE, `/api/v1/booking/${bookingId}`, { method: 'GET' }),
  cancel: (bookingId: number) =>
    apiJson<unknown>(BOOKING_API_BASE, `/api/v1/booking/${bookingId}/cancel`, {
      method: 'POST',
    }),
  /** Assign driver + status (e.g. accept ride: SCHEDULED + driverId). */
  update: (bookingId: number, body: { status?: string; driverId?: number }) =>
    apiJson<UpdateBookingResponseDto>(BOOKING_API_BASE, `/api/v1/booking/${bookingId}`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  setStatus: (bookingId: number, status: string) => {
    const q = new URLSearchParams({ status });
    return apiJson<UpdateBookingResponseDto>(
      BOOKING_API_BASE,
      `/api/v1/booking/${bookingId}/status?${q.toString()}`,
      { method: 'PUT' }
    );
  },
  passengerIdForBooking: (bookingId: number) =>
    apiJson<{ passengerId: number }>(
      BOOKING_API_BASE,
      `/api/v1/booking/${bookingId}/passenger-id`,
      { method: 'GET' }
    ),
};

export const paymentApi = {
  base: PAYMENT_API_BASE,
  estimateFare: (body: {
    startLat: number;
    startLng: number;
    endLat: number;
    endLng: number;
    carType: string;
  }) =>
    apiJson<FareEstimateDto>(PAYMENT_API_BASE, '/api/v1/fare/estimate', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  initiate: (body: { bookingId: number; amount: number }) =>
    apiJson<{
      paymentId?: number;
      orderId?: string;
      amount?: number;
      currency?: string;
      status?: string;
    }>(PAYMENT_API_BASE, '/api/v1/payment/initiate', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  confirm: (body: {
    paymentId: number;
    razorpayOrderId?: string;
    razorpayPaymentId?: string;
    razorpaySignature?: string;
  }) =>
    apiJson<{ success?: boolean; paymentId?: string; status?: string; amount?: number }>(
      PAYMENT_API_BASE,
      '/api/v1/payment/confirm',
      {
        method: 'POST',
        body: JSON.stringify(body),
      }
    ),
};
