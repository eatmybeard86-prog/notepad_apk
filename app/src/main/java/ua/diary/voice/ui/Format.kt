package ua.diary.voice.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val uk = Locale("uk", "UA")
private val longFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", uk)
private val dayFmt = DateTimeFormatter.ofPattern("EEEE", uk)

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(longFmt)

fun formatWeekday(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (d) {
        today -> "Сьогодні"
        today.minusDays(1) -> "Вчора"
        else -> d.format(dayFmt).replaceFirstChar { it.titlecase(uk) }
    }
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

fun formatWeekdayPlain(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFmt)
