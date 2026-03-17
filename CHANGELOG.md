# Changelog

All notable changes in this repository are documented here for auditability and rollback.

## [v2.0.0-a11y-zh.1] - 2026-03-17

### Source Baseline
- Upstream project: `Siddhesh2377/ToolNeuron`
- Upstream branch: `re-write`
- Upstream commit snapshot: `a77bf7e9b80558526556c4c8bd7641efe01ba33c`

### Added
- Chinese resource directory: `app/src/main/res/values-zh-rCN/strings.xml`
- Extended base string resources in `app/src/main/res/values/strings.xml`
- Runtime locale-aware text translation helper: `app/src/main/java/com/dark/tool_neuron/i18n/TnI18n.kt`
- Cloud build workflow: `.github/workflows/build-apk.yml`
- Audit documents under `docs/`

### Changed
- Replaced `contentDescription = null` in UI with readable labels via `tn("Action icon")`
- Added `tn(...)` wrapping for many hard-coded Compose `Text(...)` calls
- Localized ActionButton-family labels and descriptions centrally in `ActionButtons.kt`

### Scope
- Accessibility hardening (first wave)
- Simplified Chinese localization (first wave)

### Notes
- This release focuses only on A11y and Chinese adaptation as Phase 1.
- Subsequent releases will continue full-screen and edge-case localization/accessibility passes.
