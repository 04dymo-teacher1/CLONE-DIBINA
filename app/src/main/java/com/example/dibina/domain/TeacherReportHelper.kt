package com.example.dibina.domain

import com.example.dibina.model.JournalEntry
import com.example.dibina.model.SpreadsheetSyncStatus
import com.example.dibina.model.StudentProfile
import com.example.dibina.model.TeacherReportRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain mapper and helper for Teacher Reporting and Google Spreadsheet Synchronization.
 *
 * Requirements:
 * - Architecture: Android -> Firebase -> Cloud Functions -> Google Apps Script -> Google Spreadsheet
 * - Android MUST NOT directly access Google Spreadsheet.
 * - Spreadsheet URL must NOT be exposed to students.
 * - uniqueKey = "uid|YYYY-MM-DD" (e.g. abc123|2026-09-20)
 * - UPSERT rule: insert if not exists, update existing if exists. Never append another row when editing.
 * - Strictly NO mineral field.
 * - Status values: pending, success, failed.
 * - Failure handling: Keep the journal in Firestore, never delete, set status = failed, retry later idempotently.
 */
object TeacherReportHelper {

    private val yyyyMMddFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Converts YYYYMMDD date string to YYYY-MM-DD.
     */
    fun toIsoDate(dateString: String): String {
        return try {
            if (dateString.matches(Regex("\\d{8}"))) {
                val parsed = yyyyMMddFormat.parse(dateString) ?: return dateString
                isoDateFormat.format(parsed)
            } else if (dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                dateString
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Generates primary idempotency key for Spreadsheet synchronization:
     * uniqueKey = "uid|YYYY-MM-DD"
     * Example: "abc123|2026-09-20"
     */
    fun generateUniqueKey(uid: String, dateString: String): String {
        require(uid.isNotBlank()) { "UID cannot be empty for uniqueKey" }
        val isoDate = toIsoDate(dateString)
        return "$uid|$isoDate"
    }

    /**
     * Maps a JournalEntry + StudentProfile into the exact 27-column TeacherReportRow.
     * Guaranteed NO "mineral" field.
     */
    fun toTeacherReportRow(
        entry: JournalEntry,
        profile: StudentProfile
    ): TeacherReportRow {
        val isoDate = toIsoDate(entry.date)
        val key = generateUniqueKey(entry.uid.ifBlank { profile.uid }, entry.date)

        val bangunPagiStr = when {
            entry.bangunPagiTime.isNotBlank() -> "Ya (${entry.bangunPagiTime})"
            entry.bangunPagi -> "Ya"
            else -> "Tidak"
        }

        val ibadahStr = when {
            entry.ibadahSholat.isNotEmpty() -> "Ya (${entry.ibadahSholat.joinToString(", ")})"
            entry.ibadahDetails.isNotBlank() -> "Ya (${entry.ibadahDetails})"
            entry.ibadah -> "Ya"
            else -> "Tidak"
        }

        val olahragaStr = when {
            entry.olahragaActivity.isNotBlank() -> "Ya (${entry.olahragaActivity})"
            entry.olahraga -> "Ya"
            else -> "Tidak"
        }

        val karboStr = when {
            entry.karbohidratText.isNotBlank() -> "Ya (${entry.karbohidratText})"
            entry.karbohidrat -> "Ya"
            else -> "Tidak"
        }

        val proteinStr = when {
            entry.proteinText.isNotBlank() -> "Ya (${entry.proteinText})"
            entry.protein -> "Ya"
            else -> "Tidak"
        }

        val lemakStr = when {
            entry.lemakText.isNotBlank() -> "Ya (${entry.lemakText})"
            entry.lemak -> "Ya"
            else -> "Tidak"
        }

        val vitaminStr = when {
            entry.vitaminText.isNotBlank() -> "Ya (${entry.vitaminText})"
            entry.vitamin -> "Ya"
            else -> "Tidak"
        }

        val seratStr = when {
            entry.seratText.isNotBlank() -> "Ya (${entry.seratText})"
            entry.serat -> "Ya"
            else -> "Tidak"
        }

        val airStr = when {
            entry.airGelas > 0 -> "Ya (${entry.airGelas} Gelas)"
            entry.air -> "Ya"
            else -> "Tidak"
        }

        val tidurCepatStr = when {
            entry.tidurCepatTime.isNotBlank() -> "Ya (${entry.tidurCepatTime})"
            entry.tidurCepat -> "Ya"
            else -> "Tidak"
        }

        val bermasyarakatStr = when {
            entry.bermasyarakatActivity.isNotBlank() -> "Ya (${entry.bermasyarakatActivity})"
            entry.bermasyarakat -> "Ya"
            else -> "Tidak"
        }

        return TeacherReportRow(
            uniqueKey = key,
            timestamp = entry.createdAt,
            tanggal = isoDate,
            uid = entry.uid.ifBlank { profile.uid },
            nama = profile.name.ifBlank { "Siswa DIBINA" },
            NIS = profile.nis,
            sekolah = profile.schoolName.ifBlank { "SD Negeri Karangtalun" },
            kelas = profile.grade.ifBlank { "6A" },
            kodeKelas = profile.classCode ?: "K6-26",
            bangunPagi = bangunPagiStr,
            ibadah = ibadahStr,
            olahraga = olahragaStr,
            karbohidrat = karboStr,
            protein = proteinStr,
            lemak = lemakStr,
            vitamin = vitaminStr,
            serat = seratStr,
            air = airStr,
            materiBelajar = entry.materiBelajar,
            durasiBelajar = entry.durasiBelajar,
            bermasyarakat = bermasyarakatStr,
            tidurCepat = tidurCepatStr,
            exp = entry.exp,
            level = profile.level,
            streak = profile.currentStreak,
            isBackdate = entry.isBackdate,
            updatedAt = entry.updatedAt
        )
    }

    /**
     * Columns ordered exactly as specified for the Google Spreadsheet header:
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
     *
     * STRICTLY NO "mineral" column!
     */
    val SPREADSHEET_COLUMNS = listOf(
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
    )
}
