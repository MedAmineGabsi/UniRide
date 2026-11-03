import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import * as ImagePicker from 'expo-image-picker';
import RegistrationScreen from '../src/RegistrationScreen';
import { API_BASE_URL } from '../src/config';

jest.mock('expo-image-picker', () => ({
  launchImageLibraryAsync: jest.fn(),
}));

const FIELD_LABELS = [
  'First name',
  'Last name',
  'Student ID',
  'Email',
  'Phone number',
  'Password',
  'Confirm password',
];

async function fillValidPassenger() {
  await fireEvent.changeText(screen.getByLabelText('First name'), 'Ada');
  await fireEvent.changeText(screen.getByLabelText('Last name'), 'Lovelace');
  await fireEvent.changeText(screen.getByLabelText('Student ID'), '260123456');
  await fireEvent.changeText(screen.getByLabelText('Email'), 'ada@mail.mcgill.ca');
  await fireEvent.changeText(screen.getByLabelText('Phone number'), '5145551234');
  await fireEvent.changeText(screen.getByLabelText('Password'), 'Passw0rd!');
  await fireEvent.changeText(screen.getByLabelText('Confirm password'), 'Passw0rd!');
  await fireEvent.press(screen.getByRole('checkbox', { name: 'Passenger' }));
}

beforeEach(() => {
  jest.resetAllMocks();
});

describe('RegistrationScreen rendering', () => {
  it('renders all fields, role checkboxes and the submit button', async () => {
    await render(<RegistrationScreen />);
    for (const label of FIELD_LABELS) {
      expect(screen.getByLabelText(label)).toBeTruthy();
    }
    expect(screen.getByRole('checkbox', { name: 'Passenger' })).toBeTruthy();
    expect(screen.getByRole('checkbox', { name: 'Driver' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Register' })).toBeTruthy();
  });

  it('shows and hides driver fields with the Driver checkbox', async () => {
    await render(<RegistrationScreen />);
    expect(screen.queryByLabelText('License number')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Choose license picture' })).toBeNull();

    await fireEvent.press(screen.getByRole('checkbox', { name: 'Driver' }));
    expect(screen.getByLabelText('License number')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Choose license picture' })).toBeTruthy();

    await fireEvent.press(screen.getByRole('checkbox', { name: 'Driver' }));
    expect(screen.queryByLabelText('License number')).toBeNull();
  });

  it('shows the chosen license picture preview', async () => {
    (ImagePicker.launchImageLibraryAsync as jest.Mock).mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'file:///license.jpg' }],
    });
    await render(<RegistrationScreen />);
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Driver' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Choose license picture' }));
    expect(await screen.findByTestId('license-image-preview')).toBeTruthy();
  });
});

describe('RegistrationScreen validation', () => {
  it('shows required errors for empty fields and does not call the backend', async () => {
    const fetchMock = jest.fn();
    globalThis.fetch = fetchMock;
    await render(<RegistrationScreen />);
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    expect(screen.getByText('First name is required')).toBeTruthy();
    expect(screen.getByText('Last name is required')).toBeTruthy();
    expect(screen.getByText('Student ID is required')).toBeTruthy();
    expect(screen.getByText('Email is required')).toBeTruthy();
    expect(screen.getByText('Phone number is required')).toBeTruthy();
    expect(screen.getByText('Password is required')).toBeTruthy();
    expect(screen.getByText('Select at least one role')).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('shows errors for invalid email, weak password, mismatch and bad license', async () => {
    globalThis.fetch = jest.fn();
    await render(<RegistrationScreen />);
    await fillValidPassenger();
    await fireEvent.changeText(screen.getByLabelText('Email'), 'ada@gmail.com');
    await fireEvent.changeText(screen.getByLabelText('Password'), 'weak');
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Driver' }));
    await fireEvent.changeText(screen.getByLabelText('License number'), '12345');
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    expect(screen.getByTestId('error-email')).toHaveTextContent(/McGill/);
    expect(screen.getByTestId('error-password')).toHaveTextContent(/at least 8/);
    expect(screen.getByTestId('error-passwordConfirmation')).toHaveTextContent('Passwords do not match');
    expect(screen.getByTestId('error-licenseNumber')).toHaveTextContent(/Quebec format/);
  });
});

describe('RegistrationScreen submission', () => {
  it('POSTs the JSON body and shows success', async () => {
    const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 201 });
    globalThis.fetch = fetchMock;
    await render(<RegistrationScreen />);
    await fillValidPassenger();
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    await waitFor(() => expect(screen.getByTestId('success-message')).toBeTruthy());
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe(`${API_BASE_URL}/api/users/register`);
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({
      firstName: 'Ada',
      lastName: 'Lovelace',
      studentId: '260123456',
      email: 'ada@mail.mcgill.ca',
      phoneNumber: '5145551234',
      password: 'Passw0rd!',
      passwordConfirmation: 'Passw0rd!',
      roles: ['PASSENGER'],
    });
  });

  it('includes licenseNumber and DRIVER role for drivers', async () => {
    const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 201 });
    globalThis.fetch = fetchMock;
    await render(<RegistrationScreen />);
    await fillValidPassenger();
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Driver' }));
    await fireEvent.changeText(screen.getByLabelText('License number'), 'A1234-567890-12');
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.roles).toEqual(['PASSENGER', 'DRIVER']);
    expect(body.licenseNumber).toBe('A1234-567890-12');
  });

  it('displays the backend error message (409 Already registered)', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({ message: 'Already registered', errors: {} }),
    });
    await render(<RegistrationScreen />);
    await fillValidPassenger();
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    expect(await screen.findByTestId('server-error')).toHaveTextContent('Already registered');
    expect(screen.queryByTestId('success-message')).toBeNull();
  });

  it('displays a message when the server is unreachable', async () => {
    globalThis.fetch = jest.fn().mockRejectedValue(new TypeError('Network request failed'));
    await render(<RegistrationScreen />);
    await fillValidPassenger();
    await fireEvent.press(screen.getByRole('button', { name: 'Register' }));

    expect(await screen.findByTestId('server-error')).toHaveTextContent(/Could not reach the server/);
  });
});
