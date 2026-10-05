package com.partituresfesteres.app.model

import android.net.Uri

data class LibraryFolder(
    val name: String,
    val uri: Uri,
    val directPdfCount: Int,
    val relativePath: String = "",
)
