package io.github.cdsap.experimentdiagnostics.cli

import com.github.ajalt.clikt.core.main
import io.github.cdsap.experimentdiagnostics.application.ReportInput
import io.github.cdsap.experimentdiagnostics.application.ReportInputLoader
import io.github.cdsap.experimentdiagnostics.github.ArtifactBundle
import io.github.cdsap.experimentdiagnostics.github.ArtifactSeriesLoader
import io.github.cdsap.experimentdiagnostics.github.ArtifactSourceOptions
import io.github.cdsap.experimentdiagnostics.model.ArtifactKind
import io.github.cdsap.experimentdiagnostics.model.JoinKey
import io.github.cdsap.experimentdiagnostics.parse.ArtifactDirectoryParser
import io.github.cdsap.experimentdiagnostics.parse.SampleSeries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReportCommandTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun usesInjectedInputLoaderAndPropagatesItsResult() {
        val bundle = ArtifactBundle(ArtifactKind.CPU, JoinKey("variant", "job", 0), tempDir)
        val source =
            FakeReportInputLoader(
                ReportInput(
                    bundles = listOf(bundle),
                    warnings = listOf("source warning"),
                ),
            )
        val seriesLoader =
            ArtifactSeriesLoader(
                mapOf(
                    ArtifactKind.CPU to
                        ArtifactDirectoryParser { directory ->
                            assertEquals(bundle.directory, directory)
                            listOf(SampleSeries("cpu", "load", "system", "CPU", listOf(10.0))) to
                                listOf("artifact warning")
                        },
                ),
            )
        val output = File(tempDir, "report.json")
        ReportCommand(source, seriesLoader).main(arrayOf("--output", output.path))

        assertTrue(output.exists())
        assertTrue(output.readText().contains("source warning"))
        assertTrue(output.readText().contains("artifact warning"))
        assertEquals(ArtifactSourceOptions(null, null, null, null, null), source.receivedOptions)
    }

    private class FakeReportInputLoader(
        private val result: ReportInput,
    ) : ReportInputLoader {
        var receivedOptions: ArtifactSourceOptions? = null

        override fun load(options: ArtifactSourceOptions): ReportInput {
            receivedOptions = options
            return result
        }
    }
}
