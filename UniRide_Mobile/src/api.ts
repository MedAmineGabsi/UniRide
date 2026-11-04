import { API_BASE_URL } from './config';
import type { RegistrationFormValues, Role } from './validation';

/** Matches UniRide_Backend dto/RegisterUserRequest.java. */
export interface RegisterUserRequest {
  firstName: string;
  lastName: string;
  studentId: string;
  email: string;
  phoneNumber: string;
  password: string;
  passwordConfirmation: string;
  roles: Role[];
  licenseNumber?: string;
}

export type RegisterResult =
  | { ok: true }
  | { ok: false; message: string; errors: Record<string, string> };

export function buildRegisterRequest(values: RegistrationFormValues): RegisterUserRequest {
  const isDriver = values.roles.includes('DRIVER');
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    studentId: values.studentId.trim(),
    email: values.email.trim(),
    phoneNumber: values.phoneNumber.trim(),
    password: values.password,
    passwordConfirmation: values.passwordConfirmation,
    roles: values.roles,
    ...(isDriver ? { licenseNumber: values.licenseNumber.trim() } : {}),
  };
}

/** POSTs the registration. Never throws: failures are returned as { ok: false, message }. */
export async function registerUser(values: RegistrationFormValues): Promise<RegisterResult> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/api/users/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(buildRegisterRequest(values)),
    });
  } catch {
    return { ok: false, message: 'Could not reach the server. Please try again.', errors: {} };
  }
  if (response.ok) return { ok: true };

  // ErrorResponse { message, errors }
  try {
    const body = await response.json();
    return {
      ok: false,
      message: body?.message || `Registration failed (${response.status})`,
      errors: body?.errors ?? {},
    };
  } catch {
    return { ok: false, message: `Registration failed (${response.status})`, errors: {} };
  }
}
