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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.file.BackupManager
import com.example.timer.PomodoroManager
import com.example.ui.MainScreen
import com.example.ui.focus.FocusViewModel
import com.example.ui.library.LibraryViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.tasks.TasksViewModel
import com.example.ui.theme.ScholarSpaceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ScholarApplication

        val backupManager = BackupManager(this, app.database, app.fileManager)

        val libraryViewModel by viewModels<LibraryViewModel> {
            LibraryViewModel.Factory(app.libraryRepository, app.settingsRepository)
        }

        val tasksViewModel by viewModels<TasksViewModel> {
            TasksViewModel.Factory(app.taskRepository, app.libraryRepository)
        }

        val focusViewModel by viewModels<FocusViewModel> {
            FocusViewModel.Factory(
                app.pomodoroManager,
                app.focusRepository,
                app.settingsRepository,
                app.taskRepository,
                app.libraryRepository
            )
        }

        val settingsViewModel by viewModels<SettingsViewModel> {
            SettingsViewModel.Factory(
                app.settingsRepository,
                app.libraryRepository,
                app.fileManager,
                backupManager
            )
        }

        setContent {
            // Observe user theme setting
            val userSettings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val isDark = when (userSettings?.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            // Notification permission prompt (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted / denied handled gracefully */ }

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

            ScholarSpaceTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        libraryViewModel = libraryViewModel,
                        tasksViewModel = tasksViewModel,
                        focusViewModel = focusViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
