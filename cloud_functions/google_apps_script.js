/**
 * Google Apps Script for DIBINA Teacher Reporting System
 *
 * Architecture:
 * Android -> Firebase (Firestore) -> Cloud Functions -> Google Apps Script -> Google Spreadsheet
 *
 * IMPORTANT SECURITY RULES:
 * - Android MUST NOT directly access Google Spreadsheet.
 * - The Spreadsheet URL must NOT be exposed to students.
 * - Apps Script is deployed as a Web App (Execute as: Me, Who has access: Anyone or Service Account).
 *
 * IDEMPOTENCY KEY:
 * uniqueKey = uid|YYYY-MM-DD (e.g. abc123|2026-09-20)
 *
 * UPSERT RULE:
 * - If uniqueKey does not exist: INSERT ROW
 * - If uniqueKey already exists: UPDATE EXISTING ROW
 * - NEVER append another row when editing.
 *
 * SPREADSHEET COLUMNS (Exact 27 columns, NO "mineral"):
 * 1. uniqueKey
 * 2. timestamp
 * 3. tanggal
 * 4. uid
 * 5. nama
 * 6. NIS
 * 7. sekolah
 * 8. kelas
 * 9. kodeKelas
 * 10. bangunPagi
 * 11. ibadah
 * 12. olahraga
 * 13. karbohidrat
 * 14. protein
 * 15. lemak
 * 16. vitamin
 * 17. serat
 * 18. air
 * 19. materiBelajar
 * 20. durasiBelajar
 * 21. bermasyarakat
 * 22. tidurCepat
 * 23. exp
 * 24. level
 * 25. streak
 * 26. isBackdate
 * 27. updatedAt
 */

// TODO: MANUAL CONFIGURATION REQUIRED
// Paste your target Google Spreadsheet ID below (or leave blank to use the container-bound spreadsheet)
var SPREADSHEET_ID = ""; // e.g. "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
var SHEET_NAME = "Jurnal_7KAIH";

// Column headers exactly matching specification (DO NOT include 'mineral')
var EXPECTED_COLUMNS = [
  "uniqueKey",
  "timestamp",
  "tanggal",
  "uid",
  "nama",
  "NIS",
  "sekolah",
  "kelas",
  "kodeKelas",
  "bangunPagi",
  "ibadah",
  "olahraga",
  "karbohidrat",
  "protein",
  "lemak",
  "vitamin",
  "serat",
  "air",
  "materiBelajar",
  "durasiBelajar",
  "bermasyarakat",
  "tidurCepat",
  "exp",
  "level",
  "streak",
  "isBackdate",
  "updatedAt"
];

/**
 * Web App entry point for POST requests from Cloud Functions.
 */
function doPost(e) {
  var lock = LockService.getScriptLock();
  // Wait up to 30 seconds for other concurrent executions to finish
  var hasLock = lock.tryLock(30000);
  if (!hasLock) {
    return createJsonResponse(503, {
      status: "error",
      code: "LOCK_TIMEOUT",
      message: "Server busy, please retry."
    });
  }

  try {
    if (!e || !e.postData || !e.postData.contents) {
      return createJsonResponse(400, {
        status: "error",
        code: "MISSING_PAYLOAD",
        message: "Request body is empty."
      });
    }

    var payload;
    try {
      payload = JSON.parse(e.postData.contents);
    } catch (parseErr) {
      return createJsonResponse(400, {
        status: "error",
        code: "INVALID_JSON",
        message: "Malformed JSON payload: " + parseErr.message
      });
    }

    // 1. Validate required fields
    var validation = validatePayload(payload);
    if (!validation.valid) {
      return createJsonResponse(422, {
        status: "error",
        code: "VALIDATION_FAILED",
        message: validation.message
      });
    }

    // 2. Access Sheet
    var sheet = getOrCreateSheet();

    // 3. Perform Idempotent UPSERT
    var upsertResult = upsertRow(sheet, payload);

    return createJsonResponse(200, {
      status: "success",
      action: upsertResult.action, // "INSERT" or "UPDATE"
      uniqueKey: payload.uniqueKey,
      rowNumber: upsertResult.rowNumber,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    return createJsonResponse(500, {
      status: "error",
      code: "INTERNAL_ERROR",
      message: error.toString()
    });
  } finally {
    lock.releaseLock();
  }
}

/**
 * Validates incoming JSON payload.
 * Ensures uniqueKey follows "uid|YYYY-MM-DD" and required fields exist.
 */
function validatePayload(data) {
  if (!data) return { valid: false, message: "Payload cannot be null" };

  if (!data.uniqueKey || typeof data.uniqueKey !== "string" || !data.uniqueKey.includes("|")) {
    return { valid: false, message: "Field 'uniqueKey' is required and must follow 'uid|YYYY-MM-DD'" };
  }

  if (!data.uid || typeof data.uid !== "string") {
    return { valid: false, message: "Field 'uid' is required" };
  }

  if (!data.tanggal || typeof data.tanggal !== "string") {
    return { valid: false, message: "Field 'tanggal' is required" };
  }

  // Strict check: DO NOT allow 'mineral'
  if (data.hasOwnProperty("mineral")) {
    return { valid: false, message: "Field 'mineral' is forbidden in 7 KAIH specification." };
  }

  return { valid: true };
}

/**
 * Gets or initializes the target sheet with standard headers.
 */
function getOrCreateSheet() {
  var ss;
  if (SPREADSHEET_ID && SPREADSHEET_ID.trim().length > 0) {
    ss = SpreadsheetApp.openById(SPREADSHEET_ID.trim());
  } else {
    ss = SpreadsheetApp.getActiveSpreadsheet();
  }

  if (!ss) {
    throw new Error("Unable to access spreadsheet. Please configure SPREADSHEET_ID.");
  }

  var sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
  }

  // Check header row (Row 1)
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(EXPECTED_COLUMNS);
    var headerRange = sheet.getRange(1, 1, 1, EXPECTED_COLUMNS.length);
    headerRange.setFontWeight("bold");
    headerRange.setBackground("#1D4ED8");
    headerRange.setFontColor("#FFFFFF");
    sheet.setFrozenRows(1);
  }

  return sheet;
}

/**
 * UPSERT Function:
 * If uniqueKey does not exist: INSERT ROW
 * If uniqueKey already exists: UPDATE EXISTING ROW
 * NEVER appends another row when editing.
 */
function upsertRow(sheet, data) {
  var targetKey = String(data.uniqueKey).trim();
  var lastRow = sheet.getLastRow();
  var rowIndex = -1;

  if (lastRow > 1) {
    // Column 1 is uniqueKey
    var keyRange = sheet.getRange(2, 1, lastRow - 1, 1);
    var keyValues = keyRange.getValues();

    for (var i = 0; i < keyValues.length; i++) {
      if (String(keyValues[i][0]).trim() === targetKey) {
        rowIndex = i + 2; // 1-based index (+1 for header, +1 for 0-index)
        break;
      }
    }
  }

  // Build row values strictly matching EXPECTED_COLUMNS
  var rowData = [
    data.uniqueKey || "",
    data.timestamp || new Date().getTime(),
    data.tanggal || "",
    data.uid || "",
    data.nama || "",
    data.NIS || "",
    data.sekolah || "",
    data.kelas || "",
    data.kodeKelas || "",
    data.bangunPagi || "Tidak",
    data.ibadah || "Tidak",
    data.olahraga || "Tidak",
    data.karbohidrat || "Tidak",
    data.protein || "Tidak",
    data.lemak || "Tidak",
    data.vitamin || "Tidak",
    data.serat || "Tidak",
    data.air || "Tidak",
    data.materiBelajar || "",
    data.durasiBelajar || 0,
    data.bermasyarakat || "Tidak",
    data.tidurCepat || "Tidak",
    data.exp || 0,
    data.level || 1,
    data.streak || 0,
    data.isBackdate === true,
    data.updatedAt || new Date().getTime()
  ];

  if (rowIndex > 0) {
    // UPDATE EXISTING ROW
    var targetRange = sheet.getRange(rowIndex, 1, 1, rowData.length);
    targetRange.setValues([rowData]);
    return { action: "UPDATE", rowNumber: rowIndex };
  } else {
    // INSERT NEW ROW
    sheet.appendRow(rowData);
    return { action: "INSERT", rowNumber: sheet.getLastRow() };
  }
}

/**
 * Creates standardized HTTP JSON Response.
 */
function createJsonResponse(statusCode, bodyObject) {
  var output = ContentService.createTextOutput(JSON.stringify(bodyObject));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}
