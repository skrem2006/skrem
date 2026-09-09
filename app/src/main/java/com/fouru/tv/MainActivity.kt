package com.fouru.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var catalogView: View
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    companion object {
        private const val STREAM_URL = "http://80.253.254.74/live/69d2c6f62704.m3u8"
        private const val CHANNEL_NAME = "4U LIVE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enterImmersive()

        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            finish()
            return
        }

        if (isVpnActive()) {
            Toast.makeText(this, "يرجى إيقاف VPN لتشغيل 4U TV", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        buildUi()
    }

    private fun buildUi() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(7, 10, 15))
        }

        playerView = PlayerView(this).apply {
            visibility = View.GONE
            useController = true
            controllerAutoShow = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(playerView)

        catalogView = createCatalog()
        root.addView(catalogView)
        setContentView(root)
    }

    private fun createCatalog(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val brand = TextView(this).apply {
            text = "4U TV"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        body.addView(brand, LinearLayout.LayoutParams(-1, dp(60)))

        val subtitle = TextView(this).apply {
            text = "البث المباشر"
            textSize = 16f
            setTextColor(Color.rgb(160, 170, 184))
            gravity = Gravity.CENTER
        }
        body.addView(subtitle, LinearLayout.LayoutParams(-1, dp(42)))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.rgb(18, 24, 33))
            isClickable = true
            isFocusable = true
            setOnClickListener { playChannel() }
        }

        val logo = TextView(this).apply {
            text = "4U"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 229, 176))
            setBackgroundColor(Color.rgb(10, 16, 22))
        }
        card.addView(logo, LinearLayout.LayoutParams(dp(84), dp(84)))

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, 0, 0)
        }

        val name = TextView(this).apply {
            text = CHANNEL_NAME
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        details.addView(name)

        val info = TextView(this).apply {
            text = "اضغط للمشاهدة • الجودة تلقائية"
            textSize = 14f
            setTextColor(Color.rgb(145, 156, 170))
            setPadding(0, dp(8), 0, 0)
        }
        details.addView(info)

        card.addView(details, LinearLayout.LayoutParams(0, -2, 1f))

        val live = TextView(this).apply {
            text = "LIVE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(0, 229, 176))
            gravity = Gravity.CENTER
        }
        card.addView(live, LinearLayout.LayoutParams(dp(70), dp(50)))

        val lp = LinearLayout.LayoutParams(-1, dp(124)).apply {
            topMargin = dp(28)
        }
        body.addView(card, lp)

        val footer = TextView(this).apply {
            text = "4U Secure Streaming"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(88, 98, 112))
            setPadding(0, dp(28), 0, 0)
        }
        body.addView(footer, LinearLayout.LayoutParams(-1, dp(72)))

        scroll.addView(body)
        return scroll
    }

    private fun playChannel() {
        if (isVpnActive()) {
            Toast.makeText(this, "يرجى إيقاف VPN", Toast.LENGTH_LONG).show()
            return
        }

        catalogView.visibility = View.GONE
        playerView.visibility = View.VISIBLE

        releasePlayer()

        val selector = DefaultTrackSelector(this)
        trackSelector = selector

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(false)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setUserAgent("4UTV/1.0 Android")

        val sourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpFactory)

        val item = MediaItem.Builder()
            .setUri(Uri.parse(STREAM_URL))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(selector)
            .setMediaSourceFactory(sourceFactory)
            .build()
            .also { exo ->
                playerView.player = exo
                exo.repeatMode = Player.REPEAT_MODE_OFF
                exo.setMediaItem(item)
                exo.prepare()
                exo.playWhenReady = true
            }

        addQualityButton()
    }

    private fun addQualityButton() {
        val quality = TextView(this).apply {
            text = "الجودة"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(190, 20, 24, 30))
            setOnClickListener { showQualityDialog() }
        }
        val lp = FrameLayout.LayoutParams(dp(100), dp(48), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(24)
            rightMargin = dp(24)
        }
        quality.tag = "quality_button"
        root.findViewWithTag<View>("quality_button")?.let { root.removeView(it) }
        root.addView(quality, lp)
    }

    private fun showQualityDialog() {
        val choices = arrayOf("تلقائي", "SD 480p", "HD 720p", "FHD 1080p")
        AlertDialog.Builder(this)
            .setTitle("اختيار الجودة")
            .setItems(choices) { _, which ->
                val selector = trackSelector ?: return@setItems
                val params = selector.buildUponParameters()
                when (which) {
                    0 -> params.clearVideoSizeConstraints()
                    1 -> params.setMaxVideoSize(854, 480)
                    2 -> params.setMaxVideoSize(1280, 720)
                    3 -> params.setMaxVideoSize(1920, 1080)
                }
                selector.parameters = params.build()
            }
            .show()
    }

    private fun isVpnActive(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = cm.allNetworks
        return networks.any { network ->
            val caps = cm.getNetworkCapabilities(network)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    private fun enterImmersive() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
        trackSelector = null
    }

    override fun onBackPressed() {
        if (::playerView.isInitialized && playerView.visibility == View.VISIBLE) {
            releasePlayer()
            playerView.visibility = View.GONE
            root.findViewWithTag<View>("quality_button")?.let { root.removeView(it) }
            catalogView.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
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
