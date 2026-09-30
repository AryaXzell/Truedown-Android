package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.PostType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.BuiltInAudioPlayerScreen
import com.example.ui.screens.BuiltInVideoPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SlideshowGridScreen
import com.example.ui.theme.TruedownTheme
import com.example.util.LocaleHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)

        val isShareIntent = handleIncomingShareIntent(intent)
        viewModel.initializeStartingScreen(isShareIntent)

        setContent {
            val preferences by viewModel.preferences.collectAsState()

            // Dynamically apply locale when preference changes
            LaunchedEffect(preferences.language) {
                LocaleHelper.applyLanguage(this@MainActivity, preferences.language)
            }

            TruedownTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.dynamicColor
            ) {
                MainAppContent(
                    viewModel = viewModel,
                    onFinishActivity = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (sharedText.isNotBlank()) {
                Toast.makeText(this, getString(R.string.home_checking_link), Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    val result = viewModel.downloadProvider.resolve(sharedText)
                    result.onSuccess { resolvedPost ->
                        if (resolvedPost.type == PostType.SLIDESHOW && resolvedPost.photoUrls.size > 1) {
                            // Multiple photo slideshow -> open grid (PRD F2)
                            viewModel.navigateTo(AppScreen.SlideshowGrid(resolvedPost))
                        } else {
                            // Video or 1-photo slideshow -> background download & auto close (PRD F1)
                            viewModel.startDownload(resolvedPost, downloadMp3Only = false)
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.preview_starting_download),
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }.onFailure {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_invalid_link),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return true
            }
        }
        return false
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onFinishActivity: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    val showBottomBar = currentScreen is AppScreen.Home || currentScreen is AppScreen.Library

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Home,
                        onClick = { viewModel.navigateTo(AppScreen.Home) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is AppScreen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.nav_home)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Library,
                        onClick = { viewModel.navigateTo(AppScreen.Library) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is AppScreen.Library) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                contentDescription = stringResource(R.string.nav_library)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_library)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.Onboarding -> {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinished = {
                            viewModel.setOnboardingCompleted()
                        }
                    )
                }
                is AppScreen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { viewModel.navigateTo(AppScreen.Settings) },
                        onNavigateToLibrary = { viewModel.navigateTo(AppScreen.Library) },
                        onNavigateToItemDetail = { postWithMedia ->
                            if (postWithMedia.post.type == "VIDEO" || postWithMedia.post.type == PostType.VIDEO.name) {
                                viewModel.navigateTo(AppScreen.VideoPlayer(postWithMedia))
                            } else {
                                viewModel.navigateTo(AppScreen.Library)
                            }
                        }
                    )
                }
                is AppScreen.Library -> {
                    BackHandler {
                        viewModel.navigateTo(AppScreen.Home)
                    }
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenVideoPlayer = { postWithMedia ->
                            viewModel.navigateTo(AppScreen.VideoPlayer(postWithMedia))
                        },
                        onOpenAudioPlayer = { postWithMedia ->
                            viewModel.navigateTo(AppScreen.AudioPlayer(postWithMedia))
                        },
                        onOpenSlideshow = { _ ->
                            // Detail in library
                        }
                    )
                }
                is AppScreen.Preview -> {
                    PreviewScreen(
                        post = screen.post,
                        viewModel = viewModel,
                        onBack = { viewModel.popBackStack() }
                    )
                }
                is AppScreen.SlideshowGrid -> {
                    SlideshowGridScreen(
                        post = screen.post,
                        viewModel = viewModel,
                        onBack = { viewModel.popBackStack() },
                        onOpenPhotoViewer = { index ->
                            viewModel.navigateTo(AppScreen.PhotoViewer(screen.post, index))
                        }
                    )
                }
                is AppScreen.PhotoViewer -> {
                    PhotoViewerScreen(
                        post = screen.post,
                        initialIndex = screen.initialIndex,
                        viewModel = viewModel,
                        onClose = { viewModel.popBackStack() }
                    )
                }
                is AppScreen.VideoPlayer -> {
                    BuiltInVideoPlayerScreen(
                        postWithMedia = screen.postWithMedia,
                        onClose = { viewModel.popBackStack() }
                    )
                }
                is AppScreen.AudioPlayer -> {
                    BuiltInAudioPlayerScreen(
                        postWithMedia = screen.postWithMedia,
                        onClose = { viewModel.popBackStack() }
                    )
                }
                is AppScreen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.popBackStack() }
                    )
                }
            }
        }
    }
}
