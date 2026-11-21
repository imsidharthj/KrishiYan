package com.krishiyan.location

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    private val bengaluru = LatLng(12.9716, 77.5946)
    private val mysuru = LatLng(12.2958, 76.6394)

    @Test
    fun samePointIsZero() {
        assertEquals(0.0, distanceKm(bengaluru, bengaluru), 1e-9)
    }

    @Test
    fun bengaluruToMysuruIsAbout127Km() {
        assertEquals(127.0, distanceKm(bengaluru, mysuru), 2.0)
    }

    @Test
    fun distanceIsSymmetric() {
        assertEquals(distanceKm(bengaluru, mysuru), distanceKm(mysuru, bengaluru), 1e-9)
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111.2, distanceKm(LatLng(20.0, 78.0), LatLng(21.0, 78.0)), 0.5)
    }

    @Test
    fun displayNeverShowsLessThanOneKm() {
        assertEquals(1, displayKm(bengaluru, LatLng(12.9717, 77.5946)))
    }
}
