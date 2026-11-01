package io.github.cdsap.experimentdiagnostics.github

import io.github.cdsap.experimentdiagnostics.parse.ArtifactNameParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.gson.gson
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipInputStream

data class GithubRunRef(
    val owner: String,
    val repo: String,
    val runId: Long,
    val token: String,
)

/**
 * Lists and downloads CPU/JFR artifacts for a GitHub Actions run.
 */
class GithubArtifactDownloader(
    private val run: GithubRunRef,
    private val workDir: File,
    private val client: HttpClient = defaultClient(),
) {
    fun load(): ArtifactLoadResult =
        runBlocking {
            val warnings = mutableListOf<String>()
            val artifacts = listArtifacts(run)
            val matched =
                artifacts.mapNotNull { artifact ->
                    val parsed = ArtifactNameParser.parse(artifact.name) ?: return@mapNotNull null
                    artifact to parsed
                }
            if (matched.isEmpty()) {
                error("No cpu-* / jfr-* artifacts found for run ${run.runId}")
            }

            val bundles = mutableListOf<ArtifactBundle>()
            for ((artifact, parsed) in matched) {
                val dest =
                    File(
                        workDir,
                        "${parsed.kind.name.lowercase()}/${parsed.joinKey.variant}/" +
                            "${parsed.joinKey.job}/${parsed.joinKey.jobIndex}",
                    )
                dest.mkdirs()
                try {
                    downloadZip(run, artifact.id, dest)
                    bundles += ArtifactBundle(parsed.kind, parsed.joinKey, dest)
                } catch (e: Exception) {
                    warnings += "Failed to download ${artifact.name}: ${e.message}"
                }
            }
            ArtifactLoadResult(bundles = bundles, warnings = warnings)
        }

    private suspend fun listArtifacts(run: GithubRunRef): List<ArtifactDto> {
        val url =
            "https://api.github.com/repos/${run.owner}/${run.repo}/actions/runs/${run.runId}/artifacts?per_page=100"
        val response: ArtifactsResponse =
            client
                .get(url) {
                    bearerAuth(run.token)
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2022-11-28")
                }.body()
        return response.artifacts
    }

    private suspend fun downloadZip(
        run: GithubRunRef,
        artifactId: Long,
        destDir: File,
    ) {
        val url = "https://api.github.com/repos/${run.owner}/${run.repo}/actions/artifacts/$artifactId/zip"
        val response: HttpResponse =
            client.get(url) {
                bearerAuth(run.token)
                accept(ContentType.Application.OctetStream)
                header("X-GitHub-Api-Version", "2022-11-28")
            }
        ZipInputStream(response.bodyAsChannel().toInputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = File(destDir, entry.name.substringAfterLast('/').ifEmpty { entry.name })
                    out.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    fun close() = client.close()

    private data class ArtifactsResponse(
        val artifacts: List<ArtifactDto> = emptyList(),
    )

    private data class ArtifactDto(
        val id: Long,
        val name: String,
    )

    companion object {
        fun defaultClient(): HttpClient =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    gson()
                }
            }
    }
}
