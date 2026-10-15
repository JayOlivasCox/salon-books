import { CameraView, useCameraPermissions } from 'expo-camera';
import { router, useLocalSearchParams } from 'expo-router';
import { useRef, useState } from 'react';
import { View } from 'react-native';

import { Button, ErrorNote, Field, Sheet, Txt } from '@/components/ui';
import { useBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { C, R, S } from '@/lib/theme';

/** Pulls the token out of "salonbooks://join?token=..." or accepts a bare token. */
function tokenFrom(text: string): string {
  const m = text.match(/[?&]token=([^&\s]+)/);
  return (m ? decodeURIComponent(m[1]) : text).trim();
}

export default function Join() {
  const params = useLocalSearchParams<{ token?: string }>();
  const { setBusiness } = useBusiness();
  const [permission, requestPermission] = useCameraPermissions();
  // Opened from the QR link in the Camera app: the code arrives pre-filled.
  const [code, setCode] = useState(params.token ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const handled = useRef(false);

  async function accept(raw: string) {
    if (handled.current) return;
    handled.current = true;
    setBusy(true);
    setError(undefined);
    try {
      setBusiness(await api.acceptInvite(tokenFrom(raw)));
    } catch (e) {
      handled.current = false;
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Sheet title="Join a business" onClose={() => router.back()}>
      {permission?.granted ? (
        <View style={{ height: 300, borderRadius: R.md, overflow: 'hidden', backgroundColor: C.chair }}>
          <CameraView
            style={{ flex: 1 }}
            facing="back"
            barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
            onBarcodeScanned={busy ? undefined : ({ data }) => accept(data)}
          />
        </View>
      ) : (
        <View style={{ gap: S.sm }}>
          <Txt color={C.inkSoft}>Allow the camera to scan your partner’s invite code.</Txt>
          <Button kind="secondary" icon="camera" title="Allow camera" onPress={requestPermission} />
        </View>
      )}
      <Txt color={C.inkSoft}>Or paste the code they sent you.</Txt>
      <Field label="Invite code" value={code} onChangeText={setCode} autoCapitalize="none" autoCorrect={false} />
      <ErrorNote message={error} />
      <Button title="Join business" onPress={() => accept(code)} loading={busy} disabled={!code.trim()} />
    </Sheet>
  );
}
