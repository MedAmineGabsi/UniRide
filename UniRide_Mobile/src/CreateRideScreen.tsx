import { useRef, useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from 'react-native';
import { createRide, type AccessTokenProvider } from './rideApi';
import { EMPTY_RIDE, validateRide, type RideErrors, type RideField } from './rideValidation';

const fields: { name: RideField; label: string; hint?: string; numeric?: boolean }[] = [
  { name: 'origin', label: 'Origin address' },
  { name: 'destination', label: 'Destination address' },
  {
    name: 'departureDateTime',
    label: 'Departure date and time',
    hint: 'YYYY-MM-DD HH:mm · Montréal time · at least 1 hour from now',
  },
  { name: 'durationMinutes', label: 'Duration (minutes)', numeric: true },
  { name: 'totalSeats', label: 'Passenger seats offered', numeric: true },
  { name: 'pricePerSeat', label: 'Price per seat (CAD)', numeric: true },
];

interface FieldProps extends TextInputProps {
  label: string;
  name: RideField;
  hint?: string;
  error?: string;
}

function Field({ label, name, hint, error, ...inputProps }: FieldProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      {hint ? <Text>{hint}</Text> : null}
      <TextInput
        accessibilityLabel={label}
        testID={`input-${name}`}
        style={[styles.input, error ? styles.inputError : null]}
        autoCapitalize="none"
        {...inputProps}
      />
      {error ? (
        <Text accessibilityLiveRegion="polite" style={styles.error} testID={`error-${name}`}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

interface CreateRideScreenProps {
  getAccessToken: AccessTokenProvider;
  onSignIn?: () => void;
}

export default function CreateRideScreen({ getAccessToken, onSignIn }: CreateRideScreenProps) {
  const [values, setValues] = useState({ ...EMPTY_RIDE });
  const [errors, setErrors] = useState<RideErrors>({});
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [needsSignIn, setNeedsSignIn] = useState(false);
  // Guard immediately, before React renders the disabled submit button.
  const busy = useRef(false);

  const setField = (name: RideField) => (text: string) => {
    setValues((previous) => ({ ...previous, [name]: text }));
    setErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const onSubmit = async () => {
    if (busy.current) return;
    const found = validateRide(values);
    setErrors(found);
    setMessage('');
    setSuccess(false);
    setNeedsSignIn(false);
    if (Object.keys(found).length > 0) {
      setMessage('Please correct the highlighted fields.');
      return;
    }

    busy.current = true;
    setSubmitting(true);
    try {
      const result = await createRide(values, getAccessToken);
      if (result.ok) {
        setSuccess(true);
        setMessage('Your ride was created.');
      } else {
        setMessage(result.message);
        setErrors(result.errors);
        setNeedsSignIn(!!result.authenticationRequired);
      }
    } catch {
      setMessage('Something went wrong. Your details have been kept.');
    } finally {
      busy.current = false;
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setValues({ ...EMPTY_RIDE });
    setSuccess(false);
    setMessage('');
    setErrors({});
  };

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>Create a ride</Text>
      {fields.map(({ name, label, hint, numeric }) => (
        <Field key={name} name={name} label={label} hint={hint}
          value={values[name]} error={errors[name]} onChangeText={setField(name)}
          editable={!submitting && !success} keyboardType={numeric ? 'decimal-pad' : 'default'} />
      ))}
      {message ? (
        <Text accessibilityRole="alert" accessibilityLiveRegion="polite"
          testID={success ? 'success-message' : 'server-error'}
          style={success ? styles.success : styles.error}>
          {message}
        </Text>
      ) : null}
      {needsSignIn && onSignIn ? (
        <Pressable accessibilityRole="button" onPress={onSignIn}>
          <Text>Sign in</Text>
        </Pressable>
      ) : null}
      <Pressable
        onPress={success ? resetForm : onSubmit}
        disabled={submitting}
        accessibilityRole="button"
        accessibilityLabel={success ? 'Create another ride' : 'Create ride'}
        accessibilityState={{ disabled: submitting, busy: submitting }}
        style={[styles.button, submitting ? styles.buttonDisabled : null]}
      >
        <Text style={styles.buttonText}>
          {submitting ? 'Creating ride…' : success ? 'Create another ride' : 'Create ride'}
        </Text>
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
  button: {
    backgroundColor: '#b71c1c', borderRadius: 6, padding: 14,
    alignItems: 'center', marginTop: 16, marginBottom: 32,
  },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: '#fff', fontWeight: '700' },
});
