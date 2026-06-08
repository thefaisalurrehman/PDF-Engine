package com.powerfull.pdf

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.powerfull.pdf.ui.screens.screen.home.DemoHomeScreen
import com.powerfull.pdf.ui.screens.screen.viewer.DemoViewerScreen
import com.powerfull.pdf.ui.theme.PDFCoreTheme

/**
 * Hosts the SDK demo: a "pick a PDF" home screen and a viewer screen built on
 * top of the PdfViewer composable from the :pdfviewer SDK module.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PDFCoreTheme {
                DemoApp(initialPdfUri = intent?.takeIf { it.action == android.content.Intent.ACTION_VIEW }?.data)
            }
        }
    }
}

private sealed interface DemoRoute {
    data object Home : DemoRoute
    data class Viewer(val uri: Uri) : DemoRoute
}

@Composable
private fun DemoApp(initialPdfUri: Uri?) {
    var route by remember { mutableStateOf<DemoRoute>(DemoRoute.Home) }

    LaunchedEffect(initialPdfUri) {
        initialPdfUri?.let { route = DemoRoute.Viewer(it) }
    }

    when (val current = route) {
        is DemoRoute.Home -> DemoHomeScreen(
            onPdfPicked = { uri -> route = DemoRoute.Viewer(uri) }
        )

        is DemoRoute.Viewer -> DemoViewerScreen(
            uri = current.uri,
            fileName = current.uri.lastPathSegment ?: "Document.pdf",
            backPress = { route = DemoRoute.Home }
        )
    }
}
