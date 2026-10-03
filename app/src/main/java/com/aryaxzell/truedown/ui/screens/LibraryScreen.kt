package com.aryaxzell.truedown.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.aryaxzell.truedown.ui.components.FloatingPillSnackbarHost
import com.aryaxzell.truedown.util.VideoThumbnailHelper
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
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
import com.aryaxzell.truedown.domain.model.OpenTarget
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.resolveOpenTarget
import com.aryaxzell.truedown.domain.model.toHumanReadableSize
import com.aryaxzell.truedown.ui.DeleteResult
import com.aryaxzell.truedown.ui.MainViewModel
import com.aryaxzell.truedown.ui.components.ShimmerGalleryListSkeleton
import com.aryaxzell.truedown.ui.components.ShimmerPostSkeletonItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryFilter {
    ALL, VIDEO, PHOTO, AUDIO
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onOpenVideoPlayer: (PostWithMedia) -> Unit,
    onOpenAudioPlayer: (PostWithMedia) -> Unit,
    onOpenSlideshow: (PostWithMedia) -> Unit,
    onNavigateToDownloader: () -> Unit = {}
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
    var deletedMediaPost by remember { mutableStateOf<PostWithMedia?>(null) }
    var deleteFromGallerySingle by remember { mutableStateOf(false) }
    var deleteFromGalleryBulk by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPostIds = remember { mutableStateListOf<String>() }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedPostIds.clear()
    }

    val filteredPosts = remember(allPosts, currentFilter, searchQuery) {
        val base = when (currentFilter) {
            LibraryFilter.ALL -> allPosts
            LibraryFilter.VIDEO -> allPosts.filter { it.post.type == "VIDEO" || it.post.type == PostType.VIDEO.name }
            LibraryFilter.PHOTO -> allPosts.filter { it.post.type == "SLIDESHOW" || it.post.type == PostType.SLIDESHOW.name }
            LibraryFilter.AUDIO -> allPosts.filter { postWithMedia ->
                postWithMedia.mediaItems.any { it.kind == "AUDIO" || it.kind == MediaKind.AUDIO.name }
            }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter { item ->
                item.post.title.lowercase().contains(q) ||
                item.post.authorName.lowercase().contains(q) ||
                item.post.authorHandle.lowercase().contains(q) ||
                item.post.sourceUrl.lowercase().contains(q) ||
                item.mediaItems.any { it.fileName.lowercase().contains(q) }
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
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedPostIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close)
                            )
                        }
                    }
                },
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = stringResource(R.string.selected_count_format, selectedPostIds.size),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
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
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val allSelected = filteredPosts.isNotEmpty() && selectedPostIds.size == filteredPosts.size
                        TextButton(
                            onClick = {
                                if (allSelected) {
                                    selectedPostIds.clear()
                                } else {
                                    selectedPostIds.clear()
                                    selectedPostIds.addAll(filteredPosts.map { it.post.id })
                                }
                            }
                        ) {
                            Text(
                                text = if (allSelected) stringResource(R.string.action_deselect_all) else stringResource(R.string.action_select_all),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedPostIds.isNotEmpty()) {
                                    deleteFromGalleryBulk = false
                                    showBulkDeleteDialog = true
                                }
                            },
                            enabled = selectedPostIds.isNotEmpty(),
                            modifier = Modifier.testTag("library_bulk_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = if (selectedPostIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    } else if (allPosts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isSelectionMode = true
                                selectedPostIds.clear()
                            },
                            modifier = Modifier.testTag("library_select_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_select),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = {
            FloatingPillSnackbarHost(
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
            // Modern Search Bar for Filtering by Title, Author, or Filename
            if (!isSelectionMode && allPosts.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("library_search_bar"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.library_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.library_search_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }

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
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.refreshLibrary()
                        delay(600)
                        isRefreshing = false
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.library_refreshed),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Modern minimalist vector illustration matching Truedown app icon motif
                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .testTag("library_empty_illustration"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_empty_library_vector),
                                        contentDescription = stringResource(R.string.library_empty_title),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val emptyTitle = when {
                                    searchQuery.isNotBlank() -> stringResource(R.string.library_no_search_results)
                                    currentFilter == LibraryFilter.VIDEO && allPosts.isNotEmpty() -> stringResource(R.string.library_filter_empty_video_title)
                                    else -> stringResource(R.string.library_empty_title)
                                }

                                val emptySubtitle = when {
                                    searchQuery.isNotBlank() -> stringResource(R.string.library_no_search_results_subtitle)
                                    currentFilter == LibraryFilter.VIDEO && allPosts.isNotEmpty() -> stringResource(R.string.library_filter_empty_video_subtitle)
                                    else -> stringResource(R.string.library_empty_cta_desc)
                                }

                                Text(
                                    text = emptyTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = emptySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                if (searchQuery.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { searchQuery = "" },
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.testTag("library_clear_search_cta")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.library_search_clear),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    // Call-to-action button to open the downloader
                                    Button(
                                        onClick = onNavigateToDownloader,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                             containerColor = MaterialTheme.colorScheme.primary,
                                             contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("library_empty_cta_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.library_empty_cta),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Feature highlights with FlowRow & custom vector icons (No emojis, no squishing bug)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Tanpa Watermark",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HighQuality,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Kualitas HD",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Audiotrack,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Audio MP3",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
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
                            val isSelected = selectedPostIds.contains(postWithMedia.post.id)
                            
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        val isDownloading = progress != null && (progress.status == MediaStatus.DOWNLOADING || progress.status == MediaStatus.PENDING)
                                        if (isDownloading) {
                                            viewModel.cancelDownload(postWithMedia.post.id)
                                        } else {
                                            viewModel.deletePost(postWithMedia, deleteFromGallery = false)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = MaterialTheme.colorScheme.errorContainer
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, shape = RoundedCornerShape(24.dp))
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = !isSelectionMode
                            ) {
                                ExpressiveLibraryPostItem(
                                    postWithMedia = postWithMedia,
                                    downloadProgress = progress,
                                    batterySaver = isBatterySaver,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onSelectToggle = {
                                        if (isSelected) {
                                            selectedPostIds.remove(postWithMedia.post.id)
                                        } else {
                                            selectedPostIds.add(postWithMedia.post.id)
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (isSelected) {
                                                selectedPostIds.remove(postWithMedia.post.id)
                                            } else {
                                                selectedPostIds.add(postWithMedia.post.id)
                                            }
                                        } else {
                                            when (val target = resolveOpenTarget(postWithMedia, context)) {
                                                is OpenTarget.Video -> onOpenVideoPlayer(target.postWithMedia)
                                                is OpenTarget.Audio -> onOpenAudioPlayer(target.postWithMedia)
                                                is OpenTarget.Slideshow -> onOpenSlideshow(target.postWithMedia)
                                                is OpenTarget.MediaDeleted -> {
                                                    deletedMediaPost = target.postWithMedia
                                                }
                                                is OpenTarget.NotFound -> {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(context.getString(R.string.player_media_not_found))
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        deleteFromGallerySingle = false
                                        postToDelete = postWithMedia
                                    },
                                    onShareClick = {
                                        val firstItem = postWithMedia.mediaItems.firstOrNull { it.status == "DONE" || it.status == MediaStatus.DONE.name }
                                        if (firstItem != null) {
                                            if (firstItem.mediaStoreUri.isNotBlank() && com.aryaxzell.truedown.util.StorageUtil.isMediaAccessible(context, firstItem.mediaStoreUri)) {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = if (firstItem.kind == "VIDEO" || firstItem.kind == MediaKind.VIDEO.name) "video/*"
                                                    else if (firstItem.kind == "PHOTO" || firstItem.kind == MediaKind.PHOTO.name) "image/*"
                                                    else "audio/*"
                                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(firstItem.mediaStoreUri))
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan"))
                                            } else {
                                                deletedMediaPost = postWithMedia
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
        }
    }

    // Delete Modal Dialog (with Gallery Delete Toggle)
    postToDelete?.let { postItem ->
        AlertDialog(
            onDismissRequest = {
                postToDelete = null
                deleteFromGallerySingle = false
            },
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
                            .clickable { deleteFromGallerySingle = !deleteFromGallerySingle }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Checkbox(
                                checked = deleteFromGallerySingle,
                                onCheckedChange = { deleteFromGallerySingle = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dialog_delete_also_gallery),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (deleteFromGallerySingle) {
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
                        val item = postItem
                        val deleteGal = deleteFromGallerySingle
                        postToDelete = null
                        deleteFromGallerySingle = false
                        viewModel.deletePost(item, deleteGal) { result ->
                            if (result is DeleteResult.DeletedButFilesFailed) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.delete_files_failed))
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        postToDelete = null
                        deleteFromGallerySingle = false
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Bulk Delete Modal Dialog (with Gallery Delete Toggle)
    if (showBulkDeleteDialog && selectedPostIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showBulkDeleteDialog = false
                deleteFromGalleryBulk = false
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_multiple_title, selectedPostIds.size),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah kamu yakin ingin menghapus ${selectedPostIds.size} item terpilih dari Library?",
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
                            .clickable { deleteFromGalleryBulk = !deleteFromGalleryBulk }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Checkbox(
                                checked = deleteFromGalleryBulk,
                                onCheckedChange = { deleteFromGalleryBulk = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dialog_delete_also_gallery),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (deleteFromGalleryBulk) {
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
                        val postsToDelete = allPosts.filter { it.post.id in selectedPostIds }
                        val count = selectedPostIds.size
                        val deleteGal = deleteFromGalleryBulk
                        selectedPostIds.clear()
                        isSelectionMode = false
                        showBulkDeleteDialog = false
                        deleteFromGalleryBulk = false
                        viewModel.deletePosts(postsToDelete, deleteGal) { result ->
                            scope.launch {
                                if (result is DeleteResult.DeletedButFilesFailed) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.delete_files_failed))
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.library_items_deleted, count),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteDialog = false
                        deleteFromGalleryBulk = false
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Modal Peringatan Berkas Media Dihapus Manual dari Galeri
    deletedMediaPost?.let { postItem ->
        AlertDialog(
            onDismissRequest = { deletedMediaPost = null },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Media Telah Dihapus dari Galeri",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Berkas media untuk konten ini tidak lagi ditemukan di penyimpanan atau telah dihapus secara manual dari Galeri perangkat Anda.\n\nApakah Anda ingin menghapus catatan item ini dari Library atau mengunduh ulang?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = postItem
                        deletedMediaPost = null
                        viewModel.deletePost(p, deleteFromGallery = false)
                        scope.launch {
                            snackbarHostState.showSnackbar("Item berhasil dihapus dari Library")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus dari Library", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (postItem.post.sourceUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val p = postItem
                                deletedMediaPost = null
                                viewModel.retryFailedDownload(p)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Memulai proses unduh ulang...")
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unduh Ulang", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(
                        onClick = { deletedMediaPost = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.SemiBold)
                    }
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

    val filterActive = stringResource(R.string.a11y_filter_active)
    val filterInactive = stringResource(R.string.a11y_filter_inactive)
    val filterClickLabel = stringResource(R.string.a11y_action_filter, label)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        tonalElevation = if (selected) 3.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                onClickLabel = filterClickLabel
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
                stateDescription = if (selected) filterActive else filterInactive
            }
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
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

    val cachedThumbFile = remember(postWithMedia.post.id) {
        VideoThumbnailHelper.getThumbnailFile(context, postWithMedia.post.id)
    }
    val thumbnailModel by androidx.compose.runtime.produceState<Any?>(initialValue = thumbnailUri, key1 = postWithMedia.post.id, key2 = thumbnailUri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (cachedThumbFile.exists() && cachedThumbFile.length() > 0) {
                cachedThumbFile
            } else if (isVideo && !thumbnailUri.isNullOrBlank()) {
                val generated = VideoThumbnailHelper.generateThumbnail(
                    context = context,
                    videoUriOrPath = thumbnailUri,
                    postId = postWithMedia.post.id
                )
                if (generated != null && generated.exists()) generated else thumbnailUri
            } else {
                thumbnailUri
            }
        }
    }

    val cardClickLabel = if (isSelectionMode) stringResource(R.string.a11y_card_select) else stringResource(R.string.a11y_card_open)
    val cardStateDesc = if (isSelected) stringResource(R.string.a11y_card_selected) else stringResource(R.string.a11y_card_not_selected)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                onClick = onClick,
                onClickLabel = cardClickLabel
            )
            .semantics {
                if (isSelectionMode) {
                    role = Role.Checkbox
                    stateDescription = cardStateDesc
                } else {
                    role = Role.Button
                }
            }
            .testTag("library_item_${postWithMedia.post.id}"),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isSelected) 4.dp else 2.dp,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
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
                        if (thumbnailModel != null) {
                            AsyncImage(
                                model = if (batterySaver) {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(thumbnailModel)
                                        .size(100, 100)
                                        .allowHardware(false)
                                        .crossfade(false)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build()
                                } else {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(thumbnailModel)
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

                if (!isSelectionMode) {
                    // Quick Action / Menu button
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.a11y_btn_more_options),
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
