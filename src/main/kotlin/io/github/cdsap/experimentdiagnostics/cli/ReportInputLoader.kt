package io.github.cdsap.experimentdiagnostics.cli

import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.github.DefaultArtifactSource

data class ReportInput(
    val bundles: List<ArtifactBundle>,
    val warnings: List<String>,
)

fun interface ReportInputLoader {
    fun load(options: ArtifactSourceOptions): ReportInput
}

class DefaultReportInputLoader(
    private val artifactSource: ArtifactSource = DefaultArtifactSource(),
) : ReportInputLoader {
    override fun load(options: ArtifactSourceOptions): ReportInput {
        val result = artifactSource.load(options)
        return ReportInput(bundles = result.bundles, warnings = result.warnings)
    }
}
