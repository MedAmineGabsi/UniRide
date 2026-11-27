import { validateVehicle } from '../src/vehicleValidation';

const valid = {
  make: 'Toyota', model: 'Corolla', color: 'Silver', licensePlate: 'XYZ 789', seatCapacity: '5',
};

it('accepts valid details without mutating the form', () => {
  const values = Object.freeze({ ...valid, make: ' Toyota ', licensePlate: ' abc   123 ' });
  expect(validateVehicle(values)).toEqual({});
  expect(values.make).toBe(' Toyota ');
  expect(values.licensePlate).toBe(' abc   123 ');
});

it.each([
  ['make', 'Make'], ['model', 'Model'], ['color', 'Color'],
  ['licensePlate', 'License plate'], ['seatCapacity', 'Seat capacity'],
] as const)('requires %s, including whitespace-only input', (field, label) => {
  for (const value of ['', '   ']) {
    expect(validateVehicle({ ...valid, [field]: value })).toEqual({ [field]: `${label} is required` });
  }
});

it('returns all missing-field errors together', () => {
  expect(validateVehicle({ make: '', model: '', color: '', licensePlate: '', seatCapacity: '' })).toEqual({
    make: 'Make is required', model: 'Model is required', color: 'Color is required',
    licensePlate: 'License plate is required', seatCapacity: 'Seat capacity is required',
  });
});

it.each([
  ['make', 'Make', 50], ['model', 'Model', 50], ['color', 'Color', 30],
  ['licensePlate', 'License plate', 20],
] as const)('checks the trimmed length boundary for %s', (field, label, max) => {
  expect(validateVehicle({ ...valid, [field]: ` ${'X'.repeat(max)} ` })).toEqual({});
  expect(validateVehicle({ ...valid, [field]: 'X'.repeat(max + 1) })).toEqual({
    [field]: `${label} must be at most ${max} characters`,
  });
});

it.each(['-', '---', ' - - ', '\t-\t'])('rejects empty-equivalent plate %j', licensePlate => {
  expect(validateVehicle({ ...valid, licensePlate })).toEqual({ licensePlate: 'License plate is required' });
});

it.each(['XYZ 789', 'f45-kpl', '4k7   z9p', 'G12 HJK'])('accepts feature-file plate %s', licensePlate => {
  expect(validateVehicle({ ...valid, licensePlate })).toEqual({});
});

it.each(['1', '0', '-3', '-2147483648'])('rejects seat capacity below two: %s', seatCapacity => {
  expect(validateVehicle({ ...valid, seatCapacity })).toEqual({
    seatCapacity: 'Seat capacity must be greater than 1',
  });
});

it.each(['2', '4', '8', ' 5 ', '2147483647'])('accepts valid seat capacity %s', seatCapacity => {
  expect(validateVehicle({ ...valid, seatCapacity })).toEqual({});
});

it.each(['2.5', 'abc', '1e2', 'NaN', 'Infinity', '2147483648', '-2147483649', '9007199254740993'])
('rejects non-integer or out-of-range seat capacity %s', seatCapacity => {
  expect(validateVehicle({ ...valid, seatCapacity }).seatCapacity).toBeDefined();
});
