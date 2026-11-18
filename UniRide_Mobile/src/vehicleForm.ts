/**
 * Shared types for the vehicle details form (ID15).
 *
 * Field names match UniRide_Backend dto/CreateCarRequest.java, so field errors returned by
 * POST /api/cars can be shown next to the matching input as they are.
 */
export interface VehicleValues {
  make: string;
  model: string;
  color: string;
  licensePlate: string;
  /** Total seats in the car, driver's seat included. Kept as text while typing. */
  seatCapacity: string;
}

export const EMPTY_VEHICLE: VehicleValues = {
  make: '',
  model: '',
  color: '',
  licensePlate: '',
  seatCapacity: '',
};

export type VehicleField = keyof VehicleValues;
export type VehicleErrors = Partial<Record<VehicleField, string>>;

export type VehicleSubmitResult =
  | { ok: true }
  | { ok: false; message: string; errors: VehicleErrors; authenticationRequired?: boolean };

/** Client-side checks run before submitting (TID108). Returns field -> message; empty if valid. */
export type VehicleValidator = (values: VehicleValues) => VehicleErrors;

/** Sends the vehicle details to the backend (TID109). */
export type VehicleSubmitter = (values: VehicleValues) => Promise<VehicleSubmitResult>;

/** Longest values the backend accepts (car table columns). */
export const VEHICLE_MAX_LENGTH: Partial<Record<VehicleField, number>> = {
  make: 50,
  model: 50,
  color: 30,
  licensePlate: 20,
};
