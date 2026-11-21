package com.krishiyan.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.krishiyan.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Local days touched by any busy range. */
fun busyDays(ranges: List<BusyRange>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> {
    val days = mutableSetOf<LocalDate>()
    for (range in ranges) {
        var day = range.start.atZone(zone).toLocalDate()
        val last = range.end.minusMillis(1).atZone(zone).toLocalDate()
        while (!day.isAfter(last)) {
            days += day
            day = day.plusDays(1)
        }
    }
    return days
}

/** A simple month view. Past days and busy days are greyed out. */
@Composable
fun AvailabilityCalendar(busy: Set<LocalDate>, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val locale = Locale.getDefault()

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > YearMonth.from(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
            }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
            }
        }
        Row {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
        val cells = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row {
                (week + List(7 - week.size) { null }).forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, isBusy = day in busy, isPast = day < today)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            Legend(MaterialTheme.colorScheme.secondaryContainer, R.string.calendar_free)
            Legend(MaterialTheme.colorScheme.surfaceVariant, R.string.calendar_busy)
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, isBusy: Boolean, isPast: Boolean) {
    val free = !isBusy && !isPast
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                if (free) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small,
            ),
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (free) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textDecoration = if (isBusy && !isPast) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(16.dp).background(color, MaterialTheme.shapes.small))
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
    }
}
