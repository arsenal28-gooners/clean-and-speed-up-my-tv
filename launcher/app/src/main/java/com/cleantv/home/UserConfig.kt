package com.cleantv.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

/**
 * Everything personal lives outside the code:
 * - assets/config.json: the names in the greeting, the monogram, the team on the match card, the default dock apps;
 * - assets/custom/ (avatar_normal, avatar_wave, characters as .webp, .png or .jpg): your own avatar in two poses
 *   and the cut-out characters.
 * Missing files are fine: the greeting drops the names, the avatar shows the monogram, the characters are left out.
 */
object UserConfig {

    class Values(
        val names: String,
        val monogram: String,
        val team: String,
        val favorites: List<String>
    )

    private var cached: Values? = null

    fun get(ctx: Context): Values = cached ?: load(ctx).also { cached = it }

    private fun load(ctx: Context): Values {
        val o = try {
            JSONObject(ctx.assets.open("config.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject()
        }
        val fav = o.optJSONArray("dock_apps")
        return Values(
            names = o.optString("names", "").trim(),
            monogram = o.optString("monogram", "").trim().ifEmpty { "TV" },
            team = o.optString("team", "").trim(),
            favorites = if (fav != null) (0 until fav.length()).map { fav.getString(it) } else DEFAULT_DOCK
        )
    }

    /** One of the user's images from assets/custom/<name>.(webp|png|jpg), decoded close to the size it is shown. */
    fun image(ctx: Context, name: String, reqW: Int, reqH: Int): Bitmap? {
        for (ext in listOf("webp", "png", "jpg")) {
            val path = "custom/$name.$ext"
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0) continue
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= reqW && bounds.outHeight / (sample * 2) >= reqH) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                return ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
            } catch (e: Exception) {
                // Not this extension; try the next one.
            }
        }
        return null
    }

    private val DEFAULT_DOCK = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.spotify.tv.android",
        "com.android.vending"
    )
}
