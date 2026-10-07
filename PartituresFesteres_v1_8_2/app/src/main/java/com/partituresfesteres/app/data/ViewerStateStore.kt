package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri

class ViewerStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPage(uri: Uri): Int = prefs.getInt(pageKey(uri), 0).coerceAtLeast(0)

    fun loadZoom(uri: Uri): Float = prefs.getFloat(zoomKey(uri), 1f).coerceIn(1f, 3f)

    fun savePage(uri: Uri, page: Int) {
        prefs.edit().putInt(pageKey(uri), page.coerceAtLeast(0)).apply()
    }

    fun saveZoom(uri: Uri, zoom: Float) {
        prefs.edit().putFloat(zoomKey(uri), zoom.coerceIn(1f, 3f)).apply()
    }

    fun clear(uri: Uri) {
        prefs.edit()
            .remove(pageKey(uri))
            .remove(zoomKey(uri))
            .apply()
    }

    fun migrateUri(oldUri: Uri, newUri: Uri) {
        if (oldUri == newUri) return
        val editor = prefs.edit()
        if (prefs.contains(pageKey(oldUri))) {
            editor.putInt(pageKey(newUri), prefs.getInt(pageKey(oldUri), 0))
            editor.remove(pageKey(oldUri))
        }
        if (prefs.contains(zoomKey(oldUri))) {
            editor.putFloat(zoomKey(newUri), prefs.getFloat(zoomKey(oldUri), 1f))
            editor.remove(zoomKey(oldUri))
        }
        editor.apply()
    }

    private fun pageKey(uri: Uri) = "viewer_page_${uri}"
    private fun zoomKey(uri: Uri) = "viewer_zoom_${uri}"

    private companion object {
        const val PREFS_NAME = "partitures_festeres_viewer"
    }
}
