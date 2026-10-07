package com.partituresfesteres.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.partituresfesteres.app.data.LanguageManager
import com.partituresfesteres.app.ui.theme.PartituresFesteresTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    private var incomingPdfUri by mutableStateOf<Uri?>(null)
    private var resumeRevision by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp >= 600) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        incomingPdfUri = extractPdfUri(intent)

        setContent {
            PartituresFesteresTheme {
                AdaptiveWindowProvider {
                    PartituresFesteresApp(
                        incomingPdfUri = incomingPdfUri,
                        onIncomingPdfConsumed = { incomingPdfUri = null },
                        refreshToken = resumeRevision,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeRevision += 1
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingPdfUri = extractPdfUri(intent)
    }

    @Suppress("DEPRECATION")
    private fun extractPdfUri(intent: Intent?): Uri? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
    }
}
