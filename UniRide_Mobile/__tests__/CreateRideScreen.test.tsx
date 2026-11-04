import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import CreateRideScreen from '../src/CreateRideScreen';
import { API_BASE_URL } from '../src/config';

const token = async () => 'test-token';
const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });
async function fill() {
  for (const [label, value] of [
    ['Origin address', '123 Main St'], ['Destination address', '845 Sherbrooke St'],
    ['Departure date and time', '2028-11-03 11:00'], ['Duration (minutes)', '60'],
    ['Passenger seats offered', '3'], ['Price per seat (CAD)', '12.34'],
  ]) await fireEvent.changeText(screen.getByLabelText(label), value);
}

it('renders the ride creation fields and submit button', async () => {
  globalThis.fetch = jest.fn();
  await render(<CreateRideScreen getAccessToken={token} />);

  expect(screen.getByText('Create a ride')).toBeTruthy();
  for (const label of [
    'Origin address', 'Destination address', 'Departure date and time',
    'Duration (minutes)', 'Passenger seats offered', 'Price per seat (CAD)',
  ]) {
    expect(screen.getByLabelText(label)).toHaveDisplayValue('');
  }
  expect(screen.getByRole('button', { name: 'Create ride' })).toBeEnabled();
  expect(screen.queryByText('This field is required')).toBeNull();
  expect(globalThis.fetch).not.toHaveBeenCalled();
});

it('submits the entered ride details to the backend and displays success', async () => {
  const fetchMock = jest.fn().mockResolvedValue({ status: 201 });
  globalThis.fetch = fetchMock;
  await render(<CreateRideScreen getAccessToken={token} />);
  await fill();
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));

  expect(await screen.findByText('Your ride was created.')).toBeTruthy();
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/rides`, expect.objectContaining({
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer test-token' },
  }));
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    origin: '123 Main St',
    destination: '845 Sherbrooke St',
    departureDateTime: '2028-11-03 11:00',
    durationMinutes: 60,
    totalSeats: 3,
    pricePerSeat: 12.34,
  });
});

it('shows field errors and does not submit an empty form', async () => {
  globalThis.fetch = jest.fn();
  await render(<CreateRideScreen getAccessToken={token} />);
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  expect(screen.getAllByText('This field is required')).toHaveLength(6);
  expect(globalThis.fetch).not.toHaveBeenCalled();
});

it('preserves input after backend errors and lets the driver correct and retry', async () => {
  const fetchMock = jest.fn().mockResolvedValueOnce({ status: 400, json: async () => ({
    message: 'Invalid Ride Details', errors: { totalSeats: 'Too many passenger seats' },
  }) }).mockResolvedValueOnce({ status: 201 });
  globalThis.fetch = fetchMock;
  await render(<CreateRideScreen getAccessToken={token} />);
  await fill();
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  expect(await screen.findByText('Too many passenger seats')).toBeTruthy();
  expect(screen.getByLabelText('Origin address')).toHaveDisplayValue('123 Main St');
  await fireEvent.changeText(screen.getByLabelText('Passenger seats offered'), '2');
  expect(screen.queryByText('Too many passenger seats')).toBeNull();
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  expect(await screen.findByText('Your ride was created.')).toBeTruthy();
  expect(fetchMock).toHaveBeenCalledTimes(2);
  await fireEvent.press(screen.getByRole('button', { name: 'Create another ride' }));
  expect(screen.getByLabelText('Origin address')).toHaveDisplayValue('');
});

it('preserves input when no session exists', async () => {
  await render(<CreateRideScreen getAccessToken={async () => null} />);
  await fill();
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  expect(await screen.findByText('Please sign in before creating a ride.')).toBeTruthy();
  expect(screen.getByLabelText('Price per seat (CAD)')).toHaveDisplayValue('12.34');
});

it('prevents duplicate submissions while a request is pending', async () => {
  let finish!: (response: { status: number }) => void;
  const fetchMock = jest.fn().mockImplementation(() => new Promise(resolve => { finish = resolve; }));
  globalThis.fetch = fetchMock;
  await render(<CreateRideScreen getAccessToken={token} />);
  await fill();
  const submission = fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  expect(screen.getByRole('button', { name: 'Create ride' })).toBeDisabled();
  await fireEvent.press(screen.getByRole('button', { name: 'Create ride' }));
  expect(fetchMock).toHaveBeenCalledTimes(1);
  finish({ status: 201 });
  await submission;
  expect(await screen.findByText('Your ride was created.')).toBeTruthy();
});
