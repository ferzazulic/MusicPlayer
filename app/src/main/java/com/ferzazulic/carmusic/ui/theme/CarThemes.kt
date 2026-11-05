package com.ferzazulic.carmusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BgPattern { NONE, GRID, HONEYCOMB, LINES, DOTS, CHEVRON }
enum class ProgressStyle { LINE, GLOW, SEGMENTED, THICK }
enum class FontStyle { DEFAULT, CONDENSED, LIGHT }

data class CarTheme(
    val id: String,
    val name: String,
    val inspiredBy: String,
    val group: String,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSecondary: Color,
    val corner: Dp,
    val pattern: BgPattern = BgPattern.NONE,
    val progress: ProgressStyle = ProgressStyle.LINE,
    val font: FontStyle = FontStyle.DEFAULT,
    val uppercaseLabels: Boolean = false,
    val glow: Boolean = false,
)

/**
 * Colour schemes inspired by factory infotainment systems. These are original approximations of the
 * general look (palette, shapes, typography) – no logos or copyrighted artwork are used.
 */
object CarThemes {
    val all: List<CarTheme> = listOf(
        CarTheme(
            id = "mib_discover", name = "Discover Pro", inspiredBy = "VW MIB2 Discover Pro", group = "Volkswagen",
            bgTop = Color(0xFF141922), bgBottom = Color(0xFF05070A), surface = Color(0xFF1A2029), surfaceHigh = Color(0xFF242C38),
            outline = Color(0xFF2E3846), text = Color(0xFFF2F5F9), textMuted = Color(0xFF8C97A6),
            accent = Color(0xFF4FA3FF), accentSecondary = Color(0xFFB9DAFF), corner = 6.dp,
            progress = ProgressStyle.LINE,
        ),
        CarTheme(
            id = "rns510", name = "RNS Navy", inspiredBy = "VW RNS 510 / RCD 510", group = "Volkswagen",
            bgTop = Color(0xFF0D2A4D), bgBottom = Color(0xFF020914), surface = Color(0xFF0F2C4C), surfaceHigh = Color(0xFF16406B),
            outline = Color(0xFF2A5D91), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9DBBD9),
            accent = Color(0xFF8FD0FF), accentSecondary = Color(0xFFFFFFFF), corner = 12.dp,
            pattern = BgPattern.GRID, progress = ProgressStyle.SEGMENTED, glow = true,
        ),
        CarTheme(
            id = "vw_gti", name = "GTI Red", inspiredBy = "VW Golf GTI performance", group = "Volkswagen",
            bgTop = Color(0xFF1A0507), bgBottom = Color(0xFF000000), surface = Color(0xFF161214), surfaceHigh = Color(0xFF241B1D),
            outline = Color(0xFF3A2427), text = Color(0xFFFFFFFF), textMuted = Color(0xFFA89497),
            accent = Color(0xFFE2001A), accentSecondary = Color(0xFFFF5A5F), corner = 2.dp,
            pattern = BgPattern.HONEYCOMB, progress = ProgressStyle.THICK, font = FontStyle.CONDENSED,
            uppercaseLabels = true, glow = true,
        ),
        CarTheme(
            id = "vw_r", name = "R Blue", inspiredBy = "VW R performance", group = "Volkswagen",
            bgTop = Color(0xFF0A1424), bgBottom = Color(0xFF000000), surface = Color(0xFF101722), surfaceHigh = Color(0xFF18233A),
            outline = Color(0xFF22324F), text = Color(0xFFFFFFFF), textMuted = Color(0xFF8D9AB2),
            accent = Color(0xFF2D7FF9), accentSecondary = Color(0xFF6FA8FF), corner = 2.dp,
            pattern = BgPattern.CHEVRON, progress = ProgressStyle.GLOW, font = FontStyle.CONDENSED,
            uppercaseLabels = true, glow = true,
        ),
        CarTheme(
            id = "audi_mmi3g", name = "MMI Classic", inspiredBy = "Audi MMI 3G / 3G+", group = "Audi",
            bgTop = Color(0xFF121212), bgBottom = Color(0xFF000000), surface = Color(0xFF181818), surfaceHigh = Color(0xFF262626),
            outline = Color(0xFF3A3A3A), text = Color(0xFFE8E8E8), textMuted = Color(0xFF8A8A8A),
            accent = Color(0xFFCC0033), accentSecondary = Color(0xFFFFFFFF), corner = 0.dp,
            pattern = BgPattern.LINES, progress = ProgressStyle.LINE, font = FontStyle.CONDENSED, uppercaseLabels = true,
        ),
        CarTheme(
            id = "audi_vc", name = "Virtual Cockpit", inspiredBy = "Audi MMI Touch / Virtual Cockpit", group = "Audi",
            bgTop = Color(0xFF1A1C20), bgBottom = Color(0xFF000000), surface = Color(0xFF15171A), surfaceHigh = Color(0xFF22252A),
            outline = Color(0xFF33373D), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9097A0),
            accent = Color(0xFFF50537), accentSecondary = Color(0xFFB3B8BF), corner = 3.dp,
            progress = ProgressStyle.GLOW, font = FontStyle.LIGHT, glow = true,
        ),
        CarTheme(
            id = "audi_rs", name = "RS Performance", inspiredBy = "Audi RS / Sport mode", group = "Audi",
            bgTop = Color(0xFF1C0B08), bgBottom = Color(0xFF050505), surface = Color(0xFF171311), surfaceHigh = Color(0xFF261E1A),
            outline = Color(0xFF3C2C24), text = Color(0xFFFFFFFF), textMuted = Color(0xFFA0938C),
            accent = Color(0xFFFF3B1F), accentSecondary = Color(0xFFFFA25C), corner = 0.dp,
            pattern = BgPattern.HONEYCOMB, progress = ProgressStyle.SEGMENTED, font = FontStyle.CONDENSED,
            uppercaseLabels = true, glow = true,
        ),
        CarTheme(
            id = "skoda_amundsen", name = "Amundsen Green", inspiredBy = "Škoda Amundsen / Columbus", group = "Škoda",
            bgTop = Color(0xFF0E1D17), bgBottom = Color(0xFF030806), surface = Color(0xFF122119), surfaceHigh = Color(0xFF1A3024),
            outline = Color(0xFF274536), text = Color(0xFFF3FAF6), textMuted = Color(0xFF8FA99B),
            accent = Color(0xFF4BA82E), accentSecondary = Color(0xFF78FAAE), corner = 10.dp,
            pattern = BgPattern.DOTS, progress = ProgressStyle.LINE,
        ),
        CarTheme(
            id = "skoda_electric", name = "Electric Green", inspiredBy = "Škoda Enyaq / new design", group = "Škoda",
            bgTop = Color(0xFF0E3A2F), bgBottom = Color(0xFF041210), surface = Color(0xFF0F2D26), surfaceHigh = Color(0xFF154038),
            outline = Color(0xFF1F5A4D), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9CC4B8),
            accent = Color(0xFF78FAAE), accentSecondary = Color(0xFF419468), corner = 16.dp,
            progress = ProgressStyle.GLOW, glow = true,
        ),
        CarTheme(
            id = "seat_media", name = "Media System", inspiredBy = "SEAT Media System Plus", group = "SEAT / CUPRA",
            bgTop = Color(0xFF1A1715), bgBottom = Color(0xFF050505), surface = Color(0xFF1C1917), surfaceHigh = Color(0xFF2A2522),
            outline = Color(0xFF3D3530), text = Color(0xFFF7F4F2), textMuted = Color(0xFF9E958F),
            accent = Color(0xFFE8601C), accentSecondary = Color(0xFFFFA06A), corner = 8.dp,
            progress = ProgressStyle.THICK,
        ),
        CarTheme(
            id = "cupra", name = "Copper", inspiredBy = "CUPRA", group = "SEAT / CUPRA",
            bgTop = Color(0xFF15110E), bgBottom = Color(0xFF000000), surface = Color(0xFF16120F), surfaceHigh = Color(0xFF241D18),
            outline = Color(0xFF3B2F25), text = Color(0xFFF5EEE8), textMuted = Color(0xFF9E8E80),
            accent = Color(0xFFC1875A), accentSecondary = Color(0xFFE5B48A), corner = 0.dp,
            pattern = BgPattern.CHEVRON, progress = ProgressStyle.GLOW, font = FontStyle.CONDENSED,
            uppercaseLabels = true, glow = true,
        ),
        CarTheme(
            id = "porsche_pcm", name = "PCM", inspiredBy = "Porsche Communication Management", group = "Porsche",
            bgTop = Color(0xFF111214), bgBottom = Color(0xFF000000), surface = Color(0xFF17181B), surfaceHigh = Color(0xFF232428),
            outline = Color(0xFF35373C), text = Color(0xFFFFFFFF), textMuted = Color(0xFF96979C),
            accent = Color(0xFFD5001C), accentSecondary = Color(0xFFC9A96E), corner = 4.dp,
            progress = ProgressStyle.LINE, font = FontStyle.LIGHT,
        ),
        CarTheme(
            id = "opel_cid", name = "Astra CID", inspiredBy = "Opel Astra H colour display (bonus)", group = "Bonus",
            bgTop = Color(0xFF12306B), bgBottom = Color(0xFF040E24), surface = Color(0xFF0F2754), surfaceHigh = Color(0xFF173777),
            outline = Color(0xFF2B4F94), text = Color(0xFFFFFFFF), textMuted = Color(0xFFA6B9DE),
            accent = Color(0xFFFFB627), accentSecondary = Color(0xFFFFFFFF), corner = 8.dp,
            pattern = BgPattern.GRID, progress = ProgressStyle.SEGMENTED,
        ),
        CarTheme(
            id = "midnight", name = "Midnight", inspiredBy = "CarMusic originalna tema", group = "Bonus",
            bgTop = Color(0xFF17112B), bgBottom = Color(0xFF000000), surface = Color(0xFF17142A), surfaceHigh = Color(0xFF241F3D),
            outline = Color(0xFF332C55), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9D96BA),
            accent = Color(0xFF8B5CF6), accentSecondary = Color(0xFF22D3EE), corner = 22.dp,
            progress = ProgressStyle.GLOW, glow = true,
        ),
    )

    fun byId(id: String): CarTheme = all.firstOrNull { it.id == id } ?: all.first()

    val accentPresets: List<Color> = listOf(
        Color(0xFF4FA3FF), Color(0xFF2D7FF9), Color(0xFF8FD0FF), Color(0xFF22D3EE),
        Color(0xFF78FAAE), Color(0xFF4BA82E), Color(0xFFB4E233), Color(0xFFFFD400),
        Color(0xFFFFB627), Color(0xFFE8601C), Color(0xFFFF3B1F), Color(0xFFE2001A),
        Color(0xFFF50537), Color(0xFFFF4FA3), Color(0xFF8B5CF6), Color(0xFFC1875A),
        Color(0xFFFFFFFF),
    )
}
