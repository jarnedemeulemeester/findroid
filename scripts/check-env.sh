#!/usr/bin/env bash
# Check for JDK 21 and Android SDK presence
set -euo pipefail

echo "Checking Java and Android SDK environment..."

# Java check
if java -version 2>&1 | grep -q '21\.'; then
  echo "Java 21 is installed:"
  java -version 2>&1 | sed -n '1,3p'
else
  echo "Java 21 NOT found. Current java -version:" 
  java -version 2>&1 | sed -n '1,3p' || true
  echo "Install via Homebrew: brew install temurin@21"
  echo "Then set: export JAVA_HOME=\$(/usr/libexec/java_home -v21)"
fi

# Check common Android SDK locations
SDK_PATHS=("$HOME/Library/Android/sdk" "$HOME/Android/Sdk" "/Users/jordiguix/Library/Android/sdk" "/Users/jordiguix/Android/Sdk")
FOUND_SDK=""
for p in "${SDK_PATHS[@]}"; do
  if [ -d "$p" ]; then
    FOUND_SDK="$p"
    break
  fi
done

if [ -n "$FOUND_SDK" ]; then
  echo "Android SDK found at: $FOUND_SDK"
else
  echo "Android SDK not found in common locations."
  echo "Install Android Studio or the command-line SDK tools, then set local.properties sdk.dir to the SDK path, e.g."
  echo "  echo \"sdk.dir=\$HOME/Library/Android/sdk\" > local.properties"
fi

echo "Done."
