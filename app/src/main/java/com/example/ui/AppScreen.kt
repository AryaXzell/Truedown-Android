package com.example.ui

import com.example.data.local.PostWithMedia
import com.example.domain.model.ResolvedPost

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
}
