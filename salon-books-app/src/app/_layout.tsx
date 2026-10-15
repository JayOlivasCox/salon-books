import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';

import { ErrorNote, Loading, Button } from '@/components/ui';
import { AuthProvider, useAuth } from '@/context/auth';
import { BusinessProvider, useBusiness } from '@/context/business';
import { C, S } from '@/lib/theme';

export default function RootLayout() {
  return (
    <AuthProvider>
      <BusinessProvider>
        <StatusBar style="dark" />
        <Gate />
      </BusinessProvider>
    </AuthProvider>
  );
}

function Gate() {
  const { session, loading } = useAuth();
  const { business, loading: bizLoading, error, refresh, businesses } = useBusiness();

  if (loading || (session && bizLoading && businesses.length === 0)) {
    return <View style={{ flex: 1, backgroundColor: C.bg, justifyContent: 'center' }}><Loading /></View>;
  }
  if (session && error && businesses.length === 0) {
    return (
      <View style={{ flex: 1, backgroundColor: C.bg, justifyContent: 'center', padding: S.lg, gap: S.md }}>
        <ErrorNote message={error} />
        <Button title="Try again" onPress={refresh} />
      </View>
    );
  }

  const signedIn = !!session;
  const ready = signedIn && !!business;

  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: C.bg } }}>
      <Stack.Protected guard={!signedIn}>
        <Stack.Screen name="sign-in" />
      </Stack.Protected>

      <Stack.Protected guard={signedIn && !business}>
        <Stack.Screen name="setup" />
        <Stack.Screen name="join" options={{ presentation: 'modal' }} />
      </Stack.Protected>

      <Stack.Protected guard={ready}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="appointment-new" options={{ presentation: 'modal' }} />
        <Stack.Screen name="expense-new" options={{ presentation: 'modal' }} />
        <Stack.Screen name="lease" options={{ presentation: 'modal' }} />
        <Stack.Screen name="invite" options={{ presentation: 'modal' }} />
        <Stack.Screen name="services" options={{ presentation: 'modal' }} />
      </Stack.Protected>
    </Stack>
  );
}
