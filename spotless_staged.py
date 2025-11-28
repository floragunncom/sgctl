#!/usr/bin/env python3
import sys
import os
import subprocess
from pathlib import Path

if len(sys.argv) < 2:
    print("No files provided.")
    sys.exit(1)

git_root = subprocess.run(
    ["git", "rev-parse", "--show-toplevel"],
    capture_output=True,
    text=True,
    check=True
).stdout.strip()

mvnw = Path(git_root) / ("mvnw.cmd" if os.name == "nt" else "mvnw")
if not mvnw.exists():
    print("[HOOK ERROR] Maven Wrapper not found!")
    sys.exit(1)

files_to_process = []
for f in sys.argv[1:]:
    diff = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--ignore-space-at-eol", "--", f],
        capture_output=True,
        text=True
    ).stdout.strip()
    if diff:
        files_to_process.append(str(Path(git_root) / f))
    else:
        print(f"[Spotless] Skipping {f} (only EOL/whitespace changes)")

if not files_to_process:
    print("[Spotless] No staged Java files found.")
    sys.exit(0)

files_arg = ",".join(files_to_process)
print("Running spotless:apply on staged Java files...")
print(f"Files: {files_arg}")

subprocess.run([str(mvnw), "-B", "spotless:apply", f"-DspotlessFiles={files_arg}"], check=True)