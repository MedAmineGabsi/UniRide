import { API_BASE_URL } from './config';
import { EMPTY_RIDE, type RideErrors, type RideValues } from './rideValidation';

export type AccessTokenProvider = () => Promise<string | null>;
export type CreateRideResult =
  | { ok: true }
  | { ok: false; message: string; errors: RideErrors; authenticationRequired?: boolean };

/** Matches UniRide_Backend dto/CreateRideRequest.java. */
export interface CreateRideRequest {
  origin: string;
  destination: string;
  departureDateTime: string;
  durationMinutes: number;
  totalSeats: number;
  pricePerSeat: number;
}

export function buildCreateRideRequest(values: RideValues): CreateRideRequest {
  return {
    origin: values.origin.trim(),
    destination: values.destination.trim(),
    departureDateTime: values.departureDateTime.trim(),
    durationMinutes: Number(values.durationMinutes),
    totalSeats: Number(values.totalSeats),
    pricePerSeat: Number(values.pricePerSeat),
  };
}

export async function createRide(values: RideValues, getAccessToken: AccessTokenProvider): Promise<CreateRideResult> {
  let token: string | null;
  try {
    token = await getAccessToken();
  } catch {
    token = null;
  }
  if (!token) return { ok: false, message: 'Please sign in before creating a ride.', errors: {}, authenticationRequired: true };
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(`${API_BASE_URL}/api/rides`, {
      method: 'POST',
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(buildCreateRideRequest(values)),
    });
    if (response.status === 201) return { ok: true };
    if (response.status === 401) return { ok: false, message: 'Your session has expired. Please sign in again.', errors: {}, authenticationRequired: true };
    if (response.status === 403) return { ok: false, message: 'Only registered drivers can create rides.', errors: {} };
    if (response.status >= 500) return { ok: false, message: 'The server could not create your ride. Please try again later.', errors: {} };
    let body: unknown;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    const errors: RideErrors = {};
    const payload = body && typeof body === 'object' ? body as Record<string, unknown> : {};
    if (payload.errors && typeof payload.errors === 'object') {
      for (const field of Object.keys(EMPTY_RIDE) as (keyof RideValues)[]) {
        const message = (payload.errors as Record<string, unknown>)[field];
        if (typeof message === 'string') errors[field] = message;
      }
    }
    return { ok: false, errors, message: typeof payload.message === 'string' ? payload.message : 'Could not create the ride. Check your details and try again.' };
  } catch {
    return { ok: false, message: 'Could not confirm whether your ride was created. Check your rides before retrying.', errors: {} };
  } finally {
    clearTimeout(timer);
  }
}
