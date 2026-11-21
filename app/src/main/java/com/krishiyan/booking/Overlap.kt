package com.krishiyan.booking

import com.krishiyan.equipment.BusyRange
import java.time.Instant

/** True when [start, end) touches any busy period. Ranges that only meet at an edge do not overlap. */
fun overlapsBusy(start: Instant, end: Instant, busy: List<BusyRange>): Boolean =
    busy.any { start.isBefore(it.end) && it.start.isBefore(end) }
