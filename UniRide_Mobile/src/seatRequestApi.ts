import { API_BASE_URL } from './config';
import type { AccessTokenProvider } from './rideApi';

export type SeatRequestResponseStatus = 'ACCEPTED' | 'REJECTED';

export type SeatRequestResponseResult =
  | { ok: true }
  | { ok: false; message: string; authenticationRequired?: boolean };

const REQUEST_TIMEOUT_MS = 15000;

/** PATCHes the driver's decision for one seat request. */
export async function respondToSeatRequest(
  requestId: number,
  status: SeatRequestResponseStatus,
  getAccessToken: AccessTokenProvider,
): Promise<SeatRequestResponseResult> {
  if (!Number.isInteger(requestId) || requestId <= 0) {
    return { ok: false, message: 'This seat request has an invalid ID.' };
  }

  let token: string | null;
  try {
    token = await getAccessToken();
  } catch {
    token = null;
  }
  if (!token) {
    return {
      ok: false,
      message: 'Please sign in before responding to a seat request.',
      authenticationRequired: true,
    };
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    const response = await fetch(`${API_BASE_URL}/seat-requests/${requestId}`, {
      method: 'PATCH',
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ status }),
    });
    if (response.ok) return { ok: true };
    if (response.status === 401) {
      return {
        ok: false,
        message: 'Your session has expired. Please sign in again.',
        authenticationRequired: true,
      };
    }
    if (response.status >= 500) {
      return { ok: false, message: 'The server could not update this request. Please try again later.' };
    }

    try {
      const body: unknown = await response.json();
      const message =
        body && typeof body === 'object' && 'message' in body && typeof body.message === 'string'
          ? body.message
          : null;
      return { ok: false, message: message ?? 'Could not update this seat request.' };
    } catch {
      return { ok: false, message: 'Could not update this seat request.' };
    }
  } catch {
    return { ok: false, message: 'Could not reach the server. Please try again.' };
  } finally {
    clearTimeout(timer);
  }
}
