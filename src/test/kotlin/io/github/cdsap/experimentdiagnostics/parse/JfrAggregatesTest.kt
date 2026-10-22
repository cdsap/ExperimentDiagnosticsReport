package io.github.cdsap.experimentdiagnostics.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JfrAggregatesTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun aggregatesDurationsWithInjectedReader() {
        val gradleFile = File(tempDir, "gradle-recording.jfr").apply { writeBytes(ByteArray(7)) }
        val kotlinFile = File(tempDir, "kotlin-recording.jfr").apply { writeBytes(ByteArray(11)) }
        File(tempDir, "ignored.txt").writeText("not a recording")

        val reader =
            JfrDurationReader { file ->
                when (file.name) {
                    gradleFile.name -> 1.5
                    kotlinFile.name -> 2.5
                    else -> error("Unexpected file: ${file.name}")
                }
            }

        val (series, warnings) = JfrAggregates.parseDirectory(tempDir, reader)

        assertTrue(warnings.isEmpty())
        assertEquals(7.0, series.single { it.name == "recording.size_bytes" && it.qualifier == "gradle" }.samples.single())
        assertEquals(11.0, series.single { it.name == "recording.size_bytes" && it.qualifier == "kotlin" }.samples.single())
        assertEquals(1.5, series.single { it.name == "recording.duration_seconds" && it.qualifier == "gradle" }.samples.single())
        assertEquals(2.5, series.single { it.name == "recording.duration_seconds" && it.qualifier == "kotlin" }.samples.single())
    }

    @Test
    fun warnsWhenInjectedReaderCannotReadDuration() {
        val readable = File(tempDir, "gradle-readable.jfr").apply { writeBytes(ByteArray(3)) }
        val unreadable = File(tempDir, "gradle-unreadable.jfr").apply { writeBytes(ByteArray(5)) }

        val (series, warnings) =
            JfrAggregates.parseDirectory(
                tempDir,
                JfrDurationReader { file -> if (file.name == readable.name) 4.0 else null },
            )

        assertEquals(listOf(4.0), series.single { it.name == "recording.duration_seconds" }.samples)
        assertEquals(listOf("Could not read JFR duration for ${unreadable.name}"), warnings)
    }
}
