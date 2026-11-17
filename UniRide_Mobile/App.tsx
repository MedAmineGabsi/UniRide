import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { Pressable, SafeAreaView, Text, View } from 'react-native';
import RegistrationScreen from './src/RegistrationScreen';
import CreateRideScreen from './src/CreateRideScreen';
import type { AccessTokenProvider } from './src/rideApi';
import SeatRequestResponseScreen, { type SeatRequestSummary } from './src/SeatRequestResponseScreen';

const noSession: AccessTokenProvider = async () => null;

/** Future authentication can supply a provider without changing the ride form. */
interface AppProps {
  getAccessToken?: AccessTokenProvider;
  seatRequests?: SeatRequestSummary[];
  onSignIn?: () => void;
}

export default function App({ getAccessToken = noSession, seatRequests = [], onSignIn }: AppProps) {
  const [page, setPage] = useState<'register' | 'ride' | 'seatRequests'>('register');
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={{ flexDirection: 'row', padding: 12, gap: 24 }}>
        <Pressable accessibilityRole="tab" accessibilityState={{ selected: page === 'register' }}
          onPress={() => setPage('register')}><Text>Register</Text></Pressable>
        <Pressable accessibilityRole="tab" accessibilityState={{ selected: page === 'ride' }}
          onPress={() => setPage('ride')}><Text>Create a ride</Text></Pressable>
        <Pressable accessibilityRole="tab" accessibilityState={{ selected: page === 'seatRequests' }}
          onPress={() => setPage('seatRequests')}><Text>Seat requests</Text></Pressable>
      </View>
      <View style={{ flex: 1, display: page === 'register' ? 'flex' : 'none' }}>
        <RegistrationScreen />
      </View>
      <View style={{ flex: 1, display: page === 'ride' ? 'flex' : 'none' }}>
        <CreateRideScreen getAccessToken={getAccessToken} />
      </View>
      <View style={{ flex: 1, display: page === 'seatRequests' ? 'flex' : 'none' }}>
        <SeatRequestResponseScreen requests={seatRequests} getAccessToken={getAccessToken} onSignIn={onSignIn} />
      </View>
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}
