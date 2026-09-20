package com.example

import com.example.dibina.domain.TeacherReportHelper
import com.example.dibina.model.JournalEntry
import com.example.dibina.model.SpreadsheetSyncStatus
import com.example.dibina.model.StudentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherReportTest {

    @Test
    fun testUniqueKeyFormatting() {
        val key1 = TeacherReportHelper.generateUniqueKey("user123", "20260920")
        assertEquals("user123|2026-09-20", key1)

        val key2 = TeacherReportHelper.generateUniqueKey("abc", "2026-09-20")
        assertEquals("abc|2026-09-20", key2)
    }

    @Test
    fun testSpreadsheetColumnsDoNotContainMineral() {
        val columns = TeacherReportHelper.SPREADSHEET_COLUMNS

        assertEquals(27, columns.size)
        assertTrue(columns.contains("uniqueKey"))
        assertTrue(columns.contains("timestamp"))
        assertTrue(columns.contains("tanggal"))
        assertTrue(columns.contains("uid"))
        assertTrue(columns.contains("nama"))
        assertTrue(columns.contains("NIS"))
        assertTrue(columns.contains("sekolah"))
        assertTrue(columns.contains("kelas"))
        assertTrue(columns.contains("kodeKelas"))
        assertTrue(columns.contains("bangunPagi"))
        assertTrue(columns.contains("ibadah"))
        assertTrue(columns.contains("olahraga"))
        assertTrue(columns.contains("karbohidrat"))
        assertTrue(columns.contains("protein"))
        assertTrue(columns.contains("lemak"))
        assertTrue(columns.contains("vitamin"))
        assertTrue(columns.contains("serat"))
        assertTrue(columns.contains("air"))
        assertTrue(columns.contains("materiBelajar"))
        assertTrue(columns.contains("durasiBelajar"))
        assertTrue(columns.contains("bermasyarakat"))
        assertTrue(columns.contains("tidurCepat"))
        assertTrue(columns.contains("exp"))
        assertTrue(columns.contains("level"))
        assertTrue(columns.contains("streak"))
        assertTrue(columns.contains("isBackdate"))
        assertTrue(columns.contains("updatedAt"))

        // CRITICAL CONSTRAINT: Strictly NO mineral column!
        assertFalse("Columns MUST NOT contain mineral!", columns.contains("mineral"))
    }

    @Test
    fun testReportRowMapping() {
        val profile = StudentProfile(
            uid = "student_99",
            name = "Siti Rahma",
            nis = "88821",
            schoolName = "SD Negeri Karangtalun",
            grade = "6A",
            classCode = "K6-26",
            level = 2,
            currentStreak = 4
        )

        val entry = JournalEntry(
            uid = "student_99",
            date = "20260920",
            bangunPagi = true,
            bangunPagiTime = "04:30",
            ibadah = true,
            ibadahSholat = listOf("Subuh", "Zuhur"),
            olahraga = true,
            olahragaActivity = "Senam",
            karbohidrat = true,
            karbohidratText = "Nasi",
            protein = true,
            proteinText = "Telur",
            lemak = true,
            vitamin = true,
            serat = true,
            air = true,
            airGelas = 8,
            materiBelajar = "Matematika",
            durasiBelajar = 30,
            bermasyarakat = true,
            bermasyarakatActivity = "Membantu ibu",
            tidurCepat = true,
            tidurCepatTime = "20:00",
            exp = 80,
            isBackdate = false,
            createdAt = 1789968000000L,
            updatedAt = 1789968000000L
        )

        val row = TeacherReportHelper.toTeacherReportRow(entry, profile)

        assertEquals("student_99|2026-09-20", row.uniqueKey)
        assertEquals("2026-09-20", row.tanggal)
        assertEquals("student_99", row.uid)
        assertEquals("Siti Rahma", row.nama)
        assertEquals("88821", row.NIS)
        assertEquals("SD Negeri Karangtalun", row.sekolah)
        assertEquals("6A", row.kelas)
        assertEquals("K6-26", row.kodeKelas)
        assertEquals("Ya (04:30)", row.bangunPagi)
        assertEquals("Ya (Subuh, Zuhur)", row.ibadah)
        assertEquals("Ya (Senam)", row.olahraga)
        assertEquals("Ya (Nasi)", row.karbohidrat)
        assertEquals("Ya (Telur)", row.protein)
        assertEquals("Ya (8 Gelas)", row.air)
        assertEquals("Matematika", row.materiBelajar)
        assertEquals(30, row.durasiBelajar)
        assertEquals("Ya (Membantu ibu)", row.bermasyarakat)
        assertEquals("Ya (20:00)", row.tidurCepat)
        assertEquals(80, row.exp)
        assertEquals(2, row.level)
        assertEquals(4, row.streak)
        assertEquals(false, row.isBackdate)
    }

    @Test
    fun testSyncStatusConstants() {
        assertEquals("pending", SpreadsheetSyncStatus.PENDING)
        assertEquals("success", SpreadsheetSyncStatus.SUCCESS)
        assertEquals("failed", SpreadsheetSyncStatus.FAILED)
    }
}
