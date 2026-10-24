package com.cleantv.home

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import java.net.Inet4Address
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Home screen in the approved Mockup 2 style: black ground with the beach + aurora backdrop,
 * a dock band with one glass highlight that springs between tiles, the focused app's name in
 * large type with a glow in its colour, and "Semua aplikasi" below that the page scrolls up to.
 */
class MainActivity : Activity() {

    private class AppEntry(val pkg: String, val label: String, val banner: Bitmap, val color: Int)

    private lateinit var px: Px
    private lateinit var str: Strings
    private lateinit var phase: Phase

    private val prefs by lazy { getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var page: FrameLayout
    private lateinit var menuLayer: FrameLayout
    private lateinit var clockTv: TextView
    private lateinit var dateTv: TextView
    private lateinit var greetTv: TextView
    private lateinit var nameTv: TextView
    private lateinit var glow: GlowView
    private lateinit var highlight: View
    private lateinit var dockRow: LinearLayout
    private lateinit var grid: LinearLayout
    private lateinit var avNormal: ImageView
    private lateinit var avWave: ImageView
    private val gridLabels = mutableListOf<TextView>()

    private var apps: List<AppEntry> = emptyList()
    private var greetIndex = 0
    private var inApps = false
    private var menuReturn: View? = null

    private val tick = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateClock()
    }

    private val greetLoop = object : Runnable {
        override fun run() {
            greetIndex++
            showGreeting(true)
            handler.postDelayed(this, 8000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        px = Px(this)
        loadApps()
        build()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(tick, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(tick, filter)
        updateClock()
        handler.postDelayed(greetLoop, 8000)
        val before = apps.map { it.pkg }
        loadApps()
        if (apps.map { it.pkg } != before) {
            fillDock()
            fillGrid()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(tick)
        handler.removeCallbacks(greetLoop)
    }

    // While another app is in front, the home screen gives its views and bitmaps back to the TV.
    private var visible = false
    private var released = false
    private val releaseLater = Runnable { release() }

    override fun onStart() {
        super.onStart()
        visible = true
        handler.removeCallbacks(releaseLater)
        if (released) {
            released = false
            loadApps()
            build()
        }
    }

    override fun onStop() {
        super.onStop()
        visible = false
        handler.postDelayed(releaseLater, 10_000)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (!visible && level >= TRIM_MEMORY_BACKGROUND) release()
    }

    private fun release() {
        if (released || visible) return
        released = true
        handler.removeCallbacks(releaseLater)
        setContentView(View(this))
        apps = emptyList()
        gridLabels.clear()
    }

    // ---------- Layout (design px on a 1920x1080 canvas) ----------

    private fun at(x: Int, y: Int, w: Int = WRAP, h: Int = WRAP) =
        FrameLayout.LayoutParams(if (w > 0) px.d(w) else w, if (h > 0) px.d(h) else h).apply { leftMargin = px.d(x); topMargin = px.d(y) }

    private fun build() {
        str = Strings(prefs.getString("lang", "id") == "en")
        phase = phaseOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        val dm = resources.displayMetrics
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        // The page is taller than the screen so "Semua aplikasi" is inside its bounds when it scrolls up.
        page = FrameLayout(this).apply { noClip() }
        root.addView(page, FrameLayout.LayoutParams(MATCH, px.d(PAGE_H)))

        // One continuous aurora for the whole page, then the beach band on top of it.
        page.addView(ImageView(this).apply {
            setImageBitmap(Backdrop.aurora(phase, dm.widthPixels, PAGE_H))
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, at(0, 0, 1920, PAGE_H))
        page.addView(ImageView(this).apply {
            setImageBitmap(Backdrop.band(phase, dm.widthPixels))
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, at(0, 0, 1920, 700))
        glow = GlowView(this, px)
        page.addView(glow, at(0, 0, GLOW_W, GLOW_H))
        // The user's own characters (assets/custom/characters.*), if they added them.
        UserConfig.image(this, "characters", px.d(600), px.d(596))?.let { src ->
            page.addView(ImageView(this).apply {
                setImageBitmap(Backdrop.fadedCharacters(src, px.d(600), px.d(596)))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, at(1200, 96, 600, 596))
        }

        page.addView(topLeft(), at(80, 64, 1100))
        page.addView(pills(), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END).apply { topMargin = px.d(64); rightMargin = px.d(80) })
        matchCard()?.let { page.addView(it, at(80, 420, 500)) }
        nameTv = TextView(this).look(px, 76, Looks.TEXT, 300).apply { letterSpacing = -0.013f; maxLines = 1 }
        page.addView(nameTv, at(80, 612, 1100))
        page.addView(dockBand(), at(72, 728, 1776, 232))
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; noClip() }
        page.addView(grid, at(96, 996, 1728))

        menuLayer = FrameLayout(this).apply { visibility = View.GONE; setOnClickListener { closeMenu() } }
        root.addView(menuLayer, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)

        fillDock()
        fillGrid()
        setApps(false, animate = false)
        showGreeting(false)
        updateClock()
        dockRow.post { dockRow.getChildAt(0)?.requestFocus() }
    }

    private fun topLeft(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val avatar = FrameLayout(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(28, 28, 30)) }
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        // Avatar: the user's two poses (assets/custom/avatar_normal.*, avatar_wave.*), over the monogram.
        val size = px.d(52)
        val normal = UserConfig.image(this, "avatar_normal", size * 2, size * 2)
        val wave = UserConfig.image(this, "avatar_wave", size * 2, size * 2) ?: normal
        avatar.addView(TextView(this).look(px, 18, Looks.ACCENT, 700).apply {
            text = UserConfig.get(this@MainActivity).monogram
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        avNormal = ImageView(this).apply { setImageBitmap(normal); scaleType = ImageView.ScaleType.CENTER_CROP }
        avWave = ImageView(this).apply { setImageBitmap(wave); scaleType = ImageView.ScaleType.CENTER_CROP; alpha = 0f }
        avatar.addView(avNormal, FrameLayout.LayoutParams(MATCH, MATCH))
        avatar.addView(avWave, FrameLayout.LayoutParams(MATCH, MATCH))
        avatar.addView(View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(px.d(2), Looks.ACCENT) }
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        row.addView(avatar, lp(px.d(52), px.d(52)))
        greetTv = TextView(this).look(px, 32, Looks.TEXT2).apply { maxLines = 1; isSingleLine = true }
        row.addView(greetTv, lp(WRAP, WRAP).apply { leftMargin = px.d(16) })
        col.addView(row)
        clockTv = TextView(this).look(px, 168, Looks.TEXT, 200).apply { letterSpacing = -0.035f }
        col.addView(clockTv, lp(WRAP, WRAP).apply { topMargin = px.d(8) })
        dateTv = TextView(this).look(px, 34, Looks.TEXT3, 300)
        col.addView(dateTv, lp(WRAP, WRAP).apply { topMargin = px.d(4) })
        return col
    }

    private fun pill(iconRes: Int, label: String?, onClick: (View) -> Unit): View {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            if (label != null) setPadding(px.d(26), 0, px.d(26), 0)
            val bg = frost(px, px.f(30f))
            background = bg
            isFocusable = true
            isClickable = true
            contentDescription = label ?: str.settings
            setOnFocusChangeListener { _, has ->
                bg.setColor(if (has) Looks.FROST_ON else Looks.FROST)
                bg.setStroke(max(1, px.d(2)), if (has) Looks.ACCENT else Looks.FROST_EDGE)
            }
            setOnClickListener { onClick(it) }
        }
        p.addView(ImageView(this).apply { setImageResource(iconRes); imageTintList = ColorStateList.valueOf(Looks.TEXT) }, lp(px.d(28), px.d(28)))
        if (label != null) p.addView(TextView(this).look(px, 26, Looks.TEXT, 500).apply { text = label }, lp(WRAP, WRAP).apply { leftMargin = px.d(12) })
        return p
    }

    private fun pills(): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(pill(R.drawable.ic_input, str.inputs) { openMenu(it, inputMenu(), 492, 400) }, lp(WRAP, px.d(60)))
        row.addView(pill(R.drawable.ic_wifi, "Wi-Fi") { openMenu(it, wifiMenu(), 154, 420) }, lp(WRAP, px.d(60)).apply { leftMargin = px.d(14) })
        row.addView(pill(R.drawable.ic_settings, null) { openMenu(it, settingsMenu(), 80, 460) }, lp(px.d(60), px.d(60)).apply { leftMargin = px.d(14) })
        return row
    }

    /** The team's next match from assets/fixtures.json; no card when config.json names no team. */
    private fun matchCard(): View? {
        val team = UserConfig.get(this).team
        if (team.isEmpty()) return null
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = frost(px, px.f(28f))
            setPadding(px.d(28), px.d(22), px.d(28), px.d(22))
        }
        val list = Fixtures.upcoming(this)
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).look(px, 24, Looks.TEXT3).apply { text = str.nextMatch(team) }, lp(0, WRAP, 1f))
        p.addView(head, lp(MATCH, WRAP))
        if (list.isEmpty()) {
            p.addView(TextView(this).look(px, 28, Looks.TEXT).apply { text = str.noFixtures }, lp(MATCH, WRAP).apply { topMargin = px.d(10) })
            return p
        }
        val m = list[0]
        head.addView(TextView(this).look(px, 22, Looks.ACCENT, 500).apply {
            text = if (m.home) str.home else str.away
            background = rounded(Color.TRANSPARENT, px.f(999f), px.d(2), Looks.ACCENT)
            setPadding(px.d(12), px.d(2), px.d(12), px.d(2))
        })
        p.addView(TextView(this).look(px, 34, Looks.TEXT, 500).apply {
            text = if (m.home) "$team vs ${m.opponent}" else "${m.opponent} vs $team"
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }, lp(MATCH, WRAP).apply { topMargin = px.d(6) })
        p.addView(TextView(this).look(px, 24, Looks.TEXT2).apply {
            text = str.longDate(m.date) + ", " + (m.time ?: str.tbc.lowercase(if (str.en) Locale.ENGLISH else Locale("id")))
            maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        }, lp(MATCH, WRAP).apply { topMargin = px.d(6) })
        return p
    }

    // Dock tiles: 16:9 banners, (1776 - 48 - 96) / 5 = 326.4 design px wide, 184 tall.
    private val tileW get() = px.f(326.4f)
    private val tileH get() = px.d(184)

    private fun dockBand(): View {
        val band = FrameLayout(this).apply {
            background = rounded(Color.argb(18, 255, 255, 255), px.f(44f), max(1, px.d(2)), Color.argb(26, 255, 255, 255))
            noClip()
        }
        highlight = View(this).apply {
            background = rounded(Color.argb(41, 255, 255, 255), px.f(32f), px.d(2), Color.argb(82, 255, 255, 255))
        }
        band.addView(highlight, FrameLayout.LayoutParams((tileW + px.f(20f)).roundToInt(), tileH + px.d(20)).apply {
            leftMargin = px.d(14); topMargin = px.d(14)
        })
        dockRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; noClip() }
        band.addView(dockRow, FrameLayout.LayoutParams(MATCH, tileH).apply {
            leftMargin = px.d(24); rightMargin = px.d(24); topMargin = px.d(24)
        })
        return band
    }

    // ---------- Apps ----------

    private fun loadApps() {
        val pm = packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            for (ri in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || seen.containsKey(pkg)) continue
                // Reuse the entry (and its banner bitmap) when the app is already known.
                seen[pkg] = apps.firstOrNull { it.pkg == pkg } ?: entry(ri)
            }
        }
        apps = seen.values.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    /** One 326x184 banner per app: the app's own TV banner, or its icon centred on its own colour. */
    private fun entry(ri: ResolveInfo): AppEntry {
        val pm = packageManager
        val w = tileW.roundToInt()
        val h = tileH
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val banner = ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
        val icon = ri.loadIcon(pm)
        val color: Int
        if (banner != null) {
            banner.setBounds(0, 0, w, h)
            banner.draw(c)
            color = dominant(bmp)
        } else {
            color = dominant(iconBitmap(icon, 32))
            c.drawColor(darker(color))
            val s = (h * 0.52f).roundToInt()
            icon.setBounds((w - s) / 2, (h - s) / 2, (w + s) / 2, (h + s) / 2)
            icon.draw(c)
        }
        return AppEntry(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), bmp, color)
    }

    private fun iconBitmap(d: Drawable, size: Int): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, size, size)
        d.draw(Canvas(b))
        return b
    }

    /** The app's colour for its glow: an average weighted towards saturated, opaque pixels. */
    private fun dominant(src: Bitmap): Int {
        val b = Bitmap.createScaledBitmap(src, 16, 9, true)
        var r = 0f; var g = 0f; var bl = 0f; var wsum = 0f
        val hsv = FloatArray(3)
        for (y in 0 until b.height) for (x in 0 until b.width) {
            val p = b.getPixel(x, y)
            val a = Color.alpha(p) / 255f
            Color.colorToHSV(p, hsv)
            val wt = a * (0.15f + hsv[1]) * (0.2f + hsv[2])
            r += Color.red(p) * wt; g += Color.green(p) * wt; bl += Color.blue(p) * wt; wsum += wt
        }
        if (b !== src) b.recycle()
        if (wsum <= 0f) return Color.rgb(95, 107, 122)
        return Color.rgb((r / wsum).roundToInt(), (g / wsum).roundToInt(), (bl / wsum).roundToInt())
    }

    private fun darker(c: Int) = Color.rgb((Color.red(c) * 0.55f).roundToInt(), (Color.green(c) * 0.55f).roundToInt(), (Color.blue(c) * 0.55f).roundToInt())

    private fun favoritePkgs(): List<String> {
        val installed = apps.map { it.pkg }.toSet()
        val stored = prefs.getString("fav", null)?.split(",")
        val base = (stored ?: UserConfig.get(this).favorites).filter { it in installed }
        return (base + apps.map { it.pkg }.filter { it !in base }).take(5)
    }

    private fun bannerView(app: AppEntry, radius: Float): ImageView = ImageView(this).apply {
        setImageBitmap(app.banner)
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = rounded(Color.BLACK, radius)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

    private fun fillDock() {
        dockRow.removeAllViews()
        favoritePkgs().forEachIndexed { i, pkg ->
            val app = apps.firstOrNull { it.pkg == pkg } ?: return@forEachIndexed
            val tile = bannerView(app, px.f(22f)).apply {
                colorFilter = DOCK_MUTED
                isFocusable = true
                isClickable = true
                contentDescription = app.label
                setOnClickListener { launch(app.pkg) }
                setOnLongClickListener { pickFavorite(i); true }
                setOnFocusChangeListener { v, has ->
                    (v as ImageView).colorFilter = if (has) null else DOCK_MUTED
                    v.animate().scaleX(if (has) 1.06f else 1f).scaleY(if (has) 1.06f else 1f).setDuration(200).start()
                    if (has) {
                        // The single glass highlight springs to the focused tile (slight overshoot, 320 ms).
                        highlight.animate().translationX(i * (tileW + px.f(24f))).alpha(1f).setDuration(320)
                            .setInterpolator(OvershootInterpolator(1.4f)).start()
                        focusApp(app, 96f + i * (326.4f + 24f) + 163.2f, 844f)
                        setApps(false, animate = true)
                    }
                }
            }
            dockRow.addView(tile, lp(0, MATCH, 1f).apply { if (i > 0) leftMargin = px.d(24) })
        }
    }

    private fun fillGrid() {
        grid.removeAllViews()
        gridLabels.clear()
        apps.chunked(6).forEachIndexed { r, chunk ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; noClip() }
            for (col in 0 until 6) {
                val app = chunk.getOrNull(col)
                val cellLp = lp(0, WRAP, 1f).apply { if (col > 0) leftMargin = px.d(24) }
                if (app == null) { row.addView(Space(this), cellLp); continue }
                val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; noClip() }
                val ring = GradientDrawable().apply { cornerRadius = px.f(22f); setColor(Color.TRANSPARENT) }
                val tileBox = FrameLayout(this).apply { foreground = ring }
                val img = bannerView(app, px.f(22f)).apply { colorFilter = GRID_MUTED }
                tileBox.addView(img, FrameLayout.LayoutParams(MATCH, MATCH))
                val label = TextView(this).look(px, 24, Looks.TEXT3).apply {
                    text = app.label; gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                    alpha = if (inApps) 1f else 0f
                }
                gridLabels += label
                tileBox.apply {
                    isFocusable = true
                    isClickable = true
                    contentDescription = app.label
                    setOnClickListener { launch(app.pkg) }
                    setOnLongClickListener { pickSlotFor(app); true }
                    setOnFocusChangeListener { v, has ->
                        img.colorFilter = if (has) null else GRID_MUTED
                        ring.setStroke(if (has) px.d(4) else 0, Color.argb(217, 255, 255, 255))
                        label.setTextColor(if (has) Looks.TEXT else Looks.TEXT3)
                        v.animate().scaleX(if (has) 1.06f else 1f).scaleY(if (has) 1.06f else 1f).setDuration(200).start()
                        if (has) {
                            highlight.animate().alpha(0f).setDuration(160).start()
                            focusApp(app, 96f + col * 292f + 134f, 996f + r * 218f + 75f)
                            setApps(true, animate = true, row = r)
                        }
                    }
                }
                cell.addView(tileBox, lp(MATCH, px.d(150)))
                cell.addView(label, lp(MATCH, WRAP).apply { topMargin = px.d(10) })
                row.addView(cell, cellLp)
            }
            grid.addView(row, lp(MATCH, WRAP).apply { if (r > 0) topMargin = px.d(28) })
        }
    }

    /** Name in large type with a glow in the app's colour, and the colour glow moved behind the focused tile (design px centre). */
    private fun focusApp(app: AppEntry, cx: Float, cy: Float) {
        nameTv.text = app.label
        nameTv.setShadowLayer(px.f(48f), 0f, 0f, withAlpha(app.color, 140))
        val first = glow.color == Color.TRANSPARENT
        glow.color = app.color
        val tx = px.f(cx - GLOW_W / 2f)
        val ty = px.f(cy - GLOW_H / 2f)
        if (first) {
            glow.translationX = tx; glow.translationY = ty
        } else {
            glow.animate().translationX(tx).translationY(ty).setDuration(320).setInterpolator(OvershootInterpolator(1.2f)).start()
        }
    }

    /** Home shows the dock with the grid peeking below; "Semua aplikasi" scrolls the page up to the grid. */
    private fun setApps(on: Boolean, animate: Boolean, row: Int = 0) {
        val y = if (on) -px.f(560f) - max(0, row - 1) * px.f(218f) else 0f
        if (animate) page.animate().translationY(y).setDuration(420).setInterpolator(DecelerateInterpolator(2f)).start() else page.translationY = y
        if (on != inApps || !animate) {
            inApps = on
            for (l in gridLabels) l.animate().alpha(if (on) 1f else 0f).setDuration(if (animate) 200 else 0).start()
            // On Home the first grid row only peeks, dimmed, as in the mockup.
            grid.animate().alpha(if (on) 1f else 0.45f).setDuration(if (animate) 200 else 0).start()
        }
    }

    private fun pickFavorite(slot: Int) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(str.pickApp)
            .setItems(apps.map { it.label }.toTypedArray()) { _, which -> setFavorite(slot, apps[which].pkg) }
            .show()
    }

    private fun pickSlotFor(app: AppEntry) {
        val favs = favoritePkgs()
        val labels = favs.mapIndexed { i, pkg -> "${i + 1}. " + (apps.firstOrNull { it.pkg == pkg }?.label ?: pkg) }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(str.pickSlot)
            .setItems(labels.toTypedArray()) { _, which -> setFavorite(which, app.pkg) }
            .show()
    }

    private fun setFavorite(slot: Int, pkg: String) {
        val cur = favoritePkgs().toMutableList()
        cur.remove(pkg)
        while (cur.size <= slot) cur.add("")
        cur.add(slot, pkg)
        prefs.edit().putString("fav", cur.filter { it.isNotEmpty() }.take(5).joinToString(",")).apply()
        fillDock()
        dockRow.getChildAt(minOf(slot, dockRow.childCount - 1))?.requestFocus()
    }

    private fun launch(pkg: String) {
        val pm = packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return
        start(intent)
    }

    private fun start(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // The TV refused this intent; stay on the home screen.
        }
    }

    // ---------- Greeting and clock ----------

    private fun showGreeting(animate: Boolean) {
        val list = str.greetings(phase)
        val i = greetIndex % list.size
        val names = UserConfig.get(this).names
        greetTv.text = if (names.isEmpty()) list[i] else "${list[i]}, $names"
        val wave = i in 1..3
        val show = if (wave) avWave else avNormal
        val hide = if (wave) avNormal else avWave
        if (animate) {
            show.animate().alpha(1f).setDuration(200).start()
            hide.animate().alpha(0f).setDuration(200).start()
        } else {
            show.alpha = 1f; hide.alpha = 0f
        }
    }

    private fun updateClock() {
        if (released) return
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        if (phaseOf(h) != phase) {
            recreate()
            return
        }
        clockTv.text = String.format(Locale.US, "%02d:%02d", h, cal.get(Calendar.MINUTE))
        dateTv.text = str.longDate(cal)
    }

    // ---------- Dropdowns under the pills (Input, Wi-Fi, Settings) ----------

    private fun menuPanel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Looks.MENU, px.f(28f), max(1, px.d(2)), Looks.FROST_EDGE)
        setPadding(px.d(12), px.d(12), px.d(12), px.d(12))
        isClickable = true
    }

    private fun menuRow(label: String, right: String? = null, rightColor: Int = Looks.TEXT3, onClick: () -> Unit): View {
        val bg = rounded(Color.TRANSPARENT, px.f(18f))
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px.d(20), 0, px.d(20), 0)
            background = bg
            isFocusable = true
            isClickable = true
            addView(TextView(this@MainActivity).look(px, 26, Looks.TEXT, 500).apply { text = label; maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, lp(0, WRAP, 1f))
            if (right != null) addView(TextView(this@MainActivity).look(px, 24, rightColor).apply { text = right })
            setOnFocusChangeListener { _, has -> bg.setColor(if (has) Looks.ROW_ON else Color.TRANSPARENT) }
            setOnClickListener { onClick() }
        }
    }

    private fun inputMenu(): View {
        val p = menuPanel()
        tvInputs().forEach { (label, action) -> p.addView(menuRow(label) { closeMenu(); action() }, lp(MATCH, px.d(64))) }
        return p
    }

    private fun wifiMenu(): View {
        val p = menuPanel()
        val (label, ip, online) = networkInfo()
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px.d(12), px.d(8), px.d(12), px.d(12)) }
        info.addView(TextView(this).look(px, 30, Looks.TEXT, 500).apply { text = label })
        info.addView(TextView(this).look(px, 24, Looks.TEXT3).apply { text = if (online) str.connected else str.offline }, lp(WRAP, WRAP).apply { topMargin = px.d(4) })
        if (ip != null) info.addView(TextView(this).look(px, 24, Looks.TEXT3).apply { text = "IP $ip" }, lp(WRAP, WRAP).apply { topMargin = px.d(2) })
        p.addView(info)
        p.addView(menuRow(str.wifiSettings, "›") { closeMenu(); start(Intent(Settings.ACTION_WIFI_SETTINGS)) }, lp(MATCH, px.d(64)))
        return p
    }

    private fun settingsMenu(): View {
        val p = menuPanel()
        p.addView(TextView(this).look(px, 24, Looks.TEXT3).apply { text = str.language; setPadding(px.d(8), px.d(8), 0, px.d(4)) })
        val langs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("id" to "Indonesia", "en" to "English").forEachIndexed { i, (code, name) ->
            val active = (code == "en") == str.en
            val bg = rounded(if (active) Color.argb(56, 255, 255, 255) else Color.argb(15, 255, 255, 255), px.f(16f), px.d(2), if (active) Looks.ACCENT else Color.TRANSPARENT)
            val b = TextView(this).look(px, 26, Looks.TEXT, 500).apply {
                text = name
                gravity = Gravity.CENTER
                background = bg
                isFocusable = true
                isClickable = true
                setOnFocusChangeListener { _, has -> bg.setStroke(px.d(2), if (has || active) Looks.ACCENT else Color.TRANSPARENT) }
                setOnClickListener {
                    prefs.edit().putString("lang", code).apply()
                    recreate()
                }
            }
            langs.addView(b, lp(0, px.d(60), 1f).apply { if (i > 0) leftMargin = px.d(10) })
        }
        p.addView(langs, lp(MATCH, WRAP).apply { bottomMargin = px.d(8) })
        p.addView(menuRow(str.screensaver, str.ambientName) { closeMenu(); start(Intent(Settings.ACTION_DREAM_SETTINGS)) }, lp(MATCH, px.d(64)))
        p.addView(menuRow(str.allSettings, "›") { closeMenu(); start(Intent(Settings.ACTION_SETTINGS)) }, lp(MATCH, px.d(64)))
        return p
    }

    private fun openMenu(from: View, panel: View, right: Int, width: Int) {
        menuReturn = from
        menuLayer.removeAllViews()
        menuLayer.addView(panel, FrameLayout.LayoutParams(px.d(width), WRAP, Gravity.END).apply { topMargin = px.d(140); rightMargin = px.d(right) })
        menuLayer.visibility = View.VISIBLE
        page.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        panel.post { firstFocusable(panel, panel)?.requestFocus() }
    }

    private fun firstFocusable(v: View, root: View): View? {
        if (v !== root && v.isFocusable) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) firstFocusable(v.getChildAt(i), root)?.let { return it }
        return null
    }

    private fun closeMenu() {
        if (menuLayer.visibility != View.VISIBLE) return
        menuLayer.visibility = View.GONE
        menuLayer.removeAllViews()
        page.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        (menuReturn ?: dockRow.getChildAt(0))?.requestFocus()
    }

    private fun networkInfo(): Triple<String, String?, Boolean> {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val net = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            val label = if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) "Ethernet" else "Wi-Fi"
            val ip = cm.getLinkProperties(net)?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address }?.hostAddress
            Triple(label, ip, net != null)
        } catch (e: Exception) {
            Triple("Wi-Fi", null, false)
        }
    }

    /** HDMI and other passthrough inputs reported by the TV, plus the tuner app. */
    private fun tvInputs(): List<Pair<String, () -> Unit>> {
        val out = mutableListOf<Pair<String, () -> Unit>>()
        packageManager.getLeanbackLaunchIntentForPackage(TUNER_PKG)?.let { intent ->
            out += str.tuner to { start(intent) }
        }
        try {
            val tim = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            tim.tvInputList.filter { it.isPassthroughInput && !it.isHidden(this) }.forEach { info ->
                val uri: Uri = TvContract.buildChannelUriForPassthroughInput(info.id)
                out += info.loadLabel(this).toString() to { start(Intent(Intent.ACTION_VIEW, uri)) }
            }
        } catch (e: Exception) {
            // No TV input service on this device.
        }
        return out.take(6)
    }

    // ---------- Keys ----------

    // Back is taken here, before any view sees it: it closes a dropdown or leaves the app grid,
    // and the home screen itself never closes.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && !released) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
                when {
                    menuLayer.visibility == View.VISIBLE -> closeMenu()
                    inApps -> {
                        setApps(false, animate = true)
                        dockRow.getChildAt(0)?.requestFocus()
                    }
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Back is handled in dispatchKeyEvent.")
    override fun onBackPressed() {
        // Never leave Home.
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (menuLayer.visibility == View.VISIBLE) closeMenu() else openMenu(dockRow.getChildAt(0) ?: page, settingsMenu(), 80, 460)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Pressing Home while already home returns to the first dock tile.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (released) return
        closeMenu()
        dockRow.getChildAt(0)?.requestFocus()
    }

    /**
     * The glow in the focused app's colour (the repo's "ambient glow", radial), sitting behind the tile.
     * It is redrawn only when the colour changes; moving it to another tile is just a translation.
     */
    private class GlowView(ctx: Context, private val px: Px) : View(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var color: Int = Color.TRANSPARENT
            set(value) {
                if (field == value) return
                field = value
                paint.shader = RadialGradient(0f, 0f, px.f(GLOW_W / 2f), withAlpha(value, 107), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                invalidate()
            }

        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

        override fun onDraw(canvas: Canvas) {
            if (paint.shader == null) return
            canvas.save()
            canvas.translate(width / 2f, height / 2f)
            canvas.scale(1f, GLOW_H.toFloat() / GLOW_W)
            canvas.drawCircle(0f, 0f, px.f(GLOW_W / 2f), paint)
            canvas.restore()
        }
    }

    companion object {
        private const val TUNER_PKG = "com.mediatek.wwtv.tvcenter"
        private const val PAGE_H = 2400
        private const val GLOW_W = 900
        private const val GLOW_H = 420

        private fun muted(sat: Float, bright: Float) = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(sat)
            postConcat(ColorMatrix(floatArrayOf(
                bright, 0f, 0f, 0f, 0f,
                0f, bright, 0f, 0f, 0f,
                0f, 0f, bright, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        })

        /** Unfocused tiles stay calm, as in the mockup: dock 75% / 80%, grid 70% / 75%. */
        private val DOCK_MUTED = muted(0.75f, 0.8f)
        private val GRID_MUTED = muted(0.7f, 0.75f)
    }
}
