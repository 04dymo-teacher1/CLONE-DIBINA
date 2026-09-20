package com.example.dibina.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dibina.domain.ExpCalculator
import com.example.dibina.domain.JournalIdHelper
import com.example.dibina.model.HabitType
import com.example.dibina.model.JournalEntry
import com.example.dibina.model.StudentProfile
import com.example.ui.theme.DibinaGoldExp
import com.example.ui.theme.DibinaOrangeAccent

/**
 * Rekap Screen displaying the student's journal history.
 *
 * Requirements:
 * - tanggal
 * - ringkasan aktivitas
 * - EXP
 * - level
 * - streak
 * - Empty state: "Belum ada jurnal."
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecapScreen(
    journalHistory: List<JournalEntry>,
    studentProfile: StudentProfile = StudentProfile(),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_recap),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Riwayat Catatan & Perkembangan Kebiasaan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Student Stats Header Card (EXP, Level, Streak)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recap_stats_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profil Pencapaian Siswa",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = studentProfile.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Level
                        RecapMetricBadge(
                            label = "Level",
                            value = "${studentProfile.level}",
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFF2563EB),
                            backgroundColor = Color(0xFFEFF6FF),
                            modifier = Modifier.weight(1f)
                        )

                        // EXP
                        RecapMetricBadge(
                            label = "Total EXP",
                            value = "${studentProfile.totalExp}",
                            icon = Icons.Default.ElectricBolt,
                            iconColor = DibinaGoldExp,
                            backgroundColor = Color(0xFFFEF3C7),
                            modifier = Modifier.weight(1f)
                        )

                        // Streak
                        RecapMetricBadge(
                            label = "Streak",
                            value = "${studentProfile.currentStreak} Hari",
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = DibinaOrangeAccent,
                            backgroundColor = Color(0xFFFFF7ED),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 7 Kebiasaan Achievement Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recap_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Statistik 7 Kebiasaan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HabitType.entries.forEach { habit ->
                        val completedTimes = journalHistory.count { it.isHabitCompleted(habit) }
                        val totalJournals = journalHistory.size.coerceAtLeast(1)
                        val progress = if (journalHistory.isEmpty()) 0f else completedTimes.toFloat() / totalJournals.toFloat()

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$completedTimes hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Riwayat Jurnal
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Riwayat Catatan Harian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Empty state or History List (NO DUMMY DATA)
        if (journalHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("empty_recap_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.empty_recap_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.empty_recap_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(journalHistory, key = { it.journalId }) { entry ->
                JournalHistoryCard(
                    entry = entry,
                    studentLevel = studentProfile.level,
                    currentStreak = studentProfile.currentStreak
                )
            }
        }
    }
}

@Composable
fun RecapMetricBadge(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalHistoryCard(
    entry: JournalEntry,
    studentLevel: Int,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val estimatedExp = ExpCalculator.calculateEstimatedExp(entry.completedCount, false)
    val activityList = buildActivitySummaryList(entry)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("journal_history_${entry.journalId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Tanggal & Badges (EXP, Level, Streak)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tanggal
                Text(
                    text = JournalIdHelper.formatFriendlyDate(entry.date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Completed habit count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.completedCount}/7 Kebiasaan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: EXP, Level, Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // EXP
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = DibinaGoldExp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$estimatedExp EXP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                // Level
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Level $studentLevel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }

                // Streak
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF7ED)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = DibinaOrangeAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentStreak Hari",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ringkasan Aktivitas (Activity summary chips / list)
            Text(
                text = "Ringkasan Aktivitas:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (activityList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activityList.forEach { act ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = act,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Aktivitas tercatat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.journalReflection.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "\"${entry.journalReflection}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Builds readable Indonesian summaries of completed activities in a journal entry.
 */
private fun buildActivitySummaryList(entry: JournalEntry): List<String> {
    val items = mutableListOf<String>()

    if (entry.bangunPagi || entry.bangunPagiTime.isNotBlank()) {
        val timeStr = if (entry.bangunPagiTime.isNotBlank()) " pukul ${entry.bangunPagiTime}" else ""
        items.add("🌅 Bangun pagi$timeStr")
    }

    if (entry.ibadah || entry.ibadahSholat.isNotEmpty() || entry.ibadahDetails.isNotBlank()) {
        val prayersStr = if (entry.ibadahSholat.isNotEmpty()) " (${entry.ibadahSholat.joinToString()})" else if (entry.ibadahDetails.isNotBlank()) " (${entry.ibadahDetails})" else ""
        items.add("🤲 Beribadah$prayersStr")
    }

    if (entry.olahraga || entry.olahragaActivity.isNotBlank()) {
        val actStr = if (entry.olahragaActivity.isNotBlank()) ": ${entry.olahragaActivity}" else ""
        items.add("🏃 Berolahraga$actStr")
    }

    if (entry.isHabitCompleted(HabitType.MAKAN_SEHAT)) {
        val waterStr = if (entry.airGelas > 0) ", ${entry.airGelas} gelas air" else ""
        items.add("🥗 Makan sehat$waterStr")
    }

    if (entry.materiBelajar.isNotBlank() && entry.durasiBelajar >= 15) {
        items.add("📖 Gemar belajar: ${entry.materiBelajar} (${entry.durasiBelajar} mnt)")
    }

    if (entry.bermasyarakat || entry.bermasyarakatActivity.isNotBlank()) {
        val actStr = if (entry.bermasyarakatActivity.isNotBlank()) ": ${entry.bermasyarakatActivity}" else ""
        items.add("🤝 Bermasyarakat$actStr")
    }

    if (entry.tidurCepat || entry.tidurCepatTime.isNotBlank()) {
        val timeStr = if (entry.tidurCepatTime.isNotBlank()) " pukul ${entry.tidurCepatTime}" else ""
        items.add("🌙 Tidur tepat waktu$timeStr")
    }

    return items
}

