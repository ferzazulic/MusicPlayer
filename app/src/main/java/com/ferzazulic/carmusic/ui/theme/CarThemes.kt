package com.ferzazulic.carmusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BgPattern { NONE, GRID, HONEYCOMB, LINES, DOTS, CHEVRON, CARBON }
enum class ProgressStyle { LINE, GLOW, SEGMENTED, THICK }
enum class FontStyle { DEFAULT, CONDENSED, LIGHT }

/**
 * Complete screen layout (frame, navigation and Now Playing composition) modelled on the structure of
 * a factory unit. Only layout, proportions and colours are recreated – no manufacturer artwork or logos.
 */
enum class PlayerLayout(val title: String) {
    RCD("RCD/RNS 510"),
    MIB3("Discover Pro MIB3"),
    AMUNDSEN("Škoda Amundsen"),
    SEAT("SEAT Media System"),
    CUPRA("CUPRA infotainment"),
    MMI("Audi MMI 3G+"),
    VC("Audi Virtual Cockpit"),
    PCM("Porsche PCM 3.1"),
    CID("Opel CID"),
    DIALS("Digitalni cockpit"),
    COCKPIT("Cover flow"),
}

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
    val layout: PlayerLayout = PlayerLayout.MIB3,
    /** Light screen (RCD 510, Opel CID): no coloured aurora behind the content. */
    val light: Boolean = false,
)

object CarThemes {
    val all: List<CarTheme> = listOf(
        CarTheme(
            id = "mib_discover", name = "Discover Pro", inspiredBy = "VW Discover Pro (MIB3)", group = "Volkswagen",
            bgTop = Color(0xFF07080A), bgBottom = Color(0xFF000000), surface = Color(0xFF111317), surfaceHigh = Color(0xFF1C1F25),
            outline = Color(0xFF3A3F47), text = Color(0xFFF4F6F8), textMuted = Color(0xFFA7AFBA),
            accent = Color(0xFF2E6BFF), accentSecondary = Color(0xFF7FA6FF), corner = 2.dp,
            layout = PlayerLayout.MIB3,
        ),
        CarTheme(
            id = "rns510", name = "RCD/RNS 510", inspiredBy = "VW RCD 510 / RNS 510", group = "Volkswagen",
            bgTop = Color(0xFFFDFDFD), bgBottom = Color(0xFFD6D6D6), surface = Color(0xFFE9E9E9), surfaceHigh = Color(0xFFFFFFFF),
            outline = Color(0xFFB4B4B4), text = Color(0xFF111111), textMuted = Color(0xFF555555),
            accent = Color(0xFFC4161C), accentSecondary = Color(0xFF7A0E12), corner = 3.dp,
            layout = PlayerLayout.RCD, light = true,
        ),
        CarTheme(
            id = "vw_gti", name = "GTI Cockpit", inspiredBy = "VW digitalni cockpit (GTI)", group = "Volkswagen",
            bgTop = Color(0xFF120608), bgBottom = Color(0xFF000000), surface = Color(0xFF160B0D), surfaceHigh = Color(0xFF241316),
            outline = Color(0xFF4A2A2E), text = Color(0xFFFFFFFF), textMuted = Color(0xFFB9A3A6),
            accent = Color(0xFFE3152B), accentSecondary = Color(0xFFFF5A64), corner = 4.dp,
            pattern = BgPattern.HONEYCOMB, glow = true, layout = PlayerLayout.DIALS,
        ),
        CarTheme(
            id = "vw_r", name = "R Cockpit", inspiredBy = "VW digitalni cockpit (R)", group = "Volkswagen",
            bgTop = Color(0xFF15171B), bgBottom = Color(0xFF050608), surface = Color(0xFF1A1C21), surfaceHigh = Color(0xFF26292F),
            outline = Color(0xFF3B3F47), text = Color(0xFFFFFFFF), textMuted = Color(0xFFA2A7B0),
            accent = Color(0xFFE30613), accentSecondary = Color(0xFF8E959F), corner = 4.dp,
            glow = true, layout = PlayerLayout.DIALS,
        ),
        CarTheme(
            id = "audi_mmi3g", name = "MMI 3G+", inspiredBy = "Audi MMI 3G+ (A6/A7 C7)", group = "Audi",
            bgTop = Color(0xFF050505), bgBottom = Color(0xFF000000), surface = Color(0xFF141414), surfaceHigh = Color(0xFF262626),
            outline = Color(0xFF4A4A4A), text = Color(0xFFF2F2F2), textMuted = Color(0xFFB0B0B0),
            accent = Color(0xFFF0A30A), accentSecondary = Color(0xFFFFCF5C), corner = 10.dp,
            layout = PlayerLayout.MMI,
        ),
        CarTheme(
            id = "audi_vc", name = "Virtual Cockpit", inspiredBy = "Audi Virtual Cockpit", group = "Audi",
            bgTop = Color(0xFF15171B), bgBottom = Color(0xFF000000), surface = Color(0xFF15171A), surfaceHigh = Color(0xFF24272C),
            outline = Color(0xFF3A3E45), text = Color(0xFFFFFFFF), textMuted = Color(0xFFC4C8CE),
            accent = Color(0xFFF5A623), accentSecondary = Color(0xFFFFD27A), corner = 3.dp,
            font = FontStyle.LIGHT, layout = PlayerLayout.VC,
        ),
        CarTheme(
            id = "audi_rs", name = "RS Cockpit", inspiredBy = "Audi RS sportski prikaz", group = "Audi",
            bgTop = Color(0xFF1C0B08), bgBottom = Color(0xFF050505), surface = Color(0xFF171311), surfaceHigh = Color(0xFF261E1A),
            outline = Color(0xFF3C2C24), text = Color(0xFFFFFFFF), textMuted = Color(0xFFA0938C),
            accent = Color(0xFFFF3B1F), accentSecondary = Color(0xFFFFA25C), corner = 0.dp,
            pattern = BgPattern.HONEYCOMB, font = FontStyle.CONDENSED, glow = true, layout = PlayerLayout.DIALS,
        ),
        CarTheme(
            id = "skoda_amundsen", name = "Amundsen", inspiredBy = "Škoda Amundsen / Columbus (MIB2)", group = "Škoda",
            bgTop = Color(0xFF050505), bgBottom = Color(0xFF000000), surface = Color(0xFF151515), surfaceHigh = Color(0xFF242424),
            outline = Color(0xFF8A8A8A), text = Color(0xFFF5F5F5), textMuted = Color(0xFFC8C8C8),
            accent = Color(0xFF8CC63F), accentSecondary = Color(0xFFD4E000), corner = 0.dp,
            layout = PlayerLayout.AMUNDSEN,
        ),
        CarTheme(
            id = "skoda_electric", name = "Electric Green", inspiredBy = "Škoda Enyaq (MIB3)", group = "Škoda",
            bgTop = Color(0xFF0E3A2F), bgBottom = Color(0xFF041210), surface = Color(0xFF0F2D26), surfaceHigh = Color(0xFF154038),
            outline = Color(0xFF1F5A4D), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9CC4B8),
            accent = Color(0xFF78FAAE), accentSecondary = Color(0xFF419468), corner = 16.dp,
            progress = ProgressStyle.GLOW, glow = true, layout = PlayerLayout.COCKPIT,
        ),
        CarTheme(
            id = "seat_media", name = "Media System Plus", inspiredBy = "SEAT Media System Plus", group = "SEAT / CUPRA",
            bgTop = Color(0xFF1B1B1D), bgBottom = Color(0xFF060606), surface = Color(0xFF1E1E21), surfaceHigh = Color(0xFF2E2E33),
            outline = Color(0xFF55555C), text = Color(0xFFFFFFFF), textMuted = Color(0xFFB8B8BE),
            accent = Color(0xFFE2241B), accentSecondary = Color(0xFFFF6A5E), corner = 2.dp,
            pattern = BgPattern.CARBON, layout = PlayerLayout.SEAT,
        ),
        CarTheme(
            id = "cupra", name = "CUPRA", inspiredBy = "CUPRA infotainment", group = "SEAT / CUPRA",
            bgTop = Color(0xFF1D2024), bgBottom = Color(0xFF0B0C0E), surface = Color(0xFF2A2E33), surfaceHigh = Color(0xFF3A3F45),
            outline = Color(0xFF4C5259), text = Color(0xFFF5F2EE), textMuted = Color(0xFFB4ADA6),
            accent = Color(0xFFC27F4E), accentSecondary = Color(0xFFE6B48A), corner = 12.dp,
            layout = PlayerLayout.CUPRA,
        ),
        CarTheme(
            id = "porsche_pcm", name = "PCM 3.1", inspiredBy = "Porsche PCM 3.1", group = "Porsche",
            bgTop = Color(0xFF0B0B0C), bgBottom = Color(0xFF000000), surface = Color(0xFF1A1B1E), surfaceHigh = Color(0xFF2B2C30),
            outline = Color(0xFF55575C), text = Color(0xFFF2F2F2), textMuted = Color(0xFFB5B6BA),
            accent = Color(0xFFF08A1C), accentSecondary = Color(0xFFFFB25E), corner = 2.dp,
            font = FontStyle.CONDENSED, layout = PlayerLayout.PCM,
        ),
        CarTheme(
            id = "opel_cid", name = "Astra CID", inspiredBy = "Opel Astra H CID (bonus)", group = "Bonus",
            bgTop = Color(0xFFF7F7F7), bgBottom = Color(0xFFE4E4E4), surface = Color(0xFFDADADA), surfaceHigh = Color(0xFFFFFFFF),
            outline = Color(0xFF9E9E9E), text = Color(0xFF111111), textMuted = Color(0xFF6E6E6E),
            accent = Color(0xFFC8102E), accentSecondary = Color(0xFF8A8A8A), corner = 0.dp,
            layout = PlayerLayout.CID, light = true,
        ),
        CarTheme(
            id = "midnight", name = "Midnight", inspiredBy = "CarMusic originalna tema", group = "Bonus",
            bgTop = Color(0xFF17112B), bgBottom = Color(0xFF000000), surface = Color(0xFF17142A), surfaceHigh = Color(0xFF241F3D),
            outline = Color(0xFF332C55), text = Color(0xFFFFFFFF), textMuted = Color(0xFF9D96BA),
            accent = Color(0xFF8B5CF6), accentSecondary = Color(0xFF22D3EE), corner = 22.dp,
            progress = ProgressStyle.GLOW, glow = true, layout = PlayerLayout.COCKPIT,
        ),
    )

    fun byId(id: String): CarTheme = all.firstOrNull { it.id == id } ?: all.first()

    val accentPresets: List<Color> = listOf(
        Color(0xFF2E6BFF), Color(0xFF4FA3FF), Color(0xFF8FD0FF), Color(0xFF22D3EE),
        Color(0xFF78FAAE), Color(0xFF8CC63F), Color(0xFFB4E233), Color(0xFFFFD400),
        Color(0xFFF5A623), Color(0xFFF08A1C), Color(0xFFFF3B1F), Color(0xFFE3152B),
        Color(0xFFC4161C), Color(0xFFFF4FA3), Color(0xFF8B5CF6), Color(0xFFC27F4E),
        Color(0xFFFFFFFF),
    )
}
