#!/usr/bin/env bash
# =============================================================================
# 星河软件站客户端 - 手动构建脚本（Android 1.5 / API 3，无 Gradle）
#
# 设计目标：
#   - 不依赖 Android Studio / Gradle / SDK Manager（均不支持 API 3 构建）
#   - 只用 aapt + javac(JDK8) + dx + zipalign + jarsigner 五件套
#   - 产物为 v1 签名 APK，可安装到 Android 1.5 ~ 15 全部版本
#
# 本脚本由 .github/workflows/android.yml 在 GitHub Actions 中调用，
# 也可在任意装有 JDK8 + bash + curl + unzip 的 Linux 上手动执行。
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

SDK_DIR="${ANDROID_SDK_HOME:-$HOME/android-sdk}"
PLATFORM="android-3"            # Android 1.5 (API 3)
BUILD_TOOLS="19.1.0"
PLATFORM_JAR="$SDK_DIR/platforms/$PLATFORM/android.jar"
BT_DIR="$SDK_DIR/build-tools/$BUILD_TOOLS"
AAPT="$BT_DIR/aapt"
DX="$BT_DIR/dx"
ZIPALIGN="$BT_DIR/zipalign"

PLATFORM_URL="https://dl.google.com/android/repository/android-1.5_r04-linux.zip"
PLATFORM_SHA1="5c134b7df5f4b8bd5b61ba93bdaebada8fa3468c"
BT_URL="https://dl.google.com/android/repository/build-tools_r19.1-linux.zip"

BUILD_DIR="$ROOT/build"
DIST_DIR="$ROOT/dist"
GEN_DIR="$BUILD_DIR/gen"
CLASSES_DIR="$BUILD_DIR/classes"
UNALIGNED_APK="$BUILD_DIR/app-unaligned.apk"
FINAL_APK="$DIST_DIR/xinghe-store.apk"

log() { echo -e "\n\033[1;36m==> $*\033[0m"; }

# -----------------------------------------------------------------------------
log "准备 SDK 组件（仅首次下载，之后走 Actions 缓存）"
mkdir -p "$SDK_DIR/platforms" "$SDK_DIR/build-tools"

if [ ! -f "$PLATFORM_JAR" ]; then
    echo "下载 Android 1.5 平台 ..."
    tmpzip="$BUILD_DIR/android15.zip"
    mkdir -p "$BUILD_DIR"
    curl -fsSL -o "$tmpzip" "$PLATFORM_URL"
    echo "$PLATFORM_SHA1  $tmpzip" | sha1sum -c -
    rm -rf "$SDK_DIR/platforms/$PLATFORM" "$SDK_DIR/tmp-android15"
    mkdir -p "$SDK_DIR/tmp-android15"
    unzip -q -o "$tmpzip" -d "$SDK_DIR/tmp-android15"
    # 旧版平台包解出来直接是 android.jar
    inner="$(find "$SDK_DIR/tmp-android15" -maxdepth 2 -name android.jar | head -1)"
    if [ -z "$inner" ]; then
        echo "错误：下载的平台包中未找到 android.jar" >&2
        exit 1
    fi
    mkdir -p "$SDK_DIR/platforms/$PLATFORM"
    cp "$inner" "$PLATFORM_JAR"
    rm -rf "$SDK_DIR/tmp-android15" "$tmpzip"
fi

if [ ! -x "$AAPT" ]; then
    echo "下载 Build-Tools 19.1.0 ..."
    tmpzip="$BUILD_DIR/build-tools.zip"
    mkdir -p "$BUILD_DIR"
    curl -fsSL -o "$tmpzip" "$BT_URL"
    rm -rf "$BT_DIR" "$SDK_DIR/build-tools/android-4.4.2"
    unzip -q -o "$tmpzip" -d "$SDK_DIR/build-tools"
    # 该 zip 的解包根目录名是 android-4.4.2，统一改名为版本号目录
    if [ -d "$SDK_DIR/build-tools/android-4.4.2" ]; then
        mv "$SDK_DIR/build-tools/android-4.4.2" "$BT_DIR"
    fi
    chmod +x "$BT_DIR"/aapt "$BT_DIR"/dx "$BT_DIR"/zipalign 2>/dev/null || true
    rm -f "$tmpzip"
fi

# -----------------------------------------------------------------------------
log "清理旧产物"
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$GEN_DIR" "$CLASSES_DIR" "$DIST_DIR"

log "aapt：编译资源并生成 R.java"
"$AAPT" package -f -m -J "$GEN_DIR" \
    -M AndroidManifest.xml \
    -S res \
    -I "$PLATFORM_JAR"

log "javac：编译 Java 源码（bootclasspath = Android 1.5）"
find src -name '*.java' > "$BUILD_DIR/sources.txt"
echo "$GEN_DIR/com/xinghe/store/R.java" >> "$BUILD_DIR/sources.txt"
javac -nowarn -encoding UTF-8 \
    -source 6 -target 6 \
    -bootclasspath "$PLATFORM_JAR" \
    -d "$CLASSES_DIR" \
    @"$BUILD_DIR/sources.txt"

log "dx：classes -> classes.dex"
"$DX" --dex --output="$BUILD_DIR/classes.dex" "$CLASSES_DIR"

log "aapt：打包未签名 APK（资源 + dex）"
"$AAPT" package -f \
    -M AndroidManifest.xml \
    -S res \
    -I "$PLATFORM_JAR" \
    -F "$UNALIGNED_APK"
( cd "$BUILD_DIR" && "$AAPT" add "$(basename "$UNALIGNED_APK")" classes.dex )

log "zipalign：4 字节对齐"
"$ZIPALIGN" -f 4 "$UNALIGNED_APK" "$DIST_DIR/app-aligned.apk"

# -----------------------------------------------------------------------------
log "jarsigner：v1 签名（API 3 设备唯一支持的签名方案）"
KEYSTORE_DIR="$BUILD_DIR/keystore"
KEYSTORE="$KEYSTORE_DIR/xinghe-release.jks"
ALIAS="${XINGHE_KEY_ALIAS:-xinghe}"
KEY_PASS="${XINGHE_KEY_PASSWORD:-xinghe123}"
STORE_PASS="${XINGHE_KEYSTORE_PASSWORD:-xinghe123}"

if [ -n "${XINGHE_KEYSTORE_B64:-}" ]; then
    # CI：从 GitHub Secrets 注入发布签名密钥
    mkdir -p "$KEYSTORE_DIR"
    printf '%s' "$XINGHE_KEYSTORE_B64" | base64 -d > "$KEYSTORE"
    echo "使用注入的发布签名密钥"
else
    # 本地/未配置密钥时：生成临时调试密钥（仅用于验证构建可复现）
    mkdir -p "$KEYSTORE_DIR"
    keytool -genkeypair -alias "$ALIAS" -keyalg RSA -keysize 2048 \
        -validity 10000 -storetype JKS \
        -dname "CN=Xinghe Store, OU=Client, O=Xinghe, C=CN" \
        -keystore "$KEYSTORE" -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
        >/dev/null 2>&1
    echo "使用本地临时密钥（未配置 XINGHE_KEYSTORE_B64）"
fi

cp "$DIST_DIR/app-aligned.apk" "$FINAL_APK"
jarsigner -keystore "$KEYSTORE" -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" -signedjar "$FINAL_APK" \
    "$DIST_DIR/app-aligned.apk" "$ALIAS"
rm -f "$DIST_DIR/app-aligned.apk"

# -----------------------------------------------------------------------------
log "构建信息自检"
"$AAPT" dump badging "$FINAL_APK" | grep -E "package|application-label|sdkVersion|targetSdkVersion" || true
"$BT_DIR/dexdump" -f "$BUILD_DIR/classes.dex" 2>/dev/null | head -5 || true
ls -lh "$FINAL_APK"

log "完成：$FINAL_APK"
