package io.github.cdsap.experimentdiagnostics.github

import io.github.cdsap.experimentdiagnostics.aggregate.ParsedArtifact
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.parse.ArtifactDirectoryParser
import io.github.cdsap.experimentdiagnostics.parse.CpuDiagnosticsParser
import io.github.cdsap.experimentdiagnostics.parse.JfrAggregates

typealias ArtifactBundle = io.github.cdsap.experimentdiagnostics.aggregate.ArtifactBundle

class ArtifactSeriesLoader(
    private val parsers: Map<ArtifactKind, ArtifactDirectoryParser> = defaultParsers,
) {
    fun load(bundles: List<ArtifactBundle>): Pair<List<ParsedArtifact>, List<String>> {
        val warnings = mutableListOf<String>()
        val artifacts =
            bundles.map { bundle ->
                val (series, localWarnings) = parsers.getValue(bundle.kind).parseDirectory(bundle.directory)
                warnings += localWarnings
                ParsedArtifact(bundle.joinKey, series)
            }
        return artifacts to warnings
    }

    private companion object {
        val defaultParsers =
            mapOf(
                ArtifactKind.CPU to CpuDiagnosticsParser,
                ArtifactKind.JFR to JfrAggregates,
            )
    }
}
