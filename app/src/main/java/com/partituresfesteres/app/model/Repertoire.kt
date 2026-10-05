package com.partituresfesteres.app.model

import android.net.Uri

data class Repertoire(
    val id: String,
    val name: String,
    val entries: List<RepertoireEntry>,
)

data class RepertoireEntry(
    val uriString: String,
    val fileName: String,
    val displayName: String,
    val relativePath: String,
) {
    fun toLibraryPdf(): LibraryPdf = LibraryPdf(
        fileName = fileName,
        displayName = displayName,
        uri = Uri.parse(uriString),
        relativePath = relativePath,
    )

    companion object {
        fun fromPdf(pdf: LibraryPdf): RepertoireEntry = RepertoireEntry(
            uriString = pdf.uri.toString(),
            fileName = pdf.fileName,
            displayName = pdf.displayName,
            relativePath = pdf.relativePath,
        )
    }
}
