package io.github.cdsap.experimentdiagnostics.application

import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.parse.ArtifactDirectoryParser
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ReportGeneratorTest {
    @Test
    fun assemblesPackageFromBundlesMetadataAndWarnings() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant", "job", 0), File("artifact"))
        val seriesLoader =
            ArtifactSeriesLoader(
                mapOf(
                    ArtifactKind.CPU to
                        ArtifactDirectoryParser { directory ->
                            assertEquals(bundle.directory, directory)
                            listOf(SampleSeries("cpu", "load", "system", "CPU", listOf(10.0, 30.0))) to
                                listOf("artifact warning")
                        },
                ),
            )

        val diagnosticsPackage =
            ReportGenerator(seriesLoader, VariantAggregator()).generate(
                bundles = listOf(bundle),
                metadata = ReportMetadata("123", "owner/repo"),
                warnings = listOf("source warning", "artifact warning"),
            )

        assertEquals("123", diagnosticsPackage.experimentRunId)
        assertEquals("owner/repo", diagnosticsPackage.sourceRepo)
        assertEquals(listOf("source warning", "artifact warning"), diagnosticsPackage.warnings)
        assertEquals("variant", diagnosticsPackage.variants.single().variant)
        assertEquals(listOf("job-0"), diagnosticsPackage.variants.single().jobKeys)
        assertEquals(20.0, diagnosticsPackage.variants.single().measurements.single().mean)
    }
}
