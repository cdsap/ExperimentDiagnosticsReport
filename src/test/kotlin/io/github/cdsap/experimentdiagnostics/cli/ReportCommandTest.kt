package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.main
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactLoadResult
import io.github.cdsap.experimentdiagnostics.github.ArtifactSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
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
    fun usesInjectedSourceAndPropagatesItsResult() {
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
        ReportCommand(source).main(arrayOf("--output", output.path))

        assertTrue(output.exists())
        assertTrue(output.readText().contains("source warning"))
        assertEquals(ArtifactSourceOptions(null, null, null, null, null), source.receivedOptions)
    }

    private class FakeArtifactSource(
        private val result: ArtifactLoadResult,
    ) : ArtifactSource {
        var receivedOptions: ArtifactSourceOptions? = null

        override fun load(options: ArtifactSourceOptions): ArtifactLoadResult {
            receivedOptions = options
            return result
        }
    }
}
