package com.aryaxzell.truedown.ui

import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.domain.model.ResolvedPost

sealed class AppScreen {
    object Onboarding : AppScreen()
    object Home : AppScreen()
    object Library : AppScreen()
    data class Preview(val post: ResolvedPost) : AppScreen()
    data class SlideshowGrid(val post: ResolvedPost) : AppScreen()
    data class PhotoViewer(val post: ResolvedPost, val initialIndex: Int = 0) : AppScreen()
    data class VideoPlayer(val postWithMedia: PostWithMedia) : AppScreen()
    data class AudioPlayer(val postWithMedia: PostWithMedia) : AppScreen()
    object Settings : AppScreen()
    object About : AppScreen()
}
