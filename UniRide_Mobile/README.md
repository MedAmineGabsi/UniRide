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

## Ride creation (TID041 / #47 and TID042 / #48)

Use the **Create a ride** tab to enter origin and destination addresses, departure
(`YYYY-MM-DD HH:mm`, Montréal time), duration in minutes, passenger seats, and price
per seat in CAD. Required and numeric errors appear inline. The backend remains
responsible for address rules, valid dates, the one-hour departure rule, and car capacity.
Its field errors appear beside the corresponding inputs. Failed requests preserve
all entered values; submission is disabled while a request is pending. After success,
choose **Create another ride** to start a fresh form.

### Authentication integration

Sign-in is intentionally deferred. `CreateRideScreen` accepts a `getAccessToken`
function returning a current bearer access token (or `null`). `App` accepts the same
optional provider and passes it to the screen. The default returns `null`: you can
open and validate the form, but authenticated ride creation requires the future
session integration. Missing/expired sessions show a message and preserve the form.
Do not place tokens in source code or public Expo environment variables.

The API client POSTs to `/api/rides` using the backend's `CreateRideRequest` fields.
It handles field errors, 401/403, server errors, malformed error responses, network
failure, and a 15-second timeout. A network failure may follow a successful server
write, so the message advises checking existing rides before retrying.

### Verification

```sh
npm ci
npm run typecheck
npm test -- --runInBand
```

Tests cover the screen's validation, preserved inputs, correction/retry, successful
submission, duplicate submission prevention, and the actual fetch request contract.
API tests cover authentication, backend field errors, HTTP failures and timeout.
An authenticated device-to-backend check still requires a session provider and a
running backend. No sign-in flow is included in this change.
