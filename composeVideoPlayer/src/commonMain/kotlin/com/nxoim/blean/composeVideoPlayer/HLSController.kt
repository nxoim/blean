package com.nxoim.blean.composeVideoPlayer

// not a data class because we dont want to allow the consumer to make their own instance of this class
class HLSFormat internal constructor(val widthPx: Int, val heightPx: Int, val fps: VideoFPS) {
    override fun toString(): String {
        return "$widthPx x $heightPx @ $fps fps"
    }

    override fun equals(other: Any?) = this.hashCode() == other.hashCode()

    override fun hashCode(): Int {
        var result = widthPx
        result = 31 * result + heightPx
        result = 31 * result + fps.hashCode()
        return result
    }
}

class HLSSubtitle internal constructor(val languageIsoCode: String) {
    override fun toString() = languageIsoCode

    override fun equals(other: Any?) = this.hashCode() == other.hashCode()

    override fun hashCode() = languageIsoCode.hashCode()
}

sealed interface VideoFPS {
    data object Unavailable : VideoFPS
    data class Static(val value: Float) : VideoFPS
//    data class Variable
}

interface HLSController {
    val availableFormatTracks: List<HLSFormat>
    val availableSubtitleTracks: List<HLSSubtitle>

    var selectedFormatTrack: HLSFormat?
    var selectedSubtitleTrack: HLSSubtitle?
}

@Suppress("ClassName")
class _FakeHLSController (
    override val availableFormatTracks: List<HLSFormat> = emptyList(),
    override val availableSubtitleTracks: List<HLSSubtitle> = emptyList()
) : HLSController {
    override var selectedFormatTrack: HLSFormat? = null
    override var selectedSubtitleTrack: HLSSubtitle? = null
}
