package io.github.cdsap.experimentdiagnostics.output

import com.google.gson.GsonBuilder
import io.github.cdsap.experimentdiagnostics.model.DiagnosticsPackage
import java.io.File

object PackageWriter {
    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    fun write(
        diagnosticsPackage: DiagnosticsPackage,
        output: File,
        force: Boolean = false,
    ) {
        if (output.exists() && !force) {
            throw IllegalStateException("Output already exists: ${output.path} (pass --force to overwrite)")
        }
        output.parentFile?.mkdirs()
        output.writeText(gson.toJson(diagnosticsPackage))
    }
}
