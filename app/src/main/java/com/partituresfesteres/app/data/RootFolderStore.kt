package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri

class RootFolderStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri) {
        prefs.edit().putString(KEY_ROOT_URI, uri.toString()).apply()
    }

    fun load(): Uri? = prefs.getString(KEY_ROOT_URI, null)?.let(Uri::parse)

    fun clear() {
        prefs.edit().remove(KEY_ROOT_URI).apply()
    }

    private companion object {
        const val PREFS_NAME = "partitures_festeres"
        const val KEY_ROOT_URI = "root_folder_uri"
    }
}
