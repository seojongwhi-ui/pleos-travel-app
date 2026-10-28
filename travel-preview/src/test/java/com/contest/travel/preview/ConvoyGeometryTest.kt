package com.contest.travel.preview

import org.junit.Assert.*
import org.junit.Test

class ConvoyGeometryTest {
    @Test fun failedRealLocationHidesOurCarWhileKeepingSimulatedCompanions() {
        for (status in listOf(ReadStatus.ERROR, ReadStatus.EMPTY, ReadStatus.LOADING, ReadStatus.STOPPED)) {
            val positions = convoyMapPositions(.38, true, Reading(status, GeoPoint(37.4, 127.1)))
            assertNull("A stale SDK value must not be shown as current", positions[0])
            assertEquals(routePosition(.26), positions[1])
            assertEquals(routePosition(.52), positions[2])
        }
    }

    @Test fun realAndSimulatedPositionsRemainSeparate() {
        val actual = GeoPoint(37.4, 127.1)
        val ready = Reading(ReadStatus.READY, actual)
        assertEquals(actual, convoyMapPositions(.38, true, ready)[0])
        assertEquals(routePosition(.38), convoyMapPositions(.38, false, ready)[0])
        assertNull(convoyMapPositions(.38, true, Reading(ReadStatus.READY, GeoPoint(0.0, 0.0)))[0])
    }

    @Test fun routeProgressClampsAndEndsAtSharedDestination() {
        assertEquals(demoRoute.first(), routePosition(-1.0))
        assertTrue(distanceMeters(demoDestination, routePosition(1.0)) < .001)
        assertEquals(demoDestination, routePosition(2.0))
        val halfway = routePosition(.5)
        assertTrue(halfway.isValid())
        assertTrue(halfway.longitude > demoRoute.first().longitude)
        assertTrue(halfway.longitude < demoDestination.longitude)
    }

    @Test fun overviewContainsDestinationAndAllCarsIncludingRealLocationOutsideDemoRoute() {
        val realCar = GeoPoint(37.20, 126.60)
        val points = demoRoute + listOf(realCar, routePosition(.3), routePosition(.7))
        for ((width, height) in listOf(1340f to 430f, 745f to 400f)) {
            val viewport = MapViewport.fit(points, width, height, 90f)
            points.forEach { point ->
                val (x, y) = viewport.project(point)
                assertTrue("longitude clipped: $point", x >= 89.9f && x <= width - 89.9f)
                assertTrue("latitude clipped: $point", y >= 89.9f && y <= height - 89.9f)
            }
        }
    }

    @Test fun threeCarLabelsStaySeparateInNarrowMapEvenWhenCarsReachSameDestination() {
        val labels = placeCarLabels(List(3) { 540f to 80f }, 745f, 400f, 150f, 84f, 12f, 64f, 48f)
        labels.forEach { (x, y) ->
            assertTrue(x >= 12 && x + 150 <= 733)
            assertTrue(y >= 64 && y + 84 <= 352)
        }
        labels.forEachIndexed { index, (x, y) ->
            labels.drop(index + 1).forEach { (otherX, otherY) ->
                assertFalse("Vehicle labels overlap", x < otherX + 150 && x + 150 > otherX && y < otherY + 84 && y + 84 > otherY)
            }
        }
    }
}
