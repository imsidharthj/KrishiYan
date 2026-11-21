package com.krishiyan.booking

import androidx.annotation.StringRes
import com.krishiyan.R
import java.time.Instant

enum class RentalTab(@StringRes val label: Int) {
    Pending(R.string.tab_pending),
    Confirmed(R.string.tab_confirmed),
    Active(R.string.tab_active),
    Past(R.string.tab_past),
}

/** Which My rentals tab a booking belongs in. A request nobody answered before its end moves to Past. */
fun rentalTab(status: BookingStatus, start: Instant, end: Instant, now: Instant): RentalTab = when {
    !end.isAfter(now) -> RentalTab.Past
    status == BookingStatus.Pending -> RentalTab.Pending
    status == BookingStatus.Accepted && start.isAfter(now) -> RentalTab.Confirmed
    status == BookingStatus.Accepted -> RentalTab.Active
    else -> RentalTab.Past
}
