package io.github.cdsap.experimentdiagnostics.parse

import java.io.File

data class SampleSeries(
    val category: String,
    val name: String,
    val qualifier: String,
    val metric: String,
    val samples: List<Double>,
)

/**
 * Parses Telltale CPU diagnostics logs into sample series.
 *
 * v1 metric set:
 * - pressure: PSI cpu some avg60 (when present)
 * - system: vmstat us / sy / id / wa
 * - processes: pidstat %CPU across rows
 */
object CpuDiagnosticsParser {
    fun parseDirectory(dir: File): Pair<List<SampleSeries>, List<String>> {
        val warnings = mutableListOf<String>()
        val series = mutableListOf<SampleSeries>()

        val pressure = File(dir, "pressure.log")
        if (pressure.isFile) {
            val samples = parsePressureAvg60(pressure.readText())
            if (samples.isEmpty()) {
                warnings += "No PSI cpu some avg60 samples in ${pressure.path}"
            } else {
                series +=
                    SampleSeries("cpu", "psi.cpu.some.avg60", "system", "CPU_DIAGNOSTICS", samples)
            }
        }

        val system = File(dir, "system.log")
        if (system.isFile) {
            val columns = parseVmstat(system.readText())
            if (columns.isEmpty()) {
                warnings += "No vmstat samples in ${system.path}"
            } else {
                for ((name, values) in columns) {
                    series += SampleSeries("cpu", "vmstat.$name", "system", "CPU_DIAGNOSTICS", values)
                }
            }
        }

        val processes = File(dir, "processes.log")
        if (processes.isFile) {
            val cpuSamples = parsePidstatCpu(processes.readText())
            if (cpuSamples.isEmpty()) {
                warnings += "No pidstat %CPU samples in ${processes.path}"
            } else {
                series +=
                    SampleSeries("cpu", "pidstat.percent_cpu", "all-processes", "CPU_DIAGNOSTICS", cpuSamples)
            }
        }

        if (!pressure.isFile && !system.isFile && !processes.isFile) {
            warnings += "Empty CPU diagnostics directory: ${dir.path}"
        }

        return series to warnings
    }

    internal fun parsePressureAvg60(text: String): List<Double> {
        val regex = Regex("""some\s+avg10=[0-9.]+\s+avg60=([0-9.]+)""")
        return regex.findAll(text).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
    }

    internal fun parseVmstat(text: String): Map<String, List<Double>> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val headerLine =
            lines.firstOrNull { line ->
                val parts = line.split(Regex("\\s+"))
                parts.contains("us") && parts.contains("sy") && parts.contains("id") && parts.contains("wa")
            } ?: return emptyMap()

        val headers = headerLine.split(Regex("\\s+"))
        val usIdx = headers.indexOf("us")
        val syIdx = headers.indexOf("sy")
        val idIdx = headers.indexOf("id")
        val waIdx = headers.indexOf("wa")
        if (usIdx < 0 || syIdx < 0 || idIdx < 0 || waIdx < 0) return emptyMap()

        val us = mutableListOf<Double>()
        val sy = mutableListOf<Double>()
        val id = mutableListOf<Double>()
        val wa = mutableListOf<Double>()

        for (line in lines) {
            if (line == headerLine || line.startsWith("procs")) continue
            val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val usVal = parts.getOrNull(usIdx)?.toDoubleOrNull() ?: continue
            val syVal = parts.getOrNull(syIdx)?.toDoubleOrNull() ?: continue
            val idVal = parts.getOrNull(idIdx)?.toDoubleOrNull() ?: continue
            val waVal = parts.getOrNull(waIdx)?.toDoubleOrNull() ?: continue
            us += usVal
            sy += syVal
            id += idVal
            wa += waVal
        }

        return mapOf("us" to us, "sy" to sy, "id" to id, "wa" to wa).filterValues { it.isNotEmpty() }
    }

    internal fun parsePidstatCpu(text: String): List<Double> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val header = lines.firstOrNull { it.contains("%CPU") } ?: return emptyList()
        val headers =
            header
                .removePrefix("#")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
        val cpuIdx = headers.indexOfFirst { it == "%CPU" || it.equals("%cpu", ignoreCase = true) }
        if (cpuIdx < 0) return emptyList()

        return lines
            .asSequence()
            .filter { it != header && !it.startsWith("Linux") && !it.trimStart().startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                parts.getOrNull(cpuIdx)?.toDoubleOrNull()
            }.toList()
    }
}
