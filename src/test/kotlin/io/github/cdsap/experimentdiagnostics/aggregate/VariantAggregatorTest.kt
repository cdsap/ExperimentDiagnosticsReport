package io.github.cdsap.experimentdiagnostics.aggregate

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.model.VariantDiagnostics
import io.github.cdsap.experimentdiagnostics.parse.ArtifactDirectoryParser
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class VariantAggregatorTest {
    @Test
    fun defaultReaderHandlesEmptyBundles() {
        val (variants, warnings) = VariantAggregator().aggregate(emptyList())

        assertEquals(emptyList<VariantDiagnostics>(), variants)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun aggregatesInMemorySeriesWithoutReadingArtifactFiles() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant-b", "job", 1), File("does-not-exist"))
        val parser =
            ArtifactDirectoryParser { directory ->
                assertEquals(bundle.directory, directory)
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

        val (variants, warnings) =
            VariantAggregator(mapOf(ArtifactKind.CPU to parser)).aggregate(listOf(bundle))

        assertEquals(listOf("reader warning"), warnings)
        val variant = variants.single()
        val measurement = variant.measurements.single()
        assertEquals("variant-b", variant.variant)
        assertEquals(listOf("job-1"), variant.jobKeys)
        assertEquals(20.0, measurement.mean)
        assertEquals(20.0, measurement.p50)
        assertEquals(30.0, measurement.p90)
    }

    @Test
    fun skipsEmptySeriesAndKeepsMeasurementOrdering() {
        val bundles =
            listOf(
                ArtifactBundle(ArtifactKind.CPU, JoinKey("variant-a", "job", 1), File("missing-a")),
                ArtifactBundle(ArtifactKind.CPU, JoinKey("variant-a", "job", 2), File("missing-b")),
            )
        val parser =
            ArtifactDirectoryParser { directory ->
                when (directory.path) {
                    "missing-a" ->
                        listOf(
                            SampleSeries("z", "z-name", "q", "Z_METRIC", emptyList()),
                            SampleSeries("cpu", "cpu-name", "q", "A_METRIC", listOf(4.0)),
                        ) to emptyList()
                    else ->
                        listOf(
                            SampleSeries("jfr", "jfr-name", "q", "A_METRIC", listOf(2.0)),
                        ) to emptyList()
                }
            }

        val (variants, warnings) =
            VariantAggregator(mapOf(ArtifactKind.CPU to parser)).aggregate(bundles)

        assertEquals(emptyList<String>(), warnings)
        assertEquals(listOf("A_METRIC", "A_METRIC"), variants.single().measurements.map { it.metric })
        assertEquals(listOf(4.0, 2.0), variants.single().measurements.map { it.mean })
    }
}
