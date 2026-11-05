package com.ferzazulic.carmusic.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferzazulic.carmusic.BuildConfig
import com.ferzazulic.carmusic.cid.CidText
import com.ferzazulic.carmusic.data.Library
import com.ferzazulic.carmusic.playback.EqPresets
import com.ferzazulic.carmusic.playback.PlayerHub
import com.ferzazulic.carmusic.settings.AnimationLevel
import com.ferzazulic.carmusic.settings.AppSettings
import com.ferzazulic.carmusic.settings.CidLayout
import com.ferzazulic.carmusic.settings.CidMode
import com.ferzazulic.carmusic.settings.SortOrder
import com.ferzazulic.carmusic.ui.theme.CarTheme
import com.ferzazulic.carmusic.ui.theme.CarThemes
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.ProgressStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Page(val title: String, val icon: ImageVector) {
    THEMES("Teme i boje", Icons.Rounded.Palette),
    DISPLAY("Prikaz i animacije", Icons.Rounded.Tv),
    AUDIO("Zvuk i ekvilajzer", Icons.Rounded.Equalizer),
    PLAYBACK("Reprodukcija", Icons.Rounded.PlayCircle),
    SWC("Komande na volanu", Icons.Rounded.DirectionsCar),
    CID("CID ekran (Astra H)", Icons.Rounded.SettingsInputComponent),
    LIBRARY("Biblioteka", Icons.Rounded.LibraryMusic),
    ABOUT("O aplikaciji", Icons.Rounded.Info),
}

class SettingsActions(
    val update: ((AppSettings) -> AppSettings) -> Unit,
    val rescan: () -> Unit,
    val cidTest: () -> Unit,
)

@Composable
fun SettingsScreen(
    s: AppSettings,
    library: Library,
    cidStatus: String,
    cidPreview: CidText,
    actions: SettingsActions,
) {
    val t = LocalCarTheme.current
    var page by rememberSaveable { mutableStateOf(Page.THEMES) }
    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier
                .width(270.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Text(t.label("Postavke"), color = t.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(8.dp))
            Page.entries.forEach { p ->
                val active = p == page
                val alpha by animateFloatAsState(if (active) 1f else 0f, tween(t.duration(250)), label = "nav")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(t.corner + 2.dp))
                        .background(t.accent.copy(alpha = 0.18f * alpha))
                        .clickable { page = p }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(3.dp).height(22.dp).background(t.accent.copy(alpha = alpha)))
                    Spacer(Modifier.width(10.dp))
                    Icon(p.icon, null, tint = if (active) t.accent else t.textMuted)
                    Spacer(Modifier.width(12.dp))
                    Text(p.title, color = if (active) t.text else t.textMuted, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val down = targetState.ordinal > initialState.ordinal
                (slideInVertically(tween(t.duration(380))) { if (down) it / 6 else -it / 6 } + fadeIn(tween(t.duration(380)))) togetherWith
                    (slideOutVertically(tween(t.duration(260))) { if (down) -it / 6 else it / 6 } + fadeOut(tween(t.duration(200))))
            },
            label = "settingsPage",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) { p ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (p) {
                    Page.THEMES -> ThemesPage(s, actions.update)
                    Page.DISPLAY -> DisplayPage(s, actions.update)
                    Page.AUDIO -> AudioPage(s, actions.update)
                    Page.PLAYBACK -> PlaybackPage(s, actions.update)
                    Page.SWC -> SwcPage(s, actions.update)
                    Page.CID -> CidPage(s, cidStatus, cidPreview, actions)
                    Page.LIBRARY -> LibraryPage(s, library, actions)
                    Page.ABOUT -> AboutPage()
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ themes

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemesPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val t = LocalCarTheme.current
    SettingsCard("Teme inspirisane fabričkim sistemima") {
        Text(
            "Palete, oblici i tipografija po uzoru na tvorničke radio/navigacijske sisteme. Prelaz između tema je animiran.",
            color = t.textMuted, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        CarThemes.all.groupBy { it.group }.forEach { (group, themes) ->
            Text(group, color = t.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                themes.forEach { theme ->
                    ThemePreviewCard(theme, s.customAccent?.let { Color(it.toInt()) }, theme.id == s.themeId) {
                        update { it.copy(themeId = theme.id) }
                    }
                }
            }
        }
    }
    AccentCard(s, update)
}

@Composable
private fun ThemePreviewCard(theme: CarTheme, accentOverride: Color?, selected: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, spring(dampingRatio = 0.5f), label = "themeScale")
    val accent = accentOverride ?: theme.accent
    val shape = RoundedCornerShape(theme.corner + 4.dp)
    Column(
        Modifier
            .width(196.dp)
            .scale(scale)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.outline, shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                val r = CornerRadius(theme.corner.toPx() / 2)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), Offset(w * 0.2f, h * 0.4f), w * 0.5f), w * 0.5f, Offset(w * 0.2f, h * 0.4f))
                drawRoundRect(Brush.linearGradient(listOf(theme.surfaceHigh, accent.copy(alpha = 0.6f))), Offset(0f, 0f), Size(h * 0.72f, h * 0.72f), r)
                val x0 = h * 0.85f
                drawRoundRect(theme.text, Offset(x0, h * 0.08f), Size(w * 0.42f, h * 0.1f), CornerRadius(4f))
                drawRoundRect(accent, Offset(x0, h * 0.25f), Size(w * 0.3f, h * 0.07f), CornerRadius(4f))
                drawRoundRect(theme.textMuted, Offset(x0, h * 0.38f), Size(w * 0.24f, h * 0.06f), CornerRadius(4f))
                val py = h * 0.62f
                when (theme.progress) {
                    ProgressStyle.SEGMENTED -> for (i in 0 until 12) {
                        val sw = (w - x0) / 12f
                        drawRect(if (i < 7) accent else theme.text.copy(alpha = 0.2f), Offset(x0 + i * sw, py), Size(sw - 2f, 6f))
                    }
                    else -> {
                        val sw = if (theme.progress == ProgressStyle.THICK) 7f else 3f
                        drawLine(theme.text.copy(alpha = 0.2f), Offset(x0, py), Offset(w, py), sw)
                        drawLine(accent, Offset(x0, py), Offset(x0 + (w - x0) * 0.6f, py), sw)
                    }
                }
                val cy = h * 0.88f
                val cx = x0 + (w - x0) / 2
                drawCircle(accent, h * 0.1f, Offset(cx, cy))
                drawCircle(theme.text.copy(alpha = 0.7f), h * 0.045f, Offset(cx - w * 0.14f, cy))
                drawCircle(theme.text.copy(alpha = 0.7f), h * 0.045f, Offset(cx + w * 0.14f, cy))
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(t.accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = t.onAccent, modifier = Modifier.size(18.dp)) }
            }
        }
        Column(Modifier.fillMaxWidth().background(theme.surface).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(theme.name, color = theme.text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(theme.inspiredBy, color = theme.textMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentCard(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val t = LocalCarTheme.current
    SettingsCard("Accent boja") {
        SettingSwitch(
            "Koristi boju teme", "Isključi da odabereš vlastitu accent boju",
            checked = s.customAccent == null,
        ) { useTheme ->
            update { it.copy(customAccent = if (useTheme) null else (t.accent.toArgb().toLong() and 0xFFFFFFFFL)) }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CarThemes.accentPresets.forEach { c ->
                val selected = s.customAccent != null && Color(s.customAccent.toInt()) == c
                val sc by animateFloatAsState(if (selected) 1.2f else 1f, spring(dampingRatio = 0.4f), label = "swatch")
                Box(
                    Modifier
                        .size(48.dp)
                        .scale(sc)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected) 3.dp else 1.dp, if (selected) t.text else Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { update { it.copy(customAccent = c.toArgb().toLong() and 0xFFFFFFFFL) } },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, tint = if (c.red + c.green + c.blue > 2f) Color.Black else Color.White)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val hsv = remember(s.customAccent) {
            FloatArray(3).also { android.graphics.Color.colorToHSV((s.customAccent ?: t.accent.toArgb().toLong()).toInt(), it) }
        }
        var hue by remember(s.customAccent) { mutableFloatStateOf(hsv[0]) }
        var sat by remember(s.customAccent) { mutableFloatStateOf(hsv[1]) }
        var bri by remember(s.customAccent) { mutableFloatStateOf(hsv[2]) }
        fun commit() = update {
            it.copy(customAccent = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri)).toLong() and 0xFFFFFFFFL)
        }
        Text("Vlastita boja", color = t.text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }))
        )
        SettingSlider("Nijansa", hue, 0f..359f, label = { "${it.toInt()}°" }) { hue = it; commit() }
        SettingSlider("Zasićenost", sat, 0f..1f, label = { "${(it * 100).toInt()}%" }) { sat = it; commit() }
        SettingSlider("Svjetlina", bri, 0.2f..1f, label = { "${(it * 100).toInt()}%" }) { bri = it; commit() }
    }
}

// ----------------------------------------------------------------------------------------- display

@Composable
private fun DisplayPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsCard("Animacije") {
        SettingChoice(
            "Nivo animacija", "Smanji na sporijim head unitima",
            listOf(AnimationLevel.FULL to "Pune", AnimationLevel.REDUCED to "Smanjene", AnimationLevel.OFF to "Isključene"),
            s.animationLevel,
        ) { v -> update { it.copy(animationLevel = v) } }
        SettingSwitch("Ambijentalna pozadina", "Boja pozadine prati omot albuma", s.artworkBackground) { v -> update { it.copy(artworkBackground = v) } }
        SettingSwitch("Spektar vizualizator", "Animirani stupci ispod kontrola", s.showVisualizer) { v -> update { it.copy(showVisualizer = v) } }
    }
    SettingsCard("Ekran") {
        SettingSwitch("Prikaži sat", null, s.showClock) { v -> update { it.copy(showClock = v) } }
        SettingSwitch("Ekran uvijek upaljen", "Sprječava gašenje ekrana dok je aplikacija otvorena", s.keepScreenOn) { v -> update { it.copy(keepScreenOn = v) } }
        SettingSwitch("Velike kontrole", "Veći tasteri i tekst – lakše dok voziš", s.largeControls) { v -> update { it.copy(largeControls = v) } }
    }
}

// ------------------------------------------------------------------------------------------- audio

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val t = LocalCarTheme.current
    SettingsCard("Ekvilajzer") {
        SettingSwitch("Uključi ekvilajzer i efekte", "5-pojasni EQ, bas, prostorni zvuk i pojačanje glasnoće", s.eqEnabled) { v -> update { it.copy(eqEnabled = v) } }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EqPresets.presets.forEach { (name, bands) ->
                Chip(name, s.eqPreset == name) {
                    update { it.copy(eqPreset = name, eqBands = if (name == "Custom") it.eqBands else bands, eqEnabled = true) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            s.eqBands.forEachIndexed { i, level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%+.1f".format(level / 100f), color = t.accent, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    VerticalBandSlider(level, -1200, 1200, s.eqEnabled, 200.dp, onChange = { v ->
                        update { st -> st.copy(eqPreset = "Custom", eqBands = st.eqBands.toMutableList().also { it[i] = v }) }
                    })
                    Spacer(Modifier.height(6.dp))
                    Text(EqPresets.labels[i] + " Hz", color = t.textMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingSlider("Bas boost", s.bassBoost.toFloat(), 0f..1000f, label = { "${(it / 10).toInt()}%" }) { v -> update { it.copy(bassBoost = v.toInt()) } }
        SettingSlider("Prostorni zvuk (virtualizer)", s.virtualizer.toFloat(), 0f..1000f, label = { "${(it / 10).toInt()}%" }) { v -> update { it.copy(virtualizer = v.toInt()) } }
        SettingSlider("Pojačanje glasnoće", s.loudnessGain.toFloat(), 0f..1500f, label = { "+%.1f dB".format(it / 100f) }) { v -> update { it.copy(loudnessGain = v.toInt()) } }
    }
    SettingsCard("Obrada") {
        SettingSwitch("Fade pri play/pause", "Glatko pojačavanje i stišavanje", s.fadeOnPlayPause) { v -> update { it.copy(fadeOnPlayPause = v) } }
        if (s.fadeOnPlayPause) {
            SettingSlider("Trajanje fade-a", s.fadeDurationMs.toFloat(), 100f..1500f, steps = 13, label = { "${it.toInt()} ms" }) { v -> update { it.copy(fadeDurationMs = v.toInt()) } }
        }
        SettingSwitch("Preskoči tišinu", "Uklanja tišinu na početku/kraju pjesama", s.skipSilence) { v -> update { it.copy(skipSilence = v) } }
        SettingSlider("Brzina reprodukcije", s.playbackSpeed, 0.5f..2f, steps = 5, label = { "%.2fx".format(it) }) { v -> update { it.copy(playbackSpeed = v) } }
        Text("Gapless reprodukcija je uvijek uključena.", color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------------------------- playback

@Composable
private fun PlaybackPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsCard("Pokretanje") {
        SettingSwitch("Nastavi gdje si stao", "Vraća red reprodukcije nakon paljenja auta", s.resumeOnStart) { v -> update { it.copy(resumeOnStart = v) } }
        SettingSwitch("Zapamti poziciju u pjesmi", null, s.rememberPosition) { v -> update { it.copy(rememberPosition = v) } }
        SettingSwitch("Automatski pusti pri pokretanju", "Muzika kreće čim se aplikacija otvori", s.autoPlayOnStart) { v -> update { it.copy(autoPlayOnStart = v) } }
        SettingSwitch("Pokreni pri paljenju head unita", "Otvara aplikaciju nakon boota (ACC ON)", s.launchOnBoot) { v -> update { it.copy(launchOnBoot = v) } }
    }
    SettingsCard("Ponašanje") {
        SettingSwitch("'Prethodna' vraća na početak", "Ako je pjesma odmakla više od 3 s, prvo je vrati na početak", s.previousRestartsTrack) { v -> update { it.copy(previousRestartsTrack = v) } }
        SettingSwitch("Pauziraj pri prekidu zvuka", "Kada se Bluetooth/AUX izlaz odspoji", s.pauseOnDisconnect) { v -> update { it.copy(pauseOnDisconnect = v) } }
    }
}

// --------------------------------------------------------------------------------- steering wheel

@Composable
private fun SwcPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val t = LocalCarTheme.current
    SettingsCard("Komande na volanu") {
        Text(
            "Aplikacija koristi Android MediaSession – head unit (CAN-box) šalje tastere s volana kao standardne medijske tipke " +
                "(sljedeća, prethodna, play/pause). Na nekim head unitima u njihovim postavkama treba odabrati ovu aplikaciju kao zadani muzički player.",
            color = t.textMuted, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(6.dp))
        SettingSwitch("Dugi pritisak = premotavanje", "Drži ▲/▼ na volanu za premotavanje unutar pjesme", s.swcLongPressSeek) { v -> update { it.copy(swcLongPressSeek = v) } }
        if (s.swcLongPressSeek) {
            SettingSlider("Korak premotavanja", s.swcSeekStepSec.toFloat(), 5f..30f, steps = 4, label = { "${it.toInt()} s" }) { v -> update { it.copy(swcSeekStepSec = v.toInt()) } }
        }
        SettingSwitch("Dupli pritisak play/pause = sljedeća", "Korisno ako volan nema zasebne tipke", s.swcDoublePressNext) { v -> update { it.copy(swcDoublePressNext = v) } }
    }
    SettingsCard("Dijagnostika tastera") {
        val keys by PlayerHub.keys.collectAsState()
        Text("Pritisni taster na volanu – ovdje se prikazuje šta aplikacija prima.", color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        if (keys.isEmpty()) {
            Text("Još nije primljen nijedan taster.", color = t.text, style = MaterialTheme.typography.titleMedium)
        }
        val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
        keys.forEach { k ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(fmt.format(Date(k.time)), color = t.textMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(90.dp))
                Text(k.name, color = t.accent, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${k.action}  (kod ${k.keyCode}${if (k.repeat > 0) ", ponavljanje ${k.repeat}" else ""})", color = t.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --------------------------------------------------------------------------------------------- CID

@Composable
private fun CidPage(s: AppSettings, status: String, preview: CidText, actions: SettingsActions) {
    val t = LocalCarTheme.current
    val update = actions.update
    SettingsCard("Gornji info ekran (CID)") {
        CidPreview(preview)
        Spacer(Modifier.height(10.dp))
        Text("Status: $status", color = t.accent, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        SettingChoice(
            "Način slanja",
            null,
            listOf(CidMode.OFF to "Isključeno", CidMode.BROADCAST to "Head unit CAN-box", CidMode.USB_SLCAN to "USB CAN adapter (eksperimentalno)"),
            s.cidMode,
        ) { v -> update { it.copy(cidMode = v) } }
        Text(
            when (s.cidMode) {
                CidMode.OFF -> "Na CID se ne šalju podaci."
                CidMode.BROADCAST -> "Šalje standardne 'metachanged' broadcaste i MediaSession podatke. Android head uniti za Astru H sa CAN-box dekoderom (Raise/Hiworld/Simple Soft) ih prosljeđuju na fabrički CID."
                CidMode.USB_SLCAN -> "Direktno piše na MS-CAN (95,2 kbit/s, OBD pinovi 3 i 11) preko USB CAN adaptera sa SLCAN firmverom (npr. CANable). Protokol je rekonstruisan iz community istraživanja – testiraj dugmetom ispod."
            },
            color = t.textMuted, style = MaterialTheme.typography.bodyMedium,
        )
    }
    SettingsCard("Sadržaj prikaza") {
        SettingChoice(
            "Raspored", null,
            listOf(
                CidLayout.TITLE_ARTIST to "Naslov / izvođač",
                CidLayout.ARTIST_TITLE to "Izvođač / naslov",
                CidLayout.TITLE_ONLY to "Samo naslov",
                CidLayout.SCROLLING to "Pokretni tekst",
            ),
            s.cidLayout,
        ) { v -> update { it.copy(cidLayout = v) } }
        SettingSwitch("Ikona ▶ / ❚❚", "Prikazuje stanje reprodukcije ispred teksta", s.cidShowPlayIcon) { v -> update { it.copy(cidShowPlayIcon = v) } }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip("Pošalji test poruku", false, actions.cidTest)
        }
    }
    if (s.cidMode == CidMode.USB_SLCAN) {
        SettingsCard("Napredno (CAN)") {
            SettingChoice(
                "Brzina CAN busa", "Astra H MS-CAN radi na 95,2 kbit/s",
                listOf(95 to "95,2k (MS-CAN)", 100 to "100k", 125 to "125k"),
                s.cidSlcanBitrate,
            ) { v -> update { it.copy(cidSlcanBitrate = v) } }
            HexField("CAN ID poruke (radio → ekran)", s.cidTxId) { v -> update { it.copy(cidTxId = v) } }
            HexField("CAN ID flow-control (ekran → radio)", s.cidFcId) { v -> update { it.copy(cidFcId = v) } }
        }
    }
}

@Composable
private fun HexField(title: String, value: Int, onChange: (Int) -> Unit) {
    val t = LocalCarTheme.current
    var text by remember(value) { mutableStateOf("%03X".format(value)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = t.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("0x", color = t.textMuted, style = MaterialTheme.typography.titleMedium)
        BasicTextField(
            text,
            { v ->
                val clean = v.uppercase().filter { it in "0123456789ABCDEF" }.take(3)
                text = clean
                clean.toIntOrNull(16)?.let(onChange)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = t.accent, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(t.accent),
            modifier = Modifier
                .width(90.dp)
                .clip(RoundedCornerShape(t.corner))
                .background(t.surfaceHigh)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** Mock-up of the Astra H colour information display showing the current CID text. */
@Composable
private fun CidPreview(text: CidText) {
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B3F8A), Color(0xFF0A1D4A))))
            .border(3.dp, Color(0xFF222222), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("CD/MP3", color = Color(0xFFFFC542), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("+12°C   " + clock.format(Date()), color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(text.line1, label = "cid1", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }) {
                Text(it, color = Color.White, fontSize = 22.sp, fontFamily = FontFamily.Monospace, maxLines = 1, textAlign = TextAlign.Center)
            }
            Text(text.line2, color = Color(0xFFD5E2FF), fontSize = 18.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text(text.line3, color = Color(0xFF9FB6E8), fontSize = 14.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

// ----------------------------------------------------------------------------------------- library

@Composable
private fun LibraryPage(s: AppSettings, library: Library, actions: SettingsActions) {
    val t = LocalCarTheme.current
    val update = actions.update
    SettingsCard("Skeniranje") {
        SettingSlider("Zanemari fajlove kraće od", s.minDurationSec.toFloat(), 0f..120f, steps = 11, label = { "${it.toInt()} s" }) { v ->
            update { it.copy(minDurationSec = v.toInt()) }
        }
        Text("Korisno za skrivanje zvukova obavijesti i glasovnih poruka.", color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Chip("Ponovo skeniraj memoriju", false, actions.rescan)
        Spacer(Modifier.height(6.dp))
        Text("${library.songs.size} pjesama · ${library.albums.size} albuma · ${library.artists.size} izvođača", color = t.text, style = MaterialTheme.typography.titleMedium)
    }
    SettingsCard("Sortiranje pjesama") {
        SettingChoice(
            "Redoslijed", null,
            listOf(SortOrder.TITLE to "Naslov", SortOrder.ARTIST to "Izvođač", SortOrder.ALBUM to "Album", SortOrder.DATE_ADDED to "Najnovije"),
            s.sortOrder,
        ) { v -> update { it.copy(sortOrder = v) } }
    }
    SettingsCard("Folderi") {
        Text("Isključi foldere koje ne želiš u biblioteci (npr. WhatsApp, snimke poziva).", color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
        val all = (library.folders.map { it.path } + s.excludedFolders).distinct().sorted()
        all.forEach { path ->
            val included = path !in s.excludedFolders
            SettingSwitch(path.substringAfterLast('/'), path, included) { inc ->
                update { it.copy(excludedFolders = if (inc) it.excludedFolders - path else it.excludedFolders + path) }
            }
        }
    }
}

@Composable
private fun AboutPage() {
    val t = LocalCarTheme.current
    SettingsCard("CarMusic") {
        Text("Verzija ${BuildConfig.VERSION_NAME}", color = t.text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Offline muzički player za Android head unite – optimizovan za Opel Astru H: velike kontrole, komande s volana, " +
                "prikaz pjesme na gornjem CID ekranu, ekvilajzer i teme inspirisane fabričkim sistemima VAG grupacije.",
            color = t.textMuted, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Album, null, tint = t.accent)
            Spacer(Modifier.width(8.dp))
            Text("Formati: MP3, FLAC, AAC/M4A, OGG, OPUS, WAV", color = t.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
