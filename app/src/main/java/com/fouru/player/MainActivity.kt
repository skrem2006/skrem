package com.fouru.player

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var homeOverlay: LinearLayout
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
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
            setPadding(54, 48, 54, 48)
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
            text = "الصق أي رابط بث أو افتحه مباشرة من AppCreator24"
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 26)
        }

        urlInput = EditText(this).apply {
            hint = "https://example.com/live.m3u8"
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            setPadding(24, 12, 24, 12)
        }

        val playButton = Button(this).apply {
            text = "▶ تشغيل الرابط"
            textSize = 19f
            isAllCaps = false
            setOnClickListener {
                val value = urlInput.text?.toString()?.trim().orEmpty()
                playUrl(value, null)
            }
        }

        val status = TextView(this).apply {
            text = "4U • DIRECT URL PLAYER"
            textSize = 13f
            setTextColor(Color.rgb(0, 229, 176))
            gravity = Gravity.CENTER
            setPadding(0, 26, 0, 0)
        }

        homeOverlay.addView(title)
        homeOverlay.addView(subtitle)
        homeOverlay.addView(urlInput)
        homeOverlay.addView(playButton)
        homeOverlay.addView(status)
        root.addView(homeOverlay)

        setContentView(root)
        handleIntentData(intent?.data)
    }

    private fun enterImmersive() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    private fun handleIntentData(uri: Uri?) {
        if (uri == null) return
        if (uri.scheme != "fouruplayer" || uri.host != "play") return

        val streamUrl = uri.getQueryParameter("url")?.trim().orEmpty()
        if (streamUrl.isEmpty()) return

        val headers = linkedMapOf<String, String>()
        uri.getQueryParameter("ua")?.takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
        uri.getQueryParameter("referer")?.takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
        uri.getQueryParameter("origin")?.takeIf { it.isNotBlank() }?.let { headers["Origin"] = it }

        playUrl(streamUrl, headers)
    }

    private fun playUrl(streamUrl: String, customHeaders: Map<String, String>?) {
        val parsed = runCatching { Uri.parse(streamUrl) }.getOrNull()
        if (parsed == null || (parsed.scheme != "http" && parsed.scheme != "https")) {
            Toast.makeText(this, "الرابط غير صالح", Toast.LENGTH_SHORT).show()
            return
        }

        homeOverlay.visibility = View.GONE
        releasePlayer()

        val headers = linkedMapOf<String, String>()
        headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 4UPlayer/2.0"
        if (customHeaders != null) headers.putAll(customHeaders)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setUserAgent(headers["User-Agent"] ?: "4UPlayer/2.0")
            .setDefaultRequestProperties(headers)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpFactory)

        val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
        if (streamUrl.contains(".m3u8", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->
                playerView.player = exo
                exo.repeatMode = Player.REPEAT_MODE_OFF
                exo.setMediaItem(mediaItemBuilder.build())
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentData(intent?.data)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersive()
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
