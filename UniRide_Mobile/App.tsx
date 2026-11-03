import { StatusBar } from 'expo-status-bar';
import { SafeAreaView } from 'react-native';
import RegistrationScreen from './src/RegistrationScreen';

export default function App() {
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <RegistrationScreen />
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}
