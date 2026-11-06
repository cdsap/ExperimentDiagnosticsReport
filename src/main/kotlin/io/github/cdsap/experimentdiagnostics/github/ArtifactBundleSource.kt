package io.github.cdsap.experimentdiagnostics.github

import java.io.File

data class ArtifactLoadResult(
    val bundles: List<ArtifactBundle>,
    val warnings: List<String>,
)

/** Acquires the artifact bundles for one report without exposing source-specific details. */
interface ArtifactBundleSource : AutoCloseable {
    fun load(): ArtifactLoadResult

    override fun close() = Unit
}

/** Selects and creates the source for the command-line options. */
fun interface ArtifactBundleSourceResolver {
    fun resolve(options: ArtifactSourceOptions): ArtifactBundleSource
}

/** Adapts the offline artifact loader to the bundle-source boundary. */
class LocalArtifactSource(
    private val loader: LocalArtifactLoader,
) : ArtifactBundleSource {
    override fun load(): ArtifactLoadResult = loader.load()
}

/** Adapts the GitHub downloader to the bundle-source boundary. */
class GithubArtifactSource(
    private val downloader: GithubArtifactDownloader,
) : ArtifactBundleSource {
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

class DefaultArtifactBundleSourceResolver(
    private val environment: Map<String, String> = System.getenv(),
) : ArtifactBundleSourceResolver {
    override fun resolve(options: ArtifactSourceOptions): ArtifactBundleSource {
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

/** Compatibility boundary retained for callers of the previous options-based source. */
fun interface ArtifactSource {
    fun load(options: ArtifactSourceOptions): ArtifactLoadResult
}

/** Compatibility adapter retained for callers that used the previous options-based source. */
class DefaultArtifactSource(
    private val resolver: ArtifactBundleSourceResolver = DefaultArtifactBundleSourceResolver(),
) : ArtifactSource {
    override fun load(options: ArtifactSourceOptions): ArtifactLoadResult =
        resolver.resolve(options).use { source -> source.load() }
}
