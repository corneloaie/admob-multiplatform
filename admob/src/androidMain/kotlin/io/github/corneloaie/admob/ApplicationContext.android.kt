package io.github.corneloaie.admob

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

@Volatile
private var applicationContext: Context? = null

/** The application Context, which [AdMobInitProvider] stores before `Application.onCreate`. */
internal fun applicationContext(): Context = applicationContext ?: error(
    "io.github.corneloaie:admob has no Context: its AdMobInitProvider didn't run. Is it removed from the merged manifest?",
)

/** Runs at app startup, as the SDKs' own providers do, to keep the application Context. */
internal class AdMobInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
