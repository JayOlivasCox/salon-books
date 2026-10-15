# Salon Books (iOS app)

Expo SDK 57 / React Native app for the salon bookkeeping backend.

## Screens
- **Today**: the day's appointments (named or just "Booked"), day totals, and the quick-entry panel at the bottom.
  Tap an appointment, pick the service, tap the suggested price if you want it, enter the tip, save.
  Long-press an appointment to remove it.
- **Expenses**: month view, filter by vendor and category, sort by date/category/vendor/amount, add or edit with items.
- **Reports**: week (per-day bars), month (totals, category ring, top vendors, comparisons), year (monthly bars,
  busiest and slowest months, next-year projection, tax PDF export).
- **Settings**: business name, booth rent and contract length, where rent shows, default sort, services, partners (QR invite), sign out.

## Run it in the iPhone Simulator
1. Install **Xcode** from the App Store and open it once. In Xcode > Settings > Components, install an iOS Simulator.
2. Start the backend in another terminal (`mvn spring-boot:run`).
3. In this folder:
   ```bash
   npm install
   npx expo start
   ```
   Press `i`. The Simulator opens and the app loads.
4. Sign in with your Supabase test user.

`.env` already has the Supabase URL and publishable key (both are public). Never put the secret key here.

## Run it on your iPhone
1. `brew install cocoapods`
2. Plug the phone in, then run `npx expo run:ios --device`. Pick your phone. Sign with your Apple ID if asked.
3. Set `EXPO_PUBLIC_API_URL` in `.env` to your Mac's Wi-Fi IP (System Settings > Wi-Fi > Details), e.g. `http://192.168.1.20:8080`.
   Phone and Mac must be on the same Wi-Fi. Restart `npx expo start` after changing `.env`.

## Not in this version yet
- Receipt scanning with ML Kit (needs the dev build from `npx expo run:ios`)
- Google and Apple sign-in buttons (need provider setup in Supabase and Apple)
- Offline queue (the backend sync endpoints are ready)
