package com.fouru.player

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var homeOverlay: LinearLayout

    companion object {
        private const val TEST_CHANNEL_ID = "test1"
        private const val TEST_STREAM = "http://80.253.254.74/live/69d2c6f62704.m3u8"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(playerView)

        homeOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.rgb(8, 12, 18))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "4U PLAYER"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "مشغل بث مباشر"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 34)
        }

        val playButton = Button(this).apply {
            text = "▶ تشغيل القناة"
            textSize = 20f
            isAllCaps = false
            setOnClickListener { playChannel(TEST_CHANNEL_ID) }
        }

        val status = TextView(this).apply {
            text = "4U • SECURE PLAYER"
            textSize = 14f
            setTextColor(Color.rgb(0, 229, 176))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }

        homeOverlay.addView(title)
        homeOverlay.addView(subtitle)
        homeOverlay.addView(playButton)
        homeOverlay.addView(status)
        root.addView(homeOverlay)

        setContentView(root)
        handleIntentData(intent?.data)
    }

    private fun handleIntentData(uri: Uri?) {
        if (uri == null) return
        if (uri.scheme != "fouruplayer" || uri.host != "play") return

        val channelId = uri.getQueryParameter("channel_id") ?: return
        if (channelId == TEST_CHANNEL_ID) {
            playChannel(channelId)
        }
    }

    private fun playChannel(channelId: String) {
        if (channelId != TEST_CHANNEL_ID) return

        homeOverlay.visibility = View.GONE
        releasePlayer()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 4UPlayer/1.0")

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->
                playerView.player = exo
                exo.repeatMode = Player.REPEAT_MODE_OFF
                exo.setMediaItem(MediaItem.fromUri(TEST_STREAM))
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
