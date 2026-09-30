package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.PowerToolsScreen
import com.example.ui.screens.RealtimeVoiceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskSchedulerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssistantViewModel

enum class NavDestination(val label: String, val icon: ImageVector, val tag: String) {
    ASSISTANT("Assistant", Icons.Default.AutoAwesome, "nav_assistant"),
    SCHEDULER("Tasks", Icons.Default.Alarm, "nav_scheduler"),
    TOOLS("Tools", Icons.Default.Widgets, "nav_tools"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle permission response */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AssistantViewModel) {
    var selectedNavIndex by rememberSaveable { mutableIntStateOf(0) }
    var isInRealtimeVoiceMode by rememberSaveable { mutableStateOf(false) }

    if (isInRealtimeVoiceMode) {
        RealtimeVoiceScreen(
            viewModel = viewModel,
            onBack = { isInRealtimeVoiceMode = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavDestination.entries.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = selectedNavIndex == index,
                            onClick = { selectedNavIndex = index },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(destination.label) },
                            modifier = Modifier.testTag(destination.tag),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedNavIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "nav_content_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> AssistantScreen(
                        viewModel = viewModel,
                        onNavigateToRealtimeVoice = { isInRealtimeVoiceMode = true }
                    )
                    1 -> TaskSchedulerScreen(
                        viewModel = viewModel
                    )
                    2 -> PowerToolsScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { selectedNavIndex = 0 }
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
