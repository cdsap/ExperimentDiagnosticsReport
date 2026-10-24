package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.main
import io.github.cdsap.experimentdiagnostics.aggregate.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactLoadResult
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactSource
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.artifact.ArtifactSourceResolver
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReportCommandTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun usesInjectedSourceAndClosesItAfterLoading() {
        val source =
            FakeArtifactSource(
                ArtifactLoadResult(
                    bundles =
                        listOf(
                            ArtifactBundle(
                                kind = ArtifactKind.CPU,
                                joinKey = JoinKey("variant", "job", 0),
                                directory = tempDir,
                            ),
                        ),
                    warnings = listOf("source warning"),
                ),
            )
        val output = File(tempDir, "report.json")
        var receivedOptions: ArtifactSourceOptions? = null
        val resolver =
            ArtifactSourceResolver { options ->
                receivedOptions = options
                source
            }

        ReportCommand(resolver).main(arrayOf("--output", output.path))

        assertTrue(output.exists())
        assertTrue(output.readText().contains("source warning"))
        assertEquals(ArtifactSourceOptions(null, null, null, null, null), receivedOptions)
        assertTrue(source.closed)
    }

    private class FakeArtifactSource(
        private val result: ArtifactLoadResult,
    ) : ArtifactSource {
        var closed = false

        override fun load() = result

        override fun close() {
            closed = true
        }
    }
}
