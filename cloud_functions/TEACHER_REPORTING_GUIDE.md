# DIBINA Teacher Reporting System: Google Spreadsheet Integration

## 1. Architecture Overview

```
┌─────────────────┐
│   Android App   │ (DIBINA Client - Zero Spreadsheet access)
└────────┬────────┘
         │ Writes Journal to Firestore (journals/{journalId})
         ▼
┌─────────────────┐
│ Firebase / Cloud│
│    Functions    │ (Trigger: onWrite journals/{journalId})
└────────┬────────┘
         │ POST JSON Payload to Web App URL (Secure HTTPS)
         ▼
┌─────────────────┐
│Google Apps Script│ (doPost(e) - Validates, Acquires Lock, Finds uniqueKey)
└────────┬────────┘
         │ Idempotent UPSERT (INSERT if not exists, UPDATE existing row)
         ▼
┌─────────────────┐
│Google Spreadsheet│ (Teacher Dashboard: Jurnal_7KAIH)
└─────────────────┘
```

> **CRITICAL SECURITY RULES**:
> 1. Android **MUST NOT** directly access Google Spreadsheet.
> 2. The Google Spreadsheet URL and Apps Script deployment credentials are **NEVER exposed to students or stored in the Android APK**.
> 3. Android client communicates only with Firestore.

---

## 2. Idempotency Key Specification

- **Field**: `uniqueKey`
- **Formula**: `uid|YYYY-MM-DD`
- **Example**: `abc123|2026-09-20`
- **Constraint**: Each student (`uid`) can have at most **one** entry per date (`YYYY-MM-DD`).
- **Upsert Rule**:
  - If `uniqueKey` **does not exist**: `INSERT ROW` (append).
  - If `uniqueKey` **already exists**: `UPDATE EXISTING ROW` at that row index.
  - **Never** append another row when editing.

---

## 3. Spreadsheet Structure (27 Columns)

Sheet Name: `Jurnal_7KAIH`

| # | Column Name | Type | Description |
|---|---|---|---|
| 1 | `uniqueKey` | String | Idempotency Key (`uid\|YYYY-MM-DD`) |
| 2 | `timestamp` | Number | Creation epoch timestamp in milliseconds |
| 3 | `tanggal` | String | ISO Date format (`YYYY-MM-DD`) |
| 4 | `uid` | String | Firebase Auth Student UID |
| 5 | `nama` | String | Student Full Name |
| 6 | `NIS` | String | Student Identification Number |
| 7 | `sekolah` | String | School Name (e.g., SD Negeri Karangtalun) |
| 8 | `kelas` | String | Class/Grade (e.g., 6A) |
| 9 | `kodeKelas` | String | Class Code (e.g., K6-26) |
| 10 | `bangunPagi` | String | Wake up habit status (e.g., "Ya (04:30)") |
| 11 | `ibadah` | String | Prayer/Devotion details (e.g., "Ya (Subuh, Zuhur, Asar, Magrib, Isya)") |
| 12 | `olahraga` | String | Exercise activity (e.g., "Ya (Senam Pagi 20 mnt)") |
| 13 | `karbohidrat` | String | Carbohydrate nutrition item |
| 14 | `protein` | String | Protein nutrition item |
| 15 | `lemak` | String | Healthy fats nutrition item |
| 16 | `vitamin` | String | Vitamin nutrition item |
| 17 | `serat` | String | Fiber nutrition item |
| 18 | `air` | String | Hydration (e.g., "Ya (8 Gelas)") |
| 19 | `materiBelajar` | String | Study subject / topic |
| 20 | `durasiBelajar` | Number | Study duration in minutes (multiples of 15) |
| 21 | `bermasyarakat` | String | Social / helping activity |
| 22 | `tidurCepat` | String | Bedtime habit status (e.g., "Ya (20:30)") |
| 23 | `exp` | Number | Experience points awarded |
| 24 | `level` | Number | Student Level |
| 25 | `streak` | Number | Consecutive daily streak count |
| 26 | `isBackdate` | Boolean | True if recorded past journal (within 7 days) |
| 27 | `updatedAt` | Number | Last update epoch timestamp in milliseconds |

> **IMPORTANT REQUIREMENT**: Strictly **NO** `mineral` column!

---

## 4. Request JSON Structure (Cloud Functions -> Apps Script)

```json
{
  "uniqueKey": "abc123456789|2026-09-20",
  "timestamp": 1789968000000,
  "tanggal": "2026-09-20",
  "uid": "abc123456789",
  "nama": "Ahmad Dani",
  "NIS": "12345",
  "sekolah": "SD Negeri Karangtalun",
  "kelas": "6A",
  "kodeKelas": "K6-26",
  "bangunPagi": "Ya (04:30)",
  "ibadah": "Ya (Subuh, Zuhur, Asar, Magrib, Isya)",
  "olahraga": "Ya (Senam Pagi)",
  "karbohidrat": "Ya (Nasi)",
  "protein": "Ya (Telur, Tempe)",
  "lemak": "Ya (Alpukat)",
  "vitamin": "Ya (Jeruk)",
  "serat": "Ya (Bayam)",
  "air": "Ya (8 Gelas)",
  "materiBelajar": "Matematika Bangun Ruang",
  "durasiBelajar": 30,
  "bermasyarakat": "Ya (Membantu menyapu halaman)",
  "tidurCepat": "Ya (20:30)",
  "exp": 100,
  "level": 3,
  "streak": 5,
  "isBackdate": false,
  "updatedAt": 1789968000000
}
```

---

## 5. Response JSON Structure (Apps Script -> Cloud Functions)

### Success (HTTP 200) - INSERT:
```json
{
  "status": "success",
  "action": "INSERT",
  "uniqueKey": "abc123456789|2026-09-20",
  "rowNumber": 14,
  "timestamp": "2026-09-20T10:00:00.000Z"
}
```

### Success (HTTP 200) - UPDATE:
```json
{
  "status": "success",
  "action": "UPDATE",
  "uniqueKey": "abc123456789|2026-09-20",
  "rowNumber": 14,
  "timestamp": "2026-09-20T10:15:00.000Z"
}
```

### Error (HTTP 422 - Validation Failure):
```json
{
  "status": "error",
  "code": "VALIDATION_FAILED",
  "message": "Field 'mineral' is forbidden in 7 KAIH specification."
}
```

### Error (HTTP 503 - Lock Timeout):
```json
{
  "status": "error",
  "code": "LOCK_TIMEOUT",
  "message": "Server busy, please retry."
}
```

---

## 6. Sync Status & Failure Handling

In Firestore `journals/{journalId}`:
- `spreadsheetSyncStatus`: `"pending"` | `"success"` | `"failed"`
- `spreadsheetSyncedAt`: Epoch timestamp of successful sync
- `spreadsheetSyncError`: Error message if sync failed

### Resilience Guarantees:
1. **Never Delete on Failure**: If Apps Script is down, network fails, or times out, the journal is safely stored in Firestore and **never deleted**.
2. **Status Flagged**: `spreadsheetSyncStatus` is marked `"failed"`.
3. **Automated Idempotent Retry**:
   - The scheduled Cloud Function `retryFailedSpreadsheetSyncs` runs periodically to retry records with `"failed"` or `"pending"` status.
   - Because of `uniqueKey` UPSERT logic, retries **never create duplicate rows**, **never create duplicate journals**, and **never create duplicate feed posts**.

---

## 7. Deployment Instructions

### Step 1: Create the Google Spreadsheet
1. Open [Google Sheets](https://sheets.new).
2. Rename spreadsheet to **DIBINA - Rekap Jurnal Kelas (Guru)**.
3. Name the first sheet tab **`Jurnal_7KAIH`**.
4. Note the Spreadsheet ID from the browser URL:
   `https://docs.google.com/spreadsheets/d/<SPREADSHEET_ID>/edit`

### Step 2: Deploy the Google Apps Script
1. In Google Sheets, click **Extensions > Apps Script**.
2. Replace all code in `Code.gs` with the content from `/cloud_functions/google_apps_script.js`.
3. Fill in `SPREADSHEET_ID = "<YOUR_SPREADSHEET_ID>";` (or leave empty if using container-bound script).
4. Click **Deploy > New deployment**.
5. Select type: **Web app**.
6. Settings:
   - **Execute as**: `Me`
   - **Who has access**: `Anyone` (or Google Workspace domain if restricted to school organization)
7. Click **Deploy**, authorize permissions, and copy the **Web App URL** (`https://script.google.com/macros/s/.../exec`).

### Step 3: Configure Cloud Functions
1. In your Firebase project terminal, configure the Apps Script Web App URL:
   ```bash
   # TODO: MANUAL CONFIGURATION REQUIRED
   firebase functions:config:set apps_script.url="https://script.google.com/macros/s/<DEPLOYMENT_ID>/exec"
   ```
2. Deploy the functions:
   ```bash
   firebase deploy --only functions
   ```
