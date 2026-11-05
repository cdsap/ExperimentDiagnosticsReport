package io.github.cdsap.experimentdiagnostics.application

import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.aggregate.ParsedArtifact
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReportGeneratorTest {
    @Test
    fun assemblesPackageFromParsedArtifactsMetadataAndWarnings() {
        val diagnosticsPackage =
            ReportGenerator(VariantAggregator()).generate(
                artifacts =
                    listOf(
                        ParsedArtifact(
                            JoinKey("variant", "job", 0),
                            listOf(SampleSeries("cpu", "load", "system", "CPU", listOf(10.0, 30.0))),
                        ),
                    ),
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
