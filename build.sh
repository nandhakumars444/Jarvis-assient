#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# JARVIS Android – Build Script
# Run:  chmod +x build.sh && ./build.sh
# Requires: JDK 17, Android SDK (set ANDROID_HOME), or just Android Studio.
# ─────────────────────────────────────────────────────────────────────────────
set -e

echo ""
echo "  ╔══════════════════════════════════════╗"
echo "  ║   J.A.R.V.I.S  –  Build System      ║"
echo "  ╚══════════════════════════════════════╝"
echo ""

# ── 1. Check Java ─────────────────────────────────────────────────────────────
if ! command -v java &> /dev/null; then
  echo "[ERROR] Java not found. Install JDK 17: https://adoptium.net"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
echo "[✓] Java $JAVA_VER detected"

# ── 2. Make gradlew executable ────────────────────────────────────────────────
chmod +x gradlew 2>/dev/null || true

# ── 3. Download fonts (Orbitron + Share Tech Mono) ───────────────────────────
FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

if [ ! -f "$FONT_DIR/orbitron.ttf" ]; then
  echo "[→] Downloading Orbitron font..."
  curl -sL "https://github.com/google/fonts/raw/main/ofl/orbitron/Orbitron%5Bwght%5D.ttf" \
    -o "$FONT_DIR/orbitron.ttf" || \
  echo "[WARN] Font download failed. Add orbitron.ttf manually to $FONT_DIR"
fi

if [ ! -f "$FONT_DIR/share_tech_mono.ttf" ]; then
  echo "[→] Downloading Share Tech Mono font..."
  curl -sL "https://github.com/google/fonts/raw/main/apache/sharetechmono/ShareTechMono-Regular.ttf" \
    -o "$FONT_DIR/share_tech_mono.ttf" || \
  echo "[WARN] Font download failed. Add share_tech_mono.ttf manually to $FONT_DIR"
fi

# ── 4. Build debug APK ────────────────────────────────────────────────────────
echo ""
echo "[→] Building JARVIS debug APK…"
echo "    (First build downloads Gradle + dependencies – may take 3-5 minutes)"
echo ""

./gradlew assembleDebug --no-daemon

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
  SIZE=$(du -sh "$APK_PATH" | cut -f1)
  echo ""
  echo "  ╔══════════════════════════════════════════════════╗"
  echo "  ║  ✅  BUILD SUCCESSFUL                            ║"
  echo "  ║                                                  ║"
  echo "  ║  APK → $APK_PATH"
  echo "  ║  Size: $SIZE"
  echo "  ╚══════════════════════════════════════════════════╝"
  echo ""
  echo "  Install on your phone:"
  echo "    adb install -r $APK_PATH"
  echo ""
  echo "  Or transfer the APK file to your phone and open it."
  echo "  (Enable 'Install from unknown sources' in phone settings)"
  echo ""
else
  echo "[ERROR] APK not found. Check the Gradle output above for errors."
  exit 1
fi
