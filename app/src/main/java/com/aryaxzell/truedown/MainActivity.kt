package com.aryaxzell.truedown

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.aryaxzell.truedown.ui.components.GlobalDownloadProgressBar
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aryaxzell.truedown.domain.model.PostType
import com.aryaxzell.truedown.domain.model.ProviderError
import com.aryaxzell.truedown.ui.AppScreen
import com.aryaxzell.truedown.ui.MainViewModel
import com.aryaxzell.truedown.ui.screens.BuiltInAudioPlayerScreen
import com.aryaxzell.truedown.ui.screens.BuiltInVideoPlayerScreen
import com.aryaxzell.truedown.ui.screens.HomeScreen
import com.aryaxzell.truedown.ui.screens.LibraryScreen
import com.aryaxzell.truedown.ui.screens.OnboardingScreen
import com.aryaxzell.truedown.ui.screens.PhotoViewerScreen
import com.aryaxzell.truedown.ui.screens.PreviewScreen
import com.aryaxzell.truedown.ui.screens.SettingsScreen
import com.aryaxzell.truedown.ui.screens.SlideshowGridScreen
import com.aryaxzell.truedown.ui.theme.TruedownTheme
import com.aryaxzell.truedown.util.LocaleHelper
import com.aryaxzell.truedown.util.NotificationHelper
import com.aryaxzell.truedown.util.UrlExtractor
import com.aryaxzell.truedown.work.CleanupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPipMode.value = isInPictureInPictureMode
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splash.setKeepOnScreenCondition { !viewModel.startupReady.value }

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule periodic cache and temporary files cleanup
        CleanupWorker.schedule(this)

        val isShareIntent = if (savedInstanceState == null) handleIncomingShareIntent(intent) else false
        viewModel.initializeStartingScreen(isShareIntent)

        setContent {
            val preferences by viewModel.preferences.collectAsState()
            val pipMode by isInPipMode.collectAsState()

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
                    isInPipMode = pipMode,
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
                val urlText = UrlExtractor.extractFirstUrl(sharedText) ?: UrlExtractor.cleanCandidate(sharedText)
                if (urlText.isNotBlank()) {
                    Toast.makeText(this, getString(R.string.home_checking_link), Toast.LENGTH_SHORT).show()
                    viewModel.resolveUrl(urlText)
                    return true
                }
            }
        }
        return false
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    isInPipMode: Boolean = false,
    onFinishActivity: () -> Unit
) {
    val startupReady by viewModel.startupReady.collectAsState()
    if (!startupReady) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val globalDownloadStatus by viewModel.globalDownloadStatus.collectAsState()
    val showBottomBar = (currentScreen is AppScreen.Home || currentScreen is AppScreen.Library) && !isInPipMode

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen Content Layer with Material 3 Motion Patterns
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val enterSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                val exitSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )

                if (initialState is AppScreen.Home && targetState is AppScreen.Library) {
                    // Premium sliding transition to the left (forward)
                    (slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it / 3 } +
                            fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { -it / 3 } +
                                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                        )
                } else if (initialState is AppScreen.Library && targetState is AppScreen.Home) {
                    // Premium sliding transition to the right (backward)
                    (slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { -it / 3 } +
                            fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                        )
                } else if (targetState is AppScreen.Preview || targetState is AppScreen.VideoPlayer || targetState is AppScreen.AudioPlayer || targetState is AppScreen.SlideshowGrid || targetState is AppScreen.Settings) {
                    // Container Transform Scale & Fade expansion into detail / preview / players
                    (scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) +
                            fadeIn(animationSpec = enterSpec))
                        .togetherWith(
                            scaleOut(targetScale = 1.04f, animationSpec = exitSpec) +
                                    fadeOut(animationSpec = exitSpec)
                        )
                } else {
                    // Container Transform Collapse back to parent
                    (scaleIn(initialScale = 1.04f, animationSpec = enterSpec) +
                            fadeIn(animationSpec = enterSpec))
                        .togetherWith(
                            scaleOut(targetScale = 0.90f, animationSpec = exitSpec) +
                                    fadeOut(animationSpec = exitSpec)
                        )
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "screen_motion_transition"
        ) { screen ->
            when (screen) {
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
                        },
                        onNavigateToDownloader = {
                            viewModel.navigateTo(AppScreen.Home)
                        }
                    )
                }
                is AppScreen.Preview -> {
                    PreviewScreen(
                        post = screen.post,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.resetResolveState()
                            val popped = viewModel.popBackStack()
                            if (!popped) {
                                viewModel.navigateTo(AppScreen.Home)
                            }
                        }
                    )
                }
                is AppScreen.SlideshowGrid -> {
                    SlideshowGridScreen(
                        post = screen.post,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.resetResolveState()
                            val popped = viewModel.popBackStack()
                            if (!popped) {
                                viewModel.navigateTo(AppScreen.Home)
                            }
                        },
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
                        isInPipMode = isInPipMode,
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

        // Global Progress Indicator for active downloads managed by WorkManager
        GlobalDownloadProgressBar(
            globalStatus = globalDownloadStatus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (showBottomBar) 88.dp else 16.dp),
            onClick = {
                viewModel.navigateTo(AppScreen.Library)
            }
        )

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
