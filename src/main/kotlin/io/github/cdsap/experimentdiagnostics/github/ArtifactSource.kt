package io.github.cdsap.experimentdiagnostics.github

import java.io.File

data class ArtifactLoadResult(
    val bundles: List<ArtifactBundle>,
    val warnings: List<String>,
)

/** Acquires report artifacts without exposing source-specific lifecycle details. */
fun interface ArtifactSource {
    fun load(options: ArtifactSourceOptions): ArtifactLoadResult
}

private interface ResolvedArtifactSource : AutoCloseable {
    fun load(): ArtifactLoadResult

    override fun close() = Unit
}

/** Adapts the local loader to the resolved artifact-source boundary. */
private class LocalArtifactSource(
    private val loader: LocalArtifactLoader,
) : ResolvedArtifactSource {
    override fun load(): ArtifactLoadResult = loader.load()
}

/** Adapts the GitHub downloader to the resolved artifact-source boundary. */
private class GithubArtifactSource(
    private val downloader: GithubArtifactDownloader,
) : ResolvedArtifactSource {
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

class DefaultArtifactSource(
    private val environment: Map<String, String> = System.getenv(),
) : ArtifactSource {
    override fun load(options: ArtifactSourceOptions): ArtifactLoadResult =
        resolve(options).use { source -> source.load() }

    private fun resolve(options: ArtifactSourceOptions): ResolvedArtifactSource {
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
