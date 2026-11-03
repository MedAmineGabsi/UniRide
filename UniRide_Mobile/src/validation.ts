/**
 * Pure client-side validation for the registration form. Mirrors the rules of the backend
 * (UniRide_Backend service/PersonValidator.java) so users get feedback before submitting.
 * The backend remains the source of truth.
 */

export type Role = 'PASSENGER' | 'DRIVER';

export interface RegistrationFormValues {
  firstName: string;
  lastName: string;
  studentId: string;
  email: string;
  phoneNumber: string;
  password: string;
  passwordConfirmation: string;
  roles: Role[];
  licenseNumber: string;
}

export type FieldName = keyof RegistrationFormValues;
export type FormErrors = Partial<Record<FieldName, string>>;

export const EMAIL_FORMAT = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
export const MCGILL_DOMAINS = ['mail.mcgill.ca', 'mcgill.ca'];
export const MAX_EMAIL_LENGTH = 255;
export const MIN_PASSWORD_LENGTH = 8;
/** BCrypt only reads the first 72 bytes of a password. */
export const MAX_PASSWORD_BYTES = 72;
/** Quebec (SAAQ) license: a letter then 12 digits, A1234-567890-12. The dashes are optional. */
export const LICENSE_NUMBER_FORMAT = /^([A-Z])(\d{4})-?(\d{6})-?(\d{2})$/;

const isBlank = (value: string): boolean => value.trim().length === 0;

export function utf8ByteLength(value: string): number {
  let bytes = 0;
  for (const char of value) {
    const code = char.codePointAt(0) as number;
    bytes += code < 0x80 ? 1 : code < 0x800 ? 2 : code < 0x10000 ? 3 : 4;
  }
  return bytes;
}

export function validateEmail(email: string): string | undefined {
  if (isBlank(email)) return 'Email is required';
  const normalized = email.trim().toLowerCase();
  if (normalized.length > MAX_EMAIL_LENGTH || !EMAIL_FORMAT.test(normalized)) {
    return 'Invalid email format';
  }
  const domain = normalized.substring(normalized.indexOf('@') + 1);
  if (!MCGILL_DOMAINS.includes(domain)) {
    return 'Email must be a McGill address (@mail.mcgill.ca or @mcgill.ca)';
  }
  return undefined;
}

export function isStrongPassword(password: string): boolean {
  return (
    password.length >= MIN_PASSWORD_LENGTH &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9]/.test(password)
  );
}

export function validatePassword(password: string): string | undefined {
  if (password.trim().length === 0) return 'Password is required';
  if (!isStrongPassword(password)) {
    return 'Password must be at least 8 characters with an uppercase letter, a lowercase letter, a digit and a symbol';
  }
  if (utf8ByteLength(password) > MAX_PASSWORD_BYTES) {
    return 'Password is too long (maximum 72 bytes)';
  }
  return undefined;
}

export function validatePasswordConfirmation(
  password: string,
  passwordConfirmation: string,
): string | undefined {
  if (passwordConfirmation.length === 0) return 'Please confirm your password';
  if (password !== passwordConfirmation) return 'Passwords do not match';
  return undefined;
}

export function validateLicenseNumber(licenseNumber: string): string | undefined {
  if (isBlank(licenseNumber)) return 'License number is required';
  if (!LICENSE_NUMBER_FORMAT.test(licenseNumber.trim().toUpperCase())) {
    return 'License number must be in the Quebec format A1234-567890-12';
  }
  return undefined;
}

export function validateRoles(roles: Role[]): string | undefined {
  if (roles.length === 0) return 'Select at least one role';
  return undefined;
}

/** Validates the whole form. Returns an empty object when everything is valid. */
export function validateRegistration(values: RegistrationFormValues): FormErrors {
  const errors: FormErrors = {};
  const set = (field: FieldName, message: string | undefined) => {
    if (message) errors[field] = message;
  };

  set('firstName', isBlank(values.firstName) ? 'First name is required' : undefined);
  set('lastName', isBlank(values.lastName) ? 'Last name is required' : undefined);
  set('studentId', isBlank(values.studentId) ? 'Student ID is required' : undefined);
  set('email', validateEmail(values.email));
  set('phoneNumber', isBlank(values.phoneNumber) ? 'Phone number is required' : undefined);
  set('password', validatePassword(values.password));
  set(
    'passwordConfirmation',
    validatePasswordConfirmation(values.password, values.passwordConfirmation),
  );
  set('roles', validateRoles(values.roles));
  if (values.roles.includes('DRIVER')) {
    set('licenseNumber', validateLicenseNumber(values.licenseNumber));
  }
  return errors;
}
