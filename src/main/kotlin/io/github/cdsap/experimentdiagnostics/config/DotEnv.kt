package io.github.cdsap.experimentdiagnostics.config

import java.io.File

/**
 * Loads KEY=VALUE pairs from a `.env` file into the process environment map used by lookups.
 * Does not override variables already present in [System.getenv].
 */
object DotEnv {
    fun load(
        file: File = File(".env"),
        into: MutableMap<String, String> = linkedMapOf(),
    ): Map<String, String> {
        if (!file.isFile) return into
        file.readLines().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val idx = line.indexOf('=')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim()
            var value = line.substring(idx + 1).trim()
            if (
                (value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))
            ) {
                value = value.substring(1, value.length - 1)
            }
            if (key.isNotEmpty()) {
                into.putIfAbsent(key, value)
            }
        }
        return into
    }

    fun getenv(
        name: String,
        file: File = File(".env"),
    ): String? = System.getenv(name) ?: load(file)[name]
}
