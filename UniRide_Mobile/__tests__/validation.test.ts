import {
  utf8ByteLength,
  validateEmail,
  validateLicenseNumber,
  validatePassword,
  validatePasswordConfirmation,
  validateRegistration,
  type RegistrationFormValues,
} from '../src/validation';

const valid: RegistrationFormValues = {
  firstName: 'Ada',
  lastName: 'Lovelace',
  studentId: '260123456',
  email: 'ada@mail.mcgill.ca',
  phoneNumber: '5145551234',
  password: 'Passw0rd!',
  passwordConfirmation: 'Passw0rd!',
  roles: ['PASSENGER'],
  licenseNumber: '',
};

describe('validateRegistration', () => {
  it('accepts a valid passenger', () => {
    expect(validateRegistration(valid)).toEqual({});
  });

  it('requires every field and a role', () => {
    const errors = validateRegistration({
      ...valid,
      firstName: '',
      lastName: ' ',
      studentId: '',
      email: '',
      phoneNumber: '',
      password: '',
      passwordConfirmation: '',
      roles: [],
    });
    expect(Object.keys(errors).sort()).toEqual(
      ['email', 'firstName', 'lastName', 'password', 'passwordConfirmation', 'phoneNumber', 'roles', 'studentId'],
    );
  });

  it('requires a license number only for drivers', () => {
    expect(validateRegistration({ ...valid, roles: ['DRIVER'] }).licenseNumber).toBe(
      'License number is required',
    );
    expect(validateRegistration({ ...valid, roles: ['PASSENGER'] }).licenseNumber).toBeUndefined();
  });
});

describe('validateEmail', () => {
  it.each(['a@mail.mcgill.ca', 'a@mcgill.ca', 'A@MCGILL.CA'])('accepts %s', (e) => {
    expect(validateEmail(e)).toBeUndefined();
  });
  it('rejects bad format and non-McGill domains', () => {
    expect(validateEmail('nope')).toBe('Invalid email format');
    expect(validateEmail('a@gmail.com')).toMatch(/McGill/);
  });
});

describe('validatePassword', () => {
  it('rejects weak passwords', () => {
    for (const p of ['Sh0rt!', 'alllower1!', 'ALLUPPER1!', 'NoDigits!!', 'NoSymbol123']) {
      expect(validatePassword(p)).toMatch(/at least 8/);
    }
  });
  it('rejects more than 72 bytes', () => {
    const long = 'Aa1!' + 'a'.repeat(69);
    expect(validatePassword(long)).toMatch(/too long/);
    expect(validatePassword('Aa1!' + 'a'.repeat(68))).toBeUndefined();
  });
  it('counts bytes, not characters', () => {
    expect(utf8ByteLength('é')).toBe(2);
    expect(utf8ByteLength('€')).toBe(3);
    expect(utf8ByteLength('😀')).toBe(4);
  });
  it('checks confirmation match', () => {
    expect(validatePasswordConfirmation('a', 'b')).toBe('Passwords do not match');
    expect(validatePasswordConfirmation('a', 'a')).toBeUndefined();
  });
});

describe('validateLicenseNumber', () => {
  it.each(['A1234-567890-12', 'a1234567890 12'.replace(' ', ''), 'A1234567890-12'])('accepts %s', (l) => {
    expect(validateLicenseNumber(l)).toBeUndefined();
  });
  it.each(['1234-567890-12', 'A123-567890-12', 'AB234-567890-12'])('rejects %s', (l) => {
    expect(validateLicenseNumber(l)).toMatch(/Quebec format/);
  });
});
