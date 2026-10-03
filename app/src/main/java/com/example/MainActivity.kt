package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BodyDoubleScreen
import com.example.ui.screens.BrainDumpScreen
import com.example.ui.screens.ErrandsMapScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppTab(val label: String, val testTag: String) {
    TASKS("کارها", "nav_tasks"),
    BRAIN_DUMP("تخلیه ذهن", "nav_brain_dump"),
    BODY_DOUBLE("دستیار AI", "nav_body_double"),
    ERRANDS("نقشه و بیرون", "nav_errands"),
    SETTINGS("تنظیمات", "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(AppTab.TASKS) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var timerTaskId by remember { mutableStateOf<Long?>(null) }
    var timerTaskTitle by remember { mutableStateOf("تمرکز آزاد") }
    var timerMinutes by remember { mutableIntStateOf(25) }

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()
    val level by viewModel.userLevel.collectAsStateWithLifecycle()
    val xp by viewModel.userXp.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val todayEnergy by viewModel.todayEnergy.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.dopamineRewardBanner.collectAsStateWithLifecycle()
    val breakdownTaskId by viewModel.breakdownTaskId.collectAsStateWithLifecycle()

    // Handle back button
    BackHandler(enabled = showTimerSheet || currentTab != AppTab.TASKS) {
        if (showTimerSheet) {
            showTimerSheet = false
        } else {
            currentTab = AppTab.TASKS
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!showTimerSheet) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val navItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.TASKS,
                        onClick = { currentTab = AppTab.TASKS },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "کارها") },
                        label = { Text("کارها", fontSize = 10.sp, fontWeight = if (currentTab == AppTab.TASKS) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag(AppTab.TASKS.testTag)
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.BRAIN_DUMP,
                        onClick = { currentTab = AppTab.BRAIN_DUMP },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "تخلیه ذهن") },
                        label = { Text("تخلیه ذهن", fontSize = 10.sp, fontWeight = if (currentTab == AppTab.BRAIN_DUMP) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag(AppTab.BRAIN_DUMP.testTag)
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.BODY_DOUBLE,
                        onClick = { currentTab = AppTab.BODY_DOUBLE },
                        icon = { Icon(Icons.Default.Face, contentDescription = "دستیار AI") },
                        label = { Text("دستیار AI", fontSize = 10.sp, fontWeight = if (currentTab == AppTab.BODY_DOUBLE) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag(AppTab.BODY_DOUBLE.testTag)
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.ERRANDS,
                        onClick = { currentTab = AppTab.ERRANDS },
                        icon = { Icon(Icons.Default.Map, contentDescription = "نقشه و بیرون") },
                        label = { Text("نقشه و بیرون", fontSize = 10.sp, fontWeight = if (currentTab == AppTab.ERRANDS) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag(AppTab.ERRANDS.testTag)
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { currentTab = AppTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                        label = { Text("تنظیمات", fontSize = 10.sp, fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag(AppTab.SETTINGS.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (showTimerSheet) {
            FocusTimerScreen(
                viewModel = viewModel,
                tasks = tasks,
                subtasks = subtasks,
                initialTaskId = timerTaskId,
                initialTaskTitle = timerTaskTitle,
                initialMinutes = timerMinutes,
                onClose = { showTimerSheet = false },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (currentTab) {
                AppTab.TASKS -> {
                    TasksScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        subtasks = subtasks,
                        level = level,
                        xp = xp,
                        completedCount = completedCount,
                        todayEnergy = todayEnergy,
                        bannerMessage = bannerMessage,
                        breakdownTaskId = breakdownTaskId,
                        onNavigateToTimer = { id, title, mins ->
                            timerTaskId = id
                            timerTaskTitle = title
                            timerMinutes = mins
                            showTimerSheet = true
                        },
                        onNavigateToBrainDump = {
                            currentTab = AppTab.BRAIN_DUMP
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppTab.BRAIN_DUMP -> {
                    BrainDumpScreen(
                        viewModel = viewModel,
                        onFinishedImport = {
                            currentTab = AppTab.TASKS
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppTab.BODY_DOUBLE -> {
                    BodyDoubleScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppTab.ERRANDS -> {
                    ErrandsMapScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
