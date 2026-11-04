package io.github.cdsap.experimentdiagnostics.cli

import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactLoadResult
import io.github.cdsap.experimentdiagnostics.github.ArtifactSource
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class ReportInputLoaderTest {
    @Test
    fun returnsBundlesAndWarningsFromItsArtifactSource() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant", "job", 0), File("artifacts"))
        val source =
            FakeArtifactSource(
                ArtifactLoadResult(
                    bundles = listOf(bundle),
                    warnings = listOf("source warning"),
                ),
            )
        val options = ArtifactSourceOptions("artifacts", "owner", "repo", 42, "token")

        val result = DefaultReportInputLoader(source).load(options)

        assertEquals(ReportInput(listOf(bundle), listOf("source warning")), result)
        assertEquals(options, source.receivedOptions)
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
