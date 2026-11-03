import { createRide } from '../src/rideApi';
import { API_BASE_URL } from '../src/config';

const values = { origin: ' 123 Main St ', destination: '845 Sherbrooke St',
  departureDateTime: '2028-11-03 11:00', durationMinutes: '60', totalSeats: '3', pricePerSeat: '12.34' };
const token = async () => 'test-token';
const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; jest.useRealTimers(); });

it('uses the real endpoint, bearer token and numeric request fields', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ status: 201 });
  globalThis.fetch = fetchMock;
  expect(await createRide(values, token)).toEqual({ ok: true });
  const [url, init] = fetchMock.mock.calls[0];
  expect(url).toBe(`${API_BASE_URL}/api/rides`);
  expect(init.headers.Authorization).toBe('Bearer test-token');
  expect(JSON.parse(init.body)).toEqual({ ...values, origin: '123 Main St',
    durationMinutes: 60, totalSeats: 3, pricePerSeat: 12.34 });
});

it('does not submit without authentication', async () => {
  globalThis.fetch = jest.fn();
  expect(await createRide(values, async () => null)).toMatchObject({ ok: false, authenticationRequired: true });
  expect(globalThis.fetch).not.toHaveBeenCalled();
});

it('handles a failed token provider', async () => {
  expect(await createRide(values, async () => { throw new Error('expired'); }))
    .toMatchObject({ ok: false, authenticationRequired: true });
});

it('maps only recognized string field errors from the backend', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 400, json: async () => ({
    message: 'Invalid Ride Details', errors: { totalSeats: 'Too many seats', origin: {}, other: 'ignored' },
  }) });
  expect(await createRide(values, token)).toEqual({ ok: false, message: 'Invalid Ride Details', errors: { totalSeats: 'Too many seats' } });
});

it.each([401, 403, 500, 502])('handles HTTP %i', async status => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status });
  const result = await createRide(values, token);
  expect(result.ok).toBe(false);
  if (!result.ok) expect(typeof result.message).toBe('string');
});

it('handles a non-JSON error response', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ status: 400, json: async () => { throw new Error('HTML'); } });
  expect(await createRide(values, token)).toMatchObject({ ok: false, errors: {} });
});

it('handles network failure without encouraging an accidental duplicate ride', async () => {
  globalThis.fetch = jest.fn().mockRejectedValue(new TypeError('offline'));
  const result = await createRide(values, token);
  expect(result).toMatchObject({ ok: false, message: expect.stringContaining('Check your rides before retrying') });
});

it('aborts a request after 15 seconds', async () => {
  jest.useFakeTimers();
  globalThis.fetch = jest.fn().mockImplementation((_url: string, init: RequestInit) => new Promise((_resolve, reject) => {
    init?.signal?.addEventListener('abort', () => reject(new Error('timeout')));
  }));
  const pending = createRide(values, token);
  await jest.advanceTimersByTimeAsync(15000);
  expect(await pending).toMatchObject({ ok: false });
});
