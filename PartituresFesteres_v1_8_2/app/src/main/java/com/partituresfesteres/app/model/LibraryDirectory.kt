package com.partituresfesteres.app.model

data class LibraryDirectory(
    val folders: List<LibraryFolder>,
    val pdfs: List<LibraryPdf>,
)
