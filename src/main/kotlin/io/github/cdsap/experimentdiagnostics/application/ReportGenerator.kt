package io.github.cdsap.experimentdiagnostics.application

import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage

data class ReportMetadata(
    val experimentRunId: String?,
    val sourceRepo: String?,
)

class ReportGenerator(
    private val artifactSeriesLoader: ArtifactSeriesLoader = ArtifactSeriesLoader(),
    private val variantAggregator: VariantAggregator = VariantAggregator(),
) {
    fun generate(
        bundles: List<ArtifactBundle>,
        metadata: ReportMetadata,
        warnings: List<String> = emptyList(),
    ): DiagnosticsPackage {
        val (artifacts, artifactWarnings) = artifactSeriesLoader.load(bundles)
        return DiagnosticsPackage(
            experimentRunId = metadata.experimentRunId,
            sourceRepo = metadata.sourceRepo,
            variants = variantAggregator.aggregate(artifacts),
            warnings = (warnings + artifactWarnings).distinct(),
        )
    }
}
