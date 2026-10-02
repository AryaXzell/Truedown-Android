package com.aryaxzell.truedown.ui

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aryaxzell.truedown.data.local.PostWithMedia
import com.aryaxzell.truedown.data.local.TruedownDatabase
import com.aryaxzell.truedown.data.preferences.UserPreferences
import com.aryaxzell.truedown.data.preferences.UserPreferencesRepository
import com.aryaxzell.truedown.data.provider.DownloadProvider
import com.aryaxzell.truedown.data.provider.TikWmDownloadProvider
import com.aryaxzell.truedown.data.storage.MediaStoreDownloader
import com.aryaxzell.truedown.domain.DownloadProgressTracker
import com.aryaxzell.truedown.domain.DownloadScheduler
import com.aryaxzell.truedown.domain.model.DownloadProgress
import com.aryaxzell.truedown.domain.model.MediaKind
import com.aryaxzell.truedown.domain.model.MediaStatus
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.ProviderError
import com.aryaxzell.truedown.domain.model.ResolvedPost
import com.aryaxzell.truedown.util.ClipboardDeduplicator
import com.aryaxzell.truedown.util.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GlobalDownloadStatus(
    val hasActiveDownloads: Boolean = false,
    val activeCount: Int = 0,
    val progressPercent: Int = 0,
    val isIndeterminate: Boolean = true,
    val latestTitle: String = ""
)

sealed class ResolveState {
    object Idle : ResolveState()
    object Loading : ResolveState()
    data class Success(val post: ResolvedPost) : ResolveState()
    data class Error(val messageResId: Int) : ResolveState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val userPreferencesRepository = UserPreferencesRepository(application)
    val database = TruedownDatabase.getInstance(application)
    val downloadProvider: DownloadProvider = TikWmDownloadProvider()
    val mediaStoreDownloader = MediaStoreDownloader(application)
    val downloadScheduler = DownloadScheduler(application)

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _startupReady = MutableStateFlow(false)
    /** false sampai preferensi dari DataStore selesai dimuat dan layar awal ditentukan. */
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()
    private var startupInitialized = false

    private val _screenStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.Home))
    val currentScreen: StateFlow<AppScreen> = MutableStateFlow<AppScreen>(AppScreen.Home).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                value = stack.lastOrNull() ?: AppScreen.Home
            }
        }
    }

    private val _resolveState = MutableStateFlow<ResolveState>(ResolveState.Idle)
    val resolveState: StateFlow<ResolveState> = _resolveState.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val recentPosts: StateFlow<List<PostWithMedia>> = database.postDao().getRecentPostsWithMedia(3)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPosts: StateFlow<List<PostWithMedia>> = database.postDao().getAllPostsWithMedia()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = DownloadProgressTracker.downloadProgressMap

    private val workManager = androidx.work.WorkManager.getInstance(application)

    val globalDownloadStatus: StateFlow<GlobalDownloadStatus> = combine(
        workManager.getWorkInfosByTagFlow("download"),
        downloadProgress
    ) { workInfos, progressMap ->
        val activeWorkInfos = workInfos.filter {
            it.state == androidx.work.WorkInfo.State.RUNNING || it.state == androidx.work.WorkInfo.State.ENQUEUED
        }
        val activeProgresses = progressMap.values.filter {
            it.status == MediaStatus.DOWNLOADING || it.status == MediaStatus.PENDING
        }

        val isActive = activeWorkInfos.isNotEmpty() || activeProgresses.isNotEmpty()
        val count = maxOf(activeWorkInfos.size, activeProgresses.size)

        if (!isActive) {
            GlobalDownloadStatus()
        } else {
            val validPercents = activeProgresses.map { it.progressPercent }.filter { it > 0 }
            val avgPercent = if (validPercents.isNotEmpty()) validPercents.average().toInt() else 0
            val latestTitle = activeProgresses.lastOrNull()?.title ?: ""
            val isIndeterminate = avgPercent <= 0

            GlobalDownloadStatus(
                hasActiveDownloads = true,
                activeCount = count,
                progressPercent = avgPercent,
                isIndeterminate = isIndeterminate,
                latestTitle = latestTitle
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalDownloadStatus())

    init {
        // Reconcile stuck DOWNLOADING items on startup asynchronously on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.mediaItemDao().reconcileStuckDownloadingItems()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initializeStartingScreen(isShareIntent: Boolean = false) {
        if (startupInitialized) return
        startupInitialized = true
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = try {
                userPreferencesRepository.userPreferencesFlow.first()
            } catch (e: Exception) {
                UserPreferences(onboardingCompleted = true)
            }
            if (!prefs.onboardingCompleted && !isShareIntent) {
                _screenStack.value = listOf(AppScreen.Onboarding)
            }
            (downloadProvider as? TikWmDownloadProvider)?.updateDohProvider(prefs.dohProvider)
            _startupReady.value = true
        }
    }

    fun navigateTo(screen: AppScreen) {
        val currentList = _screenStack.value.toMutableList()
        if (screen is AppScreen.Home) {
            _screenStack.value = listOf(AppScreen.Home)
        } else {
            // Prevent pushing identical screen onto the stack
            if (currentList.lastOrNull() == screen) {
                return
            }
            // If already on a Preview screen, replace the top screen instead of accumulating duplicate screens
            if (screen is AppScreen.Preview && currentList.lastOrNull() is AppScreen.Preview) {
                currentList[currentList.lastIndex] = screen
                _screenStack.value = currentList
                return
            }
            if (screen is AppScreen.SlideshowGrid && currentList.lastOrNull() is AppScreen.SlideshowGrid) {
                currentList[currentList.lastIndex] = screen
                _screenStack.value = currentList
                return
            }
            currentList.add(screen)
            _screenStack.value = currentList
        }
    }

    fun popBackStack(): Boolean {
        val currentList = _screenStack.value.toMutableList()
        if (currentList.size > 1) {
            currentList.removeAt(currentList.lastIndex)
            _screenStack.value = currentList
            return true
        } else if (currentList.firstOrNull() !is AppScreen.Home) {
            _screenStack.value = listOf(AppScreen.Home)
            return true
        }
        return false
    }

    val clipboardDeduplicator = ClipboardDeduplicator()

    fun clipboardKey(text: String): String = clipboardDeduplicator.clipboardKey(text)

    /**
     * Memeriksa apakah teks clipboard dan timestamp yang diberikan merupakan event baru.
     * Mengembalikan true jika valid dan baru (tidak duplikat), serta mengupdate state jika [applyState] true.
     */
    fun shouldProcessClipboardText(text: String, timestamp: Long = 0L, applyState: Boolean = false): Boolean {
        val shouldProcess = clipboardDeduplicator.shouldProcess(text, timestamp) { isTikTokUrl(it) }
        if (shouldProcess && applyState) {
            _detectedClipboardUrl.value = text
        }
        return shouldProcess
    }

    fun checkClipboardForTikTokUrl(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val description = clipboard.primaryClipDescription
                if (description != null && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) {
                    val item = clipboard.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString()?.trim() ?: ""
                    val timestamp = description.timestamp
                    if (shouldProcessClipboardText(text, timestamp, applyState = true)) {
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _detectedClipboardUrl.value = null
    }

    fun clearDetectedClipboardUrl() {
        clipboardDeduplicator.clearHandled(_detectedClipboardUrl.value)
        _detectedClipboardUrl.value = null
    }

    fun markClipboardUrlHandled(url: String, timestamp: Long = 0L) {
        clipboardDeduplicator.markHandled(url, timestamp)
        val current = _detectedClipboardUrl.value
        if (current != null && clipboardDeduplicator.clipboardKey(current) == clipboardDeduplicator.clipboardKey(url)) {
            _detectedClipboardUrl.value = null
        }
    }

    private fun isTikTokUrl(text: String): Boolean {
        if (text.isBlank()) return false
        val regex = Regex("https?://([a-zA-Z0-9_-]+\\.)?tiktok\\.com(/.*)?", RegexOption.IGNORE_CASE)
        val shortRegex = Regex("https?://(vt|vm)\\.tiktok\\.com/([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text) || shortRegex.containsMatchIn(text) || text.contains("tiktok.com/")
    }

    fun errorResIdFor(error: Throwable): Int = when (error) {
        is ProviderError.InvalidLink -> com.aryaxzell.truedown.R.string.error_invalid_link
        is ProviderError.NotTikTokLink -> com.aryaxzell.truedown.R.string.error_not_tiktok
        is ProviderError.DouyinUnsupported -> com.aryaxzell.truedown.R.string.error_douyin_unsupported
        is ProviderError.PostUnavailable -> com.aryaxzell.truedown.R.string.error_post_unavailable
        is ProviderError.NetworkError -> com.aryaxzell.truedown.R.string.error_network
        is ProviderError.TimeoutError -> com.aryaxzell.truedown.R.string.error_timeout
        is ProviderError.RateLimited -> com.aryaxzell.truedown.R.string.error_rate_limited
        else -> com.aryaxzell.truedown.R.string.error_unknown
    }

    fun resolveUrl(url: String, onNavigate: ((AppScreen) -> Unit)? = null) {
        markClipboardUrlHandled(url)
        val cleanUrl = extractUrl(url)
        if (cleanUrl.isBlank() || !isTikTokUrl(cleanUrl)) {
            _resolveState.value = ResolveState.Error(com.aryaxzell.truedown.R.string.error_invalid_link)
            return
        }

        viewModelScope.launch {
            _resolveState.value = ResolveState.Loading
            val result = downloadProvider.resolve(cleanUrl)
            result.onSuccess { resolvedPost ->
                _resolveState.value = ResolveState.Success(resolvedPost)
                if (resolvedPost.type == PostType.SLIDESHOW && resolvedPost.photoUrls.size > 1) {
                    val nextScreen = AppScreen.SlideshowGrid(resolvedPost)
                    navigateTo(nextScreen)
                    onNavigate?.invoke(nextScreen)
                } else {
                    val nextScreen = AppScreen.Preview(resolvedPost)
                    navigateTo(nextScreen)
                    onNavigate?.invoke(nextScreen)
                }
            }.onFailure { error ->
                val errorResId = errorResIdFor(error)
                _resolveState.value = ResolveState.Error(errorResId)
            }
        }
    }

    /**
     * Digunakan untuk Auto-Download saat URL TikTok terdeteksi di clipboard (R-24 & R-31).
     * Jika video atau 1 foto: langsung mendownload tanpa membuka Preview.
     * Jika slideshow banyak foto: membuka SlideshowGridScreen.
     */
    fun autoDownloadFromClipboard(url: String, onDownloadStarted: (Int) -> Unit) {
        markClipboardUrlHandled(url)
        val cleanUrl = extractUrl(url)
        if (cleanUrl.isBlank() || !isTikTokUrl(cleanUrl)) {
            _resolveState.value = ResolveState.Error(com.aryaxzell.truedown.R.string.error_invalid_link)
            return
        }

        viewModelScope.launch {
            _resolveState.value = ResolveState.Loading
            val result = downloadProvider.resolve(cleanUrl)
            result.onSuccess { resolvedPost ->
                _resolveState.value = ResolveState.Success(resolvedPost)
                if (resolvedPost.type == PostType.SLIDESHOW && resolvedPost.photoUrls.size > 1) {
                    val nextScreen = AppScreen.SlideshowGrid(resolvedPost)
                    navigateTo(nextScreen)
                } else {
                    startDownload(resolvedPost)
                    onDownloadStarted(com.aryaxzell.truedown.R.string.preview_starting_download)
                }
            }.onFailure { error ->
                val errorResId = errorResIdFor(error)
                _resolveState.value = ResolveState.Error(errorResId)
            }
        }
    }

    private fun extractUrl(input: String): String {
        return UrlExtractor.extractFirstUrl(input) ?: input.trim()
    }

    fun resetResolveState() {
        _resolveState.value = ResolveState.Idle
    }

    /** Menjadwalkan download dan menunggu sampai masuk antrean (belum menunggu file selesai diunduh). */
    suspend fun enqueueDownload(
        post: ResolvedPost,
        downloadMp3Only: Boolean = false,
        selectedPhotoIndices: List<Int>? = null
    ): Result<List<Long>> {
        val kind = if (downloadMp3Only) MediaKind.AUDIO
        else if (post.type == PostType.SLIDESHOW) MediaKind.PHOTO
        else MediaKind.VIDEO
        return downloadScheduler.scheduleDownload(
            post = post,
            kind = kind,
            photoIndices = selectedPhotoIndices ?: emptyList(),
            explicitQuality = null   // lihat R6
        )
    }

    fun startDownload(post: ResolvedPost, downloadMp3Only: Boolean = false, selectedPhotoIndices: List<Int>? = null) {
        viewModelScope.launch { enqueueDownload(post, downloadMp3Only, selectedPhotoIndices) }
    }

    fun cancelDownload(postId: String) {
        downloadScheduler.cancelDownload(postId)
    }

    fun deletePost(postWithMedia: PostWithMedia, deleteFromGallery: Boolean) {
        deletePosts(listOf(postWithMedia), deleteFromGallery)
    }

    fun deletePosts(posts: List<PostWithMedia>, deleteFromGallery: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (deleteFromGallery) {
                posts.forEach { postWithMedia ->
                    postWithMedia.mediaItems.forEach { item ->
                        if (item.mediaStoreUri.isNotBlank()) {
                            try {
                                val uri = android.net.Uri.parse(item.mediaStoreUri)
                                getApplication<Application>().contentResolver.delete(uri, null, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            posts.forEach { postWithMedia ->
                database.postDao().deletePostById(postWithMedia.post.id)
            }
        }
    }

    suspend fun refreshLibrary() = withContext(Dispatchers.IO) {
        try {
            database.mediaItemDao().reconcileStuckDownloadingItems()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllLibrary(deleteFromGallery: Boolean) {
        viewModelScope.launch {
            if (deleteFromGallery) {
                val all = database.postDao().getAllPostsWithMediaSync()
                all.forEach { postWithMedia ->
                    postWithMedia.mediaItems.forEach { item ->
                        if (item.mediaStoreUri.isNotBlank()) {
                            try {
                                val uri = android.net.Uri.parse(item.mediaStoreUri)
                                getApplication<Application>().contentResolver.delete(uri, null, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            database.postDao().deleteAllPosts()
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _screenStack.value = listOf(AppScreen.Home)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(false)
            _screenStack.value = listOf(AppScreen.Onboarding)
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(langCode)
        }
    }

    fun updateTheme(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDynamicColor(enabled)
        }
    }

    fun updateDefaultQuality(quality: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultQuality(quality)
        }
    }

    fun updateQualityFallback(fallback: String) {
        viewModelScope.launch {
            userPreferencesRepository.setQualityFallback(fallback)
        }
    }

    fun updateDuplicateRule(rule: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDuplicateRule(rule)
        }
    }

    fun updateShowNotificationActions(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowNotificationActions(enabled)
        }
    }

    fun updateDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeveloperMode(enabled)
            com.aryaxzell.truedown.util.AppLogger.i("MainViewModel", "Developer mode set to $enabled")
        }
    }

    fun updateBatterySaver(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBatterySaver(enabled)
            com.aryaxzell.truedown.util.AppLogger.i("MainViewModel", "Battery saver set to $enabled")
        }
    }

    fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setWifiOnly(enabled)
            com.aryaxzell.truedown.util.AppLogger.i("MainViewModel", "Wi-Fi only set to $enabled")
        }
    }

    fun updateAutoDownloadOnDetect(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoDownloadOnDetect(enabled)
            com.aryaxzell.truedown.util.AppLogger.i("MainViewModel", "Auto download on detect set to $enabled")
        }
    }

    fun updateDohProvider(providerKey: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDohProvider(providerKey)
            (downloadProvider as? TikWmDownloadProvider)?.updateDohProvider(providerKey)
            com.aryaxzell.truedown.util.AppLogger.i("MainViewModel", "DNS over HTTPS provider changed to $providerKey")
        }
    }

    fun clearLogs() {
        com.aryaxzell.truedown.util.AppLogger.clear()
    }

    fun retryFailedDownload(postWithMedia: PostWithMedia) {
        viewModelScope.launch {
            val sourceUrl = postWithMedia.post.sourceUrl
            if (sourceUrl.isBlank()) return@launch

            val failedItems = postWithMedia.mediaItems.filter { it.status == "FAILED" || it.status == MediaStatus.FAILED.name }
            if (failedItems.isEmpty()) return@launch

            // 1. Mark failed items as PENDING to update UI states immediately
            for (item in failedItems) {
                database.mediaItemDao().updateMediaItemStatus(item.id, MediaStatus.PENDING.name)
            }

            // 2. Resolve link again to get fresh signed CDN URLs
            val result = downloadProvider.resolve(sourceUrl)
            result.onSuccess { resolvedPost ->
                // 3. Reschedule each of the previously failed media items
                for (item in failedItems) {
                    val kind = when (item.kind) {
                        "AUDIO", MediaKind.AUDIO.name -> MediaKind.AUDIO
                        "PHOTO", MediaKind.PHOTO.name -> MediaKind.PHOTO
                        else -> MediaKind.VIDEO
                    }
                    val photoIndices = if (kind == MediaKind.PHOTO) listOf(item.itemIndex) else emptyList()
                    downloadScheduler.scheduleDownload(
                        post = resolvedPost,
                        kind = kind,
                        photoIndices = photoIndices,
                        explicitQuality = item.quality
                    )
                }
            }.onFailure { error ->
                // Restore FAILED status if resolving fails
                for (item in failedItems) {
                    database.mediaItemDao().updateMediaItemStatus(item.id, MediaStatus.FAILED.name)
                }
            }
        }
    }
}
