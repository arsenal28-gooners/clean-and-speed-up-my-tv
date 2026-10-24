package com.cleantv.home

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

class Fixture(val opponent: String, val home: Boolean, val date: Calendar, val time: String?)

/** The team's schedule from assets/fixtures.json (team name in config.json; an API can replace this later). */
object Fixtures {
    fun upcoming(ctx: Context): List<Fixture> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return try {
            val text = ctx.assets.open("fixtures.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(text).getJSONArray("matches")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val (y, m, d) = o.getString("date").split("-").map { it.toInt() }
                val cal = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }
                Fixture(o.getString("opponent"), o.getBoolean("home"), cal, if (o.isNull("time")) null else o.getString("time"))
            }.filter { !it.date.before(today) }.sortedBy { it.date.timeInMillis }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
