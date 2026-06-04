#!/bin/sh
#
# Maven wrapper script for Unix/Linux/macOS
#

set -e

# OS specific support
cygwin=false
darwin=false
mingw=false
msys=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MINGW*) mingw=true ;;
  MSYS*) msys=true ;;
esac

[ -z "$JAVA_HOME" ] && echo "Please set JAVA_HOME" && exit 1

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

# Read distributionUrl from properties
DISTRIBUTION_URL=$(grep "^distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)

MAVEN_HOME="${HOME}/.m2/wrapper/dists/$(basename "$DISTRIBUTION_URL" .zip)"

if [ ! -d "$MAVEN_HOME" ]; then
  mkdir -p "$MAVEN_HOME"
  echo "Downloading Maven from $DISTRIBUTION_URL"
  TMP_FILE="$MAVEN_HOME/maven.zip"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$TMP_FILE" "$DISTRIBUTION_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$TMP_FILE" "$DISTRIBUTION_URL"
  else
    echo "Neither curl nor wget found. Please install one of them." && exit 1
  fi
  unzip -q "$TMP_FILE" -d "$MAVEN_HOME"
  rm "$TMP_FILE"
fi

MAVEN_BIN=$(find "$MAVEN_HOME" -name "mvn" -type f | head -1)

exec "$MAVEN_BIN" "$@"
