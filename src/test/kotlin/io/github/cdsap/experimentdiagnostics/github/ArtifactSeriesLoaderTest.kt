package io.github.cdsap.experimentdiagnostics.github

import io.github.cdsap.experimentdiagnostics.aggregate.ParsedArtifact
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.parse.ArtifactDirectoryParser
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ArtifactSeriesLoaderTest {
    @Test
    fun dispatchesByArtifactKindAndPreservesSeriesAndWarnings() {
        val cpuDir = File("cpu-artifact")
        val jfrDir = File("jfr-artifact")
        val parsers =
            mapOf(
                ArtifactKind.CPU to
                    ArtifactDirectoryParser { directory ->
                        assertEquals(cpuDir, directory)
                        listOf(
                            SampleSeries("cpu", "load", "system", "CPU", listOf(1.0)),
                        ) to listOf("cpu warning")
                    },
                ArtifactKind.JFR to
                    ArtifactDirectoryParser { directory ->
                        assertEquals(jfrDir, directory)
                        listOf(
                            SampleSeries("jfr", "size", "other", "JFR", listOf(2.0)),
                        ) to listOf("jfr warning")
                    },
            )

        val result =
            ArtifactSeriesLoader(parsers).load(
                listOf(
                    ArtifactBundle(ArtifactKind.CPU, JoinKey("v", "job", 0), cpuDir),
                    ArtifactBundle(ArtifactKind.JFR, JoinKey("v", "job", 1), jfrDir),
                ),
            )

        assertEquals(
            listOf(
                ParsedArtifact(JoinKey("v", "job", 0), listOf(SampleSeries("cpu", "load", "system", "CPU", listOf(1.0)))),
                ParsedArtifact(JoinKey("v", "job", 1), listOf(SampleSeries("jfr", "size", "other", "JFR", listOf(2.0)))),
            ),
            result.first,
        )
        assertEquals(listOf("cpu warning", "jfr warning"), result.second)
    }
}
