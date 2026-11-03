import { EMPTY_RIDE, validateRide } from '../src/rideValidation';
const valid = { origin: '123 Main St', destination: '845 Sherbrooke St',
  departureDateTime: '2028-11-03 11:00', durationMinutes: '60', totalSeats: '3', pricePerSeat: '12.34' };
it('requires all six fields', () => expect(Object.keys(validateRide(EMPTY_RIDE))).toHaveLength(6));
it('accepts valid values', () => expect(validateRide(valid)).toEqual({}));
it.each(['0', '-1', '1.5', '1e3', '2147483648'])('rejects invalid whole-number fields: %s', value => {
  expect(validateRide({ ...valid, totalSeats: value, durationMinutes: value })).toMatchObject({
    totalSeats: expect.any(String), durationMinutes: expect.any(String),
  });
});
it.each(['0', '-0.01', '1.001', '100000000', 'NaN', 'Infinity'])('rejects invalid price: %s', pricePerSeat => {
  expect(validateRide({ ...valid, pricePerSeat }).pricePerSeat).toBeDefined();
});
it.each(['0.01', '12.3400', '99999999.99'])('accepts valid price boundary: %s', pricePerSeat => {
  expect(validateRide({ ...valid, pricePerSeat })).toEqual({});
});
