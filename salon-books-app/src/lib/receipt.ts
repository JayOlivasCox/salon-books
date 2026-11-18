import { File } from 'expo-file-system';
import * as ImagePicker from 'expo-image-picker';

import { ocrAvailable, recognizeText } from '../../modules/receipt-ocr';
import { api } from './api';
import { linesToText } from './receipt-text';
import { supabase } from './supabase';
import type { ParsedReceipt } from './types';

export { ocrAvailable };

export type Photo = { uri: string; mimeType: string; ext: string };

/** Take a photo or pick one from the camera roll. Returns undefined if cancelled. */
export async function pickReceiptPhoto(source: 'camera' | 'library'): Promise<Photo | undefined> {
  const options: ImagePicker.ImagePickerOptions = { mediaTypes: ['images'], quality: 0.6 };
  let result: ImagePicker.ImagePickerResult;
  if (source === 'camera') {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (!perm.granted) throw new Error('Allow camera access in Settings to scan receipts.');
    result = await ImagePicker.launchCameraAsync(options);
  } else {
    result = await ImagePicker.launchImageLibraryAsync(options);
  }
  const asset = result.canceled ? undefined : result.assets[0];
  if (!asset) return undefined;
  const mimeType = asset.mimeType ?? 'image/jpeg';
  const ext = mimeType.includes('png') ? 'png' : mimeType.includes('heic') ? 'heic' : 'jpg';
  return { uri: asset.uri, mimeType, ext };
}

/** Read the receipt on the phone, then ask the server to pull out vendor, date, total and items. */
export async function readReceipt(businessId: string, photo: Photo): Promise<{ parsed: ParsedReceipt; text: string }> {
  const text = linesToText(await recognizeText(photo.uri));
  if (!text.trim()) throw new Error('No text found. Try a closer, flatter photo with good light.');
  return { parsed: await api.parseReceipt(businessId, text), text };
}

/** Upload the photo to private storage and return the path to save on the expense. */
export async function uploadReceipt(businessId: string, expenseId: string, photo: Photo): Promise<string> {
  const { uploadUrl, path } = await api.receiptUploadUrl(businessId, expenseId, photo.ext);
  const token = new URL(uploadUrl).searchParams.get('token');
  if (!token) throw new Error('Upload link was missing a token.');
  const body = await new File(photo.uri).arrayBuffer();
  const bucketPath = path; // backend paths are already relative to the receipts bucket
  const { error } = await supabase.storage
    .from('receipts')
    .uploadToSignedUrl(bucketPath, token, body, { contentType: photo.mimeType });
  if (error) throw new Error(`Couldn't upload the photo: ${error.message}`);
  return path;
}
