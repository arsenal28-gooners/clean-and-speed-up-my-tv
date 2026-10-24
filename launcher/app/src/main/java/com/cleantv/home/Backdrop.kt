package com.cleantv.home

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The approved "kombinasi" backdrop, painted once per time of day as two layers:
 * - aurora(): one continuous aurora for the whole page, quarter size (it is soft anyway),
 *   so there is no seam when "Semua aplikasi" scrolls up;
 * - band(): the beach band, half size, transparent where it fades into the aurora
 *   (at its bottom, and on the left behind the clock).
 * Nothing here runs per frame.
 */
object Backdrop {

    // Aurora ribbons over the whole page (design px): left, top, width, height, rotation, hue b?, opacity.
    // None sit in the dock band (728-960), so the only light behind a dock tile is the focused app's glow.
    private val RIBBONS = arrayOf(
        floatArrayOf(-200f, 930f, 1200f, 220f, -6f, 1f, 0.40f),
        floatArrayOf(700f, 960f, 1200f, 200f, -4f, 0f, 0.34f),
        floatArrayOf(1350f, 920f, 800f, 200f, 8f, 1f, 0.30f),
        floatArrayOf(-100f, 1140f, 1300f, 300f, -8f, 0f, 0.32f),
        floatArrayOf(1000f, 1300f, 1000f, 260f, 6f, 1f, 0.28f),
        floatArrayOf(300f, 1700f, 1400f, 320f, -5f, 1f, 0.26f),
        floatArrayOf(1200f, 1940f, 900f, 260f, 10f, 0f, 0.24f)
    )

    /** Quarter size: 480 x 600 for a 2400-design-px page at 1080p, about 1.1 MB. */
    fun aurora(phase: Phase, screenW: Int, pageHDesign: Int): Bitmap {
        val bw = max(1, screenW / 4)
        val s = bw / 1920f
        val bh = max(1, (pageHDesign * s).roundToInt())
        val au = Looks.auroras.getValue(phase)
        val out = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // Ground: base at the top, black under the dock, base again behind the grid, black at the very end.
        p.shader = LinearGradient(0f, 0f, 0f, bh.toFloat(), intArrayOf(au.base, Color.BLACK, au.base, Color.BLACK),
            floatArrayOf(0f, 0.37f, 0.65f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, bw.toFloat(), bh.toFloat(), p)
        p.shader = null
        for (r in RIBBONS) ribbon(cv, p, s, r, if (r[5] > 0f) au.b else au.a)
        boxBlur(out, max(1, (28f * s).roundToInt()))
        return out
    }

    /** Half size: 960 x 350 at 1080p, about 1.3 MB; transparent where the aurora shows through. */
    fun band(phase: Phase, screenW: Int): Bitmap {
        val bw = max(1, screenW / 2)
        val s = bw / 1920f
        val bandH = max(1, (700 * s).roundToInt())
        val band = Bitmap.createBitmap(bw, bandH, Bitmap.Config.ARGB_8888)
        val bc = Canvas(band)
        beachBand(bc, s, phase)
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        mask.shader = LinearGradient(0f, 0f, 0f, bandH.toFloat(), intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
        bc.drawRect(0f, 0f, bw.toFloat(), bandH.toFloat(), mask)
        mask.shader = LinearGradient(0f, 0f, bw.toFloat(), 0f,
            intArrayOf(Color.TRANSPARENT, withAlpha(Color.BLACK, 20), withAlpha(Color.BLACK, 166), Color.BLACK),
            floatArrayOf(0f, 0.28f, 0.48f, 0.62f), Shader.TileMode.CLAMP)
        bc.drawRect(0f, 0f, bw.toFloat(), bandH.toFloat(), mask)
        return band
    }

    /**
     * Idle screen beach, full width: opaque to 40%, then easing to 45% so it stays faintly
     * visible behind the clock. The long dark fade on top is [ambientShade].
     */
    fun ambient(phase: Phase, screenW: Int, screenH: Int): Bitmap {
        val bw = max(1, screenW / 2)
        val bh = max(1, screenH / 2)
        val s = bw / 1920f
        val sc = Looks.scenes.getValue(phase)
        val out = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        cv.drawColor(Color.BLACK)
        val pw = bw
        val panel = Bitmap.createBitmap(max(1, pw), bh, Bitmap.Config.ARGB_8888)
        val pc = Canvas(panel)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, 800 * s, sc.skyTop, sc.skyLow, Shader.TileMode.CLAMP)
        pc.drawRect(0f, 0f, pw.toFloat(), 800 * s, p)
        p.shader = null
        if (sc.stars) {
            p.color = Color.WHITE; p.alpha = 230
            val st = floatArrayOf(160f, 90f, 420f, 200f, 700f, 80f, 880f, 320f, 300f, 380f)
            var i = 0
            while (i < st.size) { pc.drawCircle(st[i] * s, st[i + 1] * s, max(1f, 2 * s), p); i += 2 }
        }
        sun(pc, p, 760f * s, sc.ambientSunY * s, sc.ambientSunR * s, sc.sun)
        p.color = sc.sea; p.alpha = 255
        pc.drawRect(0f, 800 * s, pw.toFloat(), bh.toFloat(), p)
        p.color = sc.sheen; p.alpha = 128
        pc.drawRect(0f, 812 * s, pw.toFloat(), 816 * s, p)
        p.alpha = 153
        pc.drawOval(RectF(620 * s, 823 * s, 900 * s, 837 * s), p)
        p.color = sc.sand; p.alpha = 255
        pc.drawPath(Path().apply {
            moveTo(0f, 920 * s); cubicTo(300 * s, 890 * s, 650 * s, 950 * s, 1100 * s, 924 * s)
            cubicTo(1450 * s, 908 * s, 1700 * s, 940 * s, 1920 * s, 926 * s)
            lineTo(pw.toFloat(), bh.toFloat()); lineTo(0f, bh.toFloat()); close()
        }, p)
        p.color = Color.BLACK; p.alpha = 56
        pc.drawRect(0f, 0f, pw.toFloat(), bh.toFloat(), p)
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(0f, 0f, pw.toFloat(), 0f,
                intArrayOf(Color.BLACK, Color.BLACK, withAlpha(Color.BLACK, 179), withAlpha(Color.BLACK, 128), withAlpha(Color.BLACK, 115)),
                floatArrayOf(0f, 0.4f, 0.55f, 0.7f, 1f), Shader.TileMode.CLAMP)
        }
        pc.drawRect(0f, 0f, pw.toFloat(), bh.toFloat(), mask)
        cv.drawBitmap(panel, 0f, 0f, null)
        panel.recycle()
        return out
    }

    /**
     * The approved long dark fade over beach and characters: black at 0% to x 600, 34% at the middle,
     * 60% at x 1230 and 80% from x 1380. A 192 x 1 strip, stretched across the screen.
     */
    fun ambientShade(): Bitmap {
        val out = Bitmap.createBitmap(192, 1, Bitmap.Config.ARGB_8888)
        Canvas(out).drawRect(0f, 0f, 192f, 1f, Paint().apply {
            shader = LinearGradient(0f, 0f, 192f, 0f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(Color.BLACK, 87), withAlpha(Color.BLACK, 153),
                    withAlpha(Color.BLACK, 204), withAlpha(Color.BLACK, 204)),
                floatArrayOf(0f, 0.31f, 0.5f, 0.64f, 0.72f, 1f), Shader.TileMode.CLAMP)
        })
        return out
    }

    /** The user's cut-out characters with their lower part fading out, baked once (mockup: opaque to 58%, gone by 98%). */
    fun fadedCharacters(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createScaledBitmap(src, max(1, w), max(1, h), true)
        if (out !== src) src.recycle()
        val m = if (out.isMutable) out else out.copy(Bitmap.Config.ARGB_8888, true).also { out.recycle() }
        Canvas(m).drawRect(0f, 0f, m.width.toFloat(), m.height.toFloat(), Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(0f, 0f, 0f, m.height.toFloat(), intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                floatArrayOf(0f, 0.58f, 0.98f), Shader.TileMode.CLAMP)
        })
        return m
    }

    private fun beachBand(cv: Canvas, s: Float, phase: Phase) {
        val sc = Looks.scenes.getValue(phase)
        val w = 1920 * s
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, 500 * s, sc.skyTop, sc.skyLow, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, 500 * s, p)
        p.shader = null
        if (sc.stars) {
            p.color = Color.WHITE; p.alpha = 230
            val st = floatArrayOf(980f, 70f, 1240f, 40f, 1420f, 160f, 1700f, 60f, 1860f, 230f, 1080f, 300f)
            var i = 0
            while (i < st.size) { cv.drawCircle(st[i] * s, st[i + 1] * s, max(1f, 2 * s), p); i += 2 }
        }
        sun(cv, p, 1180 * s, sc.sunY * s, sc.sunR * s, sc.sun)
        p.color = sc.sea; p.alpha = 255
        cv.drawRect(0f, 500 * s, w, 700 * s, p)
        p.color = sc.sheen; p.alpha = 128
        cv.drawRect(0f, 510 * s, w, 513 * s, p)
        p.alpha = 153
        cv.drawOval(RectF(1050 * s, 520 * s, 1310 * s, 532 * s), p)
        p.color = sc.sand; p.alpha = 255
        cv.drawPath(Path().apply {
            moveTo(0f, 590 * s)
            cubicTo(400 * s, 565 * s, 800 * s, 610 * s, 1200 * s, 596 * s)
            cubicTo(1500 * s, 585 * s, 1750 * s, 604 * s, w, 592 * s)
            lineTo(w, 700 * s); lineTo(0f, 700 * s); close()
        }, p)
        p.color = Color.BLACK; p.alpha = 56
        cv.drawRect(0f, 0f, w, 700 * s, p)
    }

    private fun sun(cv: Canvas, p: Paint, x: Float, y: Float, r: Float, color: Int) {
        p.color = color; p.alpha = 56
        cv.drawCircle(x, y, r * 2.2f, p)
        p.alpha = 255
        cv.drawCircle(x, y, r, p)
    }

    private fun ribbon(cv: Canvas, p: Paint, s: Float, r: FloatArray, color: Int) {
        val cx = (r[0] + r[2] / 2) * s
        val cy = (r[1] + r[3] / 2) * s
        val rx = r[2] / 2 * s
        val ry = r[3] / 2 * s
        cv.save()
        cv.translate(cx, cy)
        cv.rotate(r[4])
        cv.scale(1f, ry / rx)
        p.shader = RadialGradient(0f, 0f, rx, withAlpha(color, (r[6] * 255).roundToInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawCircle(0f, 0f, rx, p)
        p.shader = null
        cv.restore()
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    /** Three box-blur passes each way approximate a gaussian; edges are clamped. Works on opaque bitmaps. */
    private fun boxBlur(bmp: Bitmap, r: Int) {
        val w = bmp.width
        val h = bmp.height
        val a = IntArray(w * h)
        val b = IntArray(w * h)
        bmp.getPixels(a, 0, w, 0, 0, w, h)
        repeat(3) {
            pass(a, b, w, h, r, horizontal = true)
            pass(b, a, w, h, r, horizontal = false)
        }
        bmp.setPixels(a, 0, w, 0, 0, w, h)
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val div = 2 * r + 1
        for (line in 0 until lines) {
            var sr = 0; var sg = 0; var sb = 0
            fun at(i: Int): Int {
                val k = i.coerceIn(0, len - 1)
                return if (horizontal) src[line * w + k] else src[k * w + line]
            }
            for (i in -r..r) {
                val px = at(i)
                sr += (px shr 16) and 255; sg += (px shr 8) and 255; sb += px and 255
            }
            for (i in 0 until len) {
                val o = if (horizontal) line * w + i else i * w + line
                dst[o] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = at(i + r + 1)
                val sub = at(i - r)
                sr += ((add shr 16) and 255) - ((sub shr 16) and 255)
                sg += ((add shr 8) and 255) - ((sub shr 8) and 255)
                sb += (add and 255) - (sub and 255)
            }
        }
    }
}
