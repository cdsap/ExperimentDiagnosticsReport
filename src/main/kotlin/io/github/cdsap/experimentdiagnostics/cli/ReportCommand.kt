package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import io.github.cdsap.experimentdiagnostics.application.ReportGenerator
import io.github.cdsap.experimentdiagnostics.application.ReportMetadata
import io.github.cdsap.experimentdiagnostics.github.ArtifactSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.output.PackageWriter
import java.io.File

class ReportCommand(
    private val reportInputLoader: ReportInputLoader = DefaultReportInputLoader(),
    private val artifactSeriesLoader: ArtifactSeriesLoader = ArtifactSeriesLoader(),
) : CliktCommand(name = "report") {
    constructor(artifactSource: ArtifactSource) : this(DefaultReportInputLoader(artifactSource), ArtifactSeriesLoader())

    private val owner by option(help = "GitHub owner")
    private val repo by option(help = "GitHub repository")
    private val runId by option("--run-id", help = "GitHub Actions run id").long()
    private val token by option(help = "GitHub token (or GITHUB_TOKEN / GH_TOKEN env)")
    private val artifactsDir by option("--artifacts-dir", help = "Local artifacts directory (offline)")
    private val output by option("--output", help = "Output package JSON path").required()
    private val force by option("--force", help = "Overwrite output").flag(default = false)

    override fun run() {
        val warnings = mutableListOf<String>()
        val sourceOptions =
            ArtifactSourceOptions(artifactsDir, owner, repo, runId, token)
        val result = reportInputLoader.load(sourceOptions)
        warnings += result.warnings
        val bundles = result.bundles

        if (bundles.isEmpty()) {
            error(warnings.joinToString("\n").ifEmpty { "No artifacts to process" })
        }

        val (artifacts, artifactWarnings) = artifactSeriesLoader.load(bundles)
        warnings += artifactWarnings

        val diagnosticsPackage =
            ReportGenerator().generate(
                artifacts = artifacts,
                metadata =
                    ReportMetadata(
                        experimentRunId = runId?.toString(),
                        sourceRepo = if (owner != null && repo != null) "$owner/$repo" else null,
                    ),
                warnings = warnings,
            )

        PackageWriter.write(diagnosticsPackage, File(output), force = force)
        echo("Wrote ${diagnosticsPackage.variants.size} variant(s) to $output")
        if (diagnosticsPackage.warnings.isNotEmpty()) {
            echo("Warnings:")
            diagnosticsPackage.warnings.forEach { echo("  - $it") }
        }
    }
}
