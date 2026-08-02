package com.example.timetracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timetracker.data.TimeTrackerRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.timetracker.ui.calendar.CalendarScreen
import com.example.timetracker.ui.stats.StatsScreen
import com.example.timetracker.ui.theme.TimeTrackerTheme
import com.example.timetracker.ui.timer.CategoryManagementScreen
import com.example.timetracker.ui.timer.TimerScreen
import com.example.timetracker.ui.timer.TimerViewModel
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TimeTrackerApp

        setContent {
            TimeTrackerTheme {
                TimeTrackerAppContent(repository = app.repository)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Timer : Screen("timer", "计时", Icons.Default.Home)
    data object Calendar : Screen("calendar", "日历", Icons.Default.DateRange)
    data object Stats : Screen("stats", "统计", Icons.Default.Star)
    data object CategoryManagement : Screen("category_management", "标签管理", Icons.Default.Home)
}

@Composable
fun TimeTrackerAppContent(repository: TimeTrackerRepository) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val items = listOf(Screen.Timer, Screen.Calendar, Screen.Stats)
    val context = LocalContext.current
    val allRecords by repository.allRecords.collectAsState(initial = emptyList())

    val csvExportRange = remember { mutableStateOf<Pair<Instant, Instant>?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val (start, end) = csvExportRange.value ?: return@let
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("id,categoryName,startTime,endTime,durationSeconds\n")
                        allRecords
                            .filter { it.startTime >= start && it.startTime < end }
                            .sortedBy { it.startTime }
                            .forEach { record ->
                                val startFmt = DateTimeFormatter.ISO_INSTANT.format(record.startTime)
                                val endFmt = record.endTime?.let {
                                    DateTimeFormatter.ISO_INSTANT.format(it)
                                } ?: ""
                                writer.write("${record.id},${record.categoryName},$startFmt,$endFmt,${record.durationSeconds}\n")
                            }
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timer.route) {
                val viewModel: TimerViewModel = viewModel(
                    factory = TimerViewModel.Factory(repository)
                )
                TimerScreen(
                    viewModel = viewModel,
                    onManageCategories = {
                        navController.navigate(Screen.CategoryManagement.route)
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(repository = repository)
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    repository = repository,
                    onExportCsv = { start, end ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        csvExportRange.value = start to end
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "text/csv"
                            putExtra(Intent.EXTRA_TITLE, "time_records_${LocalDate.now()}.csv")
                        }
                        csvLauncher.launch(intent)
                    }
                )
            }
            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
