package com.powerfull.pdf.ui.screens.screen.viewer

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powerfull.pdf.R
import com.powerfull.pdf.ui.theme.Gray
import com.faisal.pdfengine.compose.PdfSource
import com.faisal.pdfengine.compose.PdfViewer
import com.faisal.pdfengine.compose.rememberPdfViewerState

/**
 * Shows how to integrate the PDF Viewer SDK: a thin toolbar/page-indicator
 * chrome wrapped around [PdfViewer]. Everything below the toolbar — rendering,
 * zoom, swipe, dark mode, page-by-page mode — is handled by the SDK; this screen
 * only owns the UI chrome and forwards user choices straight to [PdfViewer]'s params.
 */
@Composable
fun DemoViewerScreen(uri: Uri, fileName: String, backPress: () -> Unit) {
    var isDarkMode by remember { mutableStateOf(false) }
    var isPageByPage by remember { mutableStateOf(false) }
    val state = rememberPdfViewerState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(White)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = backPress)
            )
            Text(
                text = fileName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Text(
                text = "${state.currentPage + 1}/${state.pageCount}",
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { isPageByPage = !isPageByPage }) {
                Icon(
                    imageVector = if (isPageByPage) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz,
                    contentDescription = "Toggle page-by-page mode"
                )
            }
            IconButton(onClick = { isDarkMode = !isDarkMode }) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle dark mode"
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Gray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PdfViewer(
                source = PdfSource.fromUri(uri),
                modifier = Modifier.fillMaxSize(),
                state = state,
                isDarkMode = isDarkMode,
                isPageByPage = isPageByPage,
            )
        }
    }
}
