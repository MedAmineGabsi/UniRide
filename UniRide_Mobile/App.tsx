import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { Pressable, SafeAreaView, Text, View } from 'react-native';
import RegistrationScreen from './src/RegistrationScreen';
import CreateRideScreen from './src/CreateRideScreen';
import type { AccessTokenProvider } from './src/rideApi';

const noSession: AccessTokenProvider = async () => null;

/** Future authentication can supply a provider without changing the ride form. */
export default function App({ getAccessToken = noSession }: { getAccessToken?: AccessTokenProvider }) {
  const [page, setPage] = useState<'register' | 'ride'>('register');
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={{ flexDirection: 'row', padding: 12, gap: 24 }}>
        <Pressable accessibilityRole="tab" accessibilityState={{ selected: page === 'register' }}
          onPress={() => setPage('register')}><Text>Register</Text></Pressable>
        <Pressable accessibilityRole="tab" accessibilityState={{ selected: page === 'ride' }}
          onPress={() => setPage('ride')}><Text>Create a ride</Text></Pressable>
      </View>
      <View style={{ flex: 1, display: page === 'register' ? 'flex' : 'none' }}>
        <RegistrationScreen />
      </View>
      <View style={{ flex: 1, display: page === 'ride' ? 'flex' : 'none' }}>
        <CreateRideScreen getAccessToken={getAccessToken} />
      </View>
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}
