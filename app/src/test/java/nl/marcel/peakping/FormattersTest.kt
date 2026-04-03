package nl.marcel.peakping

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    // ── elevationM / elevationFt ──────────────────────────────────────────────

    @Test fun `elevationM formats zero`() = assertEquals("0", elevationM(0.0))
    @Test fun `elevationM formats negative`() = assertEquals("-6", elevationM(-6.3))
    @Test fun `elevationM formats thousands with comma`() = assertEquals("1,234", elevationM(1234.9))

    @Test fun `elevationFt converts metres to feet`() = assertEquals("3", elevationFt(1.0))
    @Test fun `elevationFt converts negative metres`() = assertEquals("-20", elevationFt(-6.3))
    @Test fun `elevationFt formats thousands with comma`() = assertEquals("4,048", elevationFt(1234.0))

    // ── formatLat ─────────────────────────────────────────────────────────────

    @Test fun `formatLat positive is N`() = assertEquals("51.99799° N", formatLat(51.99799))
    @Test fun `formatLat negative is S`() = assertEquals("33.86785° S", formatLat(-33.86785))
    @Test fun `formatLat zero is N`() = assertEquals("0.00000° N", formatLat(0.0))
    @Test fun `formatLat rounds to 5 dp`() = assertEquals("51.99799° N", formatLat(51.997994))

    // ── formatLon ─────────────────────────────────────────────────────────────

    @Test fun `formatLon positive is E`() = assertEquals("4.46760° E", formatLon(4.4676))
    @Test fun `formatLon negative is W`() = assertEquals("74.00597° W", formatLon(-74.00597))
    @Test fun `formatLon zero is E`() = assertEquals("0.00000° E", formatLon(0.0))

    // ── formatAccuracy ────────────────────────────────────────────────────────

    @Test fun `formatAccuracy metric`() = assertEquals("3.5 m", formatAccuracy(3.5f, UnitSystem.METRIC))
    @Test fun `formatAccuracy imperial`() = assertEquals("11.5 ft", formatAccuracy(3.5f, UnitSystem.IMPERIAL))
    @Test fun `formatAccuracy zero metric`() = assertEquals("0.0 m", formatAccuracy(0f, UnitSystem.METRIC))

    // ── formatPressure ────────────────────────────────────────────────────────

    @Test fun `formatPressure metric shows hPa`() = assertEquals("1013.3 hPa", formatPressure(1013.3f, UnitSystem.METRIC))
    @Test fun `formatPressure imperial shows inHg`() = assertEquals("29.92 inHg", formatPressure(1013.3f, UnitSystem.IMPERIAL))
}
