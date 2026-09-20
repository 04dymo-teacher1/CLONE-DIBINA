/**
 * Firebase Cloud Function: syncJournalToSpreadsheet
 *
 * Architecture:
 * Android -> Firestore (journals/{journalId}) -> Cloud Functions -> Google Apps Script -> Google Spreadsheet
 *
 * Rules:
 * - Trigger: onWrite (onCreate or onUpdate) of journals/{journalId}
 * - Android NEVER accesses Google Spreadsheet directly.
 * - Spreadsheet URL is kept strictly on server/cloud functions.
 * - Failure handling: If Apps Script fails, KEEP the journal in Firestore.
 *   Set spreadsheetSyncStatus = "failed", record error.
 *   Retry later with exponential backoff or scheduled task.
 *   Retry is strictly idempotent because uniqueKey = "uid|YYYY-MM-DD" guarantees UPSERT.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

if (!admin.apps.length) {
  admin.initializeApp();
}

// TODO: MANUAL CONFIGURATION REQUIRED
// Set your Apps Script Web App Deployment URL in Firebase functions config or secret manager:
// firebase functions:config:set apps_script.url="https://script.google.com/macros/s/AKfycb.../exec"
const APPS_SCRIPT_URL = process.env.APPS_SCRIPT_URL || functions.config().apps_script?.url || "";

/**
 * Maps Firestore journal and user profile to the exact 27-field Apps Script request payload.
 * STRICTLY NO "mineral" field.
 */
function buildReportPayload(journal, user) {
  // Convert date format: YYYYMMDD -> YYYY-MM-DD
  let isoDate = journal.date;
  if (journal.date && journal.date.length === 8) {
    isoDate = `${journal.date.substring(0, 4)}-${journal.date.substring(4, 6)}-${journal.date.substring(6, 8)}`;
  }

  const uniqueKey = `${journal.uid}|${isoDate}`;

  const formatBangunPagi = journal.bangunPagiTime ? `Ya (${journal.bangunPagiTime})` : (journal.bangunPagi ? "Ya" : "Tidak");
  const formatIbadah = (journal.ibadahSholat && journal.ibadahSholat.length > 0)
    ? `Ya (${journal.ibadahSholat.join(", ")})`
    : (journal.ibadah ? "Ya" : "Tidak");
  const formatOlahraga = journal.olahragaActivity ? `Ya (${journal.olahragaActivity})` : (journal.olahraga ? "Ya" : "Tidak");

  const formatKarbo = journal.karbohidratText ? `Ya (${journal.karbohidratText})` : (journal.karbohidrat ? "Ya" : "Tidak");
  const formatProtein = journal.proteinText ? `Ya (${journal.proteinText})` : (journal.protein ? "Ya" : "Tidak");
  const formatLemak = journal.lemakText ? `Ya (${journal.lemakText})` : (journal.lemak ? "Ya" : "Tidak");
  const formatVitamin = journal.vitaminText ? `Ya (${journal.vitaminText})` : (journal.vitamin ? "Ya" : "Tidak");
  const formatSerat = journal.seratText ? `Ya (${journal.seratText})` : (journal.serat ? "Ya" : "Tidak");
  const formatAir = (journal.airGelas && journal.airGelas > 0) ? `Ya (${journal.airGelas} Gelas)` : (journal.air ? "Ya" : "Tidak");

  const formatBermasyarakat = journal.bermasyarakatActivity ? `Ya (${journal.bermasyarakatActivity})` : (journal.bermasyarakat ? "Ya" : "Tidak");
  const formatTidurCepat = journal.tidurCepatTime ? `Ya (${journal.tidurCepatTime})` : (journal.tidurCepat ? "Ya" : "Tidak");

  return {
    uniqueKey: uniqueKey,
    timestamp: journal.createdAt || Date.now(),
    tanggal: isoDate,
    uid: journal.uid,
    nama: user?.name || "Siswa DIBINA",
    NIS: user?.nis || "",
    sekolah: user?.schoolName || "SD Negeri Karangtalun",
    kelas: user?.grade || "",
    kodeKelas: user?.classCode || journal.classId || "",
    bangunPagi: formatBangunPagi,
    ibadah: formatIbadah,
    olahraga: formatOlahraga,
    karbohidrat: formatKarbo,
    protein: formatProtein,
    lemak: formatLemak,
    vitamin: formatVitamin,
    serat: formatSerat,
    air: formatAir,
    materiBelajar: journal.materiBelajar || "",
    durasiBelajar: Number(journal.durasiBelajar) || 0,
    bermasyarakat: formatBermasyarakat,
    tidurCepat: formatTidurCepat,
    exp: Number(journal.exp) || 0,
    level: Number(user?.level) || 1,
    streak: Number(user?.currentStreak) || 0,
    isBackdate: Boolean(journal.isBackdate),
    updatedAt: journal.updatedAt || Date.now()
  };
}

/**
 * Cloud Function Trigger:
 * Executes whenever a journal document is created or updated in Firestore.
 */
exports.onJournalWriteSyncSpreadsheet = functions.firestore
  .document("journals/{journalId}")
  .onWrite(async (change, context) => {
    // If deleted, do not sync
    if (!change.after.exists) {
      return null;
    }

    const journal = change.after.data();
    const journalId = context.params.journalId;

    // Avoid infinite loop if this update was just a sync status reflection
    const beforeData = change.before.data();
    if (beforeData && beforeData.spreadsheetSyncStatus === journal.spreadsheetSyncStatus &&
        beforeData.updatedAt === journal.updatedAt) {
      return null;
    }

    if (!APPS_SCRIPT_URL) {
      console.warn("TODO: MANUAL CONFIGURATION REQUIRED. APPS_SCRIPT_URL not configured. Sync skipped.");
      return null;
    }

    // Fetch user profile for display metadata
    let user = null;
    try {
      const userDoc = await admin.firestore().collection("users").document(journal.uid).get();
      if (userDoc.exists) {
        user = userDoc.data();
      }
    } catch (e) {
      console.warn(`Could not fetch user ${journal.uid}:`, e.message);
    }

    const payload = buildReportPayload(journal, user);

    try {
      // POST payload to Google Apps Script Web App
      // Apps Script redirects HTTP 302 to googleusercontent, axios follows redirects automatically
      const response = await axios.post(APPS_SCRIPT_URL, payload, {
        headers: { "Content-Type": "application/json" },
        timeout: 25000,
        maxRedirects: 5
      });

      if (response.status === 200 && response.data && response.data.status === "success") {
        // Sync Success
        await change.after.ref.update({
          spreadsheetSyncStatus: "success",
          spreadsheetSyncedAt: admin.firestore.FieldValue.serverTimestamp(),
          spreadsheetSyncError: admin.firestore.FieldValue.delete()
        });
        console.log(`Successfully synced journal ${journalId} (Key: ${payload.uniqueKey}) to Spreadsheet via Apps Script.`);
      } else {
        throw new Error(response.data?.message || `Apps Script returned status: ${response.status}`);
      }
    } catch (err) {
      // FAILURE HANDLING: Keep the journal in Firestore, NEVER delete, set status = "failed"
      console.error(`Spreadsheet sync failed for ${journalId}:`, err.message);
      await change.after.ref.update({
        spreadsheetSyncStatus: "failed",
        spreadsheetSyncError: err.message || "Network/Server failure syncing to Google Spreadsheet"
      });
    }

    return null;
  });

/**
 * Scheduled Cloud Function for Retrying Failed Syncs:
 * Runs every 30 minutes, queries journals where spreadsheetSyncStatus == 'failed' or 'pending'.
 * Retries are strictly idempotent (UPSERT via uniqueKey).
 */
exports.retryFailedSpreadsheetSyncs = functions.pubsub
  .schedule("every 30 minutes")
  .onRun(async (context) => {
    if (!APPS_SCRIPT_URL) return null;

    const failedQuery = await admin.firestore().collection("journals")
      .where("spreadsheetSyncStatus", "in", ["failed", "pending"])
      .limit(50)
      .get();

    if (failedQuery.empty) {
      return null;
    }

    for (const doc of failedQuery.docs) {
      const journal = doc.data();
      let user = null;
      try {
        const userDoc = await admin.firestore().collection("users").document(journal.uid).get();
        if (userDoc.exists) user = userDoc.data();
      } catch (e) {}

      const payload = buildReportPayload(journal, user);
      try {
        const response = await axios.post(APPS_SCRIPT_URL, payload, { timeout: 20000 });
        if (response.data && response.data.status === "success") {
          await doc.ref.update({
            spreadsheetSyncStatus: "success",
            spreadsheetSyncedAt: admin.firestore.FieldValue.serverTimestamp(),
            spreadsheetSyncError: admin.firestore.FieldValue.delete()
          });
        }
      } catch (e) {
        console.warn(`Retry failed for journal ${doc.id}:`, e.message);
      }
    }
    return null;
  });
