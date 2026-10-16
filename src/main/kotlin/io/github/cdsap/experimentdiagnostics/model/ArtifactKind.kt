package io.github.cdsap.experimentdiagnostics.model

enum class ArtifactKind {
    CPU,
    JFR,
    ;

    val prefix: String
        get() =
            when (this) {
                CPU -> "cpu-"
                JFR -> "jfr-"
            }
}
