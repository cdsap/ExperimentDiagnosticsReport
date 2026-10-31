package io.github.cdsap.experimentdiagnostics.parse

import java.io.File

fun interface ArtifactDirectoryParser {
    fun parseDirectory(dir: File): Pair<List<SampleSeries>, List<String>>
}
