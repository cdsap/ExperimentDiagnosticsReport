package io.github.cdsap.experimentdiagnostics.application

import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.aggregate.ParsedArtifact
import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage

data class ReportMetadata(
    val experimentRunId: String?,
    val sourceRepo: String?,
)

class ReportGenerator(
    private val variantAggregator: VariantAggregator = VariantAggregator(),
) {
    fun generate(
        artifacts: List<ParsedArtifact>,
        metadata: ReportMetadata,
        warnings: List<String> = emptyList(),
    ): DiagnosticsPackage {
        return DiagnosticsPackage(
            experimentRunId = metadata.experimentRunId,
            sourceRepo = metadata.sourceRepo,
            variants = variantAggregator.aggregate(artifacts),
            warnings = warnings.distinct(),
        )
    }
}
