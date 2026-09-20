package com.example.dibina.model

/**
 * Report payload representing a student journal row for Google Spreadsheet synchronization.
 *
 * Architecture:
 * Android -> Firebase (Firestore: journals/{journalId}) -> Cloud Functions -> Google Apps Script -> Google Spreadsheet
 *
 * NOTE:
 * - Android NEVER directly contacts Google Spreadsheet.
 * - Spreadsheet URL is NEVER stored in or exposed to Android clients.
 * - uniqueKey = "uid|YYYY-MM-DD"
 * - DO NOT include "mineral" (Strict requirement).
 */
data class TeacherReportRow(
    val uniqueKey: String = "",       // uid|YYYY-MM-DD (e.g. abc123|2026-09-20)
    val timestamp: Long = 0L,         // Epoch millis
    val tanggal: String = "",         // YYYY-MM-DD
    val uid: String = "",             // Student Firebase Auth UID
    val nama: String = "",            // Student display name
    val NIS: String = "",             // Nomor Induk Siswa
    val sekolah: String = "",         // School name
    val kelas: String = "",           // Grade / class name (e.g. "6A")
    val kodeKelas: String = "",       // Class code (e.g. "K6-26")
    val bangunPagi: String = "",      // "Ya" / "Tidak" or formatted time e.g. "Ya (04:30)"
    val ibadah: String = "",          // "Ya" / "Tidak" or sholat list e.g. "Subuh, Zuhur, Asar, Magrib, Isya"
    val olahraga: String = "",        // "Ya" / "Tidak" or activity name e.g. "Senam Pagi (20 mnt)"
    val karbohidrat: String = "",     // "Ya" / "Tidak" or menu e.g. "Nasi Putih"
    val protein: String = "",         // "Ya" / "Tidak" or menu e.g. "Telur, Tahu"
    val lemak: String = "",           // "Ya" / "Tidak" or menu e.g. "Alpukat"
    val vitamin: String = "",         // "Ya" / "Tidak" or menu e.g. "Jeruk"
    val serat: String = "",           // "Ya" / "Tidak" or menu e.g. "Bayam"
    val air: String = "",             // "Ya" / "Tidak" or e.g. "8 Gelas"
    val materiBelajar: String = "",   // Learning subject e.g. "Matematika Pecahan"
    val durasiBelajar: Int = 0,       // Duration in minutes (multiples of 15)
    val bermasyarakat: String = "",   // Social / helping activity e.g. "Membantu ibu menyapu"
    val tidurCepat: String = "",      // "Ya" / "Tidak" or formatted time e.g. "Ya (20:30)"
    val exp: Int = 0,                 // EXP earned for journal
    val level: Int = 1,               // Student level
    val streak: Int = 0,              // Current streak count
    val isBackdate: Boolean = false,  // Backdated flag
    val updatedAt: Long = 0L          // Last updated timestamp
)

/**
 * Spreadsheet sync status constants as required:
 * - pending
 * - success
 * - failed
 */
object SpreadsheetSyncStatus {
    const val PENDING = "pending"
    const val SUCCESS = "success"
    const val FAILED = "failed"
}
