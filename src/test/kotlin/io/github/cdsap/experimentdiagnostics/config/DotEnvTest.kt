package io.github.cdsap.experimentdiagnostics.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DotEnvTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun loadsKeyValuesAndIgnoresComments() {
        val file = File(tempDir, ".env")
        file.writeText(
            """
            # comment
            GITHUB_TOKEN=abc123
            GH_TOKEN="quoted-token"
            EMPTY=
            """.trimIndent(),
        )

        val values = DotEnv.load(file)
        assertEquals("abc123", values["GITHUB_TOKEN"])
        assertEquals("quoted-token", values["GH_TOKEN"])
        assertEquals("", values["EMPTY"])
    }

    @Test
    fun missingFileReturnsEmpty() {
        assertNull(DotEnv.load(File(tempDir, "missing.env"))["GITHUB_TOKEN"])
    }
}
