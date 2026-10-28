package io.github.cdsap.experimentdiagnostics.aggregate

import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.model.PackageMeasurement
import io.github.cdsap.experimentdiagnostics.model.VariantDiagnostics
import io.github.cdsap.experimentdiagnostics.parse.ArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.parse.ProductionArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import java.io.File

data class ArtifactBundle(
    val kind: ArtifactKind,
    val joinKey: JoinKey,
    val directory: File,
)

class VariantAggregator(
    private val loader: ArtifactSeriesLoader = ProductionArtifactSeriesLoader,
) {
    fun aggregate(bundles: List<ArtifactBundle>): Pair<List<VariantDiagnostics>, List<String>> {
        val warnings = mutableListOf<String>()
        val byVariant = bundles.groupBy { it.joinKey.variant }
        val variants =
            byVariant
                .map { (variant, variantBundles) ->
                val jobKeys =
                    variantBundles
                        .map { "${it.joinKey.job}-${it.joinKey.jobIndex}" }
                        .distinct()
                        .sorted()

                val merged = mutableMapOf<SeriesKey, MutableList<Double>>()
                val meta = mutableMapOf<SeriesKey, SampleSeries>()

                for (bundle in variantBundles) {
                    val (seriesList, localWarnings) = loader.load(bundle)
                    warnings += localWarnings
                    for (series in seriesList) {
                        val key = SeriesKey(series.category, series.name, series.metric, series.qualifier)
                        meta[key] = series
                        merged.getOrPut(key) { mutableListOf() }.addAll(series.samples)
                    }
                }

                val measurements =
                    merged
                        .mapNotNull { (key, samples) ->
                        val stats = aggregate(samples) ?: return@mapNotNull null
                        val series = meta.getValue(key)
                        PackageMeasurement(
                            category = series.category,
                            name = series.name,
                            mean = stats.mean,
                            p50 = stats.p50,
                            p90 = stats.p90,
                            qualifier = series.qualifier,
                            metric = series.metric,
                        )
                    }.sortedWith(compareBy({ it.metric }, { it.category }, { it.name }, { it.qualifier }))

                VariantDiagnostics(variant = variant, jobKeys = jobKeys, measurements = measurements)
            }.sortedBy { it.variant }

        return variants to warnings
    }

    private data class SeriesKey(
        val category: String,
        val name: String,
        val metric: String,
        val qualifier: String,
    )
}
