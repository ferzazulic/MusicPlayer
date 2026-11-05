package com.ferzazulic.carmusic.playback

import android.content.Context

/** Persists the play queue so music continues where it stopped when the ignition is turned on again. */
class QueueStore(context: Context) {
    private val prefs = context.getSharedPreferences("queue", Context.MODE_PRIVATE)

    data class Saved(val ids: List<Long>, val index: Int, val positionMs: Long, val shuffle: Boolean, val repeat: Int)

    fun save(ids: List<Long>, index: Int, positionMs: Long, shuffle: Boolean, repeat: Int) {
        prefs.edit()
            .putString("ids", ids.joinToString(","))
            .putInt("index", index)
            .putLong("pos", positionMs)
            .putBoolean("shuffle", shuffle)
            .putInt("repeat", repeat)
            .apply()
    }

    fun savePosition(index: Int, positionMs: Long) {
        prefs.edit().putInt("index", index).putLong("pos", positionMs).apply()
    }

    fun load(): Saved? {
        val ids = prefs.getString("ids", null)?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (ids.isEmpty()) return null
        return Saved(
            ids,
            prefs.getInt("index", 0),
            prefs.getLong("pos", 0L),
            prefs.getBoolean("shuffle", false),
            prefs.getInt("repeat", 0),
        )
    }
}
