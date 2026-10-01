package com.aryaxzell.truedown.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.ui.MainViewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == LibraryFilter.ALL,
                        onClick = { currentFilter = LibraryFilter.ALL },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == LibraryFilter.VIDEO,
                        onClick = { currentFilter = LibraryFilter.VIDEO },
                        label = { Text(stringResource(R.string.filter_video)) }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == LibraryFilter.PHOTO,
                        onClick = { currentFilter = LibraryFilter.PHOTO },
                        label = { Text(stringResource(R.string.filter_photo)) }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == LibraryFilter.AUDIO,
                        onClick = { currentFilter = LibraryFilter.AUDIO },
                        label = { Text(stringResource(R.string.filter_audio)) }
                    )
                }
            }

            // Items List or Anime Character Empty State
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_anime_empty_state),
                            contentDescription = stringResource(R.string.library_empty_title),
                            modifier = Modifier
                                .size(180.dp)
                                .testTag("library_anime_empty_state")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPosts, key = { it.post.id }) { postWithMedia ->
                        val progress = downloadProgressMap[postWithMedia.post.id]
                        LibraryPostItem(
                            postWithMedia = postWithMedia,
                            downloadProgress = progress,
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
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah kamu yakin ingin menghapus '${postItem.post.title.ifBlank { "postingan ini" }}'?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { deleteFromGallery = !deleteFromGallery }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = deleteFromGallery,
                            onCheckedChange = { deleteFromGallery = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_also_gallery),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (deleteFromGallery) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LibraryPostItem(
    postWithMedia: PostWithMedia,
    downloadProgress: com.aryaxzell.truedown.domain.model.DownloadProgress? = null,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val isDownloading = downloadProgress?.status == MediaStatus.DOWNLOADING ||
            downloadProgress?.status == MediaStatus.PENDING ||
            postWithMedia.mediaItems.any { it.status == MediaStatus.DOWNLOADING.name || it.status == MediaStatus.PENDING.name }

    val firstMedia = postWithMedia.mediaItems.firstOrNull()
    val thumbnailUri = firstMedia?.mediaStoreUri

    val dateFormatted = remember(postWithMedia.post.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(postWithMedia.post.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("library_item_${postWithMedia.post.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Card(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thumbnailUri.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbnailUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (postWithMedia.post.type == "VIDEO" || postWithMedia.post.type == PostType.VIDEO.name) {
                                    Icons.Default.Movie
                                } else {
                                    Icons.Default.PhotoLibrary
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = postWithMedia.post.title.ifBlank { "TikTok ${postWithMedia.post.authorName}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${postWithMedia.post.authorHandle.ifBlank { postWithMedia.post.authorName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (postWithMedia.post.type == "SLIDESHOW" || postWithMedia.post.type == PostType.SLIDESHOW.name) {
                                val doneCount = postWithMedia.mediaItems.count { it.status == "DONE" || it.status == MediaStatus.DONE.name }
                                "$doneCount Foto"
                            } else {
                                "Video MP4"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  $dateFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Menu button
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                menuExpanded = false
                                onShareClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }

            if (isDownloading) {
                val percent = downloadProgress?.progressPercent ?: 0
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.preview_downloading),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
