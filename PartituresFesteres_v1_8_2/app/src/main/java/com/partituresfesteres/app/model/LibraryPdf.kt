package com.partituresfesteres.app.model

import android.net.Uri

data class LibraryPdf(
    val fileName: String,
    val displayName: String,
    val uri: Uri,
    /** Ruta relativa a la carpeta arrel. Buida quan no és necessària. */
    val relativePath: String = "",
)
