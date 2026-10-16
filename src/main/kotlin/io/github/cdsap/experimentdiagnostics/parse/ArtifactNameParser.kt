package io.github.cdsap.experimentdiagnostics.parse

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey

data class ParsedArtifactName(
    val kind: ArtifactKind,
    val joinKey: JoinKey,
)

/**
 * Parses Telltale diagnostics artifact names.
 *
 * After the `cpu-` / `jfr-` prefix, the trailing token is numeric job-index;
 * the token sequence immediately before it must be a known Telltale diagnostics
 * job id (`execution-caching` or `execution-no-caching`); everything before that
 * job id is the variant (variants may contain hyphens).
 */
object ArtifactNameParser {
    val knownJobs: List<String> = listOf("execution-caching", "execution-no-caching")

    fun parse(artifactName: String): ParsedArtifactName? {
        val kind =
            when {
                artifactName.startsWith(ArtifactKind.CPU.prefix) -> ArtifactKind.CPU
                artifactName.startsWith(ArtifactKind.JFR.prefix) -> ArtifactKind.JFR
                else -> return null
            }
        val remainder = artifactName.removePrefix(kind.prefix)
        val lastDash = remainder.lastIndexOf('-')
        if (lastDash <= 0) return null
        val indexToken = remainder.substring(lastDash + 1)
        val jobIndex = indexToken.toIntOrNull() ?: return null
        val beforeIndex = remainder.substring(0, lastDash)
        val job =
            knownJobs.firstOrNull { beforeIndex == it || beforeIndex.endsWith("-$it") }
                ?: return null
        val variant =
            if (beforeIndex == job) {
                return null
            } else {
                beforeIndex.removeSuffix("-$job")
            }
        if (variant.isEmpty()) return null
        return ParsedArtifactName(kind, JoinKey(variant, job, jobIndex))
    }
}
