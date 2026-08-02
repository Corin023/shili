package com.example.timetracker.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timetracker.data.Category
import com.example.timetracker.data.TimeTrackerRepository
import com.example.timetracker.ui.theme.MorandiPrimary
import com.example.timetracker.ui.theme.MorandiSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    repository: TimeTrackerRepository,
    onBack: () -> Unit
) {
    val allCategories by repository.allCategories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var newRootName by remember { mutableStateOf("") }

    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToAddChild by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("标签管理", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Add root category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newRootName,
                    onValueChange = { newRootName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("新建一级分类，如：学习", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MorandiPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val name = newRootName.trim()
                        if (name.isNotBlank()) {
                            scope.launch { repository.insertCategory(name) }
                            newRootName = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MorandiPrimary)
                ) {
                    Text("添加")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "长按可拖动排序（暂未支持）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category tree
            val roots = allCategories.filter { it.parentId == null }.sortedBy { it.name }
            roots.forEach { root ->
                CategoryTreeItem(
                    category = root,
                    depth = 0,
                    allCategories = allCategories,
                    repository = repository,
                    onDelete = { categoryToDelete = it },
                    onEdit = { categoryToEdit = it },
                    onAddChild = { categoryToAddChild = it }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (categoryToDelete != null) {
        val category = categoryToDelete!!
        val hasChildren = allCategories.any { it.parentId == category.id }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("删除标签") },
            text = {
                Text(
                    if (hasChildren)
                        "确定删除「${category.name}」及其所有子标签吗？相关记录会变成未分类。"
                    else
                        "确定删除「${category.name}」吗？相关记录会变成未分类。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { repository.deleteCategory(category.id) }
                        categoryToDelete = null
                    }
                ) {
                    Text("删除", color = MorandiSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    categoryToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // Edit dialog
    if (categoryToEdit != null) {
        EditCategoryDialog(
            category = categoryToEdit!!,
            allCategories = allCategories,
            repository = repository,
            onDismiss = { categoryToEdit = null }
        )
    }

    // Add child dialog
    if (categoryToAddChild != null) {
        AddChildCategoryDialog(
            parent = categoryToAddChild!!,
            repository = repository,
            onDismiss = { categoryToAddChild = null }
        )
    }
}

@Composable
private fun CategoryTreeItem(
    category: Category,
    depth: Int,
    allCategories: List<Category>,
    repository: TimeTrackerRepository,
    onDelete: (Category) -> Unit,
    onEdit: (Category) -> Unit,
    onAddChild: (Category) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val children = allCategories.filter { it.parentId == category.id }.sortedBy { it.name }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width((depth * 24).dp))

        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        TextButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAddChild(category)
        }) {
            Text("+子类", color = MorandiPrimary)
        }

        TextButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEdit(category)
        }) {
            Text("编辑", color = MorandiPrimary)
        }

        TextButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete(category)
        }) {
            Text("删除", color = MorandiSecondary)
        }
    }

    children.forEach { child ->
        CategoryTreeItem(
            category = child,
            depth = depth + 1,
            allCategories = allCategories,
            repository = repository,
            onDelete = onDelete,
            onEdit = onEdit,
            onAddChild = onAddChild
        )
    }
}

@Composable
private fun AddChildCategoryDialog(
    parent: Category,
    repository: TimeTrackerRepository,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加「${parent.name}」的子标签") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("如：游泳") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val n = name.trim()
                    if (n.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { repository.insertCategory(n, parent.id) }
                        onDismiss()
                    }
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
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    allCategories: List<Category>,
    repository: TimeTrackerRepository,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(category.name) }
    var selectedParentId by remember { mutableStateOf<Long?>(category.parentId) }

    val possibleParents = allCategories
        .filter { it.id != category.id }
        .filter { it.parentId != category.id }
        .filter { !isDescendant(it.id, category.id, allCategories) }
        .sortedBy { it.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Text("上一级分类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)

                // "None" option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedParentId = null
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (selectedParentId == null) "● 无（作为一级分类）" else "○ 无（作为一级分类）",
                        color = if (selectedParentId == null) MorandiPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }

                possibleParents.forEach { parent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedParentId = parent.id
                            }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (selectedParentId == parent.id) "● ${parent.name}" else "○ ${parent.name}",
                            color = if (selectedParentId == parent.id) MorandiPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val n = name.trim()
                    if (n.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            repository.updateCategoryName(category.id, n)
                            if (selectedParentId != category.parentId) {
                                repository.updateCategoryParent(category.id, selectedParentId)
                            }
                        }
                        onDismiss()
                    }
                }
            ) {
                Text("保存", color = MorandiPrimary)
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
}

private fun isDescendant(candidateId: Long, ancestorId: Long, allCategories: List<Category>): Boolean {
    val category = allCategories.find { it.id == candidateId } ?: return false
    if (category.parentId == ancestorId) return true
    return if (category.parentId != null) {
        isDescendant(category.parentId, ancestorId, allCategories)
    } else {
        false
    }
}
