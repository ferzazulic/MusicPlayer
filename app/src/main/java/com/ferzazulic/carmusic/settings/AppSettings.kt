package com.ferzazulic.carmusic.settings

enum class CidMode { OFF, BROADCAST, USB_SLCAN }
enum class AnimationLevel { FULL, REDUCED, OFF }
enum class SortOrder { TITLE, ARTIST, ALBUM, DATE_ADDED }
enum class CidLayout { TITLE_ARTIST, ARTIST_TITLE, TITLE_ONLY, SCROLLING }

data class AppSettings(
    // Appearance
    val themeId: String = "mib_discover",
    val customAccent: Long? = null,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val artworkBackground: Boolean = true,
    val showVisualizer: Boolean = true,
    val showClock: Boolean = true,
    val keepScreenOn: Boolean = true,
    val largeControls: Boolean = true,

    // Audio
    val eqEnabled: Boolean = false,
    val eqPreset: String = "Flat",
    val eqBands: List<Int> = listOf(0, 0, 0, 0, 0), // millibels, -1500..1500
    val bassBoost: Int = 0,          // 0..1000
    val virtualizer: Int = 0,        // 0..1000
    val loudnessGain: Int = 0,       // millibels 0..1500
    val fadeOnPlayPause: Boolean = true,
    val fadeDurationMs: Int = 400,
    val skipSilence: Boolean = false,
    val playbackSpeed: Float = 1f,

    // Playback
    val resumeOnStart: Boolean = true,
    val autoPlayOnStart: Boolean = false,
    val launchOnBoot: Boolean = false,
    val rememberPosition: Boolean = true,
    val previousRestartsTrack: Boolean = true,
    val pauseOnDisconnect: Boolean = true,

    // Steering wheel
    val swcLongPressSeek: Boolean = true,
    val swcSeekStepSec: Int = 10,
    val swcDoublePressNext: Boolean = false,

    // Library
    val minDurationSec: Int = 30,
    val excludedFolders: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.TITLE,

    // CID (Opel Astra H colour information display)
    val cidMode: CidMode = CidMode.BROADCAST,
    val cidLayout: CidLayout = CidLayout.TITLE_ARTIST,
    val cidShowPlayIcon: Boolean = true,
    val cidSlcanBitrate: Int = 95, // kbit/s, Astra H MS-CAN = 95.2
    val cidTxId: Int = 0x6C1,
    val cidFcId: Int = 0x2C1,
    val cidEmulateRadio: Boolean = true,

    // Extras
    val favorites: Set<String> = emptySet(),
    val nightDim: Boolean = true,
    val nightFrom: Int = 20,
    val nightTo: Int = 6,
    val swipeOnCover: Boolean = true,
)
