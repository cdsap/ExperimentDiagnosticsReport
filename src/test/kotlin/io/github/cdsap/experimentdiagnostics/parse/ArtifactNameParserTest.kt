package io.github.cdsap.experimentdiagnostics.parse

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ArtifactNameParserTest {
    @Test
    fun parsesAe3Example() {
        val parsed = ArtifactNameParser.parse("cpu-lint-4-1-execution-caching-0")!!
        assertEquals(ArtifactKind.CPU, parsed.kind)
        assertEquals("lint-4-1", parsed.joinKey.variant)
        assertEquals("execution-caching", parsed.joinKey.job)
        assertEquals(0, parsed.joinKey.jobIndex)
    }

    @Test
    fun parsesJfrWithNoCachingJob() {
        val parsed = ArtifactNameParser.parse("jfr-demo-variant-execution-no-caching-3")!!
        assertEquals(ArtifactKind.JFR, parsed.kind)
        assertEquals("demo-variant", parsed.joinKey.variant)
        assertEquals("execution-no-caching", parsed.joinKey.job)
        assertEquals(3, parsed.joinKey.jobIndex)
    }

    @Test
    fun ignoresUnknownPrefix() {
        assertNull(ArtifactNameParser.parse("logs-lint-4-1-execution-caching-0"))
    }

    @Test
    fun rejectsUnknownJob() {
        assertNull(ArtifactNameParser.parse("cpu-lint-4-1-seed-0"))
    }
}
