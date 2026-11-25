/** Structurally matches VehicleValues in the pending vehicle form PR (#177). */
interface VehicleValidationValues {
  make: string;
  model: string;
  color: string;
  licensePlate: string;
  seatCapacity: string;
}

type VehicleValidationErrors = Partial<Record<keyof VehicleValidationValues, string>>;

/**
 * Pure client-side checks matching CarValidator; compatible with the form's validate prop.
 * Driver permissions and duplicate cars/plates must still be checked by the backend.
 * The input is left unchanged; plate normalization happens when the backend saves it.
 */
export function validateVehicle(values: VehicleValidationValues): VehicleValidationErrors {
  const errors: VehicleValidationErrors = {};
  const textFields = [
    { field: 'make', label: 'Make', max: 50 },
    { field: 'model', label: 'Model', max: 50 },
    { field: 'color', label: 'Color', max: 30 },
    { field: 'licensePlate', label: 'License plate', max: 20 },
  ] as const;

  for (const { field, label, max } of textFields) {
    const value = values[field].trim();
    if (!value) {
      errors[field] = `${label} is required`;
    } else if (value.length > max) {
      errors[field] = `${label} must be at most ${max} characters`;
    } else if (field === 'licensePlate' && !value.replace(/[\s-]/g, '')) {
      errors.licensePlate = 'License plate is required';
    }
  }

  const seats = values.seatCapacity.trim();
  if (!seats) {
    errors.seatCapacity = 'Seat capacity is required';
  } else if (!/^-?\d+$/.test(seats) || !Number.isInteger(Number(seats)) ||
      Number(seats) < -2147483648 || Number(seats) > 2147483647) {
    errors.seatCapacity = 'Enter a whole number no greater than 2147483647';
  } else if (Number(seats) <= 1) {
    errors.seatCapacity = 'Seat capacity must be greater than 1';
  }

  return errors;
}
