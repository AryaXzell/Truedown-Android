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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen Content Layer
        Box(modifier = Modifier.fillMaxSize()) {
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

        // Floating Pill Navbar & Floating Pill Settings Button Overlay
        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Floating Pill Navbar (Home & Library)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("floating_pill_navbar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Home Pill Item
                        FloatingNavPillItem(
                            selected = currentScreen is AppScreen.Home,
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home,
                            label = stringResource(R.string.nav_home),
                            onClick = { viewModel.navigateTo(AppScreen.Home) },
                            testTag = "nav_item_home"
                        )

                        // Library Pill Item
                        FloatingNavPillItem(
                            selected = currentScreen is AppScreen.Library,
                            selectedIcon = Icons.Filled.PhotoLibrary,
                            unselectedIcon = Icons.Outlined.PhotoLibrary,
                            label = stringResource(R.string.nav_library),
                            onClick = { viewModel.navigateTo(AppScreen.Library) },
                            testTag = "nav_item_library"
                        )
                    }
                }

                // Individual Floating Pill Settings Button (Independent)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.navigateTo(AppScreen.Settings) }
                        .testTag("floating_settings_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavPillItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        label = "nav_pill_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_pill_content"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
