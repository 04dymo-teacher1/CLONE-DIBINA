package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dibina.model.StudentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthProfileSettingsTest {

    @Test
    fun testDefaultProfileValues() {
        val defaultProfile = StudentProfile()
        assertEquals(0L, defaultProfile.totalExp)
        assertEquals(1, defaultProfile.level)
        assertEquals(0, defaultProfile.currentStreak)
        assertEquals(0, defaultProfile.longestStreak)
        assertEquals("student", defaultProfile.role)
        assertNull(defaultProfile.classId)
        assertNull(defaultProfile.classCode)
        assertNull(defaultProfile.profilePhotoUrl)
    }

    @Test
    fun testResolvedNameFallback() {
        // Fallback test logic: Google displayName -> email prefix -> fallback
        fun resolveName(displayName: String?, email: String?): String {
            return when {
                !displayName.isNullOrBlank() -> displayName
                !email.isNullOrBlank() && email.contains("@") -> email.substringBefore("@")
                else -> "Siswa DIBINA"
            }
        }

        assertEquals("Budi Santoso", resolveName("Budi Santoso", "budi@gmail.com"))
        assertEquals("ahmad123", resolveName("", "ahmad123@guru.sd.belajar.id"))
        assertEquals("ahmad123", resolveName(null, "ahmad123@guru.sd.belajar.id"))
        assertEquals("Siswa DIBINA", resolveName(null, null))
    }

    @Test
    fun testAboutScreenStringsInResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("DIBINA", context.getString(R.string.about_app_name))
        assertEquals("Lilik Budi Maryanto", context.getString(R.string.about_developer_name))
        assertEquals("SD Negeri Karangtalun", context.getString(R.string.about_school))
        assertEquals("© Copyright 2026", context.getString(R.string.about_copyright))
        assertEquals("lilikmaryanto43@guru.sd.belajar.id", context.getString(R.string.about_email_val))
        assertTrue(context.getString(R.string.about_exact_description).contains("7 KAIH"))
        assertTrue(context.getString(R.string.about_exact_description).contains("7 Kebiasaan Anak Indonesia Hebat"))
    }

    @Test
    fun testLogoutStringsInResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Apakah kamu yakin ingin keluar?", context.getString(R.string.logout_confirm_prompt))
        assertEquals("Batal", context.getString(R.string.btn_batal))
        assertEquals("Keluar", context.getString(R.string.btn_keluar))
        assertEquals("Logout", context.getString(R.string.settings_logout))
    }
}
