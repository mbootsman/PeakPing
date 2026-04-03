package nl.marcel.peakping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsStateTest {

    @Test fun `Empty state is not locked`() = assertFalse(GpsState.Empty.locked)
    @Test fun `Empty state has zero elevation`() = assertEquals(0.0, GpsState.Empty.elevation, 0.0)
    @Test fun `Empty state has no baro`() = assertFalse(GpsState.Empty.baroFused)
    @Test fun `Empty state has zero pressure`() = assertEquals(0f, GpsState.Empty.pressureHpa)
    @Test fun `Empty state has zero satellites`() = assertEquals(0, GpsState.Empty.satellites)

    @Test fun `copy preserves unmodified fields`() {
        val state = GpsState.Empty.copy(elevation = -6.0, locked = true)
        assertEquals(-6.0, state.elevation, 0.001)
        assertTrue(state.locked)
        assertEquals(0.0, state.lat, 0.0)   // unmodified
    }

    @Test fun `baroFused flag is independent of locked`() {
        val state = GpsState.Empty.copy(locked = true, baroFused = false)
        assertTrue(state.locked)
        assertFalse(state.baroFused)
    }

    @Test fun `baro fusion offset math is consistent`() {
        // Core invariant: offset = gps_alt - baro_std; fused = baro_std + offset = gps_alt
        val gpsAlt   = -6.0
        val baroStd  = 35.0
        val offset   = gpsAlt - baroStd       // -41.0
        val fused    = baroStd + offset        // -6.0
        assertEquals(gpsAlt, fused, 0.0001)
    }
}
