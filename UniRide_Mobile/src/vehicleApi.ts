import { API_BASE_URL } from './config';
import type { AccessTokenProvider } from './rideApi';

/**
 * Structurally matches VehicleValues in the pending vehicle form PR (#177).
 * TODO(TID109): After #177 merges, import VehicleValues, VehicleErrors and
 * VehicleSubmitResult from vehicleForm instead of the local equivalents below.
 * In App.tsx, replace vehicleApiNotReady with createVehicleSubmitter(getAccessToken).
 * Run typecheck and tests after connecting the form; this module is not wired to the UI yet.
 */
interface VehicleApiValues {
  make: string;
  model: string;
  color: string;
  licensePlate: string;
  seatCapacity: string;
}

type VehicleApiErrors = Partial<Record<keyof VehicleApiValues, string>>;
export type CreateVehicleResult =
  | { ok: true }
  | { ok: false; message: string; errors: VehicleApiErrors; authenticationRequired?: boolean };

/** Matches UniRide_Backend dto/CreateCarRequest.java. Driver identity comes from the token. */
export interface CreateVehicleRequest {
  make: string;
  model: string;
  color: string;
  licensePlate: string;
  seatCapacity: number;
}

export function buildCreateVehicleRequest(values: VehicleApiValues): CreateVehicleRequest {
  return {
    make: values.make.trim(),
    model: values.model.trim(),
    color: values.color.trim(),
    licensePlate: values.licensePlate.trim(),
    seatCapacity: Number(values.seatCapacity),
  };
}

const VEHICLE_FIELDS = ['make', 'model', 'color', 'licensePlate', 'seatCapacity'] as const;

/** POSTs validated vehicle values. Failures are returned to the form rather than thrown. */
export async function createVehicle(
  values: VehicleApiValues,
  getAccessToken: AccessTokenProvider,
): Promise<CreateVehicleResult> {
  let token: string | null;
  try {
    token = await getAccessToken();
  } catch {
    token = null;
  }
  if (!token) {
    return { ok: false, message: 'Please sign in before registering a vehicle.', errors: {}, authenticationRequired: true };
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(`${API_BASE_URL}/api/cars`, {
      method: 'POST',
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(buildCreateVehicleRequest(values)),
    });
    if (response.status === 201) return { ok: true };
    if (response.status === 401) {
      return { ok: false, message: 'Your session has expired. Please sign in again.', errors: {}, authenticationRequired: true };
    }
    if (response.status >= 500) {
      return { ok: false, message: 'The server could not register your vehicle. Please try again later.', errors: {} };
    }

    let body: unknown;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    const payload = body && typeof body === 'object' ? body as Record<string, unknown> : {};
    const errors: VehicleApiErrors = {};
    if (payload.errors && typeof payload.errors === 'object') {
      for (const field of VEHICLE_FIELDS) {
        const message = (payload.errors as Record<string, unknown>)[field];
        if (typeof message === 'string') errors[field] = message;
      }
    }
    const fallback = response.status === 403
      ? 'Only drivers are allowed to register a car'
      : response.status === 404
        ? 'User not found'
        : 'Could not register the vehicle. Check your details and try again.';
    return { ok: false, errors, message: typeof payload.message === 'string' ? payload.message : fallback };
  } catch {
    // A connection failure can happen after the server saved the car; do not retry automatically.
    return { ok: false, message: 'Could not confirm whether your vehicle was registered. Check your vehicle details before retrying.', errors: {} };
  } finally {
    clearTimeout(timer);
  }
}

/** Compatible with the vehicle screen's submitVehicle prop; bind the current token provider once. */
export function createVehicleSubmitter(getAccessToken: AccessTokenProvider) {
  return (values: VehicleApiValues): Promise<CreateVehicleResult> => createVehicle(values, getAccessToken);
}
