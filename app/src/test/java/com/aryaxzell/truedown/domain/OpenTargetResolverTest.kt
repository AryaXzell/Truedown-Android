package com.aryaxzell.truedown.domain

import com.aryaxzell.truedown.data.local.MediaItemEntity
import com.aryaxzell.truedown.data.local.PostEntity
import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import com.aryaxzell.truedown.domain.model.OpenTarget
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.resolveOpenTarget
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenTargetResolverTest {

    private fun createPost(type: PostType = PostType.VIDEO): PostEntity {
        return PostEntity(
            id = "test_post_1",
            type = type.name,
            title = "Test Post",
            authorName = "Author",
            authorHandle = "handle",
            sourceUrl = "https://tiktok.com/@user/video/123",
            hasAudio = true,
            photoCount = if (type == PostType.SLIDESHOW) 2 else 0,
            createdAt = 1000L
        )
    }

    @Test
    fun testVideoPostWithDoneVideo_resolvesToVideo() {
        val post = createPost(PostType.VIDEO)
        val mediaItems = listOf(
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.VIDEO.name,
                fileName = "video.mp4",
                mediaStoreUri = "content://media/external/video/media/1",
                status = MediaStatus.DONE.name
            ),
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.AUDIO.name,
                fileName = "audio.mp3",
                mediaStoreUri = "content://media/external/audio/media/1",
                status = MediaStatus.DONE.name
            )
        )
        val target = resolveOpenTarget(PostWithMedia(post, mediaItems))
        assertTrue("Must resolve to Video when video is DONE", target is OpenTarget.Video)
    }

    @Test
    fun testVideoPostWithOnlyAudioDone_resolvesToAudio() {
        val post = createPost(PostType.VIDEO)
        val mediaItems = listOf(
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.VIDEO.name,
                fileName = "video.mp4",
                mediaStoreUri = "",
                status = MediaStatus.PENDING.name
            ),
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.AUDIO.name,
                fileName = "audio.mp3",
                mediaStoreUri = "content://media/external/audio/media/1",
                status = MediaStatus.DONE.name
            )
        )
        val target = resolveOpenTarget(PostWithMedia(post, mediaItems))
        assertTrue("Must resolve to Audio when only audio is DONE", target is OpenTarget.Audio)
    }

    @Test
    fun testSlideshowPost_resolvesToSlideshow() {
        val post = createPost(PostType.SLIDESHOW)
        val mediaItems = listOf(
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.PHOTO.name,
                fileName = "photo1.jpg",
                mediaStoreUri = "content://media/external/images/media/1",
                status = MediaStatus.DONE.name
            )
        )
        val target = resolveOpenTarget(PostWithMedia(post, mediaItems))
        assertTrue("Must resolve to Slideshow when post is SLIDESHOW with done photos", target is OpenTarget.Slideshow)
    }

    @Test
    fun testPostWithNoDoneFiles_resolvesToNotFound() {
        val post = createPost(PostType.VIDEO)
        val mediaItems = listOf(
            MediaItemEntity(
                postId = post.id,
                kind = MediaKind.VIDEO.name,
                fileName = "video.mp4",
                mediaStoreUri = "",
                status = MediaStatus.FAILED.name
            )
        )
        val target = resolveOpenTarget(PostWithMedia(post, mediaItems))
        assertTrue("Must resolve to NotFound when media is missing/failed", target is OpenTarget.NotFound)
    }
}
