@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ferzazulic.carmusic.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.data.Library
import com.ferzazulic.carmusic.data.Song
import com.ferzazulic.carmusic.settings.SortOrder
import com.ferzazulic.carmusic.ui.PlaybackUi
import com.ferzazulic.carmusic.ui.components.ArtworkImage
import com.ferzazulic.carmusic.ui.components.CarIconButton
import com.ferzazulic.carmusic.ui.components.SpectrumBars
import com.ferzazulic.carmusic.ui.components.formatTime
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme
import com.ferzazulic.carmusic.ui.theme.PlayerLayout
import kotlinx.coroutines.delay

private data class Detail(val title: String, val subtitle: String, val songs: List<Song>)

private val tabs = listOf("Pjesme", "Albumi", "Izvođači", "Folderi", "Red")

@Composable
fun LibraryScreen(
    library: Library,
    pb: PlaybackUi,
    sort: SortOrder,
    scanning: Boolean,
    onPlay: (List<Song>, Int, Boolean) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onRescan: () -> Unit,
) {
    val t = LocalCarTheme.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<Detail?>(null) }
    BackHandler(detail != null) { detail = null }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        // Header: tabs + search + actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabStrip(tab) { tab = it; detail = null }
            Spacer(Modifier.weight(1f))
            SearchField(query) { query = it }
            Spacer(Modifier.width(8.dp))
            if (scanning) CircularProgressIndicator(Modifier.size(32.dp), color = t.accent, strokeWidth = 3.dp)
            else CarIconButton(Icons.Rounded.Refresh, "Osvježi", onRescan, size = 52.dp)
        }
        Spacer(Modifier.height(14.dp))

        val q = query.trim()
        val filteredSongs = remember(library, q, sort) {
            val base = if (q.isEmpty()) library.songs else library.songs.filter {
                it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
            }
            when (sort) {
                SortOrder.TITLE -> base
                SortOrder.ARTIST -> base.sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.album }.thenBy { it.track })
                SortOrder.ALBUM -> base.sortedWith(compareBy<Song> { it.album.lowercase() }.thenBy { it.track })
                SortOrder.DATE_ADDED -> base.sortedByDescending { it.dateAdded }
            }
        }

        AnimatedContent(
            targetState = Triple(tab, detail, q.isNotEmpty() && tab != 0),
            contentKey = { "${it.first}-${it.second?.title}-${it.third}" },
            transitionSpec = {
                val forward = targetState.second != null || targetState.first > initialState.first
                val d = if (forward) 1 else -1
                (slideInHorizontally(tween(t.duration(380), easing = FastOutSlowInEasing)) { it / 5 * d } + fadeIn(tween(t.duration(380)))) togetherWith
                    (slideOutHorizontally(tween(t.duration(300))) { -it / 5 * d } + fadeOut(tween(t.duration(220))))
            },
            label = "library",
        ) { (currentTab, currentDetail, searching) ->
            when {
                library.songs.isEmpty() -> EmptyState(scanning)
                currentDetail != null -> DetailView(currentDetail, pb, onPlay) { detail = null }
                searching || currentTab == 0 -> SongList(filteredSongs, pb, header = {
                    PlayAllHeader("${filteredSongs.size} pjesama", filteredSongs, onPlay)
                }) { i -> onPlay(filteredSongs, i, false) }
                currentTab == 1 -> AlbumGrid(library) { a -> detail = Detail(a.title, a.artist, a.songs) }
                currentTab == 2 -> SimpleList(
                    library.artists.map { Triple(it.name, "${it.albumCount} albuma · ${it.songs.size} pjesama", it.songs) },
                    Icons.Rounded.Person,
                ) { (n, s, songs) -> detail = Detail(n, s, songs) }
                currentTab == 3 -> SimpleList(
                    library.folders.map { Triple(it.name, it.path, it.songs) },
                    Icons.Rounded.Folder,
                ) { (n, s, songs) -> detail = Detail(n, s, songs) }
                else -> {
                    val queue = pb.queueIds.mapNotNull { library.song(it) }
                    SongList(queue, pb, highlightIndex = pb.index, header = {
                        Text("Red reprodukcije · ${queue.size}", color = t.textMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp))
                    }) { i -> onPlayQueueIndex(i) }
                }
            }
        }
    }
}

@Composable
private fun TabStrip(selected: Int, onSelect: (Int) -> Unit) {
    val t = LocalCarTheme.current
    Row(
        Modifier
            .clip(RoundedCornerShape(t.corner + 4.dp))
            .background(t.surface.copy(alpha = 0.7f))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, name ->
            val active = i == selected
            val bgAlpha by androidx.compose.animation.core.animateFloatAsState(if (active) 1f else 0f, tween(t.duration(250)), label = "tab")
            Box(
                Modifier
                    .clip(RoundedCornerShape(t.corner))
                    .background(t.accent.copy(alpha = bgAlpha))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(t.label(name), color = if (active) t.onAccent else t.text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val t = LocalCarTheme.current
    var focused by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (focused || value.isNotEmpty()) 280.dp else 190.dp, tween(t.duration(300)), label = "searchW")
    Row(
        Modifier
            .width(width)
            .height(52.dp)
            .clip(RoundedCornerShape(t.corner + 4.dp))
            .background(t.surface.copy(alpha = 0.7f))
            .border(1.dp, if (focused) t.accent else t.outline, RoundedCornerShape(t.corner + 4.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = t.textMuted)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Traži…", color = t.textMuted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = t.text),
                cursorBrush = SolidColor(t.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            Icon(Icons.Rounded.Close, "Obriši", tint = t.textMuted, modifier = Modifier.clickable { onChange("") })
        }
    }
}

@Composable
private fun PlayAllHeader(subtitle: String, songs: List<Song>, onPlay: (List<Song>, Int, Boolean) -> Unit) {
    val t = LocalCarTheme.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(subtitle, color = t.textMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        PillButton("Pusti sve", Icons.Rounded.PlayArrow) { onPlay(songs, 0, false) }
        Spacer(Modifier.width(10.dp))
        PillButton("Nasumično", Icons.Rounded.Shuffle) { onPlay(songs, -1, true) }
    }
}

@Composable
fun PillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    Row(
        Modifier
            .clip(RoundedCornerShape(if (t.corner >= 8.dp) 50.dp else t.corner))
            .background(t.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = t.onAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(t.label(text), color = t.onAccent, style = MaterialTheme.typography.labelLarge)
    }
}

/** Fades & lifts list items in with a small stagger the first time they appear. */
@Composable
private fun Modifier.staggeredEntrance(index: Int): Modifier {
    val t = LocalCarTheme.current
    val progress = remember { Animatable(if (t.animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay((index % 14) * 28L)
            progress.animateTo(1f, tween(t.duration(380), easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40f
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    pb: PlaybackUi,
    highlightIndex: Int? = null,
    header: @Composable () -> Unit = {},
    onClick: (Int) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { header() }
        itemsIndexed(songs, key = { i, s -> "${s.id}_$i" }) { i, song ->
            val current = if (highlightIndex != null) i == highlightIndex else song.id == pb.songId
            SongRow(song, current, pb.isPlaying, Modifier.animateItem().staggeredEntrance(i)) { onClick(i) }
        }
    }
}

@Composable
private fun SongRow(song: Song, current: Boolean, playing: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val layout = t.base.layout
    val listStyle = layout == PlayerLayout.MMI || layout == PlayerLayout.RNS
    val bg by androidx.compose.animation.core.animateFloatAsState(if (current) 1f else 0f, tween(400), label = "rowBg")
    val rowShape = if (listStyle) RoundedCornerShape(0.dp) else RoundedCornerShape(t.corner + 2.dp)
    val highlight = if (layout == PlayerLayout.MMI) {
        Brush.horizontalGradient(listOf(t.accent.copy(alpha = bg), t.accent.copy(alpha = 0.5f * bg), t.accent.copy(alpha = 0f)))
    } else {
        SolidColor(t.accent.copy(alpha = 0.18f * bg))
    }
    val onHighlight = layout == PlayerLayout.MMI && current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = if (listStyle) 0.dp else 3.dp)
            .clip(rowShape)
            .background(highlight)
            .drawBehind {
                if (listStyle) drawLine(t.outline.copy(alpha = 0.6f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp)) {
            ArtworkImage(song, Modifier.fillMaxSize(), size = 128)
            if (current) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(t.corner)).background(t.bgBottom.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    SpectrumBars(playing, Modifier.size(28.dp, 22.dp), bars = 4)
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = if (onHighlight) androidx.compose.ui.graphics.Color.White else if (current) t.accent else t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} · ${song.album}", color = if (onHighlight) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f) else t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatTime(song.durationMs), color = t.textMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AlbumGrid(library: Library, onOpen: (com.ferzazulic.carmusic.data.Album) -> Unit) {
    val t = LocalCarTheme.current
    LazyVerticalGrid(
        GridCells.Adaptive(180.dp),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(library.albums, key = { _, a -> a.id }) { i, album ->
            Column(
                Modifier
                    .staggeredEntrance(i)
                    .clip(RoundedCornerShape(t.corner + 2.dp))
                    .clickable { onOpen(album) }
                    .padding(6.dp)
            ) {
                ArtworkImage(album.songs.first(), Modifier.fillMaxWidth().aspectRatio(1f), size = 320)
                Spacer(Modifier.height(8.dp))
                Text(album.title, color = t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SimpleList(
    items: List<Triple<String, String, List<Song>>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onOpen: (Triple<String, String, List<Song>>) -> Unit,
) {
    val t = LocalCarTheme.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        itemsIndexed(items, key = { i, it -> "${it.first}_$i" }) { i, item ->
            Row(
                Modifier
                    .animateItem()
                    .staggeredEntrance(i)
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(t.corner + 2.dp))
                    .clickable { onOpen(item) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(t.corner)).background(t.surfaceHigh), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = t.accent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.first, color = t.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.second, color = t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DetailView(d: Detail, pb: PlaybackUi, onPlay: (List<Song>, Int, Boolean) -> Unit, onBack: () -> Unit) {
    val t = LocalCarTheme.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 700.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Column(Modifier.width(260.dp)) {
                    CarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Nazad", onBack, size = 52.dp)
                    Spacer(Modifier.height(8.dp))
                    ArtworkImage(d.songs.first(), Modifier.fillMaxWidth().aspectRatio(1f), size = 512)
                    Spacer(Modifier.height(12.dp))
                    Text(d.title, color = t.text, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(d.subtitle, color = t.textMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(24.dp))
            }
            Column(Modifier.weight(1f)) {
                if (!wide) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Nazad", onBack, size = 52.dp)
                        Text(d.title, color = t.text, style = MaterialTheme.typography.headlineSmall, maxLines = 1, modifier = Modifier.offset(x = 8.dp))
                    }
                }
                SongList(d.songs, pb, header = { PlayAllHeader("${d.songs.size} pjesama", d.songs, onPlay) }) { i -> onPlay(d.songs, i, false) }
            }
        }
    }
}

@Composable
private fun EmptyState(scanning: Boolean) {
    val t = LocalCarTheme.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (scanning) {
                CircularProgressIndicator(color = t.accent)
                Spacer(Modifier.height(16.dp))
                Text("Skeniram muziku…", color = t.text, style = MaterialTheme.typography.titleLarge)
            } else {
                Text("Nema pronađene muzike", color = t.text, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Kopiraj MP3/FLAC/M4A fajlove u internu memoriju (npr. Music folder) i pritisni Osvježi.",
                    color = t.textMuted, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
