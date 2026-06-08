# PDF Engine

A lightweight, **100% Kotlin** PDF rendering library for Android with a first-class
**Jetpack Compose** API. Drop a `PdfViewer(...)` composable into your app and get
pinch-to-zoom, smooth scrolling, page-by-page mode, dark mode, and a draggable scroll
handle — backed by the native [PDFium](https://pdfium.googlesource.com/pdfium/) engine,
with **zero** Room / Koin / Firebase / AdMob baggage.

> Rendering is powered by a bundled native PDFium engine. The whole stack — the view
> engine, the coroutine-based decoder, and the Compose layer — is written in Kotlin.

---

## Features

- 🎯 **Compose-first API** — one `PdfViewer(...)` composable, plus a hoistable `PdfViewerState`.
- 🔍 **Pinch-zoom & double-tap** zoom, crisp `ARGB_8888` tile rendering.
- ↕️ **Continuous scroll** or **page-by-page** (horizontal snap) mode.
- 🌙 **Dark mode** (night-mode color inversion).
- 📐 **Fit policies** — fit width, height, or whole page.
- 🧭 **Scroll handle** with page indicator.
- 📦 **Multiple sources** — `Uri`, `File`, asset, `ByteArray`, `InputStream`.
- ⚡ **Modern internals** — `AsyncTask`/`Thread` replaced with **Kotlin coroutines + Flow**.
- 🪶 **Dependency-light** — only Compose + coroutines + the bundled native engine.

---

## Module structure

```
PDF-Engine/
├── app/                         # Demo app (com.powerfull.pdf) — shows how to consume the SDK
│   └── src/main/java/com/powerfull/pdf/
│       ├── MainActivity.kt          # Hosts Compose nav; forwards ACTION_VIEW (application/pdf) intents
│       ├── ui/screens/screen/home/DemoHomeScreen.kt      # "Open PDF" file picker
│       └── ui/screens/screen/viewer/DemoViewerScreen.kt  # Thin toolbar wrapped around PdfViewer(...)
│
└── pdfengine/                   # The SDK module — published as com.faisal:pdfengine
    └── src/main/
        ├── java/com/faisal/pdfengine/          # Public Kotlin engine + Compose API
        │   ├── compose/                            # PdfViewer, PdfViewerState, PdfSource  ← public API
        │   ├── PDFView.kt                          # Core View-based engine
        │   ├── PagesLoader.kt / RenderingHandler.kt / CacheManager.kt / PdfFile.kt
        │   ├── DocumentDecoder.kt                  # Coroutine-based decoder (replaces AsyncTask)
        │   ├── source/ link/ listener/ model/ scroll/ util/ exception/
        │
        ├── java/com/shockwave/pdfium/           # Native PDFium JNI bridge (Kotlin)
        │   └── PdfiumCore / PdfDocument / util/Size, SizeF
        │
        └── jniLibs/                             # Prebuilt native libraries (.so)
            ├── arm64-v8a/  armeabi-v7a/  x86/  x86_64/
            │   └── libjniPdfium.so, libmodpdfium.so, libmodft2.so, libmodpng.so, libc++_shared.so
```

Two Gradle modules: **`:app`** (the demo) and **`:pdfengine`** (the self-contained SDK).
The native engine lives in the `com.shockwave.pdfium` package — its name is **fixed** because
the prebuilt `.so` files resolve JNI symbols by that exact class path.

- **Min SDK:** 26
- **Language:** Kotlin (JVM target 11)
- **UI:** Jetpack Compose

---

## Installation

The SDK is published with the `maven-publish` plugin as:

```
com.faisal:pdfengine:1.0.0
```

### Option A — via JitPack

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

`build.gradle.kts` (app module):

```kotlin
dependencies {
    implementation("com.github.thefaisalurrehman:PDF-Engine:1.0.0") // JitPack coordinate
}
```

### Option B — local Maven / source

Publish to your local Maven repo and depend on the coordinate:

```bash
./gradlew :pdfengine:publishReleasePublicationToMavenLocal
```

```kotlin
dependencies {
    implementation("com.faisal:pdfengine:1.0.0")
}
```

Or just include the module directly in a multi-module project:

```kotlin
// settings.gradle.kts
include(":pdfengine")

// app/build.gradle.kts
dependencies { implementation(project(":pdfengine")) }
```

---

## Quick start

```kotlin
import com.faisal.pdfengine.compose.PdfSource
import com.faisal.pdfengine.compose.PdfViewer

@Composable
fun MyScreen(uri: Uri) {
    PdfViewer(
        source = PdfSource.fromUri(uri),
        modifier = Modifier.fillMaxSize(),
    )
}
```

That's it — pinch-zoom, scrolling, and the scroll handle all work out of the box.

### With state and callbacks

```kotlin
import com.faisal.pdfengine.compose.rememberPdfViewerState

@Composable
fun MyScreen(uri: Uri) {
    val state = rememberPdfViewerState()
    var darkMode by remember { mutableStateOf(false) }

    Column {
        // Page indicator driven by the hoisted state
        Text("Page ${state.currentPage + 1} / ${state.pageCount}")

        PdfViewer(
            source = PdfSource.fromUri(uri),
            state = state,
            isDarkMode = darkMode,
            isPageByPage = false,
            onPageChanged = { page, count -> /* persist reading progress */ },
            onTap = { /* toggle a fullscreen toolbar */ },
            onError = { it.printStackTrace() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    // Drive the viewer imperatively from anywhere:
    // state.jumpToPage(10)
    // state.resetZoom()
}
```

---

## API reference

### `PdfViewer(...)`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `source` | `PdfSource` | — | Where the document comes from (see below). |
| `modifier` | `Modifier` | `Modifier` | Standard Compose modifier. |
| `state` | `PdfViewerState` | `rememberPdfViewerState()` | Observe page/zoom and control the viewer. |
| `isDarkMode` | `Boolean` | `false` | Inverts colors for night reading. |
| `isPageByPage` | `Boolean` | `false` | Horizontal page-snap mode instead of continuous scroll. |
| `fitPolicy` | `FitPolicy` | `WIDTH` | `WIDTH`, `HEIGHT`, or `BOTH`. |
| `isBestQuality` | `Boolean` | `true` | `ARGB_8888` tiles (sharp) vs `RGB_565` (less memory). |
| `showScrollHandle` | `Boolean` | `true` | Draggable scroll indicator with page number. |
| `backgroundColor` | `Color` | `0xFF222222` | Color behind/between pages. |
| `onTap` | `() -> Unit` | `{}` | Document tapped. |
| `onPageChanged` | `(page: Int, pageCount: Int) -> Unit` | `{ _, _ -> }` | Visible page changed (zero-based). |
| `onZoomChanged` | `(zoom: Float) -> Unit` | `{}` | Zoom level changed. |
| `onLoadComplete` | `(pageCount: Int) -> Unit` | `{}` | Document finished loading. |
| `onError` | `(Throwable) -> Unit` | `{}` | Load/render failure. |

### `PdfSource`

```kotlin
PdfSource.fromUri(uri: Uri)
PdfSource.fromFile(file: File)
PdfSource.fromAsset(assetName: String)
PdfSource.fromBytes(bytes: ByteArray)
PdfSource.fromStream(stream: InputStream)
```

### `PdfViewerState` (via `rememberPdfViewerState(initialPage = 0)`)

| Member | Description |
|---|---|
| `currentPage: Int` | Current zero-based page (observable). |
| `pageCount: Int` | Total pages (observable). |
| `zoom: Float` | Current zoom level (observable). |
| `isLoaded: Boolean` | True once the document is ready. |
| `jumpToPage(page: Int, withAnimation: Boolean = false)` | Jump to a page; queued if called before load. |
| `resetZoom()` | Animate back to the default fit. |

---

## Demo app

The `:app` module is a minimal, intentionally-thin example of integrating the SDK:

- **`DemoHomeScreen`** — an "Open PDF" button using `ActivityResultContracts.OpenDocument()`.
- **`DemoViewerScreen`** — a toolbar (title, page indicator, dark-mode and page-by-page toggles)
  wrapped around `PdfViewer(...)`; everything below the toolbar is the SDK.
- **`MainActivity`** — forwards external `ACTION_VIEW` / `application/pdf` intents straight into
  the viewer (so the app also opens PDFs shared from other apps).

### Build & run

```bash
./gradlew :app:assembleDebug          # build the demo
./gradlew :pdfengine:assembleRelease  # build the SDK (.aar)
```

---

## License & credits

Licensed under the **Apache License 2.0** — see [`LICENSE`](LICENSE) and
[`pdfengine/NOTICE`](pdfengine/NOTICE).

This project is a Kotlin rewrite derived from, and credits, the following Apache-2.0 works:

- **[barteksc/AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)** — the original
  view engine (tiling, caching, gestures, scroll handle, fit policies).
- **[barteksc/PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid)** — the native PDFium
  bindings (`com.shockwave.pdfium`) and prebuilt `.so` libraries.
- **[PDFium](https://pdfium.googlesource.com/pdfium/)** — Google / Chromium, BSD-3-Clause.

The Kotlin conversion, the `AsyncTask`/`Thread` → coroutines migration, the Jetpack Compose
API, and the single-module packaging are original work by the PDF Engine author.
