package com.cleantv.home

import java.util.Calendar

/** Indonesian and English copy; switched live from the quick settings panel. */
class Strings(val en: Boolean) {
    val hdmi = if (en) "HDMI input" else "Input HDMI"
    val settings = if (en) "Settings" else "Pengaturan"
    val favorites = if (en) "Favorites" else "Favorit"
    val allApps = if (en) "All apps" else "Semua aplikasi"
    val more = if (en) "More" else "Lainnya"
    val appsWord = if (en) "apps" else "aplikasi"
    fun nextMatch(team: String) = if (en) "$team, next match" else "$team, laga berikutnya"
    val ambientName = if (en) "Clean Home ambient" else "Ambient Clean Home"
    val home = if (en) "Home" else "Kandang"
    val away = if (en) "Away" else "Tandang"
    val tbc = if (en) "Kick-off time to be confirmed" else "Jam tayang menyusul"
    val noFixtures = if (en) "No upcoming matches yet" else "Belum ada jadwal berikutnya"
    val open = if (en) "Open" else "Buka"
    val hold = if (en) "Hold OK" else "Tahan OK"
    val editFav = if (en) "Edit favorites" else "Atur favorit"
    val quickTitle = if (en) "Quick settings" else "Pengaturan cepat"
    val connected = if (en) "Connected" else "Terhubung"
    val offline = if (en) "Not connected" else "Tidak terhubung"
    val input = "Input"
    val tuner = if (en) "TV (antenna)" else "TV (antena)"
    val language = if (en) "Language" else "Bahasa"
    val active = if (en) "Active" else "Aktif"
    val allSettings = if (en) "All Android settings" else "Semua setelan Android"
    val back = if (en) "Back" else "Kembali"
    val close = if (en) "Close panel" else "Tutup panel"
    val backHome = if (en) "Back to home" else "Kembali ke beranda"
    val pickApp = if (en) "Choose an app for this slot" else "Pilih aplikasi untuk slot ini"
    val pickSlot = if (en) "Replace which favorite?" else "Ganti favorit yang mana?"
    val inputs = if (en) "Inputs" else "Input"
    val down = if (en) "Down" else "Bawah"
    val up = if (en) "Up" else "Atas"
    val wifiSettings = if (en) "Wi-Fi settings" else "Setelan Wi-Fi"
    val screensaver = "Screensaver"

    private val greet = if (en) listOf("Good morning", "Good afternoon", "Good afternoon", "Good evening")
    else listOf("Selamat pagi", "Selamat siang", "Selamat sore", "Selamat malam")
    private val pool = if (en) listOf("Hi", "Hello", "Welcome back", "How are you")
    else listOf("Hi", "Halo", "Welcome back", "Apa kabar")

    /** Index 0 is the time-of-day greeting; 1..3 use the waving avatar. */
    fun greetings(phase: Phase): List<String> = listOf(greet[phase.ordinal]) + pool

    private val days = if (en) listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    else listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    private val daysShort = if (en) listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    else listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    private val months = if (en) listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    else listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val monthsShort = if (en) listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    else listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    fun longDate(c: Calendar) = "${days[c.get(Calendar.DAY_OF_WEEK) - 1]}, ${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)]}"
    fun shortDate(c: Calendar) = "${daysShort[c.get(Calendar.DAY_OF_WEEK) - 1]} ${c.get(Calendar.DAY_OF_MONTH)} ${monthsShort[c.get(Calendar.MONTH)]}"
}
