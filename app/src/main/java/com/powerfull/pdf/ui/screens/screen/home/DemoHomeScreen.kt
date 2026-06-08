package com.powerfull.pdf.ui.screens.screen.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powerfull.pdf.R

/**
 * Sample "home" screen for the PDF Viewer SDK demo: lets the user pick a PDF
 * from the device and hands the chosen [Uri] to [onPdfPicked] to open the viewer.
 */
@Composable
fun DemoHomeScreen(onPdfPicked: (Uri) -> Unit) {
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPdfPicked)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_pdf),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )

        Text(
            text = "PDF Viewer SDK Demo",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = "Pick a PDF to open it with the PdfViewer composable from the SDK.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = { pickPdf.launch(arrayOf("application/pdf")) },
            shape = RoundedCornerShape(50)
        ) {
            Text("Open PDF")
        }
    }
}
