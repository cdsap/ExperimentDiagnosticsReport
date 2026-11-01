package io.github.cdsap.experimentdiagnostics.github

import java.io.File

data class ArtifactLoadResult(
    val bundles: List<ArtifactBundle>,
    val warnings: List<String>,
)

interface ArtifactSource : AutoCloseable {
    fun load(): ArtifactLoadResult

    override fun close() = Unit
}

/** Adapts the local loader to the report input-source boundary. */
class LocalArtifactSource(
    private val loader: LocalArtifactLoader,
) : ArtifactSource {
    override fun load(): ArtifactLoadResult = loader.load()
}

/** Adapts the GitHub downloader to the report input-source boundary. */
class GithubArtifactSource(
    private val downloader: GithubArtifactDownloader,
) : ArtifactSource {
    override fun load(): ArtifactLoadResult = downloader.load()

    override fun close() = downloader.close()
}

data class ArtifactSourceOptions(
    val artifactsDir: String?,
    val owner: String?,
    val repo: String?,
    val runId: Long?,
    val token: String?,
)

fun interface ArtifactSourceResolver {
    fun resolve(options: ArtifactSourceOptions): ArtifactSource
}

class DefaultArtifactSourceResolver(
    private val environment: Map<String, String> = System.getenv(),
) : ArtifactSourceResolver {
    override fun resolve(options: ArtifactSourceOptions): ArtifactSource {
        options.artifactsDir?.let { return LocalArtifactSource(LocalArtifactLoader(File(it))) }

        val owner = options.owner ?: error("--owner is required unless --artifacts-dir is set")
        val repo = options.repo ?: error("--repo is required unless --artifacts-dir is set")
        val runId = options.runId ?: error("--run-id is required unless --artifacts-dir is set")
        val token =
            options.token
                ?: environment["GITHUB_TOKEN"]
                ?: environment["GH_TOKEN"]
                ?: error("GitHub token required via --token or GITHUB_TOKEN/GH_TOKEN")

        val workDir = File("build/downloaded-artifacts").apply { mkdirs() }
        return GithubArtifactSource(
            GithubArtifactDownloader(GithubRunRef(owner, repo, runId, token), workDir),
        )
    }
}
