package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.partituresfesteres.app.R
import com.partituresfesteres.app.model.LibraryDirectory
import com.partituresfesteres.app.model.LibraryFolder
import com.partituresfesteres.app.model.LibraryPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepository(private val context: Context) {

    suspend fun readDirectory(directoryUri: Uri): Result<LibraryDirectory> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = openDirectory(directoryUri)
            val children = directory.listFiles()

            val folders = children
                .asSequence()
                .filter { it.isDirectory }
                .map { folder ->
                    LibraryFolder(
                        name = folder.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.folder_unnamed),
                        uri = folder.uri,
                        directPdfCount = runCatching {
                            folder.listFiles().count(::isPdf)
                        }.getOrDefault(0),
                    )
                }
                .sortedBy { it.name.lowercase() }
                .toList()

            val pdfs = children
                .asSequence()
                .filter(::isPdf)
                .map(::toLibraryPdf)
                .sortedBy { it.displayName.lowercase() }
                .toList()

            LibraryDirectory(folders = folders, pdfs = pdfs)
        }
    }

    /**
     * Construeix un índex de tots els PDF de la biblioteca, recorrent totes les
     * subcarpetes una sola vegada. Després la cerca es fa en memòria mentre
     * l'usuari escriu, evitant rellegir l'emmagatzematge en cada tecla.
     */
    suspend fun readAllPdfs(rootUri: Uri): Result<List<LibraryPdf>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = openDirectory(rootUri)
            val result = mutableListOf<LibraryPdf>()

            fun visit(directory: DocumentFile, path: List<String>) {
                directory.listFiles().forEach { child ->
                    when {
                        child.isDirectory -> {
                            val name = child.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.folder_unnamed)
                            visit(child, path + name)
                        }
                        isPdf(child) -> {
                            result += toLibraryPdf(
                                file = child,
                                relativePath = path.joinToString(" › "),
                            )
                        }
                    }
                }
            }

            visit(root, emptyList())
            result.sortedWith(
                compareBy<LibraryPdf> { it.displayName.lowercase() }
                    .thenBy { it.relativePath.lowercase() }
            )
        }
    }

    suspend fun readAllFolders(rootUri: Uri): Result<List<LibraryFolder>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = openDirectory(rootUri)
            val result = mutableListOf<LibraryFolder>()

            fun visit(directory: DocumentFile, path: List<String>) {
                directory.listFiles()
                    .filter { it.isDirectory }
                    .sortedBy { it.name?.lowercase().orEmpty() }
                    .forEach { child ->
                        val name = child.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.folder_unnamed)
                        val childPath = path + name
                        result += LibraryFolder(
                            name = name,
                            uri = child.uri,
                            directPdfCount = runCatching { child.listFiles().count(::isPdf) }.getOrDefault(0),
                            relativePath = childPath.joinToString(" › "),
                        )
                        visit(child, childPath)
                    }
            }

            visit(root, emptyList())
            result
        }
    }

    suspend fun pdfExists(directoryUri: Uri, fileName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            openDirectory(directoryUri).listFiles().any { child ->
                isPdf(child) && child.name?.equals(fileName, ignoreCase = true) == true
            }
        }.getOrDefault(false)
    }

    suspend fun savePdf(
        sourceUri: Uri,
        directoryUri: Uri,
        requestedName: String,
        relativePath: String = "",
        replaceExisting: Boolean = false,
        keepBoth: Boolean = false,
    ): Result<LibraryPdf> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = openDirectory(directoryUri)
            val baseName = requestedName.trim().ifBlank { context.getString(R.string.new_score) }
                .removeSuffixIgnoreCase(".pdf")
            var finalName = "$baseName.pdf"

            val existing = directory.listFiles().firstOrNull { child ->
                isPdf(child) && child.name?.equals(finalName, ignoreCase = true) == true
            }

            if (existing != null) {
                when {
                    replaceExisting -> {
                        if (!existing.delete()) error(context.getString(R.string.repo_replace_pdf_error))
                    }
                    keepBoth -> {
                        var suffix = 2
                        do {
                            finalName = "$baseName ($suffix).pdf"
                            suffix += 1
                        } while (directory.listFiles().any { child ->
                                isPdf(child) && child.name?.equals(finalName, ignoreCase = true) == true
                            })
                    }
                    else -> error(context.getString(R.string.repo_duplicate_name_error))
                }
            }

            val created = directory.createFile("application/pdf", finalName)
                ?: error(context.getString(R.string.repo_create_pdf_error))

            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(created.uri, "w")?.use { output ->
                        input.copyTo(output)
                    } ?: error(context.getString(R.string.repo_write_pdf_error))
                } ?: error(context.getString(R.string.repo_read_source_error))
            } catch (t: Throwable) {
                created.delete()
                throw t
            }

            toLibraryPdf(created, relativePath = relativePath)
        }
    }


    suspend fun renamePdf(pdf: LibraryPdf, requestedName: String): Result<LibraryPdf> = withContext(Dispatchers.IO) {
        runCatching {
            val baseName = requestedName.trim().ifBlank { error(context.getString(R.string.repo_empty_name_error)) }
                .removeSuffixIgnoreCase(".pdf")
            val finalName = "$baseName.pdf"
            if (finalName.equals(pdf.fileName, ignoreCase = true)) return@runCatching pdf

            val resolver = context.contentResolver
            val renamedUri = runCatching {
                DocumentsContract.renameDocument(resolver, pdf.uri, finalName)
            }.getOrNull() ?: run {
                val document = DocumentFile.fromSingleUri(context, pdf.uri)
                    ?: error(context.getString(R.string.repo_open_score_error))
                if (!document.renameTo(finalName)) error(context.getString(R.string.repo_rename_error))
                document.uri
            }

            val renamedDocument = DocumentFile.fromSingleUri(context, renamedUri)
            if (renamedDocument != null && renamedDocument.exists()) {
                toLibraryPdf(renamedDocument, pdf.relativePath)
            } else {
                LibraryPdf(
                    fileName = finalName,
                    displayName = baseName,
                    uri = renamedUri,
                    relativePath = pdf.relativePath,
                )
            }
        }
    }

    suspend fun movePdf(
        pdf: LibraryPdf,
        destinationUri: Uri,
        destinationRelativePath: String,
        replaceExisting: Boolean = false,
        keepBoth: Boolean = false,
    ): Result<LibraryPdf> = withContext(Dispatchers.IO) {
        runCatching {
            if (pdf.relativePath == destinationRelativePath) {
                error(context.getString(R.string.score_already_folder))
            }

            val copied = savePdf(
                sourceUri = pdf.uri,
                directoryUri = destinationUri,
                requestedName = pdf.fileName,
                relativePath = destinationRelativePath,
                replaceExisting = replaceExisting,
                keepBoth = keepBoth,
            ).getOrThrow()

            val deleted = runCatching {
                DocumentsContract.deleteDocument(context.contentResolver, pdf.uri)
            }.getOrDefault(false) || runCatching {
                DocumentFile.fromSingleUri(context, pdf.uri)?.delete() == true
            }.getOrDefault(false)

            if (!deleted) {
                runCatching { DocumentFile.fromSingleUri(context, copied.uri)?.delete() }
                error(context.getString(R.string.repo_move_cleanup_error))
            }

            copied
        }
    }

    suspend fun deletePdf(pdf: LibraryPdf): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val deleted = runCatching {
                DocumentsContract.deleteDocument(context.contentResolver, pdf.uri)
            }.getOrDefault(false) || runCatching {
                DocumentFile.fromSingleUri(context, pdf.uri)?.delete() == true
            }.getOrDefault(false)
            if (!deleted) error(context.getString(R.string.repo_delete_error))
        }
    }

    private fun openDirectory(uri: Uri): DocumentFile {
        val directory = DocumentFile.fromTreeUri(context, uri)
            ?: DocumentFile.fromSingleUri(context, uri)
            ?: error(context.getString(R.string.repo_open_folder_error))

        if (!directory.exists() || !directory.isDirectory) {
            error(context.getString(R.string.repo_folder_unavailable))
        }
        return directory
    }

    private fun toLibraryPdf(file: DocumentFile, relativePath: String = ""): LibraryPdf {
        val fileName = file.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.default_score_filename)
        return LibraryPdf(
            fileName = fileName,
            displayName = fileName.removeSuffixIgnoreCase(".pdf"),
            uri = file.uri,
            relativePath = relativePath,
        )
    }

    private fun isPdf(file: DocumentFile): Boolean =
        file.isFile && (
            file.type?.equals("application/pdf", ignoreCase = true) == true ||
                file.name?.endsWith(".pdf", ignoreCase = true) == true
            )

    private fun String.removeSuffixIgnoreCase(suffix: String): String =
        if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this
}
