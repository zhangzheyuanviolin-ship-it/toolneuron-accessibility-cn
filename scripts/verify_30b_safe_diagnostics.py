#!/usr/bin/env python3
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(path: str, needle: str, message: str) -> None:
    text = read(path)
    if needle not in text:
        raise AssertionError(f"{path}: {message}")


def main() -> int:
    require(
        "app/build.gradle.kts",
        'applicationId = "com.dark.tool_neuron.safe30b"',
        "parallel diagnostic applicationId is missing",
    )
    require(
        "app/src/main/res/values/strings.xml",
        '<string name="app_name">ToolNeuron 30B Diagnostics</string>',
        "English launcher label is not distinct",
    )
    require(
        "app/src/main/res/values-zh-rCN/strings.xml",
        '<string name="app_name">ToolNeuron 30B诊断版</string>',
        "Chinese launcher label is not distinct",
    )
    require(
        "app/src/main/AndroidManifest.xml",
        "com.dark.tool_neuron.safe30b.permission.BIND_LLM_SERVICE",
        "signature permission must be unique for side-by-side install",
    )
    require(
        "app/src/main/java/com/dark/tool_neuron/global/ThirtyBMoESafeDefaults.kt",
        "object ThirtyBMoESafeDefaults",
        "safe diagnostic defaults object is missing",
    )
    require(
        "app/src/main/java/com/dark/tool_neuron/activity/ModelLoadingActivity.kt",
        "ThirtyBMoESafeDefaults.loadingParamsFor",
        "local GGUF import must persist safe 30B loading defaults",
    )
    require(
        "app/src/main/java/com/dark/tool_neuron/service/ModelDownloadService.kt",
        "ThirtyBMoESafeDefaults.loadingParamsFor",
        "downloaded GGUF models must persist safe 30B loading defaults",
    )
    require(
        "app/src/main/java/com/dark/tool_neuron/viewmodel/SettingsViewModel.kt",
        "ThirtyBMoESafeDefaults.loadingParamsFor",
        "hardware tuning rewrite must use safe 30B defaults",
    )
    require(
        "app/src/main/java/com/dark/tool_neuron/viewmodel/LLMModelViewModel.kt",
        "ThirtyBMoESafeDefaults.shouldSkipWarmUp",
        "30B MoE diagnostic mode must skip warm-up",
    )
    require(
        ".github/workflows/build-apk.yml",
        'sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" "cmake;3.31.4" "ndk;26.3.11579264"',
        "cloud workflow must install the CMake version required by native modules",
    )
    require(
        ".github/workflows/build-apk.yml",
        "ToolNeuron-30B-Diagnostics",
        "artifact name must identify the diagnostic build",
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(error, file=sys.stderr)
        raise SystemExit(1)
