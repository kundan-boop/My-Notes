package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors
val PrimaryIndigo = Color(0xFF4F46E5)
val PrimaryDarkIndigo = Color(0xFF6366F1)
val SecondaryTeal = Color(0xFF0D9488)
val AccentAmber = Color(0xFFF59E0B)

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)

data class NoteColorPreset(
    val id: String,
    val name: String,
    val lightBg: Color,
    val darkBg: Color,
    val lightBorder: Color,
    val darkBorder: Color
)

object NoteColors {
    val presets = listOf(
        NoteColorPreset("default", "Default", Color.Unspecified, Color.Unspecified, Color.Transparent, Color.Transparent),
        NoteColorPreset("red", "Coral", Color(0xFFFEE2E2), Color(0xFF451A1A), Color(0xFFFCA5A5), Color(0xFF7F1D1D)),
        NoteColorPreset("orange", "Peach", Color(0xFFFFEDD5), Color(0xFF431D0A), Color(0xFFFDBA74), Color(0xFF7C2D12)),
        NoteColorPreset("yellow", "Sand", Color(0xFFFEF3C7), Color(0xFF422006), Color(0xFFFDE047), Color(0xFF713F12)),
        NoteColorPreset("green", "Mint", Color(0xFFDCFCE7), Color(0xFF052E16), Color(0xFF86EFAC), Color(0xFF14532D)),
        NoteColorPreset("teal", "Teal", Color(0xFFCCFBF1), Color(0xFF042F2C), Color(0xFF5EEAD4), Color(0xFF134E4A)),
        NoteColorPreset("blue", "Sky", Color(0xFFE0F2FE), Color(0xFF0C4A6E), Color(0xFF7DD3FC), Color(0xFF0369A1)),
        NoteColorPreset("indigo", "Lavender", Color(0xFFE0E7FF), Color(0xFF1E1B4B), Color(0xFFA5B4FC), Color(0xFF312E81)),
        NoteColorPreset("purple", "Plum", Color(0xFFF3E8FF), Color(0xFF3B0764), Color(0xFFD8B4FE), Color(0xFF581C87)),
        NoteColorPreset("pink", "Rose", Color(0xFFFCE7F3), Color(0xFF500724), Color(0xFFF472B6), Color(0xFF831843))
    )

    fun getPreset(id: String): NoteColorPreset {
        return presets.find { it.id == id } ?: presets.first()
    }
}
