# Salon Books Backend

Spring Boot REST API for the salon stylist bookkeeping app (CSC 481).
Java 21, Spring Boot 3.4, Supabase (Postgres, Auth, Storage), Flyway, OpenPDF.

## Architecture

```
React Native app ──(Supabase Auth: email / Google / Apple)──> Supabase Auth
       │  Bearer <supabase access token>
       ▼
Spring Boot API ──JDBC──> Supabase Postgres (tables locked with RLS, only the API touches them)
       └──REST (secret key)──> Supabase Storage (private "receipts" bucket, signed URLs)
```

- **Auth**: the app signs in with Supabase directly. The API only verifies the JWT (JWKS, or legacy HS256 secret).
- **Receipt OCR**: runs on the phone with Google ML Kit. The app sends the raw text to `/receipts/parse` for a draft, uploads the photo through a signed URL, then saves the expense.
- **Offline**: every record has a client-generated UUID, soft deletes, and last-write-wins on `clientModifiedAt`. See Sync below.
- **Partners**: a business can have multiple members. The owner generates a QR invite; the partner scans it and joins.

## Setup

1. Create a Supabase project. Enable Email, Google, and Apple providers under Authentication.
2. Copy `.env.example` to `.env` and fill it in:
   - `DATABASE_URL`: Project Settings > Database > JDBC string. Use the **session pooler (5432)** or direct connection, not the transaction pooler.
   - `SUPABASE_SECRET_KEY`: Project Settings > API Keys. Server only, never ship it in the app.
   - `SUPABASE_JWT_SECRET`: leave blank unless your project still uses the legacy JWT secret.
3. Run (Flyway creates the schema and the `receipts` bucket on first start):
   ```
   export $(cat .env | xargs)
   mvn spring-boot:run
   ```
4. `mvn test` runs the unit tests (receipt parser, projection, rent math). No database needed.

## Endpoints

All under `/api`, all require `Authorization: Bearer <token>`. `{b}` = business id.

| Area | Method & path | Notes |
|---|---|---|
| Me | `GET /me` | Call after sign-in. Creates profile, lists businesses |
| Business | `POST /businesses` | `{name, timezone}`. Seeds default expense categories |
| | `GET /businesses`, `GET /businesses/{b}` | |
| | `PATCH /businesses/{b}/settings` | `rentDisplay` WEEKLY/MONTHLY, `defaultExpenseSort` |
| Partners | `POST /businesses/{b}/invites` | Owner only. Returns `deepLink` to render as QR |
| | `POST /invites/accept` | `{token}` from the scanned QR |
| | `GET /businesses/{b}/members`, `DELETE .../members/{userId}` | |
| Categories | `GET/PUT/DELETE /businesses/{b}/categories[/{id}]` | Each maps to a Schedule C line |
| | `GET /businesses/{b}/categories/schedule-c-lines` | For the picker |
| Clients | `GET /businesses/{b}/clients?q=`, `PUT/DELETE /{id}` | |
| Services | `GET /businesses/{b}/service-types`, `PUT/DELETE /{id}` | No fixed price |
| Appointments | `GET /businesses/{b}/appointments?from&to`, `PUT/DELETE /{id}` | `clientId` optional (unnamed booked slot) |
| Income | `GET /businesses/{b}/income?from&to`, `PUT/DELETE /{id}` | Service amount + tip |
| | `GET /businesses/{b}/income/suggestion?clientId&serviceTypeId` | Learned price to offer as autofill |
| Expenses | `GET /businesses/{b}/expenses` | Filters: `from,to,categoryId,vendor,minAmount,maxAmount`; `sort=DATE\|CATEGORY\|VENDOR\|AMOUNT`, `dir`, `page`, `size` |
| | `PUT/DELETE /businesses/{b}/expenses/{id}` | Optional `items[]` for itemized receipts |
| | `GET .../expenses/vendors`, `GET .../expenses/{id}/receipt-url` | |
| Receipts | `POST /businesses/{b}/receipts/upload-url` | `{expenseId, fileExtension}` -> PUT the image to `uploadUrl`, save `path` as `receiptPath` |
| | `POST /businesses/{b}/receipts/parse` | `{text}` from ML Kit -> vendor/date/total/tax/items draft |
| Booth rent | `GET /businesses/{b}/leases`, `PUT/DELETE /{id}` | `{amount, frequency, startDate, endDate or durationMonths}` |
| | `GET /businesses/{b}/leases/expiring?withinDays=30` | Prompt to renew |
| Reports | `GET /businesses/{b}/reports/daily?date&stylistId` | Appointments, booked count, entries, totals |
| | `GET .../reports/weekly?start=` | 7 days + per-day totals |
| | `GET .../reports/monthly?year&month` | Totals, pie data, top vendors, vs last month and last year |
| | `GET .../reports/yearly?year` | Month rows, busiest/slowest, next-year projection |
| | `GET .../reports/tax-pdf?year` | Schedule C grouped PDF |
| Sync | `GET /businesses/{b}/sync?since=` | All changes since timestamp, incl. tombstones |
| | `POST /businesses/{b}/sync` | Batch upsert, per-record results |

`PUT` is create-or-update (the app picks the UUID). It returns `409` with the server copy if the server has a newer edit.

## Sync protocol (for the app team)

1. Every create/edit/delete is written to local storage (e.g. SQLite/WatermelonDB) and queued with `clientModifiedAt = now`.
2. When online: `POST /sync` with the queue. Results are `APPLIED`, `STALE` (server is newer, drop local edit), `NOOP`, or `ERROR` (show it, keep it queued or discard).
3. Then `GET /sync?since=<nextSince from the last pull>`. Upsert everything locally; rows with `deletedAt` are removed. Store the new `nextSince`.
4. Pull windows overlap by 30s on purpose, so expect a few repeats. Upserts make them harmless.

## Money rules

- Gross = service amount + tips (tips are taxable income for contractors).
- Booth rent comes from leases (prorated by day, a full month = exactly the monthly amount), not from logged expenses. Use the "Booth Rent" category only for one-off rent charges.
- Itemized expense: items count under their own category; any leftover (usually tax) counts under the expense's category.
- Projection: year-over-year growth when last year has data (keeps seasonality), linear trend otherwise, needs 3+ finished months.

## Frontend notes (for later)

- ML Kit needs native code, so use an **Expo dev build** (`npx expo prebuild && npx expo run:ios`) or bare RN, not Expo Go. Works with Xcode. Library: `@react-native-ml-kit/text-recognition`.
- Apple sign-in: `expo-apple-authentication` + `supabase.auth.signInWithIdToken({provider: 'apple'})`. Google: `@react-native-google-signin/google-signin` + `signInWithIdToken({provider: 'google'})`.
- QR: render `deepLink` with `react-native-qrcode-svg`; scan with `expo-camera`. Register the `salonbooks://` scheme so the link also opens the app.
