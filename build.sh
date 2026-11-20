#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
BT="${ANDROID_BUILD_TOOLS:-$SDK/build-tools/36.0.0}"
PLATFORM="${ANDROID_PLATFORM:-$SDK/platforms/android-36/android.jar}"
JDK="${JAVA_HOME:-$HOME/Android/jdk21}"
if [ -x "$JDK/bin/javac" ]; then
  JAVAC="$JDK/bin/javac"
  JAVA="$JDK/bin/java"
  KEYTOOL="$JDK/bin/keytool"
  export JAVA_HOME="$JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
else
  JAVAC="$(command -v javac)"
  JAVA="$(command -v java)"
  KEYTOOL="$(command -v keytool)"
fi
OUT="$ROOT/build"
VERSION_NAME="1.0"
VERSION_CODE="1"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$OUT/test-classes"

"$JAVAC" -encoding UTF-8 -source 17 -target 17 -Xlint:-options \
  -d "$OUT/test-classes" \
  "$ROOT/java/energy/octo/calc/CalcEngine.java" \
  "$ROOT/test/energy/octo/calc/CalcEngineTest.java"
"$JAVA" -cp "$OUT/test-classes" energy.octo.calc.CalcEngineTest

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
"$JAVAC" -encoding UTF-8 -source 17 -target 17 -Xlint:-options \
  -classpath "$PLATFORM" \
  -d "$OUT/classes" @"$OUT/sources.txt"

find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" @"$OUT/classes.txt"

cp "$OUT/unsigned.apk" "$OUT/app.apk"
(cd "$OUT/dex" && zip -q "$OUT/app.apk" classes.dex)

"$BT/zipalign" -f -p 4 "$OUT/app.apk" "$OUT/app-aligned.apk"

KS="${OCTOCALC_KEYSTORE:-$HOME/.android/octocalc.keystore}"
KS_PASS="${OCTOCALC_KS_PASS:-octocalc}"
KEY_PASS="${OCTOCALC_KEY_PASS:-octocalc}"
KEY_ALIAS="${OCTOCALC_KEY_ALIAS:-octocalc}"
if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  "$KEYTOOL" -genkeypair -v -keystore "$KS" -storepass "$KS_PASS" -keypass "$KEY_PASS" \
    -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Octo Calc, OU=Personal, O=OctoCalc, L=Paris, C=FR" >/dev/null 2>&1
fi

"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KEY_PASS" \
  --ks-key-alias "$KEY_ALIAS" --out "$ROOT/OctoCalc.apk" "$OUT/app-aligned.apk"

"$BT/apksigner" verify "$ROOT/OctoCalc.apk"
echo "APK: $ROOT/OctoCalc.apk"
