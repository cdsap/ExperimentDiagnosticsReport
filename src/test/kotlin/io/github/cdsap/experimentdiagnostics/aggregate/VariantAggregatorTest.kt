package io.github.cdsap.experimentdiagnostics.aggregate

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.model.VariantDiagnostics
import io.github.cdsap.experimentdiagnostics.parse.ArtifactSeriesReader
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class VariantAggregatorTest {
    @Test
    fun defaultReaderHandlesEmptyBundles() {
        val variants = VariantAggregator().aggregate(emptyList<ParsedArtifact>())

        assertEquals(emptyList<VariantDiagnostics>(), variants)
    }

    @Test
    fun aggregatesInMemorySeriesWithoutReadingArtifactFiles() {
        val artifacts =
            listOf(
                ParsedArtifact(
                    joinKey = JoinKey("variant-b", "job", 1),
                    series =
                        listOf(
                            SampleSeries(
                                category = "cpu",
                                name = "vmstat.us",
                                qualifier = "system",
                                metric = "CPU_DIAGNOSTICS",
                                samples = listOf(10.0, 30.0),
                            ),
                        ),
                ),
            )

        val variants = VariantAggregator().aggregate(artifacts)

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
                ParsedArtifact(
                    JoinKey("variant-a", "job", 1),
                    listOf(
                        SampleSeries("z", "z-name", "q", "Z_METRIC", emptyList()),
                        SampleSeries("cpu", "cpu-name", "q", "A_METRIC", listOf(4.0)),
                    ),
                ),
                ParsedArtifact(
                    JoinKey("variant-a", "job", 2),
                    listOf(SampleSeries("jfr", "jfr-name", "q", "A_METRIC", listOf(2.0))),
                ),
            )

        val variants = VariantAggregator().aggregate(bundles)

        assertEquals(listOf("A_METRIC", "A_METRIC"), variants.single().measurements.map { it.metric })
        assertEquals(listOf(4.0, 2.0), variants.single().measurements.map { it.mean })
    }

    @Test
    fun readerSuppliesSeriesAndWarningsBeforeAggregation() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant", "job", 1), File("artifact"))
        val reader =
            ArtifactSeriesReader { requested ->
                assertEquals(bundle, requested)
                listOf(SampleSeries("cpu", "load", "system", "CPU", listOf(10.0, 30.0))) to
                    listOf("reader warning")
            }

        val (variants, warnings) = VariantAggregator(reader).aggregate(listOf(bundle))

        assertEquals(listOf("reader warning"), warnings)
        assertEquals(
            20.0,
            variants.single().measurements.single().mean,
        )
    }
}
