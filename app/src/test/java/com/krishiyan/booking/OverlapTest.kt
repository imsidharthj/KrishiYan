package com.krishiyan.booking

import com.krishiyan.equipment.BusyRange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OverlapTest {
    private fun at(hour: Int) = Instant.parse("2030-01-01T00:00:00Z").plusSeconds(hour * 3600L)
    private val busy = listOf(BusyRange(at(10), at(12)))

    @Test
    fun fullyBeforeIsFree() = assertFalse(overlapsBusy(at(6), at(9), busy))

    @Test
    fun fullyAfterIsFree() = assertFalse(overlapsBusy(at(13), at(15), busy))

    @Test
    fun endingExactlyAtBusyStartIsFree() = assertFalse(overlapsBusy(at(8), at(10), busy))

    @Test
    fun startingExactlyAtBusyEndIsFree() = assertFalse(overlapsBusy(at(12), at(14), busy))

    @Test
    fun partlyInsideOverlaps() = assertTrue(overlapsBusy(at(11), at(13), busy))

    @Test
    fun coveringWholeBusyOverlaps() = assertTrue(overlapsBusy(at(9), at(13), busy))

    @Test
    fun insideBusyOverlaps() = assertTrue(overlapsBusy(at(10), at(11), busy))

    @Test
    fun noBusyPeriodsIsFree() = assertFalse(overlapsBusy(at(1), at(2), emptyList()))
}
