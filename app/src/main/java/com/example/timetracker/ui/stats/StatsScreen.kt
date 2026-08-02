package com.example.timetracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.Category
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TimeTrackerRepository
import com.example.timetracker.ui.theme.MorandiCalendarDay
import com.example.timetracker.ui.theme.MorandiPrimary
import com.example.timetracker.ui.theme.MorandiSecondary
import com.example.timetracker.ui.theme.MorandiTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class StatsPeriod {
    TODAY, LAST_7_DAYS, WEEK, LAST_30_DAYS, MONTH, YEAR, CUSTOM
}

private data class PeriodInfo(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val title: String,
    val rangeText: String
)

private data class Bar(val label: String, val seconds: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    repository: TimeTrackerRepository,
    onExportCsv: (Instant, Instant) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val allCategories by repository.allCategories.collectAsState(initial = emptyList())
    val allRecords by repository.allRecords.collectAsState(initial = emptyList())
    val totals = remember { mutableStateMapOf<Long, Long>() }

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }
    var customStart by remember { mutableStateOf(LocalDate.now().minusDays(14)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    var selectedRootId by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val now = LocalDate.now()

    val periodInfo = when (selectedPeriod) {
        StatsPeriod.TODAY -> PeriodInfo(now, now, "今日总专注时间", formatRange(now, now))
        StatsPeriod.LAST_7_DAYS -> {
            val start = now.minusDays(6)
            PeriodInfo(start, now, "近7天总专注时间", formatRange(start, now))
        }
        StatsPeriod.WEEK -> {
            val start = now.minusDays((now.dayOfWeek.value % 7).toLong())
            PeriodInfo(start, now, "本周总专注时间", formatRange(start, now))
        }
        StatsPeriod.LAST_30_DAYS -> {
            val start = now.minusDays(29)
            PeriodInfo(start, now, "近30天总专注时间", formatRange(start, now))
        }
        StatsPeriod.MONTH -> {
            val start = now.withDayOfMonth(1)
            PeriodInfo(start, now, "本月总专注时间", formatRange(start, now))
        }
        StatsPeriod.YEAR -> {
            val start = now.withDayOfYear(1)
            PeriodInfo(start, now, "本年总专注时间", formatRange(start, now))
        }
        StatsPeriod.CUSTOM -> PeriodInfo(
            customStart, customEnd, "自定义区间总专注时间", formatRange(customStart, customEnd)
        )
    }

    val startInstant = periodInfo.startDate.atStartOfDay(zone).toInstant()
    val endInstant = periodInfo.endDate.plusDays(1).atStartOfDay(zone).toInstant()

    LaunchedEffect(allRecords, selectedPeriod, customStart, customEnd) {
        allCategories.forEach { category ->
            totals[category.id] = repository.getTotalSecondsByCategory(
                category.id, startInstant, endInstant
            )
        }
    }

    val rootCategories = allCategories.filter { it.parentId == null }

    val rolledUpTotals = remember(allCategories, totals.toMap()) {
        val direct = allCategories.associate { it.id to (totals[it.id] ?: 0L) }.toMutableMap()
        fun rollUp(category: Category): Long {
            val children = allCategories.filter { it.parentId == category.id }
            val childrenSum = children.sumOf { rollUp(it) }
            val total = (direct[category.id] ?: 0L) + childrenSum
            direct[category.id] = total
            return total
        }
        rootCategories.forEach { rollUp(it) }
        direct
    }

    val grandTotal = rootCategories.sumOf { rolledUpTotals[it.id] ?: 0L }

    val bars = remember(allRecords, selectedPeriod, periodInfo) {
        generateBars(allRecords, periodInfo.startDate, periodInfo.endDate, selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header with CSV export icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "统计",
                style = MaterialTheme.typography.headlineLarge
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportCsv(startInstant, endInstant)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "导出 CSV",
                    modifier = Modifier.size(20.dp),
                    tint = MorandiTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Total time
        Text(
            text = periodInfo.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDuration(grandTotal),
            style = MaterialTheme.typography.displayMedium,
            color = MorandiPrimary
        )
        Text(
            text = periodInfo.rangeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Period selector
        PeriodSelector(
            selected = selectedPeriod,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedPeriod = it
                selectedRootId = null
            }
        )

        // Custom date range
        if (selectedPeriod == StatsPeriod.CUSTOM) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    label = "开始",
                    date = customStart,
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    label = "结束",
                    date = customEnd,
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bar chart
        if (grandTotal > 0L) {
            BarChart(bars = bars)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Category list
        if (selectedRootId == null) {
            val sortedRoots = rootCategories
                .map { it to (rolledUpTotals[it.id] ?: 0L) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }

            sortedRoots.forEachIndexed { index, (category, total) ->
                CompactStatItem(
                    rank = index + 1,
                    name = category.name,
                    seconds = total,
                    maxSeconds = grandTotal.coerceAtLeast(1),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRootId = category.id
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        } else {
            // Drill-down view
            val root = allCategories.find { it.id == selectedRootId }
            val subCategories = allCategories.filter { it.parentId == selectedRootId }
            val rootTotal = rolledUpTotals[selectedRootId] ?: 0L

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedRootId = null
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = root?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            subCategories.forEachIndexed { index, sub ->
                val total = totals[sub.id] ?: 0L
                CompactStatItem(
                    rank = index + 1,
                    name = sub.name,
                    seconds = total,
                    maxSeconds = rootTotal.coerceAtLeast(1),
                    onClick = null
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val directRootTotal = allRecords
                .filter { it.categoryId == selectedRootId }
                .filter { it.startTime.isAfter(startInstant) && it.startTime.isBefore(endInstant) }
                .sumOf { it.durationSeconds }

            if (directRootTotal > 0) {
                CompactStatItem(
                    rank = null,
                    name = "${root?.name}（未细分）",
                    seconds = directRootTotal,
                    maxSeconds = rootTotal.coerceAtLeast(1),
                    onClick = null
                )
            }
        }

        if (grandTotal == 0L) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有记录，开始一次专注吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { millis ->
                millis?.let {
                    customStart = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    if (customStart.isAfter(customEnd)) customEnd = customStart
                }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { millis ->
                millis?.let {
                    customEnd = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                    if (customEnd.isBefore(customStart)) customStart = customEnd
                }
                showEndDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDateSelected(datePickerState.selectedDateMillis)
                }
            ) {
                Text("确定", color = MorandiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismissRequest()
            }) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit
) {
    val periods = listOf(
        StatsPeriod.TODAY to "今日",
        StatsPeriod.LAST_7_DAYS to "近7天",
        StatsPeriod.WEEK to "本周",
        StatsPeriod.LAST_30_DAYS to "近30天",
        StatsPeriod.MONTH to "本月",
        StatsPeriod.YEAR to "本年",
        StatsPeriod.CUSTOM to "自定义"
    )
    Column {
        periods.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (period, label) ->
                    PeriodButton(
                        text = label,
                        selected = selected == period,
                        onClick = { onSelect(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size < 4) {
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PeriodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .background(
                color = if (selected) MorandiPrimary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun BarChart(bars: List<Bar>) {
    if (bars.isEmpty()) return
    val maxValue = bars.maxOfOrNull { it.seconds }?.coerceAtLeast(1f) ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight((bar.seconds / maxValue).coerceIn(0f, 1f))
                        .background(MorandiPrimary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactStatItem(
    rank: Int?,
    name: String,
    seconds: Long,
    maxSeconds: Long,
    onClick: (() -> Unit)?
) {
    val percentage = if (maxSeconds > 0) seconds.toFloat() / maxSeconds else 0f
    val color = when (rank) {
        1 -> MorandiPrimary
        2 -> MorandiSecondary
        3 -> MorandiCalendarDay
        else -> MorandiSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (rank != null && rank <= 3) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Top $rank",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MorandiPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.1f", percentage * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(2.5.dp))
            )
        }
    }
}

private fun generateBars(
    records: List<TimeRecord>,
    startDate: LocalDate,
    endDate: LocalDate,
    period: StatsPeriod
): List<Bar> {
    val zone = ZoneId.systemDefault()
    val filtered = records.filter {
        val date = it.startTime.atZone(zone).toLocalDate()
        !date.isBefore(startDate) && !date.isAfter(endDate)
    }
    val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    return when {
        period == StatsPeriod.TODAY -> {
            listOf(Bar("今", filtered.sumOf { it.durationSeconds }.toFloat()))
        }
        daysBetween <= 14 -> {
            (0 until daysBetween).map { offset ->
                val date = startDate.plusDays(offset.toLong())
                val dayRecords = filtered.filter { it.startTime.atZone(zone).toLocalDate() == date }
                val seconds = dayRecords.sumOf { it.durationSeconds }.toFloat()
                val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Bar(label, seconds)
            }
        }
        period == StatsPeriod.YEAR -> {
            val startMonth = startDate.monthValue
            val endMonth = endDate.monthValue
            (startMonth..endMonth).map { month ->
                val monthRecords = filtered.filter { it.startTime.atZone(zone).monthValue == month }
                Bar("${month}月", monthRecords.sumOf { it.durationSeconds }.toFloat())
            }
        }
        else -> {
            val weeks = 4
            val daysPerWeek = daysBetween / weeks
            (0 until weeks).map { weekIndex ->
                val weekStart = startDate.plusDays((weekIndex * daysPerWeek).toLong())
                val weekEnd = if (weekIndex == weeks - 1) endDate else weekStart.plusDays(daysPerWeek.toLong() - 1)
                val weekRecords = filtered.filter {
                    val date = it.startTime.atZone(zone).toLocalDate()
                    !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                }
                Bar("W${weekIndex + 1}", weekRecords.sumOf { it.durationSeconds }.toFloat())
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600.0
    return when {
        hours >= 1 -> "${String.format("%.1f", hours)}h"
        seconds >= 60 -> "${seconds / 60}m"
        else -> "${seconds}s"
    }
}

private fun formatRange(start: LocalDate, end: LocalDate): String {
    return if (start.year == end.year) {
        "${start.monthValue}月${start.dayOfMonth}日 - ${end.monthValue}月${end.dayOfMonth}日"
    } else {
        "${start.year}年${start.monthValue}月${start.dayOfMonth}日 - ${end.year}年${end.monthValue}月${end.dayOfMonth}日"
    }
}
