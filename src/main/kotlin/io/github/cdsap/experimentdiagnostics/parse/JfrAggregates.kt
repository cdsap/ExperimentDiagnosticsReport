package io.github.cdsap.experimentdiagnostics.parse

import java.io.File

/**
 * Derives sample series from JFR recordings (size bytes; duration when `jfr` works).
 */
object JfrAggregates {
    fun parseDirectory(dir: File): Pair<List<SampleSeries>, List<String>> {
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
                val seconds = durationSeconds(file)
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

    private fun durationSeconds(file: File): Double? {
        return try {
            val process =
                ProcessBuilder("jfr", "print", "--events", "jdk.ActiveRecording", file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.bufferedReader().readText()
            val exited = process.waitFor()
            if (exited != 0) return null
            val durationMatch =
                Regex("""duration\s*=\s*([0-9.]+)([a-zA-Z]+)""")
                    .find(output)
                    ?: Regex("""recordingDuration\s*=\s*([0-9.]+)([a-zA-Z]+)""")
                        .find(output)
                    ?: return null
            val value = durationMatch.groupValues[1].toDoubleOrNull() ?: return null
            when (durationMatch.groupValues[2].lowercase()) {
                "ns" -> value / 1_000_000_000.0
                "us", "µs" -> value / 1_000_000.0
                "ms" -> value / 1_000.0
                "s" -> value
                else -> value
            }
        } catch (_: Exception) {
            null
        }
    }
}
