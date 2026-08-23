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
        NoteColorPreset("crimson", "Ruby", Color(0xFFFFE4E6), Color(0xFF4C0519), Color(0xFFFDA4AF), Color(0xFF881337)),
        NoteColorPreset("orange", "Peach", Color(0xFFFFEDD5), Color(0xFF431D0A), Color(0xFFFDBA74), Color(0xFF7C2D12)),
        NoteColorPreset("tangerine", "Tangerine", Color(0xFFFFEAD5), Color(0xFF451E05), Color(0xFFFB923C), Color(0xFF9A3412)),
        NoteColorPreset("amber", "Amber", Color(0xFFFEF3C7), Color(0xFF451A03), Color(0xFFFCD34D), Color(0xFF78350F)),
        NoteColorPreset("yellow", "Sand", Color(0xFFFEF9C3), Color(0xFF422006), Color(0xFFFDE047), Color(0xFF713F12)),
        NoteColorPreset("lime", "Lime", Color(0xFFECFCCB), Color(0xFF1A2E05), Color(0xFFBEF264), Color(0xFF365314)),
        NoteColorPreset("green", "Mint", Color(0xFFDCFCE7), Color(0xFF052E16), Color(0xFF86EFAC), Color(0xFF14532D)),
        NoteColorPreset("emerald", "Emerald", Color(0xFFD1FAE5), Color(0xFF022C22), Color(0xFF6EE7B7), Color(0xFF064E3B)),
        NoteColorPreset("teal", "Teal", Color(0xFFCCFBF1), Color(0xFF042F2C), Color(0xFF5EEAD4), Color(0xFF134E4A)),
        NoteColorPreset("cyan", "Aqua", Color(0xFFCFFAFE), Color(0xFF083344), Color(0xFF67E8F9), Color(0xFF155E75)),
        NoteColorPreset("blue", "Sky", Color(0xFFE0F2FE), Color(0xFF0C4A6E), Color(0xFF7DD3FC), Color(0xFF0369A1)),
        NoteColorPreset("ocean", "Ocean", Color(0xFFDBEAFE), Color(0xFF172554), Color(0xFF93C5FD), Color(0xFF1E40AF)),
        NoteColorPreset("indigo", "Lavender", Color(0xFFE0E7FF), Color(0xFF1E1B4B), Color(0xFFA5B4FC), Color(0xFF312E81)),
        NoteColorPreset("violet", "Iris", Color(0xFFEDE9FE), Color(0xFF2E1065), Color(0xFFC4B5FD), Color(0xFF4C1D95)),
        NoteColorPreset("purple", "Plum", Color(0xFFF3E8FF), Color(0xFF3B0764), Color(0xFFD8B4FE), Color(0xFF581C87)),
        NoteColorPreset("magenta", "Berry", Color(0xFFFAE8FF), Color(0xFF4A044E), Color(0xFFF0ABFC), Color(0xFF701A75)),
        NoteColorPreset("pink", "Rose", Color(0xFFFCE7F3), Color(0xFF500724), Color(0xFFF472B6), Color(0xFF831843)),
        NoteColorPreset("warmgray", "Almond", Color(0xFFF5F5F4), Color(0xFF292524), Color(0xFFD6D3D1), Color(0xFF57534E)),
        NoteColorPreset("slate", "Slate", Color(0xFFF1F5F9), Color(0xFF1E293B), Color(0xFFCBD5E1), Color(0xFF475569)),
        NoteColorPreset("mocha", "Mocha", Color(0xFFF5EBE6), Color(0xFF2E1810), Color(0xFFD7CCC8), Color(0xFF4E342E))
    )

    fun getPreset(id: String): NoteColorPreset {
        return presets.find { it.id == id } ?: presets.first()
    }
}
