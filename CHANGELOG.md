# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-06-08

First public release.

### Added
- **Jetpack Compose API** as the primary entry point:
  - `PdfViewer(...)` composable with dark mode, page-by-page mode, fit policies,
    best-quality rendering, scroll handle, and tap/page/zoom/load/error callbacks.
  - `PdfViewerState` (via `rememberPdfViewerState()`) exposing observable
    `currentPage` / `pageCount` / `zoom` / `isLoaded` and `jumpToPage()` / `resetZoom()`.
  - `PdfSource` with `fromUri` / `fromFile` / `fromAsset` / `fromBytes` / `fromStream`.
- Bundled native PDFium engine (`com.shockwave.pdfium` + `jniLibs` for
  arm64-v8a, armeabi-v7a, x86, x86_64) inside the single `:pdfengine` module.
- Minimal `:app` demo (file picker, viewer toolbar, external `ACTION_VIEW` intent support).
- `maven-publish` configuration and JitPack support (`jitpack.yml`, JDK 17).

### Changed
- **Rewrote the entire engine from Java to Kotlin** (PDFView, PagesLoader,
  RenderingHandler, CacheManager, PdfFile, sources, listeners, scroll handle, utils).
- **Migrated the document decoder from `AsyncTask` to Kotlin coroutines**
  (`DocumentDecoder`, `WeakReference`-safe, cancellable).
- Converted Java getters/setters to idiomatic Kotlin properties and renamed
  members for clarity (e.g. `isSwipeEnabled`, `isPageFlingEnabled`, `nightMode`).
- **Merged the former `:pdfiumAndroid` module into `:pdfengine`** for a single,
  self-contained, publishable library.
- Renamed package `com.mobarok.pdfviewer` → `com.faisal.pdfengine`
  (the native `com.shockwave.pdfium` package is retained because the prebuilt
  `.so` files resolve JNI symbols by that exact class path).

### Fixed
- Page tiles rendering at thumbnail resolution when zoomed/scrolled: the tile
  render range used `kotlin.math.min` instead of the libGDX-style `MathUtils.min`
  clamp, collapsing the visible column/row range so only the top-left tile rendered
  at full quality. Restored the correct clamp so the whole page renders sharply.
- Enabled `ARGB_8888` ("best quality") tile rendering by default in the Compose API.

### Credits
Derived from [barteksc/AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)
and [barteksc/PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) (Apache-2.0).
See [`pdfengine/NOTICE`](pdfengine/NOTICE).

[1.0.0]: https://github.com/thefaisalurrehman/PDF-Engine/releases/tag/1.0.0
