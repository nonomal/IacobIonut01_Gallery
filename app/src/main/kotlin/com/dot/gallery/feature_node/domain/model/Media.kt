/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.parcelize.Parcelize
import java.io.File


@Parcelize
sealed class MediaItem : Parcelable {
    abstract val key: String

    data class Header(
        override val key: String,
        val text: String,
        val data: List<Media>
    ) : MediaItem()

    @Parcelize
    sealed class MediaViewItem : MediaItem() {

        abstract val media: Media

        data class Loaded(
            override val key: String,
            override val media: Media,
        ) : MediaViewItem()
    }
}

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

@Parcelize
data class Media(
    val id: Long = 0,
    val label: String,
    val uri: Uri,
    val path: String,
    val albumID: Long,
    val albumLabel: String,
    val timestamp: Long,
    val fullDate: String,
    val mimeType: String,
    val orientation: Int,
    val favorite: Int,
    val trashed: Int,
    val duration: String? = null,
) : Parcelable {

    override fun toString(): String {
        return "$id, $path, $fullDate, $mimeType, favorite=$favorite"
    }

    /**
     * Used to determine if the Media object is not accessible
     * via MediaStore.
     * This happens when the user tries to open media from an app
     * using external sources (in our case, Gallery Media Viewer), but
     * the specific media is only available internally in that app
     * (Android/data(OR media)/com.package.name/)
     *
     * If it's readUriOnly then we know that we should expect a barebone
     * Media object with limited functionality (no favorites, trash, timestamp etc)
     */
    fun readUriOnly(): Boolean = albumID == -99L && albumLabel == ""
    companion object {
        fun createFromUri(uri: Uri): Media? {
            if (uri.path == null) return null
            val extension = uri.toString().substringAfterLast(".")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
            var timestamp = 0L
            uri.path?.let { File(it) }?.let {
                timestamp = try {
                    it.lastModified()
                } catch (_: Exception) {
                    0L
                }
            }
            var formattedDate = ""
            if (timestamp != 0L) {
                formattedDate = timestamp.getDate(Constants.FULL_DATE_FORMAT)
            }
            return Media(
                label = uri.toString().substringAfterLast("/"),
                uri = uri,
                path = uri.path.toString(),
                albumID = -99L,
                albumLabel = "",
                timestamp = timestamp,
                fullDate = formattedDate,
                mimeType = mimeType,
                favorite = 0,
                trashed = 0,
                orientation = 0
            )
        }
    }
}

@Parcelize
data class Album(
    val id: Long = 0,
    val label: String,
    val pathToThumbnail: String,
    val timestamp: Long,
    var count: Long = 0,
    val selected: Boolean = false,
    val isPinned: Boolean = false,
) : Parcelable

@Entity(tableName = "pinned_table")
data class PinnedAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long
)

class InvalidMediaException(message: String) : Exception(message)
