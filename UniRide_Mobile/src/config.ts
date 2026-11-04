/**
 * Base URL of the UniRide backend. Override with the EXPO_PUBLIC_API_BASE_URL environment variable
 * (e.g. http://192.168.1.20:8080 when testing on a physical device; 10.0.2.2 reaches the host from an
 * Android emulator).
 */
export const API_BASE_URL: string =
  process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
