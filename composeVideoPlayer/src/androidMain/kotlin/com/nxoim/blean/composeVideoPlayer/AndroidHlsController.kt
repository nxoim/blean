package com.nxoim.blean.composeVideoPlayer

import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.roundToInt

// todo check for being proper
fun ExoPlayer.AndroidHlsController() = object : HLSController {
    private val tracks = this@AndroidHlsController.currentTracks

    override val availableFormatTracks = mutableListOf<HLSFormat>()
    override val availableSubtitleTracks = mutableListOf<HLSSubtitle>()

    private var _selectedFormatTrack by mutableStateOf<HLSFormat?>(null)
    override var selectedFormatTrack: HLSFormat?
        get() = _selectedFormatTrack
        set(value) {
            updateTrackSelectionForVideo(value)
            _selectedFormatTrack = value
        }

    private var _selectedSubtitleTrack by mutableStateOf<HLSSubtitle?>(null)
    override var selectedSubtitleTrack: HLSSubtitle?
        get() = _selectedSubtitleTrack
        @OptIn(UnstableApi::class)
        set(value) {
            updateTrackSelectionForSubtitle(value)
            _selectedSubtitleTrack = value
        }

    init {
        convertTracks(
            onAvailableFormatTrack = { availableFormatTracks.add(it) },
            onAvailableSubtitleTrack = { availableSubtitleTracks.add(it) },
            tracks = tracks
        )
    }

    private fun updateTrackSelectionForVideo(selected: HLSFormat?) {
        trackSelectionParameters = if (selected == null) {
            trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()
        } else {
            val track = findTrackBy(selected)
            if (track != null) {
                trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(selected.widthPx, selected.heightPx)
                    .setMinVideoSize(selected.widthPx, selected.heightPx)
                    .let {
                        if (selected.fps is VideoFPS.Static) {
                            it
                                .setMinVideoFrameRate(selected.fps.value.roundToInt())
                                .setMaxVideoFrameRate(selected.fps.value.roundToInt())
                        } else {
                            it
                        }
                    }
                    .build()
            } else {
                trackSelectionParameters
            }
        }
    }

    // Helper function to update subtitle track selection parameters.
    @OptIn(UnstableApi::class)
    private fun updateTrackSelectionForSubtitle(selected: HLSSubtitle?) {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setPreferredVideoLanguage(selected?.languageIsoCode)
            .build()
    }

    private fun findTrackBy(value: HLSFormat): Format? {
        tracks.groups.forEach { group ->
            for (formatIndex in 0 until group.length) {
                val format = group.getTrackFormat(formatIndex.coerceAtLeast(0))
                if (
                    format.width == value.widthPx &&
                    format.height == value.heightPx &&
                    format.frameRate == (value.fps as? VideoFPS.Static)?.value
                ) {
                    return format
                }
            }
        }
        return null
    }

    private fun findTrackBy(value: HLSSubtitle): Format? {
        tracks.groups.forEach { group ->
            for (formatIndex in 0 until group.length) {
                val format = group.getTrackFormat(formatIndex.coerceAtLeast(0))
                if (format.language == value.languageIsoCode) {
                    return format
                }
            }
        }
        return null
    }
}

private fun convertTracks(
    onAvailableFormatTrack: (HLSFormat) -> Unit,
    onAvailableSubtitleTrack: (HLSSubtitle) -> Unit,
    tracks: Tracks
) {
    tracks.groups.forEach { group ->
        if (group.isSupported) {
            for (formatIndex in 0 until group.length) {
                val format = group.getTrackFormat(formatIndex.coerceAtLeast(0))
                if (group.isTrackSupported(formatIndex)) {

                    if (format.width > 0 && format.height > 0) {
                        onAvailableFormatTrack(
                            HLSFormat(
                                widthPx = format.width,
                                heightPx = format.height,
                                fps = format.frameRate.let {
                                    if (it == -1f)
                                        VideoFPS.Unavailable
                                    else
                                        VideoFPS.Static(it)
                                }
                            )
                        )
                    }
                    format.language?.let { onAvailableSubtitleTrack(HLSSubtitle(it)) }
                }
            }
        }
    }
}