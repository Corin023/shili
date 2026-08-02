package com.example.timetracker.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.Category
import com.example.timetracker.ui.theme.MorandiPrimary
import com.example.timetracker.ui.theme.MorandiSecondary
import com.example.timetracker.ui.theme.MorandiTagText
import com.example.timetracker.ui.theme.MorandiTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onManageCategories: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "时厘",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "岁月不居，时节如流",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer Circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(8.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val timeText = when (uiState.timerState) {
                    is TimerState.Idle -> "00:00:00"
                    is TimerState.Running -> viewModel.formatElapsed(uiState.elapsedSeconds)
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (uiState.timerState is TimerState.Running) "专注中" else "准备开始",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start/Stop Button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when (uiState.timerState) {
                    is TimerState.Idle -> viewModel.startTimer()
                    is TimerState.Running -> viewModel.stopTimer()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.timerState is TimerState.Running)
                    MorandiSecondary else MorandiPrimary
            )
        ) {
            Text(
                text = if (uiState.timerState is TimerState.Running) "结束计时" else "开始计时",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Root categories
        val roots = uiState.categories.filter { it.parentId == null }
        val selectedRoot = uiState.selectedCategory?.let { selected ->
            if (selected.parentId == null) selected
            else uiState.categories.find { it.id == selected.parentId }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择事项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onManageCategories()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "标签管理",
                    modifier = Modifier.size(20.dp),
                    tint = MorandiTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roots.forEach { category ->
                val isSelected = selectedRoot?.id == category.id
                CategoryChip(
                    category = category,
                    isSelected = isSelected,
                    onClick = { viewModel.selectCategory(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-categories of selected root
        val children = selectedRoot?.let { root ->
            uiState.categories.filter { it.parentId == root.id }
        } ?: emptyList()

        if (children.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                children.forEach { sub ->
                    SubCategoryChip(
                        category = sub,
                        isSelected = uiState.selectedCategory?.id == sub.id,
                        onClick = { viewModel.selectCategory(sub) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Add new category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.newCategoryName,
                onValueChange = viewModel::onNewCategoryNameChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入新事项，如：冥想", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.addCategory() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MorandiPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.addCategory()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorandiSecondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("添加", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
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
