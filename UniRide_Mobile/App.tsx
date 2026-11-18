import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { Pressable, SafeAreaView, Text, View } from 'react-native';
import RegistrationScreen from './src/RegistrationScreen';
import CreateRideScreen from './src/CreateRideScreen';
import VehicleDetailsScreen from './src/VehicleDetailsScreen';
import type { AccessTokenProvider } from './src/rideApi';
import type { VehicleSubmitter } from './src/vehicleForm';

const noSession: AccessTokenProvider = async () => null;

/** Placeholder until the add_vehicle API call (TID109) is merged. */
const vehicleApiNotReady: VehicleSubmitter = async () => ({
  ok: false,
  message: 'Saving vehicle details is not available yet.',
  errors: {},
});

type Page = 'register' | 'ride' | 'vehicle';

const tabs: { page: Page; label: string }[] = [
  { page: 'register', label: 'Register' },
  { page: 'ride', label: 'Create a ride' },
  { page: 'vehicle', label: 'Vehicle details' },
];

/** Future authentication can supply a provider without changing the ride form. */
export default function App({
  getAccessToken = noSession,
  submitVehicle = vehicleApiNotReady,
}: {
  getAccessToken?: AccessTokenProvider;
  submitVehicle?: VehicleSubmitter;
}) {
  const [page, setPage] = useState<Page>('register');
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={{ flexDirection: 'row', padding: 12, gap: 24 }}>
        {tabs.map((tab) => (
          <Pressable key={tab.page} accessibilityRole="tab" accessibilityState={{ selected: page === tab.page }}
            onPress={() => setPage(tab.page)}><Text>{tab.label}</Text></Pressable>
        ))}
      </View>
      <View style={{ flex: 1, display: page === 'register' ? 'flex' : 'none' }}>
        <RegistrationScreen />
      </View>
      <View style={{ flex: 1, display: page === 'ride' ? 'flex' : 'none' }}>
        <CreateRideScreen getAccessToken={getAccessToken} />
      </View>
      <View style={{ flex: 1, display: page === 'vehicle' ? 'flex' : 'none' }}>
        <VehicleDetailsScreen submitVehicle={submitVehicle} />
      </View>
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}
