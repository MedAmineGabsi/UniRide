This is the mobile app of UniRide (React Native + Expo, TypeScript).

## Running the app

```
cd UniRide_Mobile
npm install
npm start          # then press a/i for an emulator, or scan the QR code with Expo Go
```

The backend URL is read from `EXPO_PUBLIC_API_BASE_URL` (default `http://localhost:8080`, see `src/config.ts`).
On a physical device use your computer's LAN IP; on an Android emulator use `http://10.0.2.2:8080`:

```
EXPO_PUBLIC_API_BASE_URL=http://192.168.1.20:8080 npm start
```

## Tests and type checking

```
npm test           # Jest + React Native Testing Library
npm run typecheck  # tsc --noEmit
```

Form validation lives in `src/validation.ts` (pure functions mirroring the backend `PersonValidator`).
