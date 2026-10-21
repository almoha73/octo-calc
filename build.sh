#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="$HOME/Android/Sdk"
BT="$SDK/build-tools/36.0.0"
PLATFORM="$SDK/platforms/android-36/android.jar"
JDK="$HOME/Android/jdk21"
OUT="$ROOT/build"
export JAVA_HOME="$HOME/Android/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
PKG="energy.octo.calc"
VERSION_NAME="1.0"
VERSION_CODE="1"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir "$ROOT/res" -o "$OUT/res.zip"

"$BT/aapt2" link \
  -I "$PLATFORM" \
  --manifest "$ROOT/AndroidManifest.xml" \
  --auto-add-overlay \
  -R "$OUT/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 36 \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  -o "$OUT/unsigned.apk"

find "$ROOT/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
"$JDK/bin/javac" -encoding UTF-8 -source 17 -target 17 -Xlint:-options \
  -classpath "$PLATFORM" \
  -d "$OUT/classes" @"$OUT/sources.txt"

find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" @"$OUT/classes.txt"

cp "$OUT/unsigned.apk" "$OUT/app.apk"
(cd "$OUT/dex" && zip -q "$OUT/app.apk" classes.dex)

"$BT/zipalign" -f -p 4 "$OUT/app.apk" "$OUT/app-aligned.apk"

KS="$HOME/.android/octocalc.keystore"
if [ ! -f "$KS" ]; then
  mkdir -p "$HOME/.android"
  "$JDK/bin/keytool" -genkeypair -v -keystore "$KS" -storepass octocalc -keypass octocalc \
    -alias octocalc -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Octo Calc, OU=Personal, O=OctoCalc, L=Paris, C=FR" >/dev/null 2>&1
fi

"$BT/apksigner" sign --ks "$KS" --ks-pass pass:octocalc --key-pass pass:octocalc \
  --out "$ROOT/OctoCalc.apk" "$OUT/app-aligned.apk"

"$BT/apksigner" verify "$ROOT/OctoCalc.apk"
echo "APK: $ROOT/OctoCalc.apk"
