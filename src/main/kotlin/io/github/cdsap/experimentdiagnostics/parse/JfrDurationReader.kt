package io.github.cdsap.experimentdiagnostics.parse

import java.io.File

fun interface JfrDurationReader {
    fun readDurationSeconds(file: File): Double?
}

object ProcessBuilderJfrDurationReader : JfrDurationReader {
    override fun readDurationSeconds(file: File): Double? {
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
