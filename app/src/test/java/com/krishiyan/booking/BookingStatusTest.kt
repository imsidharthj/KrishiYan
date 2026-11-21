package com.krishiyan.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BookingStatusTest {
    private val now = Instant.parse("2030-01-01T10:00:00Z")
    private val later = now.plusSeconds(3600)
    private val earlier = now.minusSeconds(3600)

    @Test
    fun onlyPendingCanBeAcceptedOrRejected() {
        BookingStatus.entries.forEach { status ->
            assertEquals(status == BookingStatus.Pending, canAcceptOrReject(status))
        }
    }

    @Test
    fun pendingCanBeCancelledEvenAfterStart() {
        assertTrue(canCancel(BookingStatus.Pending, earlier, now))
    }

    @Test
    fun acceptedCanBeCancelledBeforeStart() {
        assertTrue(canCancel(BookingStatus.Accepted, later, now))
    }

    @Test
    fun acceptedCannotBeCancelledOnceStarted() {
        assertFalse(canCancel(BookingStatus.Accepted, earlier, now))
        assertFalse(canCancel(BookingStatus.Accepted, now, now))
    }

    @Test
    fun finishedStatusesCannotBeCancelled() {
        listOf(BookingStatus.Rejected, BookingStatus.Cancelled, BookingStatus.Completed).forEach {
            assertFalse(canCancel(it, later, now))
        }
    }

    @Test
    fun databaseValuesRoundTrip() {
        BookingStatus.entries.forEach { assertEquals(it, BookingStatus.fromDb(it.dbValue)) }
    }
}
