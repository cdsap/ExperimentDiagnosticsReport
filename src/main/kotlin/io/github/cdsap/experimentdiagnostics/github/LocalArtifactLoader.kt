package io.github.cdsap.experimentdiagnostics.github

import io.github.cdsap.experimentdiagnostics.aggregate.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.parse.ArtifactNameParser
import java.io.File

/**
 * Loads artifact bundles from a local directory tree.
 *
 * Expected layout (either):
 * 1. Flat: each subdirectory named like the GHA artifact (`cpu-lint-4-1-execution-caching-0/`)
 * 2. Nested: `cpu|jfr/<variant>/<job>/<index>/` as produced by the downloader
 */
class LocalArtifactLoader(
    private val artifactsDir: File,
) {
    fun load(): ArtifactLoadResult {
        require(artifactsDir.isDirectory) { "Artifacts directory does not exist: ${artifactsDir.path}" }
        val warnings = mutableListOf<String>()
        val bundles = mutableListOf<ArtifactBundle>()

        // Nested layout
        for (kindDir in artifactsDir.listFiles()?.filter { it.isDirectory }.orEmpty()) {
            val kindName = kindDir.name.lowercase()
            if (kindName != "cpu" && kindName != "jfr") continue
            for (variantDir in kindDir.listFiles()?.filter { it.isDirectory }.orEmpty()) {
                for (jobDir in variantDir.listFiles()?.filter { it.isDirectory }.orEmpty()) {
                    for (indexDir in jobDir.listFiles()?.filter { it.isDirectory }.orEmpty()) {
                        val synthetic =
                            "${kindName}-${variantDir.name}-${jobDir.name}-${indexDir.name}"
                        val parsed = ArtifactNameParser.parse(synthetic)
                        if (parsed == null) {
                            warnings += "Skipping unrecognized nested path: $indexDir.path"
                            continue
                        }
                        bundles += ArtifactBundle(parsed.kind, parsed.joinKey, indexDir)
                    }
                }
            }
        }

        // Flat layout: immediate child dirs named as artifacts
        for (child in artifactsDir.listFiles()?.filter { it.isDirectory }.orEmpty()) {
            val parsed = ArtifactNameParser.parse(child.name) ?: continue
            bundles += ArtifactBundle(parsed.kind, parsed.joinKey, child)
        }

        if (bundles.isEmpty()) {
            warnings += "No CPU/JFR artifact directories found under ${artifactsDir.path}"
        }

        return ArtifactLoadResult(
            bundles = bundles.distinctBy { Triple(it.kind, it.joinKey, it.directory.absolutePath) },
            warnings = warnings,
        )
    }
}
