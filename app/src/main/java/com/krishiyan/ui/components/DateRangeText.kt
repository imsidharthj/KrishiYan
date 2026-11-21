package com.krishiyan.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** "21 Nov 2026, 6:00 pm to 7:00 pm", or both full dates when the rental spans days. */
fun formatRentalPeriod(start: Instant, end: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val s = start.atZone(zone)
    val e = end.atZone(zone)
    val endText = if (s.toLocalDate() == e.toLocalDate()) e.format(time) else e.format(dateTime)
    return "${s.format(dateTime)} – $endText"
}
