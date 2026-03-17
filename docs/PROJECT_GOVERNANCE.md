# Project Governance

## Branch and Release Policy

- `main` is the production-ready branch.
- Every shipped APK must map to:
  - one tagged commit
  - one changelog entry
  - one release snapshot document in `docs/releases/`

## Auditability Rules

- No undocumented hotfixes.
- Every code change must be traceable through commit history and release docs.
- Upstream rebases/syncs must explicitly record source commit hash.

## Rollback Rules

- Rollback target is always the latest stable release tag.
- Release tags use immutable semantic identifiers, for example:
  - `v2.0.0-a11y-zh.1`
  - `v2.0.0-a11y-zh.2`

## Build and Artifact Policy

- APK is built by GitHub Actions workflow (`.github/workflows/build-apk.yml`).
- Local device build is optional for debug only; official deliverables come from cloud workflow artifacts.
- Delivery package must include:
  - APK artifact
  - release notes
  - source commit and version tag
