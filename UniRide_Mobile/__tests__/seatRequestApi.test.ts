import { API_BASE_URL } from '../src/config';
import { respondToSeatRequest } from '../src/seatRequestApi';

const token = async () => 'test-token';
const originalFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = originalFetch;
  jest.useRealTimers();
});

it('PATCHes the request status with the bearer token', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 200 });
  globalThis.fetch = fetchMock;

  expect(await respondToSeatRequest(42, 'ACCEPTED', token)).toEqual({ ok: true });
  expect(fetchMock).toHaveBeenCalledWith(
    `${API_BASE_URL}/seat-requests/42`,
    expect.objectContaining({
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer test-token' },
      body: JSON.stringify({ status: 'ACCEPTED' }),
    }),
  );
});

it('sends a rejection using the same endpoint contract', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 200 });
  globalThis.fetch = fetchMock;

  expect(await respondToSeatRequest(42, 'REJECTED', token)).toEqual({ ok: true });
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ status: 'REJECTED' });
});

it('does not send a request without a valid id or authentication', async () => {
  const fetchMock = jest.fn();
  globalThis.fetch = fetchMock;

  expect(await respondToSeatRequest(0, 'ACCEPTED', token)).toMatchObject({ ok: false });
  expect(await respondToSeatRequest(42, 'ACCEPTED', async () => null))
    .toMatchObject({ ok: false, authenticationRequired: true });
  expect(await respondToSeatRequest(42, 'ACCEPTED', async () => { throw new Error('expired'); }))
    .toMatchObject({ ok: false, authenticationRequired: true });
  expect(fetchMock).not.toHaveBeenCalled();
});

it('returns backend validation errors', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({
    ok: false,
    status: 400,
    json: async () => ({ message: 'Not enough seats available' }),
  });

  expect(await respondToSeatRequest(42, 'ACCEPTED', token))
    .toEqual({ ok: false, message: 'Not enough seats available' });
});

it('handles an expired session and forbidden response', async () => {
  globalThis.fetch = jest.fn()
    .mockResolvedValueOnce({ ok: false, status: 401 })
    .mockResolvedValueOnce({
      ok: false,
      status: 403,
      json: async () => ({ message: 'Only the ride owner can respond to seat requests' }),
    });

  expect(await respondToSeatRequest(42, 'ACCEPTED', token))
    .toMatchObject({ ok: false, authenticationRequired: true });
  expect(await respondToSeatRequest(42, 'ACCEPTED', token))
    .toEqual({ ok: false, message: 'Only the ride owner can respond to seat requests' });
});

it.each([500, 503])('handles server errors (HTTP %i)', async status => {
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: false, status });
  expect(await respondToSeatRequest(42, 'REJECTED', token))
    .toMatchObject({ ok: false, message: expect.stringContaining('server') });
});

it('handles invalid error payloads and network failures', async () => {
  globalThis.fetch = jest.fn()
    .mockResolvedValueOnce({ ok: false, status: 400, json: async () => { throw new Error('not JSON'); } })
    .mockRejectedValueOnce(new TypeError('offline'));

  expect(await respondToSeatRequest(42, 'ACCEPTED', token))
    .toMatchObject({ ok: false, message: expect.stringContaining('Could not update') });
  expect(await respondToSeatRequest(42, 'ACCEPTED', token)).toMatchObject({ ok: false });
});

it('aborts a request after 15 seconds', async () => {
  jest.useFakeTimers();
  globalThis.fetch = jest.fn().mockImplementation((_url: string, init: RequestInit) =>
    new Promise((_resolve, reject) => {
      init.signal?.addEventListener('abort', () => reject(new Error('timeout')));
    }),
  );

  const pending = respondToSeatRequest(42, 'ACCEPTED', token);
  await jest.advanceTimersByTimeAsync(15000);
  expect(await pending).toMatchObject({ ok: false });
});
