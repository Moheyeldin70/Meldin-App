package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.PreferencesManager
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.repository.MeldinRepository
import com.example.localization.LocalMeldinStrings
import com.example.localization.SupportedLanguages
import com.example.localization.getStringsForLanguage
import com.example.ui.components.AccountScreen
import com.example.ui.components.AuthScreen
import com.example.ui.components.ContentFeedScreen
import com.example.ui.components.LibraryScreen
import com.example.ui.components.MeldinSplashScreen
import com.example.ui.components.PostDetailDialog
import com.example.ui.theme.MeldinTheme
import com.example.ui.theme.ThemePreference
import com.example.ui.viewmodel.MeldinViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object MeldinRoutes {
    const val ROUTE_LONG_VIDEOS = "long_videos"
    const val ROUTE_SHORT_VIDEOS = "short_videos"
    const val ROUTE_IMAGES = "images"
    const val ROUTE_ARTICLES = "articles"
    const val ROUTE_LIBRARY = "library"
    const val ROUTE_ACCOUNT = "account"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeldinRoot()
        }
    }
}

@Composable
fun MeldinRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember(context) { PreferencesManager(context.applicationContext) }

    val themePreference by prefsManager.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.SYSTEM
    )
    val languageCode by prefsManager.languageCode.collectAsStateWithLifecycle(
        initialValue = "en"
    )
    val strings = remember(languageCode) { getStringsForLanguage(languageCode) }
    val isRtl = remember(languageCode) {
        SupportedLanguages.firstOrNull { it.code == languageCode }?.isRtl == true
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    CompositionLocalProvider(
        LocalMeldinStrings provides strings,
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MeldinTheme(themePreference = themePreference) {
            AnimatedContent(
                targetState = showSplash,
                transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(380)) },
                label = "splash_to_app"
            ) { splashActive ->
                if (splashActive) {
                    MeldinSplashScreen(onFinished = { showSplash = false })
                } else {
                    val user = currentUser
                    if (user == null) {
                        AuthScreen(
                            themePreference = themePreference,
                            currentLanguageCode = languageCode,
                            onChangeTheme = { pref -> scope.launch { prefsManager.setThemePreference(pref) } },
                            onSelectLanguage = { code -> scope.launch { prefsManager.setLanguageCode(code) } },
                            onAuthSuccess = {}
                        )
                    } else {
                        key(user.uid) {
                            AuthenticatedMeldinShell(
                                user = user,
                                themePreference = themePreference,
                                currentLanguageCode = languageCode,
                                onChangeTheme = { pref -> scope.launch { prefsManager.setThemePreference(pref) } },
                                onSelectLanguage = { code -> scope.launch { prefsManager.setLanguageCode(code) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedMeldinShell(
    user: FirebaseUser,
    themePreference: ThemePreference,
    currentLanguageCode: String,
    onChangeTheme: (ThemePreference) -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val strings = LocalMeldinStrings.current
    val uid = user.uid
    val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Meldin Member"
    val photoUrl = user.photoUrl?.toString()

    val viewModel: MeldinViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MeldinViewModel(
                    repository = MeldinRepository(db),
                    currentUserId = uid,
                    currentUserName = displayName,
                    currentUserEmail = user.email,
                    isEmailVerified = user.isEmailVerified,
                    currentUserPhotoUrl = photoUrl
                )
            }
        }
    )

    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val savedPostsState by viewModel.savedPostsState.collectAsStateWithLifecycle()
    val privateItemsState by viewModel.privateItemsState.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminSecurityConfig.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val activePostId by viewModel.activePostId.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
    val isActivePostLiked by viewModel.isActivePostLiked.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnAccountScreen = currentDestination?.route == MeldinRoutes.ROUTE_ACCOUNT

    var libraryInitialSubTab by rememberSaveable { mutableStateOf(0) }
    var fallbackPost by remember { mutableStateOf<Post?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val savedPostIds = remember(savedPostsState) {
        (savedPostsState as? UiState.Success)?.data?.map { it.postId }?.toSet() ?: emptySet()
    }

    val bottomNavItems = remember(strings) {
        listOf(
            BottomNavItem(
                route = MeldinRoutes.ROUTE_LONG_VIDEOS,
                label = strings.longVideos,
                selectedIcon = Icons.Filled.Movie,
                unselectedIcon = Icons.Outlined.Movie
            ),
            BottomNavItem(
                route = MeldinRoutes.ROUTE_SHORT_VIDEOS,
                label = strings.shortVideos,
                selectedIcon = Icons.Filled.SmartDisplay,
                unselectedIcon = Icons.Outlined.SmartDisplay
            ),
            BottomNavItem(
                route = MeldinRoutes.ROUTE_IMAGES,
                label = strings.images,
                selectedIcon = Icons.Filled.Image,
                unselectedIcon = Icons.Outlined.Image
            ),
            BottomNavItem(
                route = MeldinRoutes.ROUTE_ARTICLES,
                label = strings.articles,
                selectedIcon = Icons.AutoMirrored.Filled.Article,
                unselectedIcon = Icons.AutoMirrored.Outlined.Article
            ),
            BottomNavItem(
                route = MeldinRoutes.ROUTE_LIBRARY,
                label = strings.library,
                selectedIcon = Icons.Filled.Bookmarks,
                unselectedIcon = Icons.Outlined.Bookmarks
            )
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToAccount() {
        navController.navigate(MeldinRoutes.ROUTE_ACCOUNT) {
            launchSingleTop = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail && !isOnAccountScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isOnAccountScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = MeldinRoutes.ROUTE_LONG_VIDEOS,
                        enterTransition = { fadeIn(tween(220)) },
                        exitTransition = { fadeOut(tween(180)) }
                    ) {
                        composable(MeldinRoutes.ROUTE_LONG_VIDEOS) {
                            ContentFeedScreen(
                                contentType = ContentType.LONG_VIDEO,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    viewModel.openPost(post.id)
                                },
                                onToggleSave = viewModel::toggleSavePost,
                                onNavigateToPrivateStudio = {
                                    libraryInitialSubTab = 1
                                    navigateToTab(MeldinRoutes.ROUTE_LIBRARY)
                                },
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_SHORT_VIDEOS) {
                            ContentFeedScreen(
                                contentType = ContentType.SHORT_VIDEO,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    viewModel.openPost(post.id)
                                },
                                onToggleSave = viewModel::toggleSavePost,
                                onNavigateToPrivateStudio = {
                                    libraryInitialSubTab = 1
                                    navigateToTab(MeldinRoutes.ROUTE_LIBRARY)
                                },
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_IMAGES) {
                            ContentFeedScreen(
                                contentType = ContentType.IMAGE,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    viewModel.openPost(post.id)
                                },
                                onToggleSave = viewModel::toggleSavePost,
                                onNavigateToPrivateStudio = {
                                    libraryInitialSubTab = 1
                                    navigateToTab(MeldinRoutes.ROUTE_LIBRARY)
                                },
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_ARTICLES) {
                            ContentFeedScreen(
                                contentType = ContentType.ARTICLE,
                                postsState = postsState,
                                savedPostIds = savedPostIds,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    viewModel.openPost(post.id)
                                },
                                onToggleSave = viewModel::toggleSavePost,
                                onNavigateToPrivateStudio = {
                                    libraryInitialSubTab = 1
                                    navigateToTab(MeldinRoutes.ROUTE_LIBRARY)
                                },
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_LIBRARY) {
                            LibraryScreen(
                                savedPostsState = savedPostsState,
                                privateItemsState = privateItemsState,
                                allPosts = allPosts,
                                initialSubTab = libraryInitialSubTab,
                                isBusy = isBusy,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    viewModel.openPost(post.id)
                                },
                                onUnsavePost = { saved ->
                                    val stub = Post(id = saved.postId, title = saved.title, type = saved.type)
                                    viewModel.toggleSavePost(stub, currentlySaved = true)
                                },
                                onCreatePrivateItem = viewModel::createPrivateItem,
                                onUpdatePrivateItem = viewModel::updatePrivateItem,
                                onDeletePrivateItem = viewModel::deletePrivateItem,
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_ACCOUNT) {
                            AccountScreen(
                                userName = displayName,
                                userEmail = user.email,
                                userPhotoUrl = photoUrl,
                                isAdmin = isAdmin,
                                isAdminUnlocked = isAdminUnlocked,
                                adminConfig = adminConfig,
                                postsState = postsState,
                                isBusy = isBusy,
                                themePreference = themePreference,
                                currentLanguageCode = currentLanguageCode,
                                onChangeTheme = onChangeTheme,
                                onSelectLanguage = onSelectLanguage,
                                onVerifyAdminPassword = viewModel::verifyAndUnlockAdmin,
                                onSetAdminPassword = viewModel::setAndUnlockAdminPassword,
                                onLockAdmin = viewModel::lockAdminDashboard,
                                onPublishPost = viewModel::publishPost,
                                onUpdatePost = viewModel::updatePost,
                                onDeletePost = viewModel::deletePost,
                                onPublishStarterPack = viewModel::publishStarterPack,
                                onAccountSwitched = {},
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    val activePost = activePostId?.let { id ->
        allPosts.firstOrNull { it.id == id } ?: fallbackPost?.takeIf { it.id == id }
    }
    if (activePost != null) {
        PostDetailDialog(
            post = activePost,
            isLiked = isActivePostLiked,
            isSaved = activePost.id in savedPostIds,
            commentsState = activeCommentsState,
            currentUserId = uid,
            isAdmin = isAdmin,
            onToggleLike = viewModel::toggleLikeActivePost,
            onToggleSave = { viewModel.toggleSavePost(activePost, activePost.id in savedPostIds) },
            onAddComment = { text -> viewModel.addComment(activePost.id, text) },
            onDeleteComment = { commentId -> viewModel.deleteComment(activePost.id, commentId) },
            onDismiss = {
                fallbackPost = null
                viewModel.closePost()
            }
        )
    }
}
