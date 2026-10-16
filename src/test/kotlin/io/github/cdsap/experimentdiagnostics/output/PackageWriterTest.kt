package io.github.cdsap.experimentdiagnostics.output

import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage
import io.github.cdsap.experimentdiagnostics.model.PackageMeasurement
import io.github.cdsap.experimentdiagnostics.model.VariantDiagnostics
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PackageWriterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun writesVersionedPackage() {
        val out = File(tempDir, "package.json")
        PackageWriter.write(
            DiagnosticsPackage(
                experimentRunId = "1",
                sourceRepo = "cdsap/Telltale",
                variants =
                    listOf(
                        VariantDiagnostics(
                            variant = "lint-4-1",
                            jobKeys = listOf("execution-caching-0"),
                            measurements =
                                listOf(
                                    PackageMeasurement(
                                        category = "cpu",
                                        name = "vmstat.us",
                                        mean = 10.0,
                                        p50 = 10.0,
                                        p90 = 10.0,
                                        qualifier = "system",
                                        metric = "CPU_DIAGNOSTICS",
                                    ),
                                ),
                        ),
                    ),
            ),
            out,
        )
        val text = out.readText()
        assertTrue(text.contains("\"schemaVersion\": \"1.0\""))
        assertTrue(text.contains("lint-4-1"))
        assertTrue(text.contains("CPU_DIAGNOSTICS"))
    }

    @Test
    fun refusesOverwriteWithoutForce() {
        val out = File(tempDir, "package.json")
        out.writeText("{}")
        assertThrows(IllegalStateException::class.java) {
            PackageWriter.write(DiagnosticsPackage(), out, force = false)
        }
    }
}
