package com.krishiyan.ui.components

import java.text.NumberFormat
import java.util.Locale

private val rupees = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }

fun formatRupees(amount: Double): String = "₹" + rupees.format(amount)
