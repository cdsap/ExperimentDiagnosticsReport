package io.github.cdsap.experimentdiagnostics.aggregate

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.parse.ArtifactSeriesReader
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class VariantAggregatorTest {
    @Test
    fun aggregatesInMemorySeriesWithoutReadingArtifactFiles() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant-b", "job", 1), File("does-not-exist"))
        val reader =
            ArtifactSeriesReader { artifact ->
                assertEquals(bundle, artifact)
                listOf(
                    SampleSeries(
                        category = "cpu",
                        name = "vmstat.us",
                        qualifier = "system",
                        metric = "CPU_DIAGNOSTICS",
                        samples = listOf(10.0, 30.0),
                    ),
                ) to listOf("reader warning")
            }

        val (variants, warnings) = VariantAggregator(reader).aggregate(listOf(bundle))

        assertEquals(listOf("reader warning"), warnings)
        val variant = variants.single()
        val measurement = variant.measurements.single()
        assertEquals("variant-b", variant.variant)
        assertEquals(listOf("job-1"), variant.jobKeys)
        assertEquals(20.0, measurement.mean)
        assertEquals(20.0, measurement.p50)
        assertEquals(30.0, measurement.p90)
    }
}
