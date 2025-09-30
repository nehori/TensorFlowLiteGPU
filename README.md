# TensorFlow Lite GPU Test for Android

Android 端末でTensorFlow LiteのGPU Delegate機能とOpenCLサポートを検証するためのサンプルアプリケーションです。

## 概要

このアプリは以下の機能を提供します:

- **OpenCL検出**: Android 端末でのOpenCLライブラリの存在確認
- **GPU情報取得**: Mali GPUなどのハードウェア情報の表示
- **TensorFlow Lite GPU Delegate対応確認**: GPU アクセラレーションの利用可能性チェック
- **性能テスト**: CPU vs GPU の実行時間比較とスピードアップ測定

## 検証済み環境

- **Android**: API 23以上 
- **GPU**: Mali-G57、Mali-G52など 
- **OpenCL**: `/vendor/lib/libOpenCL.so` 対応端末

## 実行結果例

```
OpenCL Available: true
Library Path: /vendor/lib/libOpenCL.so
GPU Info: Mali-G57

TensorFlow Lite GPU Delegate: Supported
GPU利用可能: Yes

CPU推論時間: 148ms
GPU推論時間: 124ms
スピードアップ: 1.19倍
🚀 GPU高速化成功! 
```

## セットアップ

### 1. プロジェクトのクローン

```bash
git clone https://github.com/yourusername/TensorFlowLiteGPU.git
cd TensorFlowLiteGPU
```

### 2. Android Studioでプロジェクトを開く

1. Android Studio を起動
2. "Open an existing Android Studio project" を選択
3. クローンしたフォルダを選択

### 3. 依存関係の確認

`app/build.gradle.kts` で以下の依存関係が設定されていることを確認:

```kotlin
dependencies {
 implementation("androidx.appcompat:appcompat:1.6.1")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
 implementation("org.tensorflow:tensorflow-lite:2.14.0")
 implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
 implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
 implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")
 implementation("androidx.leanback:leanback:1.0.0")
 implementation(libs.androidx.core.ktx)
}
```

### 4. ビルドと実行

1. プロジェクトをビルド: `Build` → `Make Project`
2. Android 端末またはエミュレータで実行

## ログ確認

### adb コマンドでログ監視

```bash
# TensorFlow Lite関連ログ
adb logcat | grep -E "(GPUTest|OpenCL|TFLite)"

# OpenCLライブラリ確認
adb shell find /vendor /system -name "libOpenCL.so" 2>/dev/null

# GPU情報確認
adb shell dumpsys | grep GLES
```

### 期待されるログ出力

```
D/OpenCL: Found: /vendor/lib/libOpenCL.so
D/GPUTest: GPU delegate created successfully
D/GPUTest: CPU thread task time: 148ms
D/GPUTest: GPU thread task time: 124ms
```

## Download Pre-built APK

**[Releases page](https://github.com/nehori/TensorFlowLiteGPU/releases)** から APK をダウンロードしてAndroid 端末またはエミュレータで実行

## ライセンス

MIT License
