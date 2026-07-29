#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
WORK_DIR="${RUNNER_TEMP:-/tmp}/intelligence-lab-latest-gguf-engine"
AI_REPO="${WORK_DIR}/Ai-Systems-New"
ANDROID_LLAMA="${WORK_DIR}/llama.cpp-android"
UPSTREAM_LLAMA="${WORK_DIR}/llama.cpp-upstream"
LLAMA_LINK="/home/home/dev/include/llama.cpp"

rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"

git clone --depth 1 https://github.com/Siddhesh2377/Ai-Systems-New.git "${AI_REPO}"
git clone --depth 1 --branch re-write https://github.com/Siddhesh2377/llama.cpp-android.git "${ANDROID_LLAMA}"
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git "${UPSTREAM_LLAMA}"

UPSTREAM_SHA="$(git -C "${UPSTREAM_LLAMA}" rev-parse HEAD)"
ANDROID_SHA="$(git -C "${ANDROID_LLAMA}" rev-parse HEAD)"
AI_SHA="$(git -C "${AI_REPO}" rev-parse HEAD)"

for path in CMakeLists.txt cmake common ggml include src vendor; do
    rm -rf "${ANDROID_LLAMA}/${path}"
    cp -a "${UPSTREAM_LLAMA}/${path}" "${ANDROID_LLAMA}/${path}"
done

if mkdir -p "$(dirname "${LLAMA_LINK}")" 2>/dev/null; then
    ln -sfn "${ANDROID_LLAMA}" "${LLAMA_LINK}"
elif command -v sudo >/dev/null 2>&1; then
    sudo mkdir -p "$(dirname "${LLAMA_LINK}")"
    sudo ln -sfn "${ANDROID_LLAMA}" "${LLAMA_LINK}"
else
    echo "Unable to create ${LLAMA_LINK}" >&2
    exit 1
fi

# GitHub's Android SDK currently provides newer CMake packages than the
# Android fork pins. Use the installed version selected by setup-android.
python3 - <<'PY' "${AI_REPO}/gguf_lib/build.gradle.kts"
from pathlib import Path
import re
import sys
path = Path(sys.argv[1])
text = path.read_text()
text = re.sub(r'version = "3\.[0-9.]+"', 'version = "3.31.6"', text)
path.write_text(text)
PY

mkdir -p "${AI_REPO}/gguf_lib/src/main/assets"
cat > "${AI_REPO}/gguf_lib/src/main/assets/intelligence_lab_engine_provenance.txt" <<EOF
ggml_org_llama_cpp=${UPSTREAM_SHA}
android_wrapper_fork=${ANDROID_SHA}
ai_systems_new=${AI_SHA}
build_time_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

chmod +x "${AI_REPO}/gradlew"
(
    cd "${AI_REPO}"
    ./gradlew --no-daemon --console=plain --stacktrace --max-workers=2 :gguf_lib:assembleRelease -x lint
)

AAR_SRC="$(find "${AI_REPO}/gguf_lib/build/outputs/aar" -name '*release.aar' | head -1)"
test -n "${AAR_SRC}"
test -s "${AAR_SRC}"
cp "${AAR_SRC}" "${ROOT_DIR}/libs/gguf_lib-release.aar"

mkdir -p "${ROOT_DIR}/out"
{
    echo "ggml_org_llama_cpp=${UPSTREAM_SHA}"
    echo "android_wrapper_fork=${ANDROID_SHA}"
    echo "ai_systems_new=${AI_SHA}"
    sha256sum "${ROOT_DIR}/libs/gguf_lib-release.aar"
} | tee "${ROOT_DIR}/out/latest-gguf-engine-provenance.txt"
