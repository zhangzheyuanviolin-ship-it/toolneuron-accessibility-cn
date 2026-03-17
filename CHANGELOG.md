# Changelog

All notable changes in this repository are documented here for auditability and rollback.

## [v2.0.0-a11y-zh.2] - 2026-03-17

### Source Baseline
- Upstream project: `Siddhesh2377/ToolNeuron`
- Upstream branch: `re-write`
- Upstream commit snapshot: `a77bf7e9b80558526556c4c8bd7641efe01ba33c`

### Changed
- Reworked accessibility labels around chat home controls with explicit function semantics:
  - top bar: open sidebar, settings, model store, import local model
  - bottom bar: more options, model selector, web search toggle, thinking toggle, send, stop
- Added fallback semantic mapping in `ActionButtons.kt` to avoid generic `Description/Action icon` output on key icon controls.
- Upgraded first-run flow Chinese adaptation for key onboarding surfaces:
  - guide screen, terms header/actions, setup option cards, performance picker, restore dialog
- Upgraded model-entry flows Chinese adaptation:
  - model picker screen, model store tabs/search/filter labels, model browsing empty/error states
- Expanded runtime Chinese translation dictionary (`TnI18n.kt`) with focused entries for onboarding, home controls, model workflows, and notifications.

### Accessibility Focus (This Iteration)
- Converted previously ambiguous labels into task-oriented labels so TalkBack users can identify button purpose before activation.
- Removed decorative icon announcements in multiple cards and banners by setting non-interactive icon descriptions to `null`.

### Notes
- This release prioritizes the user-reported blockers from the previous build: first-open readability and chat-page control discoverability.
- Remaining screens still contain legacy generic labels and will continue in the next pass with per-screen audit.

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
