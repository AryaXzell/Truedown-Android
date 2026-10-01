package com.aryaxzell.truedown.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.domain.model.DownloadProgress
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.toHumanReadableSize
import com.aryaxzell.truedown.ui.MainViewModel
import com.aryaxzell.truedown.ui.components.ShimmerGalleryListSkeleton
import com.aryaxzell.truedown.ui.components.ShimmerPostSkeletonItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryFilter {
    ALL, VIDEO, PHOTO, AUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenVideoPlayer: (PostWithMedia) -> Unit,
    onOpenAudioPlayer: (PostWithMedia) -> Unit,
    onOpenSlideshow: (PostWithMedia) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allPosts by viewModel.allPosts.collectAsState()
    val downloadProgressMap by viewModel.downloadProgress.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val isBatterySaver = preferences.batterySaver
    var currentFilter by remember { mutableStateOf(LibraryFilter.ALL) }

    var postToDelete by remember { mutableStateOf<PostWithMedia?>(null) }
    var deleteFromGallery by remember { mutableStateOf(false) }

    val filteredPosts = remember(allPosts, currentFilter) {
        when (currentFilter) {
            LibraryFilter.ALL -> allPosts
            LibraryFilter.VIDEO -> allPosts.filter { it.post.type == "VIDEO" || it.post.type == PostType.VIDEO.name }
            LibraryFilter.PHOTO -> allPosts.filter { it.post.type == "SLIDESHOW" || it.post.type == PostType.SLIDESHOW.name }
            LibraryFilter.AUDIO -> allPosts.filter { postWithMedia ->
                postWithMedia.mediaItems.any { it.kind == "AUDIO" || it.kind == MediaKind.AUDIO.name }
            }
        }
    }

    val videoCount = remember(allPosts) { allPosts.count { it.post.type == "VIDEO" || it.post.type == PostType.VIDEO.name } }
    val photoCount = remember(allPosts) { allPosts.count { it.post.type == "SLIDESHOW" || it.post.type == PostType.SLIDESHOW.name } }
    val audioCount = remember(allPosts) {
        allPosts.count { postWithMedia ->
            postWithMedia.mediaItems.any { it.kind == "AUDIO" || it.kind == MediaKind.AUDIO.name }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.library_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (allPosts.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                tonalElevation = 1.dp
                            ) {
                                Text(
                                    text = "${allPosts.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 86.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expressive Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ExpressiveFilterChip(
                        selected = currentFilter == LibraryFilter.ALL,
                        onClick = { currentFilter = LibraryFilter.ALL },
                        label = stringResource(R.string.filter_all),
                        count = allPosts.size
                    )
                }
                item {
                    ExpressiveFilterChip(
                        selected = currentFilter == LibraryFilter.VIDEO,
                        onClick = { currentFilter = LibraryFilter.VIDEO },
                        label = stringResource(R.string.filter_video),
                        count = videoCount
                    )
                }
                item {
                    ExpressiveFilterChip(
                        selected = currentFilter == LibraryFilter.PHOTO,
                        onClick = { currentFilter = LibraryFilter.PHOTO },
                        label = stringResource(R.string.filter_photo),
                        count = photoCount
                    )
                }
                item {
                    ExpressiveFilterChip(
                        selected = currentFilter == LibraryFilter.AUDIO,
                        onClick = { currentFilter = LibraryFilter.AUDIO },
                        label = stringResource(R.string.filter_audio),
                        count = audioCount
                    )
                }
            }

            // Items List or Expressive Empty State
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 44.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                                MaterialTheme.colorScheme.surfaceContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_anime_empty_state),
                                    contentDescription = stringResource(R.string.library_empty_title),
                                    modifier = Modifier
                                        .size(104.dp)
                                        .testTag("library_anime_empty_state")
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.library_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.library_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                val activeDownloadingIds = remember(downloadProgressMap, filteredPosts) {
                    downloadProgressMap.filter { (id, progress) ->
                        (progress.status == MediaStatus.DOWNLOADING || progress.status == MediaStatus.PENDING) &&
                                filteredPosts.none { it.post.id == id }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeDownloadingIds.isNotEmpty()) {
                        items(activeDownloadingIds.keys.toList(), key = { "shimmer_$it" }) { _ ->
                            ShimmerPostSkeletonItem()
                        }
                    }

                    items(
                        items = filteredPosts,
                        key = { it.post.id },
                        contentType = { it.post.type }
                    ) { postWithMedia ->
                        val progress = downloadProgressMap[postWithMedia.post.id]
                        ExpressiveLibraryPostItem(
                            postWithMedia = postWithMedia,
                            downloadProgress = progress,
                            batterySaver = isBatterySaver,
                            onClick = {
                                if (currentFilter == LibraryFilter.AUDIO ||
                                    postWithMedia.mediaItems.any { (it.kind == "AUDIO" || it.kind == MediaKind.AUDIO.name) && postWithMedia.mediaItems.none { m -> m.kind == "VIDEO" || m.kind == "PHOTO" } }) {
                                    onOpenAudioPlayer(postWithMedia)
                                } else if (postWithMedia.post.type == "VIDEO" || postWithMedia.post.type == PostType.VIDEO.name) {
                                    onOpenVideoPlayer(postWithMedia)
                                } else {
                                    onOpenSlideshow(postWithMedia)
                                }
                            },
                            onDeleteClick = {
                                postToDelete = postWithMedia
                                deleteFromGallery = false
                            },
                            onShareClick = {
                                val firstItem = postWithMedia.mediaItems.firstOrNull { it.status == "DONE" || it.status == MediaStatus.DONE.name }
                                firstItem?.let { item ->
                                    if (item.mediaStoreUri.isNotBlank()) {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = if (item.kind == "VIDEO" || item.kind == MediaKind.VIDEO.name) "video/*"
                                            else if (item.kind == "PHOTO" || item.kind == MediaKind.PHOTO.name) "image/*"
                                            else "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(item.mediaStoreUri))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan"))
                                    }
                                }
                            },
                            onRetryClick = {
                                viewModel.retryFailedDownload(postWithMedia)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Modal Dialog (with Gallery Delete Toggle)
    postToDelete?.let { postItem ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah kamu yakin ingin menghapus '${postItem.post.title.ifBlank { "postingan ini" }}'?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { deleteFromGallery = !deleteFromGallery }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Checkbox(
                                checked = deleteFromGallery,
                                onCheckedChange = { deleteFromGallery = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dialog_delete_also_gallery),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (deleteFromGallery) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(postItem, deleteFromGallery)
                        postToDelete = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { postToDelete = null },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "filter_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "filter_chip_content"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        tonalElevation = if (selected) 3.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveLibraryPostItem(
    postWithMedia: PostWithMedia,
    downloadProgress: DownloadProgress? = null,
    batterySaver: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val isDownloading = downloadProgress?.status == MediaStatus.DOWNLOADING ||
            downloadProgress?.status == MediaStatus.PENDING ||
            postWithMedia.mediaItems.any { it.status == MediaStatus.DOWNLOADING.name || it.status == MediaStatus.PENDING.name }

    val isFailed = !isDownloading && postWithMedia.mediaItems.any { it.status == "FAILED" || it.status == MediaStatus.FAILED.name }

    val firstMedia = postWithMedia.mediaItems.firstOrNull { it.mediaStoreUri.isNotBlank() } ?: postWithMedia.mediaItems.firstOrNull()
    val thumbnailUri = firstMedia?.mediaStoreUri?.ifBlank { null }

    val dateFormatted = remember(postWithMedia.post.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(postWithMedia.post.createdAt))
    }

    val isVideo = postWithMedia.post.type == "VIDEO" || postWithMedia.post.type == PostType.VIDEO.name

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("library_item_${postWithMedia.post.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-fidelity Thumbnail with play badge
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thumbnailUri.isNullOrBlank()) {
                            AsyncImage(
                                model = if (batterySaver) {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(thumbnailUri)
                                        .size(100, 100)
                                        .allowHardware(false)
                                        .crossfade(false)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build()
                                } else {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(thumbnailUri)
                                        .size(220, 220)
                                        .crossfade(true)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build()
                                },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isVideo) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = postWithMedia.post.title.ifBlank { "TikTok ${postWithMedia.post.authorName}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${postWithMedia.post.authorHandle.ifBlank { postWithMedia.post.authorName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (postWithMedia.post.type == "SLIDESHOW" || postWithMedia.post.type == PostType.SLIDESHOW.name) {
                                    val doneCount = postWithMedia.mediaItems.count { it.status == "DONE" || it.status == MediaStatus.DONE.name }
                                    "$doneCount Foto"
                                } else {
                                    "Video MP4"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Action / Menu button
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.clip(RoundedCornerShape(18.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share), fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }

            if (isDownloading) {
                val percent = downloadProgress?.progressPercent ?: 0
                val bytesDownloaded = downloadProgress?.bytesDownloaded ?: 0L
                val totalBytes = downloadProgress?.totalBytes ?: 0L
                val startEpochMs = downloadProgress?.startEpochMs ?: System.currentTimeMillis()

                Spacer(modifier = Modifier.height(14.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Progress with text inside
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { (percent / 100f).coerceIn(0f, 1f) },
                                strokeWidth = 4.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Download status text and remaining time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.preview_downloading),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))

                            // Speed & Sisa Waktu
                            val elapsedTimeSec = (System.currentTimeMillis() - startEpochMs) / 1000f
                            val speedAndEtaText = if (elapsedTimeSec > 0.5f && bytesDownloaded > 0 && totalBytes > bytesDownloaded) {
                                val speedBytesPerSec = bytesDownloaded / elapsedTimeSec
                                val remainingBytes = totalBytes - bytesDownloaded
                                val remainingSeconds = (remainingBytes / speedBytesPerSec).toLong()
                                
                                val speedStr = if (speedBytesPerSec >= 1024 * 1024) {
                                    String.format(java.util.Locale.US, "%.1f MB/dtk", speedBytesPerSec / (1024.0 * 1024.0))
                                } else {
                                    String.format(java.util.Locale.US, "%.0f KB/dtk", speedBytesPerSec / 1024.0)
                                }

                                val etaStr = if (remainingSeconds < 60) {
                                    "$remainingSeconds dtk"
                                } else {
                                    "${remainingSeconds / 60} m ${remainingSeconds % 60} dtk"
                                }

                                "$speedStr • Sisa $etaStr"
                            } else {
                                "Mengestimasi kecepatan..."
                            }

                            Text(
                                text = speedAndEtaText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )

                            if (totalBytes > 0) {
                                Spacer(modifier = Modifier.height(1.dp))
                                val sizeStr = "${bytesDownloaded.toHumanReadableSize()} / ${totalBytes.toHumanReadableSize()}"
                                Text(
                                    text = sizeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (isFailed) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gagal mengunduh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(28.dp).testTag("retry_download_button_${postWithMedia.post.id}")
                    ) {
                        Text(
                            text = "Coba Lagi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
