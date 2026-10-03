package com.aryaxzell.truedown.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.ResolvedPost

@OptIn(UnstableApi::class)
@Composable
fun QuickMediaPreviewCard(
    post: ResolvedPost,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_media_preview_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pratinjau Media",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                post.type == PostType.VIDEO && !post.videoStandardUrl.isNullOrEmpty() -> {
                    VideoPreviewSection(
                        videoUrl = post.videoHdUrl ?: post.videoStandardUrl ?: "",
                        coverUrl = post.coverUrl
                    )
                }

                post.type == PostType.SLIDESHOW && post.photoUrls.isNotEmpty() -> {
                    SlideshowPreviewSection(photoUrls = post.photoUrls)
                }

                !post.audioUrl.isNullOrEmpty() -> {
                    AudioPreviewSection(
                        audioUrl = post.audioUrl ?: "",
                        coverUrl = post.coverUrl,
                        title = post.title
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPreviewSection(
    videoUrl: String,
    coverUrl: String?
) {
    val context = LocalContext.current
    var isInlinePlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    var currentPos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var duration by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }

    var videoWidth by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var videoHeight by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Inisialisasi tunggal ExoPlayer untuk digunakan di inline maupun fullscreen
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            volume = if (isMuted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    val rawAspectRatio = if (videoWidth > 0 && videoHeight > 0) {
        videoWidth.toFloat() / videoHeight.toFloat()
    } else {
        9f / 16f
    }

    val animatedAspectRatio by animateFloatAsState(
        targetValue = rawAspectRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "video_aspect_ratio"
    )

    LaunchedEffect(isInlinePlaying, isPlaying) {
        if (isInlinePlaying && isPlaying) {
            while (true) {
                currentPos = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                kotlinx.coroutines.delay(200)
            }
        }
    }

    // Fungsi pembantu kontrol
    val togglePlay = {
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        isPlaying = !isPlaying
    }

    val toggleMute = {
        isMuted = !isMuted
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    val handleSeek: (Float) -> Unit = { percent ->
        val targetMs = (percent * duration).toLong()
        exoPlayer.seekTo(targetMs)
        currentPos = targetMs
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(animatedAspectRatio)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isInlinePlaying) {
            // Thumbnail / Cover Image
            AsyncImage(
                model = coverUrl ?: videoUrl,
                contentDescription = "Cover Pratinjau Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Play Overlay Button
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { isInlinePlaying = true },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar Pratinjau Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            // ExoPlayer View (Inline)
            if (!isFullscreen) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Kotak Kontrol Overlay
                VideoPlayerControlsOverlay(
                    currentPos = currentPos,
                    duration = duration,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    isFullscreen = false,
                    onTogglePlay = togglePlay,
                    onToggleMute = toggleMute,
                    onToggleFullscreen = { isFullscreen = true },
                    onSeek = handleSeek,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Full-Screen Dialog Overlay
    if (isInlinePlaying && isFullscreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Kotak Kontrol Overlay di Fullscreen
                VideoPlayerControlsOverlay(
                    currentPos = currentPos,
                    duration = duration,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    isFullscreen = true,
                    onTogglePlay = togglePlay,
                    onToggleMute = toggleMute,
                    onToggleFullscreen = { isFullscreen = false },
                    onSeek = handleSeek,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerControlsOverlay(
    currentPos: Long,
    duration: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    isFullscreen: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Mute / Unmute Button (Top End)
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Audio Mute Toggle",
                tint = Color.White
            )
        }

        // Fullscreen Toggle Button (Top Start)
        IconButton(
            onClick = onToggleFullscreen,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Fullscreen Toggle",
                tint = Color.White
            )
        }

        // Play / Pause Overlay Button (Center)
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause Toggle",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Bottom Controls (Seek Bar & Time Info)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTime(currentPos)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            androidx.compose.material3.Slider(
                value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                onValueChange = onSeek,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}

@Composable
private fun SlideshowPreviewSection(photoUrls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { photoUrls.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = photoUrls[page],
                    contentDescription = "Foto ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Index Badge Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioPreviewSection(
    audioUrl: String,
    coverUrl: String?,
    title: String
) {
    val context = LocalContext.current
    var isAudioPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            prepare()
        }
    }

    DisposableEffect(audioUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        isAudioPlaying = !isAudioPlaying
                        if (isAudioPlaying) {
                            exoPlayer.play()
                        } else {
                            exoPlayer.pause()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Putar Audio",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pratinjau Suara Audio TikTok",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (title.isNotBlank()) title else "Audio TikTok",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
