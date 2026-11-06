# Smooth60 — Android prototype

This is a starter project, not a finished video converter. It contains:
- A native Kotlin + Jetpack Compose shell.
- Android video picker.
- A RIFE ONNX single-frame interpolation runner for a specific ONNX input contract.

It does not yet export a complete 4K60 video. A correct converter also needs a streaming decoder, frame queue, RIFE inference, hardware encoder, and audio muxer pipeline.

## Open the project
1. Install Android Studio and Android SDK 35.
2. Open this repository in Android Studio and let Gradle sync.
3. Connect an Android phone with USB debugging enabled.
4. Run the app configuration.

## RIFE model contract
The included runner expects ONNX inputs img0 and img1 as float32 [1,3,H,W] RGB NCHW in 0..1, plus timestep float32 [1]. Output is float32 [1,3,H,W]. Model files are not bundled. Check the model license and model card before use.

## Remaining video pipeline
A production converter still needs MediaExtractor decoding, bounded frame buffering, RIFE midpoint inference, MediaCodec encoding, MediaMuxer audio handling, variable-frame-rate and rotation handling, cancellation, thermal management, and encoder capability checks. Do not assume a phone supports 4K60 encoding without querying its codec capabilities.

## Limitations
- This starter does not yet export processed video.
- RIFE can create artifacts around occlusions, fast motion, flashes, and scene cuts.
- 4K60 support depends on device hardware and thermal conditions.

References:
- https://onnxruntime.ai/docs/tutorials/mobile/
- https://onnxruntime.ai/docs/execution-providers/NNAPI-ExecutionProvider
- https://huggingface.co/edgetools/rife

## Build APK
GitHub Actions builds a debug APK on pushes to main/master and via manual dispatch. Open Actions, select Build Android APK, then download the Smooth60-debug-apk artifact from a successful run.
