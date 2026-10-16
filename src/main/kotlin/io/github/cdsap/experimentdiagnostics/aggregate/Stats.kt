package io.github.cdsap.experimentdiagnostics.aggregate

import org.nield.kotlinstatistics.percentile
import kotlin.math.round

data class AggregateStats(
    val mean: Double,
    val p50: Double,
    val p90: Double,
)

fun aggregate(values: List<Double>): AggregateStats? {
    if (values.isEmpty()) return null
    val mean = values.average()
    val p50 = values.percentile(50.0)
    val p90 = values.percentile(90.0)
    return AggregateStats(round4(mean), round4(p50), round4(p90))
}

private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0
