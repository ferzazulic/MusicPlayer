package com.hcn.MediaActivity

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import com.ferzazulic.carmusic.cid.SourceInfoClient

/**
 * CarMusic's own screen that carries the same class name as the HCN stock player screen. The HCN
 * firmware appears to accept music info for the CAN display only from a screen with this name
 * (constant HcnMediaCLASS in android.sourceservice.SourceService). Used by the CID test.
 */
class MusicPlayerUiActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "CarMusic – CID test\n\nGledaj CID (25 s)…"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
        })
        val uri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val pos = intent.getLongExtra(EXTRA_POS, 0)
        if (uri != null) SourceInfoClient.probeMediaPlayer(this, uri, pos, 25)
        handler.postDelayed({ finish() }, 27_000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_POS = "pos"
    }
}
