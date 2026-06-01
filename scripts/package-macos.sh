#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="Quoridor"
JAR_NAME="quoridor-1.0.0-SNAPSHOT.jar"

cd "$ROOT_DIR"
mvn clean package -Pproduction

rm -rf target/jpackage-input
mkdir -p target/jpackage-input
cp "target/${JAR_NAME}" target/jpackage-input/

rm -rf dist/macos
mkdir -p dist/macos

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version 1.0.0 \
  --input target/jpackage-input \
  --main-jar "$JAR_NAME" \
  --arguments "--quoridor.desktop=true" \
  --dest dist/macos

mkdir -p "dist/macos/${APP_NAME}-macos"
mv "dist/macos/${APP_NAME}.app" "dist/macos/${APP_NAME}-macos/"
cp distribution/README.txt "dist/macos/${APP_NAME}-macos/README.txt"

(
  cd dist/macos
  ditto -c -k --keepParent "${APP_NAME}-macos" "${APP_NAME}-macos.zip"
)

echo "Готово: dist/macos/${APP_NAME}-macos.zip"
