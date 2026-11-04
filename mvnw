#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAVEN_DIR="$SCRIPT_DIR/.maven/apache-maven-3.9.9"

if [ -x "$MAVEN_DIR/bin/mvn" ]; then
    exec "$MAVEN_DIR/bin/mvn" "$@"
elif command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
else
    echo "[SoundVault] Maven não foi encontrado no sistema nem em .maven/apache-maven-3.9.9."
    exit 1
fi
