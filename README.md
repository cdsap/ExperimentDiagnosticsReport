# ExperimentDiagnosticsReport

CLI that downloads Telltale experiment GitHub Actions CPU/JFR artifacts, aggregates them into BER-style statistical summaries (mean / P50 / P90), and writes a versioned JSON package that [BuildExperimentResults](https://github.com/cdsap/BuildExperimentResults) can import.

## Installation

After a `v*` tag is pushed, the fat binary is published as a GitHub Release asset (same pattern as BuildExperimentResults / TaskReport):

```
curl -L https://github.com/cdsap/ExperimentDiagnosticsReport/releases/download/v0.1.0/experiment-diagnostics-report --output experiment-diagnostics-report
chmod 0757 experiment-diagnostics-report
```

To cut a release from `main`:

```
git tag v0.1.0
git push origin v0.1.0
```

## Artifact naming

Telltale uploads:

- `cpu-<variant>-<job>-<job-index>`
- `jfr-<variant>-<job>-<job-index>`

Known job ids: `execution-caching`, `execution-no-caching`. Join key for discovery is `(variant, job, job-index)`. Aggregates pool all jobs/indices per raw variant before BER remaps onto `--variants` tags.

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

BuildExperimentResults imports the package with `--diagnostics-package diagnostics-package.json`.
