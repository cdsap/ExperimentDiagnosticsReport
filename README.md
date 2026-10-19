# ExperimentDiagnosticsReport

CLI that downloads Telltale experiment GitHub Actions CPU/JFR artifacts, aggregates them into BER-style statistical summaries (mean / P50 / P90), and writes a versioned JSON package that [BuildExperimentResults](https://github.com/cdsap/BuildExperimentResults) can import.

## Status

Scaffold — see BuildExperimentResults plan `docs/plans/2026-10-16-1142-feat-telltale-diagnostics-cli-plan.md`.

## Artifact naming

Telltale uploads:

- `cpu-<variant>-<job>-<job-index>`
- `jfr-<variant>-<job>-<job-index>`

Known job ids: `execution-caching`, `execution-no-caching`. Join key for discovery is `(variant, job, job-index)`. Aggregates pool all jobs/indices per raw variant before BER remaps onto `--variants` tags.

## Auth

Copy `.env.example` to `.env` and set a token (`.env` is gitignored):

```bash
cp .env.example .env
# edit .env — or:
echo "GITHUB_TOKEN=$(gh auth token)" > .env
```

Token resolution order: `--token` → process env → `.env` (`GITHUB_TOKEN` or `GH_TOKEN`).

## Usage

```bash
./experiment-diagnostics-report report \
  --owner cdsap \
  --repo Telltale \
  --run-id 123456789 \
  --output diagnostics-package.json
```

Offline fixtures:

```bash
./experiment-diagnostics-report report \
  --artifacts-dir ./fixtures \
  --output diagnostics-package.json
```
