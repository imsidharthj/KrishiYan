package com.krishiyan.booking

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RentalTabTest {
    private val now = Instant.parse("2030-01-01T10:00:00Z")
    private fun h(hours: Long) = now.plusSeconds(hours * 3600)

    @Test
    fun pendingInFutureIsPending() =
        assertEquals(RentalTab.Pending, rentalTab(BookingStatus.Pending, h(2), h(4), now))

    @Test
    fun pendingThatAlreadyStartedButNotEndedIsStillPending() =
        assertEquals(RentalTab.Pending, rentalTab(BookingStatus.Pending, h(-1), h(1), now))

    @Test
    fun unansweredPendingAfterEndIsPast() =
        assertEquals(RentalTab.Past, rentalTab(BookingStatus.Pending, h(-4), h(-2), now))

    @Test
    fun acceptedBeforeStartIsConfirmed() =
        assertEquals(RentalTab.Confirmed, rentalTab(BookingStatus.Accepted, h(1), h(3), now))

    @Test
    fun acceptedDuringRentalIsActive() =
        assertEquals(RentalTab.Active, rentalTab(BookingStatus.Accepted, h(-1), h(1), now))

    @Test
    fun acceptedStartingNowIsActive() =
        assertEquals(RentalTab.Active, rentalTab(BookingStatus.Accepted, now, h(1), now))

    @Test
    fun acceptedAfterEndIsPast() =
        assertEquals(RentalTab.Past, rentalTab(BookingStatus.Accepted, h(-3), h(-1), now))

    @Test
    fun rejectedAndCancelledArePast() {
        assertEquals(RentalTab.Past, rentalTab(BookingStatus.Rejected, h(1), h(2), now))
        assertEquals(RentalTab.Past, rentalTab(BookingStatus.Cancelled, h(1), h(2), now))
    }
}
