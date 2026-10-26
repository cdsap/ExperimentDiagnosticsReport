package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactSourceResolver
import io.github.cdsap.experimentdiagnostics.artifact.DefaultArtifactSourceResolver
import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage
import io.github.cdsap.experimentdiagnostics.output.PackageWriter
import io.github.cdsap.experimentdiagnostics.parse.ProductionArtifactSeriesReader
import java.io.File

class ReportCommand(
    private val sourceResolver: ArtifactSourceResolver = DefaultArtifactSourceResolver(),
) : CliktCommand(name = "report") {
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
        val bundles =
            sourceResolver.resolve(sourceOptions).use { source ->
            val result = source.load()
            warnings += result.warnings
            result.bundles
        }

        if (bundles.isEmpty()) {
            error(warnings.joinToString("\n").ifEmpty { "No artifacts to process" })
        }

        val (variants, aggWarnings) = VariantAggregator(ProductionArtifactSeriesReader).aggregate(bundles)
        warnings += aggWarnings

        val diagnosticsPackage =
            DiagnosticsPackage(
                experimentRunId = runId?.toString(),
                sourceRepo = if (owner != null && repo != null) "$owner/$repo" else null,
                variants = variants,
                warnings = warnings.distinct(),
            )

        PackageWriter.write(diagnosticsPackage, File(output), force = force)
        echo("Wrote ${variants.size} variant(s) to $output")
        if (warnings.isNotEmpty()) {
            echo("Warnings:")
            warnings.distinct().forEach { echo("  - $it") }
        }
    }
}
