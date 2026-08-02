package com.example.timetracker.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.Category
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TimeTrackerRepository
import com.example.timetracker.ui.theme.MorandiCalendarDay
import com.example.timetracker.ui.theme.MorandiPrimary
import com.example.timetracker.ui.theme.MorandiSecondary
import com.example.timetracker.ui.theme.MorandiTagBackground
import com.example.timetracker.ui.theme.MorandiTagText
import com.example.timetracker.ui.theme.MorandiTextSecondary
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(repository: TimeTrackerRepository) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val records by repository.allRecords.collectAsState(initial = emptyList())
    val categories by repository.allCategories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showManualEntryDialog by remember { mutableStateOf(false) }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentMonth = currentMonth.minusMonths(1)
                    selectedDate = null
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showMonthPicker = true
                    }
                )

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentMonth = currentMonth.plusMonths(1)
                    selectedDate = null
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val dayRecords = records.filter { it.startTime.toLocalDate() == date }
                            val totalMinutes = dayRecords.sumOf { it.durationSeconds } / 60

                            DayCell(
                                day = dayNumber,
                                totalMinutes = totalMinutes,
                                isSelected = selectedDate == date,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDate = date
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Records list header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate?.let { date ->
                        "${date.monthValue}月${date.dayOfMonth}日记录"
                    } ?: "本月记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedDate != null) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDate = null
                        }
                    ) {
                        Text("显示全部", color = MorandiPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayRecords = if (selectedDate != null) {
                records.filter { it.startTime.toLocalDate() == selectedDate }
            } else {
                records.filter {
                    val date = it.startTime.toLocalDate()
                    date.year == currentMonth.year && date.monthValue == currentMonth.monthValue
                }
            }.sortedByDescending { it.startTime }

            var recordToDelete by remember { mutableStateOf<TimeRecord?>(null) }

            LazyColumn {
                items(displayRecords, key = { it.id }) { record ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                recordToDelete = record
                                false
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFCF6679), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = Color.White
                                )
                            }
                        },
                        content = { RecordItem(record) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (recordToDelete != null) {
                val record = recordToDelete!!
                AlertDialog(
                    onDismissRequest = { recordToDelete = null },
                    title = { Text("删除记录") },
                    text = {
                        Text(
                            "确定删除这条「${record.categoryName.ifBlank { "未分类" }}」记录吗？此操作不可恢复。"
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { repository.deleteRecord(record) }
                                recordToDelete = null
                            }
                        ) {
                            Text("删除", color = Color(0xFFCF6679))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            recordToDelete = null
                        }) {
                            Text("取消")
                        }
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showManualEntryDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = CircleShape,
            containerColor = MorandiPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加记录",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showManualEntryDialog) {
        AddManualRecordDialog(
            categories = categories,
            initialDate = selectedDate ?: LocalDate.now(),
            onDismiss = { showManualEntryDialog = false },
            onConfirm = { categoryId, categoryName, startInstant, endInstant, notes ->
                scope.launch {
                    repository.insertManualRecord(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        startTime = startInstant,
                        endTime = endInstant,
                        notes = notes
                    )
                }
                showManualEntryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddManualRecordDialog(
    categories: List<Category>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (
        categoryId: Long?,
        categoryName: String,
        startInstant: Instant,
        endInstant: Instant,
        notes: String
    ) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val zone = ZoneId.systemDefault()

    var selectedDate by remember { mutableStateOf(initialDate) }
    val now = LocalTime.now()
    var startHour by remember { mutableStateOf(now.hour) }
    var startMinute by remember { mutableStateOf(now.minute) }
    var endHour by remember { mutableStateOf(now.hour) }
    var endMinute by remember { mutableStateOf(now.minute) }

    LaunchedEffect(Unit) {
        val current = LocalTime.now()
        startHour = current.hour
        startMinute = current.minute
        endHour = current.hour
        endMinute = current.minute
    }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val roots = categories.filter { it.parentId == null }
    var selectedRoot by remember { mutableStateOf<Category?>(null) }
    var selectedSub by remember { mutableStateOf<Category?>(null) }

    val children = selectedRoot?.let { root ->
        categories.filter { it.parentId == root.id }
    } ?: emptyList()

    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记录") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Date field
                Column(modifier = Modifier.clickable { showDatePicker = true }) {
                    Text(
                        text = "日期",
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
                            text = selectedDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start / End time rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "开始时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showStartTimePicker = true
                            }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", startHour, startMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "结束时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEndTimePicker = true
                            }
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", endHour, endMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category selection
                Text(
                    text = "选择事项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roots.forEach { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedRoot?.id == category.id,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedRoot = if (selectedRoot?.id == category.id) null else category
                                selectedSub = null
                            }
                        )
                    }
                }

                if (children.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        children.forEach { sub ->
                            SubCategoryChip(
                                category = sub,
                                isSelected = selectedSub?.id == sub.id,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSub = if (selectedSub?.id == sub.id) null else sub
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    placeholder = { Text("写点什么...") },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MorandiPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCF6679)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startLocalTime = LocalTime.of(startHour, startMinute)
                    val endLocalTime = LocalTime.of(endHour, endMinute)

                    val startInstant = selectedDate.atTime(startLocalTime).atZone(zone).toInstant()
                    val endInstant = selectedDate.atTime(endLocalTime).atZone(zone).toInstant()

                    if (!endInstant.isAfter(startInstant)) {
                        errorMessage = "结束时间必须晚于开始时间"
                        return@TextButton
                    }

                    if (selectedDate.isAfter(LocalDate.now())) {
                        errorMessage = "不能选择未来日期"
                        return@TextButton
                    }

                    val category = selectedSub ?: selectedRoot
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(
                        category?.id,
                        category?.name ?: "",
                        startInstant,
                        endInstant,
                        notes
                    )
                }
            ) {
                Text("添加", color = MorandiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
            }) {
                Text("取消")
            }
        }
    )

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                date?.let { selectedDate = it }
                showDatePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "选择开始时间",
            initialHour = startHour,
            initialMinute = startMinute,
            showPlus15 = false,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startHour = hour
                startMinute = minute
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "选择结束时间",
            initialHour = endHour,
            initialMinute = endMinute,
            showPlus15 = true,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endHour = hour
                endMinute = minute
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    showPlus15: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimeWheelPicker(
                    label = "",
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showPlus15) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newEnd = LocalTime.of(selectedHour, selectedMinute).plusMinutes(15)
                                selectedHour = newEnd.hour
                                selectedMinute = newEnd.minute
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreTime,
                                contentDescription = "+15分钟",
                                modifier = Modifier.size(20.dp),
                                tint = MorandiTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val now = LocalTime.now()
                            selectedHour = now.hour
                            selectedMinute = now.minute
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "当前时间",
                            modifier = Modifier.size(20.dp),
                            tint = MorandiPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm(selectedHour, selectedMinute)
                        }
                    ) {
                        Text("确定", color = MorandiPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val zone = ZoneId.systemDefault()
    val initialMillis = initialDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = PastSelectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                        onDateSelected(selected)
                    } ?: onDateSelected(null)
                }
            ) {
                Text("确定", color = MorandiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismiss()
            }) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= LocalDate.now().year
    }
}

@Composable
private fun TimeWheelPicker(
    label: String,
    selectedHour: Int,
    selectedMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 44.dp
    val visibleItems = 5
    val listHeight = itemHeight * visibleItems

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(listHeight)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WheelColumn(
                range = 0..23,
                selectedValue = selectedHour,
                onValueChange = onHourChange,
                itemHeight = itemHeight,
                visibleItems = visibleItems,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            WheelColumn(
                range = 0..59,
                selectedValue = selectedMinute,
                onValueChange = onMinuteChange,
                itemHeight = itemHeight,
                visibleItems = visibleItems,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    itemHeight: Dp,
    visibleItems: Int,
    modifier: Modifier = Modifier
) {
    val paddingItems = visibleItems / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedValue)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(selectedValue) {
        if (listState.firstVisibleItemIndex != selectedValue) {
            listState.animateScrollToItem(selectedValue)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    val target = listState.firstVisibleItemIndex
                    val clamped = target.coerceIn(range.first, range.last)
                    if (clamped != selectedValue) {
                        onValueChange(clamped)
                    }
                }
            }
    }

    Box(modifier = modifier.height(itemHeight * visibleItems)) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(paddingItems) {
                Box(modifier = Modifier.height(itemHeight))
            }

            items(range.count()) { index ->
                val value = range.first + index
                val isSelected = value == selectedValue

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'),
                        style = if (isSelected) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }

            items(paddingItems) {
                Box(modifier = Modifier.height(itemHeight))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
        )
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MorandiSecondary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubCategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MorandiPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    totalMinutes: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTime = totalMinutes > 0
    val hours = totalMinutes / 60.0

    val backgroundColor = when {
        isSelected -> MorandiPrimary
        hasTime -> MorandiCalendarDay
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        hasTime -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            if (hasTime) {
                Text(
                    text = "${String.format("%.1f", hours)}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun RecordItem(record: TimeRecord) {
    val durationText = formatDuration(record.durationSeconds)
    val timeText = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(record.startTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.categoryName.ifBlank { "未分类" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (record.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MorandiTagText
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${secs}s"
    }
}

private fun Instant.toLocalDate(): LocalDate {
    return this.atZone(ZoneId.systemDefault()).toLocalDate()
}
