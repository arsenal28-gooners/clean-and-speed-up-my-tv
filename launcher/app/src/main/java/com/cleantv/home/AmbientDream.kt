package com.cleantv.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import java.util.Locale

/** Idle screen in the Mockup 2 style: beach and characters on the left fading to black, a thin clock on the right. */
class AmbientDream : DreamService() {

    private lateinit var px: Px
    private lateinit var str: Strings
    private var phase = Phase.MALAM
    private var clockTv: TextView? = null
    private var dateTv: TextView? = null
    private var greetTv: TextView? = null

    private val tick = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = update()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        px = Px(this)
        str = Strings(getSharedPreferences("launcher", Context.MODE_PRIVATE).getString("lang", "id") == "en")
        build()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(tick, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(tick, filter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { unregisterReceiver(tick) } catch (e: IllegalArgumentException) { }
    }

    private fun build() {
        phase = phaseOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        val dm = resources.displayMetrics
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(ImageView(this).apply {
            setImageBitmap(Backdrop.ambient(phase, dm.widthPixels, dm.heightPixels))
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val cfg = UserConfig.get(this)
        UserConfig.image(this, "characters", px.d(653), px.d(649))?.let { bmp ->
            root.addView(ImageView(this).apply {
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.FIT_XY
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(px.d(653), px.d(649)).apply { leftMargin = px.d(107); topMargin = px.d(431) })
        }

        val id = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        if (cfg.names.isNotEmpty()) names.addView(TextView(this).look(px, 38, Looks.TEXT).apply { text = cfg.names })
        greetTv = TextView(this).look(px, 26, Looks.TEXT3, 300)
        names.addView(greetTv, lp(WRAP, WRAP).apply { topMargin = px.d(2) })
        id.addView(names)
        id.addView(TextView(this).look(px, 34, Looks.ACCENT, 700).apply {
            text = cfg.monogram
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(px.d(2), Looks.ACCENT) }
        }, lp(px.d(96), px.d(96)).apply { leftMargin = px.d(24) })
        root.addView(id, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END).apply { topMargin = px.d(96); rightMargin = px.d(112) })

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        clockTv = TextView(this).look(px, 300, Looks.TEXT, 200).apply { letterSpacing = -0.04f }
        col.addView(clockTv)
        dateTv = TextView(this).look(px, 44, Looks.TEXT2, 300)
        col.addView(dateTv, lp(WRAP, WRAP).apply { topMargin = px.d(16) })
        Fixtures.upcoming(this).firstOrNull()?.takeIf { cfg.team.isNotEmpty() }?.let { m ->
            col.addView(TextView(this).look(px, 30, Looks.TEXT3, 300).apply {
                text = (if (m.home) "${cfg.team} vs ${m.opponent}" else "${m.opponent} vs ${cfg.team}") + ", " + str.longDate(m.date)
            }, lp(WRAP, WRAP).apply { topMargin = px.d(24) })
        }
        root.addView(col, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END).apply { topMargin = px.d(330); rightMargin = px.d(112) })

        val hint = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        hint.addView(keycap(this, px, "OK"))
        hint.addView(TextView(this).look(px, 24, Looks.TEXT3).apply { text = str.backHome }, lp(WRAP, WRAP).apply { leftMargin = px.d(12) })
        root.addView(hint, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END or Gravity.BOTTOM).apply { rightMargin = px.d(112); bottomMargin = px.d(72) })

        setContentView(root)
        update()
    }

    private fun update() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        if (phaseOf(h) != phase) {
            build()
            return
        }
        clockTv?.text = String.format(Locale.US, "%02d:%02d", h, cal.get(Calendar.MINUTE))
        dateTv?.text = str.longDate(cal)
        greetTv?.text = str.greetings(phase)[0]
    }
}
