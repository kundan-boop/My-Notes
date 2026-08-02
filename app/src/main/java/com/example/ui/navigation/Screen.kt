package com.example.ui.navigation

sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    object NoteEdit : Screen("note_edit/{noteId}") {
        fun createRoute(noteId: String) = "note_edit/$noteId"
    }
    object Archive : Screen("archive")
    object Trash : Screen("trash")
    object Reminders : Screen("reminders")
    object Backup : Screen("backup")
    object Settings : Screen("settings")
}
