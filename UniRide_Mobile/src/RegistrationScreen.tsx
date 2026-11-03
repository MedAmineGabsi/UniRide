import { useState } from 'react';
import {
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { registerUser } from './api';
import {
  validateRegistration,
  type FieldName,
  type FormErrors,
  type RegistrationFormValues,
  type Role,
} from './validation';

const INITIAL_VALUES: RegistrationFormValues = {
  firstName: '',
  lastName: '',
  studentId: '',
  email: '',
  phoneNumber: '',
  password: '',
  passwordConfirmation: '',
  roles: [],
  licenseNumber: '',
};

interface FieldProps extends TextInputProps {
  label: string;
  name: FieldName;
  error?: string;
}

function Field({ label, name, error, ...inputProps }: FieldProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        testID={`input-${name}`}
        style={[styles.input, error ? styles.inputError : null]}
        autoCapitalize="none"
        {...inputProps}
      />
      {error ? (
        <Text style={styles.error} testID={`error-${name}`}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

function Checkbox({
  label,
  checked,
  onToggle,
}: {
  label: string;
  checked: boolean;
  onToggle: () => void;
}) {
  return (
    <Pressable
      onPress={onToggle}
      accessibilityRole="checkbox"
      accessibilityLabel={label}
      accessibilityState={{ checked }}
      style={styles.checkboxRow}
    >
      <View style={[styles.checkbox, checked ? styles.checkboxChecked : null]}>
        {checked ? <Text style={styles.checkmark}>✓</Text> : null}
      </View>
      <Text>{label}</Text>
    </Pressable>
  );
}

export default function RegistrationScreen() {
  const [values, setValues] = useState<RegistrationFormValues>(INITIAL_VALUES);
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  // UI-only for now: the backend ignores the license picture.
  const [licenseImageUri, setLicenseImageUri] = useState<string | null>(null);

  const isDriver = values.roles.includes('DRIVER');

  const setField = (name: FieldName) => (text: string) =>
    setValues((prev) => ({ ...prev, [name]: text }));

  const toggleRole = (role: Role) =>
    setValues((prev) => ({
      ...prev,
      roles: prev.roles.includes(role)
        ? prev.roles.filter((r) => r !== role)
        : [...prev.roles, role],
    }));

  const pickLicenseImage = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      setLicenseImageUri(result.assets[0].uri);
    }
  };

  const onSubmit = async () => {
    setServerError(null);
    setSuccess(false);
    const found = validateRegistration(values);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      const result = await registerUser(values);
      if (result.ok) {
        setSuccess(true);
      } else {
        setServerError(result.message);
        // Show field-specific backend errors inline when the field is on this form.
        const fieldErrors: FormErrors = {};
        for (const [field, message] of Object.entries(result.errors)) {
          if (field in INITIAL_VALUES) fieldErrors[field as FieldName] = message;
        }
        setErrors(fieldErrors);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>Create your UniRide account</Text>

      <Field label="First name" name="firstName" value={values.firstName}
        onChangeText={setField('firstName')} error={errors.firstName} autoCapitalize="words" />
      <Field label="Last name" name="lastName" value={values.lastName}
        onChangeText={setField('lastName')} error={errors.lastName} autoCapitalize="words" />
      <Field label="Student ID" name="studentId" value={values.studentId}
        onChangeText={setField('studentId')} error={errors.studentId} keyboardType="number-pad" />
      <Field label="Email" name="email" value={values.email}
        onChangeText={setField('email')} error={errors.email} keyboardType="email-address" />
      <Field label="Phone number" name="phoneNumber" value={values.phoneNumber}
        onChangeText={setField('phoneNumber')} error={errors.phoneNumber} keyboardType="phone-pad" />
      <Field label="Password" name="password" value={values.password}
        onChangeText={setField('password')} error={errors.password} secureTextEntry />
      <Field label="Confirm password" name="passwordConfirmation" value={values.passwordConfirmation}
        onChangeText={setField('passwordConfirmation')} error={errors.passwordConfirmation} secureTextEntry />

      <Text style={styles.label}>I am a</Text>
      <Checkbox label="Passenger" checked={values.roles.includes('PASSENGER')}
        onToggle={() => toggleRole('PASSENGER')} />
      <Checkbox label="Driver" checked={isDriver} onToggle={() => toggleRole('DRIVER')} />
      {errors.roles ? (
        <Text style={styles.error} testID="error-roles">{errors.roles}</Text>
      ) : null}

      {isDriver ? (
        <View testID="driver-section">
          <Field label="License number" name="licenseNumber" value={values.licenseNumber}
            onChangeText={setField('licenseNumber')} error={errors.licenseNumber}
            placeholder="A1234-567890-12" autoCapitalize="characters" />
          <Pressable
            onPress={pickLicenseImage}
            accessibilityRole="button"
            accessibilityLabel="Choose license picture"
            style={styles.secondaryButton}
          >
            <Text>{licenseImageUri ? 'Change license picture' : 'Choose license picture'}</Text>
          </Pressable>
          {licenseImageUri ? (
            <Image source={{ uri: licenseImageUri }} style={styles.preview}
              testID="license-image-preview" />
          ) : null}
        </View>
      ) : null}

      {serverError ? (
        <Text style={styles.error} testID="server-error">{serverError}</Text>
      ) : null}
      {success ? (
        <Text style={styles.success} testID="success-message">Registration successful!</Text>
      ) : null}

      <Pressable
        onPress={onSubmit}
        disabled={submitting}
        accessibilityRole="button"
        accessibilityLabel="Register"
        style={[styles.button, submitting ? styles.buttonDisabled : null]}
      >
        <Text style={styles.buttonText}>{submitting ? 'Registering...' : 'Register'}</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, paddingTop: 48 },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 16 },
  field: { marginBottom: 12 },
  label: { fontWeight: '600', marginBottom: 4 },
  input: { borderWidth: 1, borderColor: '#999', borderRadius: 6, padding: 10 },
  inputError: { borderColor: '#c62828' },
  error: { color: '#c62828', marginTop: 4 },
  success: { color: '#2e7d32', marginVertical: 8 },
  checkboxRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 6 },
  checkbox: {
    width: 22, height: 22, borderWidth: 1, borderColor: '#555', borderRadius: 4,
    marginRight: 8, alignItems: 'center', justifyContent: 'center',
  },
  checkboxChecked: { backgroundColor: '#d9e8ff' },
  checkmark: { fontWeight: '700' },
  secondaryButton: {
    borderWidth: 1, borderColor: '#555', borderRadius: 6, padding: 10,
    alignItems: 'center', marginVertical: 8,
  },
  preview: { width: 160, height: 100, marginBottom: 8 },
  button: {
    backgroundColor: '#b71c1c', borderRadius: 6, padding: 14,
    alignItems: 'center', marginTop: 16, marginBottom: 32,
  },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: '#fff', fontWeight: '700' },
});
