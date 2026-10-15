import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Share, View } from 'react-native';
import QRCode from 'react-native-qrcode-svg';

import { Button, ErrorNote, Loading, Panel, Sheet, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { C, S } from '@/lib/theme';

export default function Invite() {
  const { business } = useCurrentBusiness();
  const [invite, setInvite] = useState<{ token: string; deepLink: string; expiresAt: string }>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    api.createInvite(business.id).then(setInvite).catch((e) => setError(String(e.message ?? e)));
  }, [business.id]);

  return (
    <Sheet title="Invite a partner" onClose={() => router.back()}>
      <Txt color={C.inkSoft}>
        Your partner signs in, taps Scan invite code, and points their camera here. You’ll share all income, expenses, and reports.
      </Txt>
      <ErrorNote message={error} />
      {!invite ? !error && <Loading /> : (
        <>
          <Panel style={{ alignItems: 'center', paddingVertical: S.xl }}>
            <QRCode value={invite.deepLink} size={220} color={C.ink} backgroundColor={C.surface} />
          </Panel>
          <Txt kind="small" color={C.inkSoft} style={{ textAlign: 'center' }}>
            Works once. Expires {new Date(invite.expiresAt).toLocaleString('en-US', { weekday: 'short', hour: 'numeric', minute: '2-digit' })}.
          </Txt>
          <View style={{ gap: S.sm }}>
            <Button kind="secondary" icon="square.and.arrow.up" title="Send code instead"
              onPress={() => Share.share({ message: `Join ${business.name} on Salon Books. Invite code: ${invite.token}` })} />
          </View>
        </>
      )}
    </Sheet>
  );
}
