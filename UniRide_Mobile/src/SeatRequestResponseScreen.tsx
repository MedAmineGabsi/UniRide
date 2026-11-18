import { useEffect, useRef, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  respondToSeatRequest,
  type SeatRequestResponseStatus,
} from './seatRequestApi';
import type { AccessTokenProvider } from './rideApi';

export type SeatRequestStatus = SeatRequestResponseStatus | 'PENDING' | 'CANCELLED';

export interface SeatRequestSummary {
  id: number;
  passengerName: string;
  passengerEmail: string;
  origin: string;
  destination: string;
  departureDateTime: string;
  seatsRequested: number;
  status: SeatRequestStatus;
}

interface SeatRequestResponseScreenProps {
  requests: SeatRequestSummary[];
  getAccessToken: AccessTokenProvider;
  onSignIn?: () => void;
  onRequestResponded?: (requestId: number, status: SeatRequestResponseStatus) => void;
}

interface SeatRequestCardProps extends Omit<SeatRequestResponseScreenProps, 'requests'> {
  request: SeatRequestSummary;
  onSuccess: (message: string) => void;
}

function SeatRequestCard({
  request,
  getAccessToken,
  onSignIn,
  onRequestResponded,
  onSuccess,
}: SeatRequestCardProps) {
  const [status, setStatus] = useState(request.status);
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [needsSignIn, setNeedsSignIn] = useState(false);
  const busy = useRef(false);

  const respond = async (newStatus: SeatRequestResponseStatus) => {
    if (busy.current || status !== 'PENDING') return;

    busy.current = true;
    setSubmitting(true);
    setMessage('');
    setNeedsSignIn(false);
    try {
      const result = await respondToSeatRequest(request.id, newStatus, getAccessToken);
      if (result.ok) {
        setStatus(newStatus);
        onSuccess(newStatus === 'ACCEPTED' ? 'Seat request accepted.' : 'Seat request rejected.');
        onRequestResponded?.(request.id, newStatus);
      } else {
        setMessage(result.message);
        setNeedsSignIn(!!result.authenticationRequired);
      }
    } finally {
      busy.current = false;
      setSubmitting(false);
    }
  };

  return (
    <View style={styles.card} testID={`seat-request-${request.id}`}>
      <Text style={styles.passengerName}>{request.passengerName}</Text>
      <Text>{request.passengerEmail}</Text>
      <Text>{request.origin} to {request.destination}</Text>
      <Text>{request.departureDateTime}</Text>
      <Text>{request.seatsRequested} {request.seatsRequested === 1 ? 'seat' : 'seats'} requested</Text>
      <Text accessibilityLabel={`Request status: ${status.toLowerCase()}`} style={styles.status}>
        Status: {status.toLowerCase()}
      </Text>

      {status === 'PENDING' ? (
        <View style={styles.actions}>
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: submitting, busy: submitting }}
            disabled={submitting}
            onPress={() => respond('ACCEPTED')}
            style={[styles.button, styles.acceptButton, submitting ? styles.buttonDisabled : null]}
            testID={`accept-request-${request.id}`}
          >
            <Text style={styles.buttonText}>{submitting ? 'Updating…' : 'Accept'}</Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: submitting, busy: submitting }}
            disabled={submitting}
            onPress={() => respond('REJECTED')}
            style={[styles.button, styles.rejectButton, submitting ? styles.buttonDisabled : null]}
            testID={`reject-request-${request.id}`}
          >
            <Text style={styles.buttonText}>{submitting ? 'Updating…' : 'Reject'}</Text>
          </Pressable>
        </View>
      ) : null}

      {message ? (
        <Text
          accessibilityRole="alert"
          accessibilityLiveRegion="polite"
          style={styles.errorMessage}
          testID="seat-request-error"
        >
          {message}
        </Text>
      ) : null}
      {needsSignIn && onSignIn ? (
        <Pressable accessibilityRole="button" onPress={onSignIn}>
          <Text style={styles.signIn}>Sign in</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

export default function SeatRequestResponseScreen({
  requests,
  getAccessToken,
  onSignIn,
  onRequestResponded,
}: SeatRequestResponseScreenProps) {
  const [toast, setToast] = useState('');

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(''), 3500);
    return () => clearTimeout(timer);
  }, [toast]);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>Seat requests</Text>
        {requests.length === 0 ? (
          <Text style={styles.emptyState} testID="no-seat-requests">
            There are no seat requests to review.
          </Text>
        ) : (
          requests.map((request) => (
            <SeatRequestCard
              key={request.id}
              request={request}
              getAccessToken={getAccessToken}
              onSignIn={onSignIn}
              onRequestResponded={onRequestResponded}
              onSuccess={setToast}
            />
          ))
        )}
      </ScrollView>
      {toast ? (
        <View
          accessibilityRole="alert"
          accessibilityLiveRegion="polite"
          style={styles.toast}
          testID="seat-request-success"
        >
          <Text style={styles.toastText}>{toast}</Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: 20, paddingTop: 32 },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 16 },
  emptyState: { color: '#555', marginTop: 8 },
  card: { borderWidth: 1, borderColor: '#bbb', borderRadius: 8, padding: 16, marginBottom: 14 },
  passengerName: { fontSize: 18, fontWeight: '700', marginBottom: 4 },
  status: { fontWeight: '600', marginTop: 8, textTransform: 'capitalize' },
  actions: { flexDirection: 'row', gap: 12, marginTop: 16 },
  button: { flex: 1, borderRadius: 6, padding: 12, alignItems: 'center' },
  acceptButton: { backgroundColor: '#2e7d32' },
  rejectButton: { backgroundColor: '#b71c1c' },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: '#fff', fontWeight: '700' },
  errorMessage: { color: '#c62828', marginTop: 12 },
  signIn: { color: '#1565c0', marginTop: 8, fontWeight: '600' },
  toast: {
    position: 'absolute', bottom: 20, left: 20, right: 20,
    backgroundColor: '#2e7d32', borderRadius: 8, padding: 14,
  },
  toastText: { color: '#fff', fontWeight: '700', textAlign: 'center' },
});
