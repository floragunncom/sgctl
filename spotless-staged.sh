#!/bin/bash
set -e

MVNW="./mvnw"

if [ ! -f "$MVNW" ] && [ ! -f "$MVNW.cmd" ]; then
    echo "[HOOK ERROR] Maven Wrapper not found!"
    echo "Please ensure the wrapper files ('mvnw' and 'mvnw.cmd') are in the repository root."
    echo "If you are the project maintainer, run 'mvn -N io.takari:maven:wrapper' and commit the result."
    exit 1
fi

GIT_ROOT=$(git rev-parse --show-toplevel)
JAVA_FILES_ABS=""
for FILE in "$@"; do
    # skip if no change (why the helli does this happen?)
    if git diff --cached --name-only --ignore-space-at-eol -- "$FILE" | grep -q .; then
        # Append the absolute path, using comma as separator
        JAVA_FILES_ABS="${JAVA_FILES_ABS}$GIT_ROOT/$FILE,"
    else
        echo "[Spotless] Skipping $FILE (only EOL/ending-whitespace changes staged)"
    fi
done

if [ -z "$JAVA_FILES_ABS" ]; then
    echo "[Spotless] No staged Java files found to process."
    exit 0
fi

FILES_ABS="${JAVA_FILES_ABS%,}"

echo "Running spotless:apply on staged Java files..."
echo "Files: $FILES_ABS"

"$MVNW" -B spotless:apply -DspotlessFiles="$FILES_ABS"