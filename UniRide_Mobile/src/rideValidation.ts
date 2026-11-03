export interface RideValues {
  origin: string;
  destination: string;
  departureDateTime: string;
  durationMinutes: string;
  totalSeats: string;
  pricePerSeat: string;
}

export const EMPTY_RIDE: RideValues = {
  origin: '',
  destination: '',
  departureDateTime: '',
  durationMinutes: '',
  totalSeats: '',
  pricePerSeat: '',
};
export type RideField = keyof RideValues;
export type RideErrors = Partial<Record<RideField, string>>;

/** Pure client checks; the backend remains the source of truth for ride constraints. */
export function validateRide(values: RideValues): RideErrors {
  const errors: RideErrors = {};
  for (const field of Object.keys(EMPTY_RIDE) as RideField[]) {
    if (!values[field].trim()) errors[field] = 'This field is required';
  }
  for (const field of ['durationMinutes', 'totalSeats'] as const) {
    if (values[field].trim() && (!/^\d+$/.test(values[field].trim()) ||
        Number(values[field]) < 1 || Number(values[field]) > 2147483647)) {
      errors[field] = 'Enter a positive whole number';
    }
  }
  const price = values.pricePerSeat.trim();
  if (price && (!/^\d+(\.\d+)?$/.test(price) || Number(price) <= 0 ||
      Number(price) > 99999999.99 || (price.split('.')[1] ?? '').replace(/0+$/, '').length > 2)) {
    errors.pricePerSeat = 'Enter a price greater than 0 with at most 2 decimal places (maximum 99999999.99)';
  }
  return errors;
}
