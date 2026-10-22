package io.github.cdsap.experimentdiagnostics.parse

import java.io.File

/**
 * Derives sample series from JFR recordings (size bytes; duration when `jfr` works).
 */
object JfrAggregates {
    fun parseDirectory(dir: File): Pair<List<SampleSeries>, List<String>> = parseDirectory(dir, ProcessBuilderJfrDurationReader)

    internal fun parseDirectory(
        dir: File,
        durationReader: JfrDurationReader,
    ): Pair<List<SampleSeries>, List<String>> {
        val warnings = mutableListOf<String>()
        val series = mutableListOf<SampleSeries>()
        val jfrFiles = dir.listFiles()?.filter { it.isFile && it.extension == "jfr" }.orEmpty()
        if (jfrFiles.isEmpty()) {
            warnings += "No .jfr files in ${dir.path}"
            return series to warnings
        }

        val byQualifier =
            jfrFiles.groupBy { file ->
                when {
                    file.name.startsWith("gradle-") -> "gradle"
                    file.name.startsWith("kotlin-") -> "kotlin"
                    else -> "other"
                }
            }

        for ((qualifier, files) in byQualifier) {
            series +=
                SampleSeries(
                    category = "jfr",
                    name = "recording.size_bytes",
                    qualifier = qualifier,
                    metric = "JFR",
                    samples = files.map { it.length().toDouble() },
                )

            val durations = mutableListOf<Double>()
            for (file in files) {
                val seconds = durationReader.readDurationSeconds(file)
                if (seconds == null) {
                    warnings += "Could not read JFR duration for ${file.name}"
                } else {
                    durations += seconds
                }
            }
            if (durations.isNotEmpty()) {
                series +=
                    SampleSeries(
                        category = "jfr",
                        name = "recording.duration_seconds",
                        qualifier = qualifier,
                        metric = "JFR",
                        samples = durations,
                    )
            }
        }

        return series to warnings
    }
}
