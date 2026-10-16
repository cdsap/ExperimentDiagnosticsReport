package io.github.cdsap.experimentdiagnostics.model

data class DiagnosticsPackage(
    val schemaVersion: String = SCHEMA_VERSION,
    val experimentRunId: String? = null,
    val sourceRepo: String? = null,
    val variants: List<VariantDiagnostics> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = "1.0"
    }
}

data class VariantDiagnostics(
    val variant: String,
    val jobKeys: List<String> = emptyList(),
    val measurements: List<PackageMeasurement> = emptyList(),
)

data class PackageMeasurement(
    val category: String,
    val name: String,
    val mean: Double,
    val p50: Double,
    val p90: Double,
    val qualifier: String,
    val metric: String,
)
