#!/usr/bin/env python3
"""make_bundle.py — the single packaging primitive for BCG releases.

Asserts before zipping (drift-proof by construction):
  1. model sha256 + byte size match assets/model_manifest.json
  2. (if apksigner available) APK signing cert == ci/release_cert_fingerprint.txt
  3. (if aapt available) APK does NOT request android.permission.INTERNET
  4. (if unzip available) zip contains exactly the 4 expected flat entries

Outputs: dist/btcg-<tag>-full.zip, dist/btcg-<tag>-SHA256SUMS.txt,
         dist/SHA256SUMS-inside-bundle.txt
Usage: make_bundle.py <tag> [signed-apk] [model-file]
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(REPO)


def sh(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)


def die(msg):
    print(f"FAIL: {msg}")
    sys.exit(1)


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def find_tool(name):
    for bt in sorted(os.listdir(os.path.expanduser(
            os.environ.get("ANDROID_HOME", "/opt/data/android-sdk") + "/build-tools")), reverse=True):
        p = os.path.join(os.environ.get("ANDROID_HOME", "/opt/data/android-sdk"),
                         "build-tools", bt, name)
        if os.path.exists(p):
            return p
    return shutil.which(name)


def main():
    if len(sys.argv) < 2:
        die("usage: make_bundle.py <tag> [signed-apk] [model-file]")
    tag = sys.argv[1]
    apk = sys.argv[2] if len(sys.argv) > 2 else \
        "app/build/outputs/apk/release/app-release.apk"
    if not os.path.exists(apk):
        die(f"APK not found: {apk}")
    manifest = json.load(open("app/src/main/assets/model_manifest.json"))
    entry = next((m for m in manifest["models"]
                  if m["id"] == manifest["defaultModelId"]), None)
    if entry is None:
        die("defaultModelId not found in manifest")

    model = sys.argv[3] if len(sys.argv) > 3 else f"build/models/{entry['fileName']}"
    if not os.path.exists(model):
        die(f"model not found: {model} (run ci/fetch_and_verify_models.sh)")

    # 1. model hash + size
    a_sha, a_size = sha256_file(model), os.path.getsize(model)
    if a_sha != entry["sha256"]:
        die(f"model sha256 mismatch ({a_sha} != {entry['sha256']})")
    if a_size != entry["sizeBytes"]:
        die(f"model size mismatch ({a_size} != {entry['sizeBytes']})")

    # 2. cert pin (when apksigner available)
    apksigner = find_tool("apksigner")
    if apksigner:
        out = sh(f"'{apksigner}' verify --print-certs '{apk}'").stdout
        fp = None
        for line in out.splitlines():
            if "sha-256" in line.lower() and ":" in line:
                fp = line.split(":", 1)[1].strip().replace(":", "").lower()
                break
        pin = "".join(
            line.split("#")[0].strip() for line in open("ci/release_cert_fingerprint.txt")
            if not line.strip().startswith("#") and line.strip()
        )
        pin = "".join(c for c in pin if c.isalnum()).lower()
        if fp != pin:
            die(f"APK signing cert {fp} != pinned {pin}")
    else:
        print("WARN: apksigner not available; skipping cert pin check")

    # 3. no-INTERNET gate (when aapt available)
    aapt = find_tool("aapt")
    if aapt:
        perms = sh(f"{aapt} dump permissions '{apk}'").stdout
        if "android.permission.INTERNET" in perms:
            die("APK requests android.permission.INTERNET — core guarantee broken")
    else:
        print("WARN: aapt not available; skipping INTERNET-permission gate")

    # stage + zip (STORED: GGUF is entropy-dense; extraction = flash-speed copy)
    stage = "dist/stage"
    shutil.rmtree("dist", ignore_errors=True)
    os.makedirs(stage)
    apk_name = f"btcg-{tag}.apk"
    shutil.copy(apk, os.path.join(stage, apk_name))
    shutil.copy(model, os.path.join(stage, entry["fileName"]))
    shutil.copy("dist_README-FIRST.txt", os.path.join(stage, "README-FIRST.txt"))

    def write_sums():
        with open(os.path.join(stage, "SHA256SUMS.txt"), "w") as f:
            for name in (apk_name, entry["fileName"]):
                f.write(f"{sha256_file(os.path.join(stage, name))}  {name}\n")

    write_sums()
    zip_path = f"dist/btcg-{tag}-full.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        for name in (apk_name, entry["fileName"], "SHA256SUMS.txt", "README-FIRST.txt"):
            zf.write(os.path.join(stage, name), arcname=name,
                     compress_type=zipfile.ZIP_STORED)

    with open(f"dist/btcg-{tag}-SHA256SUMS.txt", "w") as f:
        f.write(f"{sha256_file(zip_path)}  btcg-{tag}-full.zip\n")
    shutil.copy(os.path.join(stage, "SHA256SUMS.txt"),
                "dist/SHA256SUMS-inside-bundle.txt")

    # 4. zip contents sanity
    with zipfile.ZipFile(zip_path) as zf:
        names = zf.namelist()
    expected = {apk_name, entry["fileName"], "SHA256SUMS.txt", "README-FIRST.txt"}
    if set(names) != expected:
        die(f"zip contents unexpected: {names}")

    shutil.rmtree(stage)
    print(f"OK: {zip_path}")
    print(f"OK: dist/btcg-{tag}-SHA256SUMS.txt")
    for p in os.listdir("dist"):
        print(" ", p, os.path.getsize(os.path.join("dist", p)))


if __name__ == "__main__":
    main()
