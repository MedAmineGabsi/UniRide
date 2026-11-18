import { fireEvent, render, screen } from '@testing-library/react-native';
import App from '../App';

it('opens the seat request response screen with the supplied requests', async () => {
  await render(<App
    getAccessToken={async () => 'test-token'}
    seatRequests={[{
      id: 8,
      passengerName: 'Grace Hopper',
      passengerEmail: 'grace@mail.mcgill.ca',
      origin: 'Montreal',
      destination: 'Quebec City',
      departureDateTime: '2026-12-10 08:00',
      seatsRequested: 1,
      status: 'PENDING',
    }]}
  />);

  await fireEvent.press(screen.getByRole('tab', { name: 'Seat requests' }));
  expect(screen.getByText('Grace Hopper')).toBeTruthy();
  expect(screen.getByTestId('accept-request-8')).toBeTruthy();
});
