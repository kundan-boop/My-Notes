package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.AppLockOverlay
import com.example.ui.navigation.Screen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.NoteEditScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val notesViewModel: NotesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openNoteId = intent.getStringExtra("OPEN_NOTE_ID")

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
            val appLockPin by settingsViewModel.appLockPin.collectAsState()

            var isUnlocked by remember { mutableStateOf(!appLockEnabled) }

            LaunchedEffect(appLockEnabled) {
                if (!appLockEnabled) {
                    isUnlocked = true
                }
            }

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (appLockEnabled && !isUnlocked) {
                        AppLockOverlay(
                            correctPin = appLockPin,
                            onUnlocked = { isUnlocked = true }
                        )
                    } else {
                        MainAppNavigation(
                            notesViewModel = notesViewModel,
                            settingsViewModel = settingsViewModel,
                            initialNoteId = openNoteId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(
    notesViewModel: NotesViewModel,
    settingsViewModel: SettingsViewModel,
    initialNoteId: String?
) {
    val navController = rememberNavController()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        notesViewModel.userMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val startDestination = if (!initialNoteId.isNullOrBlank()) {
        Screen.NoteEdit.createRoute(initialNoteId)
    } else {
        Screen.NotesList.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.NotesList.route) {
            NotesListScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(noteId))
                },
                onNewNoteClick = { type ->
                    navController.navigate(Screen.NoteEdit.createRoute("new_$type"))
                },
                onNavigateToArchive = {
                    navController.navigate(Screen.Archive.route)
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onNavigateToReminders = {
                    navController.navigate(Screen.Reminders.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.NoteEdit.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
            NoteEditScreen(
                noteId = noteId,
                viewModel = notesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Archive.route) {
            ArchiveScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Trash.route) {
            TrashScreen(
                viewModel = notesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                notesViewModel = notesViewModel,
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                notesViewModel = notesViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
