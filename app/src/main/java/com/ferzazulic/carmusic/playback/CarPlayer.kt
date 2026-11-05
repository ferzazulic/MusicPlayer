package com.ferzazulic.carmusic.playback

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlayer
import com.ferzazulic.carmusic.settings.AppSettings

/**
 * Wraps ExoPlayer to add premium-player behaviour: smooth volume fades on play/pause
 * and a configurable "previous" button (restart track vs. always previous track).
 */
class CarPlayer(
    private val exo: ExoPlayer,
    private val settings: () -> AppSettings,
) : ForwardingPlayer(exo) {

    private var fade: ValueAnimator? = null

    private fun animateVolume(from: Float, to: Float, durationMs: Int, end: () -> Unit) {
        fade?.cancel()
        fade = ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs.toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener { exo.volume = it.animatedValue as Float }
            var cancelled = false
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: android.animation.Animator) { cancelled = true }
                override fun onAnimationEnd(animation: android.animation.Animator) { if (!cancelled) end() }
            })
            start()
        }
    }

    override fun play() {
        val s = settings()
        fade?.cancel()
        if (s.fadeOnPlayPause && !exo.playWhenReady) {
            exo.volume = 0f
            exo.play()
            animateVolume(0f, 1f, s.fadeDurationMs) {}
        } else {
            exo.volume = 1f
            exo.play()
        }
    }

    override fun pause() {
        val s = settings()
        if (s.fadeOnPlayPause && exo.isPlaying) {
            animateVolume(exo.volume, 0f, s.fadeDurationMs) {
                exo.pause()
                exo.volume = 1f
            }
        } else {
            fade?.cancel()
            exo.pause()
            exo.volume = 1f
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) play() else pause()
    }

    override fun seekToPrevious() {
        if (!settings().previousRestartsTrack && exo.hasPreviousMediaItem()) {
            exo.seekToPreviousMediaItem()
        } else {
            exo.seekToPrevious()
        }
    }

    fun cancelFade() {
        fade?.cancel()
    }
}
