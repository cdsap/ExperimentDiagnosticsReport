package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import io.github.cdsap.experimentdiagnostics.aggregate.VariantAggregator
import io.github.cdsap.experimentdiagnostics.github.GithubArtifactDownloader
import io.github.cdsap.experimentdiagnostics.github.GithubRunRef
import io.github.cdsap.experimentdiagnostics.github.LocalArtifactLoader
import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage
import io.github.cdsap.experimentdiagnostics.output.PackageWriter
import java.io.File

class ReportCommand : CliktCommand(name = "report") {
    private val owner by option(help = "GitHub owner")
    private val repo by option(help = "GitHub repository")
    private val runId by option("--run-id", help = "GitHub Actions run id").long()
    private val token by option(help = "GitHub token (or GITHUB_TOKEN / GH_TOKEN env)")
    private val artifactsDir by option("--artifacts-dir", help = "Local artifacts directory (offline)")
    private val output by option("--output", help = "Output package JSON path").required()
    private val force by option("--force", help = "Overwrite output").flag(default = false)

    override fun run() {
        val warnings = mutableListOf<String>()
        val bundles =
            if (artifactsDir != null) {
                val (loaded, localWarnings) = LocalArtifactLoader.load(File(artifactsDir!!))
                warnings += localWarnings
                loaded
            } else {
                val resolvedOwner = owner ?: error("--owner is required unless --artifacts-dir is set")
                val resolvedRepo = repo ?: error("--repo is required unless --artifacts-dir is set")
                val resolvedRunId = runId ?: error("--run-id is required unless --artifacts-dir is set")
                val resolvedToken =
                    token
                        ?: System.getenv("GITHUB_TOKEN")
                        ?: System.getenv("GH_TOKEN")
                        ?: error("GitHub token required via --token or GITHUB_TOKEN/GH_TOKEN")
                val workDir = File("build/downloaded-artifacts").apply { mkdirs() }
                val downloader = GithubArtifactDownloader()
                try {
                    val (downloaded, downloadWarnings) =
                        downloader.download(
                            GithubRunRef(resolvedOwner, resolvedRepo, resolvedRunId, resolvedToken),
                            workDir,
                        )
                    warnings += downloadWarnings
                    downloaded
                } finally {
                    downloader.close()
                }
            }

        if (bundles.isEmpty()) {
            error(warnings.joinToString("\n").ifEmpty { "No artifacts to process" })
        }

        val (variants, aggWarnings) = VariantAggregator.aggregate(bundles)
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
