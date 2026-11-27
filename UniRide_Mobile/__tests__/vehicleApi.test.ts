import { buildCreateVehicleRequest, createVehicle, createVehicleSubmitter } from '../src/vehicleApi';
import { API_BASE_URL } from '../src/config';

const values = {
  make: ' Toyota ', model: ' Corolla ', color: ' Silver ', licensePlate: ' abc 123 ', seatCapacity: '5',
};
const token = async () => 'test-token';
const originalFetch = globalThis.fetch;

beforeEach(() => { globalThis.fetch = jest.fn(); });
afterEach(() => { globalThis.fetch = originalFetch; jest.useRealTimers(); });

it('builds the backend request without changing the form or adding driver identity', () => {
  const input = Object.freeze({ ...values });
  expect(buildCreateVehicleRequest(input)).toEqual({
    make: 'Toyota', model: 'Corolla', color: 'Silver', licensePlate: 'abc 123', seatCapacity: 5,
  });
  expect(input).toEqual(values);
});

it('POSTs to /api/cars with bearer authentication and numeric seat capacity', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ status: 201 });
  globalThis.fetch = fetchMock;
  expect(await createVehicle(values, token)).toEqual({ ok: true });
  expect(fetchMock).toHaveBeenCalledTimes(1);
  const [url, init] = fetchMock.mock.calls[0];
  expect(url).toBe(`${API_BASE_URL}/api/cars`);
  expect(init.method).toBe('POST');
  expect(init.headers).toEqual({ 'Content-Type': 'application/json', Authorization: 'Bearer test-token' });
  expect(JSON.parse(init.body)).toEqual({
    make: 'Toyota', model: 'Corolla', color: 'Silver', licensePlate: 'abc 123', seatCapacity: 5,
  });
  expect(init.signal.aborted).toBe(false);
});

it('binds the token provider using the form submitter adapter', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 201 });
  const getToken = jest.fn().mockResolvedValue('current-token');
  const submit = createVehicleSubmitter(getToken);
  expect(await submit(values)).toEqual({ ok: true });
  expect(getToken).toHaveBeenCalledTimes(1);
  expect(globalThis.fetch).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({
    headers: expect.objectContaining({ Authorization: 'Bearer current-token' }),
  }));
});

it.each([null, ''])('does not send a request without a token: %j', async value => {
  expect(await createVehicle(values, async () => value)).toMatchObject({
    ok: false, errors: {}, authenticationRequired: true,
  });
  expect(globalThis.fetch).not.toHaveBeenCalled();
});

it('handles a rejected token provider without sending a request', async () => {
  expect(await createVehicle(values, async () => { throw new Error('expired'); }))
    .toMatchObject({ ok: false, authenticationRequired: true });
  expect(globalThis.fetch).not.toHaveBeenCalled();
});

it('asks for sign-in when the session expires', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 401 });
  expect(await createVehicle(values, token)).toEqual({
    ok: false, message: 'Your session has expired. Please sign in again.', errors: {}, authenticationRequired: true,
  });
});

it.each([
  [400, 'Invalid Car Details'], [403, 'Only drivers are allowed to register a car'],
  [404, 'User not found'], [409, 'A driver can only have one car'],
  [409, 'A car with this license plate is already registered'],
])('preserves backend HTTP %i message: %s', async (status, message) => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status, json: async () => ({ message, errors: {} }) });
  expect(await createVehicle(values, token)).toEqual({ ok: false, message, errors: {} });
});

it('only maps recognized fields with string error messages', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 400, json: async () => ({
    message: 'Invalid Car Details', errors: {
      make: 'Make is required', model: 'Model is required', color: 'Color is required',
      licensePlate: 'License plate is required', seatCapacity: 'Seat capacity must be greater than 1',
      driver: 'ignored',
    },
  }) });
  expect(await createVehicle(values, token)).toEqual({ ok: false, message: 'Invalid Car Details', errors: {
    make: 'Make is required', model: 'Model is required', color: 'Color is required',
    licensePlate: 'License plate is required', seatCapacity: 'Seat capacity must be greater than 1',
  } });
});

it.each([null, 'invalid', { color: 123, make: {}, model: null, extra: 'ignored' }])
('handles malformed field-error data: %j', async errors => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 400, json: async () => ({ errors, message: 42 }) });
  expect(await createVehicle(values, token)).toEqual({
    ok: false, message: 'Could not register the vehicle. Check your details and try again.', errors: {},
  });
});

it.each([null, 'invalid', 42])('handles unexpected JSON body: %j', async body => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 400, json: async () => body });
  expect(await createVehicle(values, token)).toMatchObject({ ok: false, errors: {}, message: expect.any(String) });
});

it.each([
  [400, 'Could not register the vehicle. Check your details and try again.'],
  [403, 'Only drivers are allowed to register a car'], [404, 'User not found'],
  [409, 'Could not register the vehicle. Check your details and try again.'],
])('handles non-JSON HTTP %i errors', async (status, message) => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status, json: async () => { throw new Error('HTML'); } });
  expect(await createVehicle(values, token)).toEqual({ ok: false, message, errors: {} });
});

it.each([500, 502, 503])('handles server failure HTTP %i', async status => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status });
  expect(await createVehicle(values, token)).toEqual({
    ok: false, message: 'The server could not register your vehicle. Please try again later.', errors: {},
  });
});

it('reports an uncertain network outcome without automatically retrying', async () => {
  globalThis.fetch = jest.fn().mockRejectedValue(new TypeError('offline'));
  expect(await createVehicle(values, token)).toMatchObject({
    ok: false, message: expect.stringContaining('Check your vehicle details before retrying'),
  });
  expect(globalThis.fetch).toHaveBeenCalledTimes(1);
});

it('aborts after 15 seconds and clears its timeout', async () => {
  jest.useFakeTimers();
  let signal: AbortSignal | undefined;
  globalThis.fetch = jest.fn().mockImplementation((_url: string, init: RequestInit) => {
    signal = init.signal as AbortSignal;
    return new Promise((_resolve, reject) => {
      signal!.addEventListener('abort', () => reject(new Error('timeout')));
    });
  });
  const pending = createVehicle(values, token);
  await jest.advanceTimersByTimeAsync(14999);
  expect(signal?.aborted).toBe(false);
  await jest.advanceTimersByTimeAsync(1);
  expect(signal?.aborted).toBe(true);
  expect(await pending).toMatchObject({ ok: false });
  expect(jest.getTimerCount()).toBe(0);
  expect(globalThis.fetch).toHaveBeenCalledTimes(1);
});

it.each([201, 400, 401, 500])('clears the timeout after HTTP %i', async status => {
  jest.useFakeTimers();
  globalThis.fetch = jest.fn().mockResolvedValue({ status, json: async () => ({}) });
  await createVehicle(values, token);
  expect(jest.getTimerCount()).toBe(0);
});
