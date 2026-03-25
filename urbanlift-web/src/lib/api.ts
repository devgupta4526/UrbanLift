import { AUTH_API_BASE, DRIVER_API_BASE } from './config';

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
      if (typeof j.message === 'string') return j.message;
      if (typeof j.error === 'string') return j.error;
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
      'Content-Type': 'application/json',
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
  const res = await apiFetch(base, path, init);
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

export const authApi = {
  base: AUTH_API_BASE,
  signup: (body: Record<string, unknown>) =>
    apiJson<{ id: number; email: string; firstName?: string; lastName?: string }>(
      AUTH_API_BASE,
      '/api/v1/auth/signup/passenger',
      { method: 'POST', body: JSON.stringify(body) }
    ),
  signin: (body: { email: string; password: string }) =>
    apiJson<{ success: boolean }>(AUTH_API_BASE, '/api/v1/auth/signin/passenger', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  validate: () =>
    apiJson<{ success: boolean }>(AUTH_API_BASE, '/api/v1/auth/validate', { method: 'GET' }),
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
};
