package ua.diary.voice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val uk = Locale("uk", "UA")
private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")

/**
 * Місячний календар. [marks] — для кожного дня (epochDay) кольори крапок (розділи записів).
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    marks: Map<Long, List<Int>>,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Попередній місяць")
            }
            val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, uk)
                .replaceFirstChar { it.titlecase(uk) }
            Text(
                "$name ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable {
                    onMonthChange(YearMonth.from(today)); onSelect(today)
                },
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Наступний місяць")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1 // понеділок = 0
        val days = month.lengthOfMonth()
        val rows = (offset + days + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                isSelected = date == selected,
                                isToday = date == today,
                                dots = marks[date.toEpochDay()].orEmpty(),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isSelected: Boolean, isToday: Boolean, dots: List<Int>, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var m = Modifier.size(42.dp).clip(CircleShape)
    if (isSelected) m = m.background(scheme.primary)
    else if (isToday) m = m.border(1.5.dp, scheme.primary, CircleShape)
    Column(
        m.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) scheme.onPrimary else scheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            dots.take(3).forEach { c ->
                Box(
                    Modifier.size(5.dp).clip(CircleShape)
                        .background(if (isSelected) scheme.onPrimary else Color(c))
                )
            }
            if (dots.isEmpty()) Box(Modifier.size(5.dp))
        }
    }
}
