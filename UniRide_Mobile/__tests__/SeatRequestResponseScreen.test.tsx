import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import SeatRequestResponseScreen, { type SeatRequestSummary } from '../src/SeatRequestResponseScreen';
import { API_BASE_URL } from '../src/config';

const token = async () => 'test-token';
const request: SeatRequestSummary = {
  id: 17,
  passengerName: 'Ada Lovelace',
  passengerEmail: 'ada@mail.mcgill.ca',
  origin: 'Montreal',
  destination: 'Ottawa',
  departureDateTime: '2026-12-01 09:00',
  seatsRequested: 2,
  status: 'PENDING',
};
const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

it('shows an empty state when there are no requests to review', async () => {
  await render(<SeatRequestResponseScreen requests={[]} getAccessToken={token} />);
  expect(screen.getByText('There are no seat requests to review.')).toBeTruthy();
});

it('renders a pending request and accepts it through the API', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 200 });
  const onRequestResponded = jest.fn();
  globalThis.fetch = fetchMock;
  await render(<SeatRequestResponseScreen requests={[request]} getAccessToken={token}
    onRequestResponded={onRequestResponded} />);

  expect(screen.getByText('Ada Lovelace')).toBeTruthy();
  expect(screen.getByText('Montreal to Ottawa')).toBeTruthy();
  expect(screen.getByText('2 seats requested')).toBeTruthy();
  await fireEvent.press(screen.getByTestId('accept-request-17'));

  expect(await screen.findByText('Seat request accepted.')).toBeTruthy();
  expect(screen.getByLabelText('Request status: accepted')).toBeTruthy();
  expect(screen.queryByTestId('accept-request-17')).toBeNull();
  expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/seat-requests/17`, expect.objectContaining({
    method: 'PATCH',
    body: JSON.stringify({ status: 'ACCEPTED' }),
  }));
  expect(onRequestResponded).toHaveBeenCalledWith(17, 'ACCEPTED');
});

it('rejects a pending request and shows a confirmation', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, status: 200 });
  await render(<SeatRequestResponseScreen requests={[request]} getAccessToken={token} />);

  await fireEvent.press(screen.getByTestId('reject-request-17'));
  expect(await screen.findByText('Seat request rejected.')).toBeTruthy();
  expect(screen.getByLabelText('Request status: rejected')).toBeTruthy();
});

it('shows backend validation errors and leaves the request pending', async () => {
  globalThis.fetch = jest.fn().mockResolvedValue({
    ok: false,
    status: 400,
    json: async () => ({ message: 'Not enough seats available' }),
  });
  await render(<SeatRequestResponseScreen requests={[request]} getAccessToken={token} />);

  await fireEvent.press(screen.getByTestId('accept-request-17'));
  expect(await screen.findByText('Not enough seats available')).toBeTruthy();
  expect(screen.getByLabelText('Request status: pending')).toBeTruthy();
  expect(screen.getByTestId('accept-request-17')).toBeTruthy();
});

it('offers sign-in when the session is missing', async () => {
  const onSignIn = jest.fn();
  const fetchMock = jest.fn();
  globalThis.fetch = fetchMock;
  await render(<SeatRequestResponseScreen requests={[request]} getAccessToken={async () => null}
    onSignIn={onSignIn} />);

  await fireEvent.press(screen.getByTestId('accept-request-17'));
  expect(await screen.findByText('Please sign in before responding to a seat request.')).toBeTruthy();
  await fireEvent.press(screen.getByRole('button', { name: 'Sign in' }));
  expect(onSignIn).toHaveBeenCalledTimes(1);
  expect(fetchMock).not.toHaveBeenCalled();
});

it('prevents duplicate responses while the request is pending', async () => {
  let finish!: (response: { ok: boolean; status: number }) => void;
  const fetchMock = jest.fn().mockImplementation(() => new Promise(resolve => { finish = resolve; }));
  globalThis.fetch = fetchMock;
  await render(<SeatRequestResponseScreen requests={[request]} getAccessToken={token} />);

  const accept = screen.getByTestId('accept-request-17');
  const submission = fireEvent.press(accept);
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  await fireEvent.press(accept);
  expect(fetchMock).toHaveBeenCalledTimes(1);

  finish({ ok: true, status: 200 });
  await submission;
  expect(await screen.findByText('Seat request accepted.')).toBeTruthy();
});

it('does not show response actions for an already answered request', async () => {
  await render(<SeatRequestResponseScreen requests={[{ ...request, status: 'ACCEPTED' }]}
    getAccessToken={token} />);
  expect(screen.getByLabelText('Request status: accepted')).toBeTruthy();
  expect(screen.queryByTestId('accept-request-17')).toBeNull();
  expect(screen.queryByTestId('reject-request-17')).toBeNull();
});
