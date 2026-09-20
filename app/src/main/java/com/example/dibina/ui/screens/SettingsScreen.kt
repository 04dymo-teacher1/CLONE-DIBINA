package com.example.dibina.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.dibina.data.AuthRepository
import com.example.dibina.data.StorageRepository
import com.example.dibina.model.StudentProfile
import com.example.dibina.service.HabitReminderReceiver
import com.example.dibina.ui.components.DefaultAvatarView
import kotlinx.coroutines.launch

/**
 * Settings Screen adhering strictly to requested specifications:
 *
 * Items:
 * 1. Foto Profil (Android Photo Picker, Firebase Storage: users/{uid}/profile/, JPEG/PNG/WEBP)
 * 2. Edit Profil (Name, NIS, Sekolah, Kelas) - Email is never publicly shown.
 * 3. Pengaturan Notifikasi (Notification permission request, daily journal habit reminders)
 * 4. Kode Kelas (Backend validates code, client cannot directly set classId)
 * 5. Tentang (Displays DIBINA, Dikembangkan oleh: Lilik Budi Maryanto, SD Negeri Karangtalun, © Copyright 2026, Email, Description, IMG 3)
 * 6. Logout (Button "Logout", confirmation "Apakah kamu yakin ingin keluar?", buttons "Batal" & "Keluar")
 *
 * Does NOT provide a Spreadsheet URL field.
 */
@Composable
fun SettingsScreen(
    studentProfile: StudentProfile,
    onSignOut: () -> Unit,
    onOpenAchievement: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    authRepository: AuthRepository = remember { AuthRepository() },
    storageRepository: StorageRepository = remember { StorageRepository() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog & UI interaction states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClassCodeDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Profile local edit state (Edit Profil fields)
    var editedName by remember(studentProfile.name) { mutableStateOf(studentProfile.name) }
    var editedNis by remember(studentProfile.nis) { mutableStateOf(studentProfile.nis) }
    var editedSchool by remember(studentProfile.schoolName) {
        mutableStateOf(if (studentProfile.schoolName.isNotBlank()) studentProfile.schoolName else "SD Negeri Karangtalun")
    }
    var editedGrade by remember(studentProfile.grade) { mutableStateOf(studentProfile.grade) }

    // Join Class dialog state
    var classCodeInput by remember { mutableStateOf("") }
    var classCodeError by remember { mutableStateOf<String?>(null) }
    var isJoiningClass by remember { mutableStateOf(false) }

    // Photo upload loading state
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Notification toggles & permission
    val prefs = remember { context.getSharedPreferences("dibina_settings", Context.MODE_PRIVATE) }
    var notifPagiEnabled by remember { mutableStateOf(prefs.getBoolean("notif_pagi", true)) }
    var notifMalamEnabled by remember { mutableStateOf(prefs.getBoolean("notif_malam", true)) }
    var notifFeedEnabled by remember { mutableStateOf(prefs.getBoolean("notif_feed", true)) }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi diaktifkan!", Toast.LENGTH_SHORT).show()
            HabitReminderReceiver.scheduleReminders(context, notifPagiEnabled, notifMalamEnabled)
        } else {
            Toast.makeText(context, "Izin notifikasi ditolak. Pengingat tidak dapat muncul.", Toast.LENGTH_LONG).show()
        }
    }

    // Android Photo Picker Launcher (Modern, zero-permission, safe)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val allowedMimes = listOf("image/jpeg", "image/png", "image/webp")
            if (mimeType.lowercase() !in allowedMimes) {
                Toast.makeText(context, "Format tidak didukung. Harap pilih JPEG, PNG, atau WEBP.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            coroutineScope.launch {
                isUploadingPhoto = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.use { it.readBytes() }
                    if (bytes == null || bytes.isEmpty()) {
                        Toast.makeText(context, "Gagal membaca berkas gambar.", Toast.LENGTH_SHORT).show()
                        isUploadingPhoto = false
                        return@launch
                    }

                    val uploadResult = storageRepository.uploadProfileImage(
                        uid = studentProfile.uid,
                        imageBytes = bytes,
                        mimeType = mimeType
                    )

                    uploadResult.onSuccess { downloadUrl ->
                        authRepository.updateStudentEditableProfile(
                            uid = studentProfile.uid,
                            name = editedName,
                            nis = editedNis,
                            schoolName = editedSchool,
                            grade = editedGrade,
                            religion = studentProfile.religion,
                            profilePhotoUrl = downloadUrl
                        )
                        Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Gagal mengunggah foto: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Terjadi kesalahan saat memproses foto.", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title
        item {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Kelola akun dan pengaturan aplikasi DIBINA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // 1. PROFILE HEADER CARD
        // Shows: Foto Profil, Nama, NIS, Sekolah, Kelas, Kode Kelas, Level, EXP, Streak
        // NEVER publicly shows email.
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_profile_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        DefaultAvatarView(
                            photoUrl = studentProfile.photoUrl,
                            size = 84.dp
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("change_photo_fab"),
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isUploadingPhoto) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = stringResource(R.string.settings_profile_photo),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = studentProfile.name.ifBlank { "Siswa DIBINA" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // NIS (if required/present)
                    if (studentProfile.nis.isNotBlank()) {
                        Text(
                            text = "NIS: ${studentProfile.nis}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sekolah & Kelas
                    val schoolDisplay = studentProfile.schoolName.ifBlank { "SD Negeri Karangtalun" }
                    val gradeDisplay = if (studentProfile.grade.isNotBlank()) "Kelas ${studentProfile.grade}" else "Siswa Aktif"
                    Text(
                        text = "$schoolDisplay • $gradeDisplay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Kode Kelas
                    val displayCode = studentProfile.classCode ?: "K6-26"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Kode Kelas: $displayCode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats: Level, EXP, Streak
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(title = "Level", value = "${studentProfile.level}")
                        ProfileStatItem(title = "Total EXP", value = "${studentProfile.totalExp}")
                        ProfileStatItem(title = "Streak", value = "${studentProfile.currentStreak} Hari")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onOpenAchievement,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Prestasi & 13 Lencana",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. SETTINGS ITEMS MENU
        // Items:
        // - Foto Profil
        // - Edit Profil
        // - Pengaturan Notifikasi
        // - Kode Kelas
        // - Tentang
        // - Logout
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_menu_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    // Item 1: Foto Profil
                    SettingsRowItem(
                        icon = Icons.Default.AccountCircle,
                        title = stringResource(R.string.settings_profile_photo),
                        subtitle = "Ganti foto profil dengan galeri (JPEG/PNG/WEBP)",
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        testTag = "settings_item_foto_profil"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Item 2: Edit Profil
                    SettingsRowItem(
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.settings_edit_profile),
                        subtitle = "Perbarui nama siswa, NIS, dan nama sekolah",
                        onClick = {
                            editedName = studentProfile.name
                            editedNis = studentProfile.nis
                            editedSchool = studentProfile.schoolName.ifBlank { "SD Negeri Karangtalun" }
                            editedGrade = studentProfile.grade
                            showEditProfileDialog = true
                        },
                        testTag = "settings_item_edit_profil"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Item 3: Kode Kelas
                    SettingsRowItem(
                        icon = Icons.Default.Class,
                        title = stringResource(R.string.settings_class_code),
                        subtitle = "Kelas aktif: ${studentProfile.classCode ?: "K6-26"}",
                        onClick = {
                            classCodeInput = studentProfile.classCode ?: ""
                            classCodeError = null
                            showClassCodeDialog = true
                        },
                        testTag = "settings_item_kode_kelas"
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Item 4: Tentang (Opens dedicated About page with IMG 3)
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = "Pengembang, hak cipta © 2026, dan deskripsi DIBINA",
                        onClick = onOpenAbout,
                        testTag = "settings_item_tentang"
                    )
                }
            }
        }

        // ==========================================
        // 3. PENGATURAN NOTIFIKASI
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_notifications_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_notifications),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notif 1: Pengingat Pagi (05.00)
                    NotificationToggleRow(
                        title = "Pengingat Pagi (05.00)",
                        description = "Mengingatkan bangun pagi, ibadah shubuh, dan senam pagi",
                        checked = notifPagiEnabled,
                        onCheckedChange = { enabled ->
                            notifPagiEnabled = enabled
                            prefs.edit().putBoolean("notif_pagi", enabled).apply()
                            if (enabled) requestNotificationPermissionIfNeeded()
                            HabitReminderReceiver.scheduleReminders(context, enabled, notifMalamEnabled)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Notif 2: Pengingat Jurnal Malam (20.00)
                    NotificationToggleRow(
                        title = "Pengingat Jurnal Malam (20.00)",
                        description = "Mengingatkan melengkapi 7 Kebiasaan sebelum tidur tepat waktu",
                        checked = notifMalamEnabled,
                        onCheckedChange = { enabled ->
                            notifMalamEnabled = enabled
                            prefs.edit().putBoolean("notif_malam", enabled).apply()
                            if (enabled) requestNotificationPermissionIfNeeded()
                            HabitReminderReceiver.scheduleReminders(context, notifPagiEnabled, enabled)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Notif 3: Pemberitahuan Kabar Teman
                    NotificationToggleRow(
                        title = "Pemberitahuan Kabar Teman",
                        description = "Notifikasi saat teman sekelas membagikan kebiasaan positif",
                        checked = notifFeedEnabled,
                        onCheckedChange = { enabled ->
                            notifFeedEnabled = enabled
                            prefs.edit().putBoolean("notif_feed", enabled).apply()
                        }
                    )
                }
            }
        }

        // ==========================================
        // 4. LOGOUT BUTTON
        // Button: "Logout"
        // Confirmation: "Apakah kamu yakin ingin keluar?"
        // Buttons: "Batal" & "Keluar"
        // ==========================================
        item {
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_logout),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_edit_profile),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nama Lengkap:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "NIS (Nomor Induk Siswa):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = editedNis,
                        onValueChange = { editedNis = it },
                        singleLine = true,
                        placeholder = { Text("Contoh: 12345") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Nama Sekolah:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = editedSchool,
                        onValueChange = { editedSchool = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Kelas:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = editedGrade,
                        onValueChange = { editedGrade = it },
                        singleLine = true,
                        placeholder = { Text("Contoh: 6A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.updateStudentEditableProfile(
                                uid = studentProfile.uid,
                                name = editedName.ifBlank { studentProfile.name },
                                nis = editedNis,
                                schoolName = editedSchool,
                                grade = editedGrade,
                                religion = studentProfile.religion,
                                profilePhotoUrl = studentProfile.profilePhotoUrl
                            )
                            Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            showEditProfileDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(stringResource(R.string.btn_batal))
                }
            }
        )
    }

    // Kode Kelas Dialog (Validates code on backend; cannot directly set classId)
    if (showClassCodeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isJoiningClass) showClassCodeDialog = false
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_class_code),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.class_code_description),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = classCodeInput,
                        onValueChange = {
                            classCodeInput = it.uppercase()
                            classCodeError = null
                        },
                        singleLine = true,
                        placeholder = { Text("Contoh: K6-26") },
                        isError = classCodeError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (classCodeError != null) {
                        Text(
                            text = classCodeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (classCodeInput.isBlank()) {
                            classCodeError = "Kode kelas wajib diisi."
                            return@Button
                        }
                        isJoiningClass = true
                        coroutineScope.launch {
                            val result = authRepository.joinClassWithCode(
                                uid = studentProfile.uid,
                                rawCode = classCodeInput,
                                studentName = studentProfile.name
                            )
                            result.onSuccess { classInfo ->
                                Toast.makeText(context, "Berhasil bergabung ke ${classInfo.className}!", Toast.LENGTH_SHORT).show()
                                showClassCodeDialog = false
                            }.onFailure { err ->
                                classCodeError = err.localizedMessage ?: context.getString(R.string.error_invalid_class_code)
                            }
                            isJoiningClass = false
                        }
                    },
                    enabled = !isJoiningClass
                ) {
                    if (isJoiningClass) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.btn_join_class))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClassCodeDialog = false },
                    enabled = !isJoiningClass
                ) {
                    Text(stringResource(R.string.btn_batal))
                }
            }
        )
    }

    // Logout Confirmation Dialog
    // Title / Prompt: "Apakah kamu yakin ingin keluar?"
    // Buttons: "Batal" & "Keluar"
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_logout),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.logout_confirm_prompt),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text(stringResource(R.string.btn_keluar))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text(stringResource(R.string.btn_batal))
                }
            }
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
