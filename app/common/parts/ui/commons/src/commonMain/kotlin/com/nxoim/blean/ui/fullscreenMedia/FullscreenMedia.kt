package com.nxoim.blean.ui.fullscreenMedia

sealed interface FullScreenMedia {
    data class Image(
        val thumb: String,
        val fullsize: String,
        val altText: String?,
        val widthPx: Int?,
        val heightPx: Int?,
    ) : FullScreenMedia

    data class Video(
        val playlistUri: String,
        val thumbnail: String?,
        val altText: String?,
        val widthPx: Int?,
        val heightPx: Int?,
        val captionUris: List<String>?
    ) : FullScreenMedia
}