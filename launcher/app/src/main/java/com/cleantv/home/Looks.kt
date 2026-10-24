package com.cleantv.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

enum class Phase { PAGI, SIANG, SORE, MALAM }

fun phaseOf(hour: Int): Phase = when {
    hour < 11 -> Phase.PAGI
    hour < 15 -> Phase.SIANG
    hour < 18 -> Phase.SORE
    else -> Phase.MALAM
}

/** Beach colours; sunY/sunR are for the home band, ambientSunY for the idle screen. */
class Scene(
    val skyTop: Int, val skyLow: Int, val sun: Int, val sea: Int, val sheen: Int, val sand: Int, val stars: Boolean,
    val sunY: Float, val sunR: Float, val ambientSunY: Float, val ambientSunR: Float
)

/** Aurora colours: the night-sky ground and the two ribbon hues. */
class Aurora(val base: Int, val a: Int, val b: Int)

fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

private fun c(hex: String) = Color.parseColor(hex)

/** Colours from the approved Mockup 2 (Apple TV style, black ground, beach + aurora). */
object Looks {
    val ACCENT = c("#FF5A4F")
    val TEXT = c("#F5F5F7")
    val TEXT2 = c("#D1D1D6")
    val TEXT3 = c("#AEAEB2")
    val FROST = Color.argb(26, 255, 255, 255)
    val FROST_ON = Color.argb(66, 255, 255, 255)
    val FROST_EDGE = Color.argb(36, 255, 255, 255)
    val MENU = Color.argb(240, 28, 28, 30)
    val ROW_ON = Color.argb(46, 255, 255, 255)

    val scenes = mapOf(
        Phase.PAGI to Scene(c("#6FA8DC"), c("#FBD3B0"), c("#FFE3A3"), c("#5D9CC0"), c("#FFE3C2"), c("#EED3A6"), false, 250f, 52f, 560f, 56f),
        Phase.SIANG to Scene(c("#2F8FE0"), c("#9FD6F5"), c("#FFF7DC"), c("#1E7FB8"), c("#CFEFFF"), c("#F1DCAE"), false, 120f, 44f, 170f, 48f),
        Phase.SORE to Scene(c("#4B3F7A"), c("#F28C5A"), c("#FFB067"), c("#5B4A78"), c("#F6A06A"), c("#D6A272"), false, 380f, 64f, 680f, 72f),
        Phase.MALAM to Scene(c("#070C1F"), c("#1C2A55"), c("#E9ECF5"), c("#101C3D"), c("#AEB8D6"), c("#34364E"), true, 150f, 40f, 210f, 44f)
    )

    val auroras = mapOf(
        Phase.PAGI to Aurora(c("#0B1424"), c("#FFB38A"), c("#7CC4FF")),
        Phase.SIANG to Aurora(c("#061626"), c("#3FD0FF"), c("#2BD9A8")),
        Phase.SORE to Aurora(c("#120A1E"), c("#FF8A4C"), c("#9B6BFF")),
        Phase.MALAM to Aurora(c("#070A14"), c("#2EE6C5"), c("#7B5CFF"))
    )
}

/** The design is drawn on a 1920x1080 canvas; d() scales design px to the real screen. */
class Px(context: Context) {
    val s: Float = context.resources.displayMetrics.let { maxOf(it.widthPixels, it.heightPixels) / 1920f }
    fun d(v: Int): Int = (v * s).roundToInt()
    fun f(v: Float): Float = v * s
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

fun rounded(color: Int, radius: Float, strokePx: Int = 0, strokeColor: Int = Color.TRANSPARENT) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokePx > 0) setStroke(strokePx, strokeColor)
    }

/** Frosted surface: translucent white with a light rim, as the mockup's pills and cards. */
fun frost(px: Px, radius: Float, fill: Int = Looks.FROST, edge: Int = Looks.FROST_EDGE) =
    rounded(fill, radius, maxOf(1, px.d(2)), edge)

/** Weights follow the mockup's Outfit scale using the system faces: 200/300 light, 400 regular, 500+ medium. */
fun TextView.look(px: Px, size: Int, color: Int, weight: Int = 400): TextView {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, px.f(size.toFloat()))
    setTextColor(color)
    includeFontPadding = false
    typeface = when {
        weight >= 700 -> Typeface.create("sans-serif", Typeface.BOLD)
        weight >= 500 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        weight >= 400 -> Typeface.create("sans-serif", Typeface.NORMAL)
        else -> Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    return this
}

fun ViewGroup.noClip(): ViewGroup {
    clipChildren = false
    clipToPadding = false
    return this
}

fun keycap(ctx: Context, px: Px, text: String): TextView =
    TextView(ctx).look(px, 24, Looks.TEXT, 500).apply {
        this.text = text
        background = rounded(Color.TRANSPARENT, px.f(10f), px.d(2), Color.argb(56, 255, 255, 255))
        setPadding(px.d(14), px.d(2), px.d(14), px.d(2))
    }
