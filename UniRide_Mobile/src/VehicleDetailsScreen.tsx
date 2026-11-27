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
import {
  EMPTY_VEHICLE,
  VEHICLE_MAX_LENGTH,
  type VehicleErrors,
  type VehicleField,
  type VehicleSubmitter,
  type VehicleValidator,
} from './vehicleForm';

const fields: {
  name: VehicleField;
  label: string;
  hint?: string;
  autoCapitalize: TextInputProps['autoCapitalize'];
  numeric?: boolean;
}[] = [
  { name: 'make', label: 'Make', hint: 'e.g. Toyota', autoCapitalize: 'words' },
  { name: 'model', label: 'Model', hint: 'e.g. Corolla', autoCapitalize: 'words' },
  { name: 'color', label: 'Color', autoCapitalize: 'words' },
  { name: 'licensePlate', label: 'License plate', hint: 'e.g. ABC 123', autoCapitalize: 'characters' },
  {
    name: 'seatCapacity',
    label: 'Seat capacity',
    hint: "Total seats in the car, including the driver's seat",
    autoCapitalize: 'none',
    numeric: true,
  },
];

interface FieldProps extends TextInputProps {
  label: string;
  name: VehicleField;
  hint?: string;
  error?: string;
}

function Field({ label, name, hint, error, ...inputProps }: FieldProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
      <TextInput
        accessibilityLabel={label}
        testID={`input-${name}`}
        style={[styles.input, error ? styles.inputError : null]}
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

/** No client-side checks until the vehicle validation (TID108) is plugged in; the backend still validates. */
const noClientValidation: VehicleValidator = () => ({});

interface VehicleDetailsScreenProps {
  /** Sends the details to the backend (TID109). */
  submitVehicle: VehicleSubmitter;
  /** Client-side checks (TID108). */
  validate?: VehicleValidator;
  onSignIn?: () => void;
}

export default function VehicleDetailsScreen({
  submitVehicle,
  validate = noClientValidation,
  onSignIn,
}: VehicleDetailsScreenProps) {
  const [values, setValues] = useState({ ...EMPTY_VEHICLE });
  const [errors, setErrors] = useState<VehicleErrors>({});
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [needsSignIn, setNeedsSignIn] = useState(false);
  // Guard immediately, before React renders the disabled submit button.
  const busy = useRef(false);

  const setField = (name: VehicleField) => (text: string) => {
    setValues((previous) => ({ ...previous, [name]: text }));
    setErrors((previous) => ({ ...previous, [name]: undefined }));
  };

  const onSubmit = async () => {
    if (busy.current) return;
    const found = validate(values);
    setErrors(found);
    setMessage('');
    setNeedsSignIn(false);
    if (Object.keys(found).length > 0) {
      setMessage('Please correct the highlighted fields.');
      return;
    }

    busy.current = true;
    setSubmitting(true);
    try {
      const result = await submitVehicle(values);
      if (result.ok) {
        setSuccess(true);
        setMessage('Your vehicle details were saved.');
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

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>Vehicle details</Text>
      <Text style={styles.intro}>Passengers will see these details so they know which car to look for.</Text>
      {fields.map(({ name, label, hint, autoCapitalize, numeric }) => (
        <Field
          key={name}
          name={name}
          label={label}
          hint={hint}
          value={values[name]}
          error={errors[name]}
          onChangeText={setField(name)}
          editable={!submitting && !success}
          autoCapitalize={autoCapitalize}
          autoCorrect={false}
          maxLength={VEHICLE_MAX_LENGTH[name]}
          keyboardType={numeric ? 'number-pad' : 'default'}
        />
      ))}
      {message ? (
        <Text
          accessibilityRole="alert"
          accessibilityLiveRegion="polite"
          testID={success ? 'success-message' : 'server-error'}
          style={success ? styles.success : styles.error}
        >
          {message}
        </Text>
      ) : null}
      {needsSignIn && onSignIn ? (
        <Pressable accessibilityRole="button" onPress={onSignIn}>
          <Text>Sign in</Text>
        </Pressable>
      ) : null}
      {success ? null : (
        <Pressable
          onPress={onSubmit}
          disabled={submitting}
          accessibilityRole="button"
          accessibilityLabel="Save vehicle"
          accessibilityState={{ disabled: submitting, busy: submitting }}
          style={[styles.button, submitting ? styles.buttonDisabled : null]}
        >
          <Text style={styles.buttonText}>{submitting ? 'Saving vehicle…' : 'Save vehicle'}</Text>
        </Pressable>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, paddingTop: 48 },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 4 },
  intro: { marginBottom: 16 },
  field: { marginBottom: 12 },
  label: { fontWeight: '600', marginBottom: 4 },
  hint: { color: '#555' },
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
