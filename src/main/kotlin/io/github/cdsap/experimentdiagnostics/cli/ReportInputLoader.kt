package io.github.cdsap.experimentdiagnostics.cli

import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundleSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundleSourceResolver
import io.github.cdsap.experimentdiagnostics.github.ArtifactSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.github.DefaultArtifactBundleSourceResolver

data class ReportInput(
    val bundles: List<ArtifactBundle>,
    val warnings: List<String>,
)

fun interface ReportInputLoader {
    fun load(options: ArtifactSourceOptions): ReportInput
}

class DefaultReportInputLoader(
    private val sourceResolver: ArtifactBundleSourceResolver = DefaultArtifactBundleSourceResolver(),
) : ReportInputLoader {
    constructor(artifactSource: ArtifactSource) :
        this(
            ArtifactBundleSourceResolver { options ->
                object : ArtifactBundleSource {
                    override fun load() = artifactSource.load(options)
                }
            },
        )

    override fun load(options: ArtifactSourceOptions): ReportInput {
        return sourceResolver.resolve(options).use { source ->
            val result = source.load()
            ReportInput(bundles = result.bundles, warnings = result.warnings)
        }
    }
}
