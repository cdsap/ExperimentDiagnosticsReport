package io.github.cdsap.experimentdiagnostics.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CpuDiagnosticsParserTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun parsesPressureVmstatAndPidstat() {
        File(tempDir, "pressure.log").writeText(
            """
            === 2026-10-16T00:00:00+00:00 ===
            --- /proc/pressure/cpu
            some avg10=0.00 avg60=1.50 avg300=0.40 total=1
            some avg10=0.00 avg60=2.50 avg300=0.40 total=2
            """.trimIndent(),
        )
        File(tempDir, "system.log").writeText(
            """
            procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
             r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
             0  0      0 100000      0      0    0    0     0     0    1    1 10 20 60 10  0
             0  0      0 100000      0      0    0    0     0     0    1    1 30 10 50 10  0
            """.trimIndent(),
        )
        File(tempDir, "processes.log").writeText(
            """
            # Time        UID       PID    %usr %system  %guest   %wait    %CPU   CPU  minflt/s  majflt/s     VSZ    RSS   %MEM  Command
            12:00:01        0         1    1.00    2.00    0.00    0.00    5.00     0      0.00      0.00       0      0   0.00  init
            12:00:02        0         1    1.00    2.00    0.00    0.00   15.00     0      0.00      0.00       0      0   0.00  init
            """.trimIndent(),
        )

        val (series, warnings) = CpuDiagnosticsParser.parseDirectory(tempDir)
        assertTrue(warnings.isEmpty())
        assertEquals(1.5, series.first { it.name == "psi.cpu.some.avg60" }.samples[0])
        assertEquals(2.5, series.first { it.name == "psi.cpu.some.avg60" }.samples[1])
        assertEquals(listOf(10.0, 30.0), series.first { it.name == "vmstat.us" }.samples)
        assertEquals(listOf(5.0, 15.0), series.first { it.name == "pidstat.percent_cpu" }.samples)
    }

    @Test
    fun emptyDirectoryWarns() {
        val (series, warnings) = CpuDiagnosticsParser.parseDirectory(tempDir)
        assertTrue(series.isEmpty())
        assertTrue(warnings.any { it.contains("Empty CPU diagnostics") })
    }
}
