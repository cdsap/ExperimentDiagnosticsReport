package io.github.cdsap.experimentdiagnostics.aggregate

import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.model.PackageMeasurement
import io.github.cdsap.experimentdiagnostics.model.VariantDiagnostics
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries

data class ParsedArtifact(
    val joinKey: JoinKey,
    val series: List<SampleSeries>,
)

class VariantAggregator {
    fun aggregate(artifacts: List<ParsedArtifact>): List<VariantDiagnostics> {
        val byVariant = artifacts.groupBy { it.joinKey.variant }
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

                    for (artifact in variantBundles) {
                        for (series in artifact.series) {
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

        return variants
    }

    private data class SeriesKey(
        val category: String,
        val name: String,
        val metric: String,
        val qualifier: String,
    )
}
