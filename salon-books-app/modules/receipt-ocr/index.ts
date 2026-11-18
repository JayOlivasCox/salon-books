import { requireOptionalNativeModule } from 'expo';

export type OcrLine = { text: string; x: number; y: number; width: number; height: number };

type ReceiptOcrNative = { recognize(uri: string): Promise<OcrLine[]> };

// Optional so the app still runs (without scanning) on builds made before this module existed.
const native = requireOptionalNativeModule<ReceiptOcrNative>('ReceiptOcr');

export const ocrAvailable = native != null;

export function recognizeText(uri: string): Promise<OcrLine[]> {
  if (!native) {
    return Promise.reject(new Error('Receipt scanning needs a new build. Run npx expo run:ios.'));
  }
  return native.recognize(uri);
}
