package io.github.cdsap.experimentdiagnostics.parse

import io.github.cdsap.experimentdiagnostics.aggregate.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind

fun interface ArtifactSeriesLoader {
    fun load(bundle: ArtifactBundle): Pair<List<SampleSeries>, List<String>>
}

object ProductionArtifactSeriesLoader : ArtifactSeriesLoader {
    override fun load(bundle: ArtifactBundle): Pair<List<SampleSeries>, List<String>> =
        when (bundle.kind) {
            ArtifactKind.CPU -> CpuDiagnosticsParser.parseDirectory(bundle.directory)
            ArtifactKind.JFR -> JfrAggregates.parseDirectory(bundle.directory)
        }
}
