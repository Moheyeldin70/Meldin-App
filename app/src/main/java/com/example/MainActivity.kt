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
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.data.model.AdminSecurityConfig
import com.example.data.model.Comment
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.PrivateItem
import com.example.data.model.SavedPost
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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

    val isFirebaseReady = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty() || runCatching {
            FirebaseApp.initializeApp(context) != null
        }.getOrDefault(false)
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
                } else if (!isFirebaseReady) {
                    FirebaseConfigMissingScreen()
                } else {
                    MeldinAuthenticatedHost(
                        themePreference = themePreference,
                        languageCode = languageCode,
                        onChangeTheme = { pref -> scope.launch { prefsManager.setThemePreference(pref) } },
                        onSelectLanguage = { code -> scope.launch { prefsManager.setLanguageCode(code) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeldinAuthenticatedHost(
    themePreference: ThemePreference,
    languageCode: String,
    onChangeTheme: (ThemePreference) -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth, lifecycleOwner) {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val refreshed = auth.currentUser
                if (currentUser?.uid != refreshed?.uid || (currentUser == null) != (refreshed == null)) {
                    currentUser = refreshed
                }
            }
        }
        currentUser = auth.currentUser
        auth.addAuthStateListener(authListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            auth.removeAuthStateListener(authListener)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            runCatching {
                val credentialManager = CredentialManager.create(context)
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { currentUser = auth.currentUser },
                    onUnauthenticated = {},
                    scope = scope
                )
            }
        } else {
            currentUser = auth.currentUser
        }
    }

    val user = currentUser
    if (user == null || user.uid.isBlank()) {
        AuthScreen(
            themePreference = themePreference,
            currentLanguageCode = languageCode,
            onChangeTheme = onChangeTheme,
            onSelectLanguage = onSelectLanguage,
            onAuthSuccess = { currentUser = auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedMeldinShell(
                user = user,
                themePreference = themePreference,
                currentLanguageCode = languageCode,
                onChangeTheme = onChangeTheme,
                onSelectLanguage = onSelectLanguage,
                onAccountSwitched = { currentUser = auth.currentUser }
            )
        }
    }
}

@Composable
private fun FirebaseConfigMissingScreen(
    detail: String = "This APK was built without app/google-services.json. Add app/google-services.json to your repository or set the GOOGLE_SERVICES_JSON secret in GitHub Actions and rebuild the APK."
) {
    val strings = LocalMeldinStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.meldin_logo),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                )
                Text(
                    text = "Firebase Configuration Missing",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
    onSelectLanguage: (String) -> Unit,
    onAccountSwitched: () -> Unit
) {
    val context = LocalContext.current
    val uid = user.uid
    val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Meldin Member"
    val photoUrl = user.photoUrl?.toString()

    val firestoreResult = remember(context) {
        runCatching {
            val databaseId = context.getString(R.string.firestore_database_id)
            require(databaseId.isNotBlank()) { "firestore_database_id is blank" }
            FirebaseFirestore.getInstance(databaseId)
        }
    }
    val firestoreError = firestoreResult.exceptionOrNull()
    if (firestoreError != null) {
        FirebaseConfigMissingScreen(
            detail = firestoreError.localizedMessage ?: "Failed to initialize Firestore database."
        )
        return
    }
    val db = firestoreResult.getOrThrow()

    val viewModel: MeldinViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
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

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    MeldinAuthenticatedScaffold(
        uid = uid,
        displayName = displayName,
        userEmail = user.email,
        photoUrl = photoUrl,
        isAdmin = isAdmin,
        isAdminUnlocked = isAdminUnlocked,
        adminConfig = adminConfig,
        postsState = postsState,
        savedPostsState = savedPostsState,
        privateItemsState = privateItemsState,
        searchQuery = searchQuery,
        selectedCategory = selectedCategory,
        isBusy = isBusy,
        activePostId = activePostId,
        activeCommentsState = activeCommentsState,
        isActivePostLiked = isActivePostLiked,
        themePreference = themePreference,
        currentLanguageCode = currentLanguageCode,
        snackbarHostState = snackbarHostState,
        onChangeTheme = onChangeTheme,
        onSelectLanguage = onSelectLanguage,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onSelectCategory = viewModel::selectCategory,
        onOpenPost = viewModel::openPost,
        onClosePost = viewModel::closePost,
        onToggleSavePost = viewModel::toggleSavePost,
        onToggleLikeActivePost = viewModel::toggleLikeActivePost,
        onAddComment = viewModel::addComment,
        onDeleteComment = viewModel::deleteComment,
        onCreatePrivateItem = viewModel::createPrivateItem,
        onUpdatePrivateItem = viewModel::updatePrivateItem,
        onDeletePrivateItem = viewModel::deletePrivateItem,
        onVerifyAdminPassword = viewModel::verifyAndUnlockAdmin,
        onSetAdminPassword = viewModel::setAndUnlockAdminPassword,
        onLockAdmin = viewModel::lockAdminDashboard,
        onPublishPost = viewModel::publishPost,
        onUpdatePost = viewModel::updatePost,
        onDeletePost = viewModel::deletePost,
        onPublishStarterPack = viewModel::publishStarterPack,
        onAccountSwitched = onAccountSwitched
    )
}

@Composable
internal fun MeldinAuthenticatedScaffold(
    uid: String,
    displayName: String,
    userEmail: String?,
    photoUrl: String?,
    isAdmin: Boolean,
    isAdminUnlocked: Boolean,
    adminConfig: AdminSecurityConfig?,
    postsState: UiState<List<Post>>,
    savedPostsState: UiState<List<SavedPost>>,
    privateItemsState: UiState<List<PrivateItem>>,
    searchQuery: String,
    selectedCategory: String?,
    isBusy: Boolean,
    activePostId: String?,
    activeCommentsState: UiState<List<Comment>>,
    isActivePostLiked: Boolean,
    themePreference: ThemePreference,
    currentLanguageCode: String,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onChangeTheme: (ThemePreference) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenPost: (String) -> Unit,
    onClosePost: () -> Unit,
    onToggleSavePost: (Post, Boolean) -> Unit,
    onToggleLikeActivePost: () -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String, String) -> Unit,
    onCreatePrivateItem: (ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onUpdatePrivateItem: (String, ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onDeletePrivateItem: (String) -> Unit,
    onVerifyAdminPassword: (String, () -> Unit) -> Unit,
    onSetAdminPassword: (String, Boolean, (String) -> Unit) -> Unit,
    onLockAdmin: () -> Unit,
    onPublishPost: (ContentType, String, String, String, String, String, String, Long, String, () -> Unit) -> Unit,
    onUpdatePost: (String, ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onPublishStarterPack: () -> Unit,
    onAccountSwitched: () -> Unit
) {
    val strings = LocalMeldinStrings.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnAccountScreen = currentDestination?.route == MeldinRoutes.ROUTE_ACCOUNT

    var libraryInitialSubTab by rememberSaveable { mutableStateOf(0) }
    var fallbackPost by remember { mutableStateOf<Post?>(null) }

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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = MeldinRoutes.ROUTE_LONG_VIDEOS,
                        modifier = Modifier.fillMaxSize(),
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
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectCategory = onSelectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    onOpenPost(post.id)
                                },
                                onToggleSave = onToggleSavePost,
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
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectCategory = onSelectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    onOpenPost(post.id)
                                },
                                onToggleSave = onToggleSavePost,
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
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectCategory = onSelectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    onOpenPost(post.id)
                                },
                                onToggleSave = onToggleSavePost,
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
                                onSearchQueryChange = onSearchQueryChange,
                                onSelectCategory = onSelectCategory,
                                onOpenPost = { post ->
                                    fallbackPost = post
                                    onOpenPost(post.id)
                                },
                                onToggleSave = onToggleSavePost,
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
                                    onOpenPost(post.id)
                                },
                                onUnsavePost = { saved ->
                                    val stub = Post(id = saved.postId, title = saved.title, type = saved.type)
                                    onToggleSavePost(stub, true)
                                },
                                onCreatePrivateItem = onCreatePrivateItem,
                                onUpdatePrivateItem = onUpdatePrivateItem,
                                onDeletePrivateItem = onDeletePrivateItem,
                                onOpenAccount = ::navigateToAccount
                            )
                        }

                        composable(MeldinRoutes.ROUTE_ACCOUNT) {
                            AccountScreen(
                                userName = displayName,
                                userEmail = userEmail,
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
                                onVerifyAdminPassword = onVerifyAdminPassword,
                                onSetAdminPassword = onSetAdminPassword,
                                onLockAdmin = onLockAdmin,
                                onPublishPost = onPublishPost,
                                onUpdatePost = onUpdatePost,
                                onDeletePost = onDeletePost,
                                onPublishStarterPack = onPublishStarterPack,
                                onAccountSwitched = onAccountSwitched,
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
            onToggleLike = onToggleLikeActivePost,
            onToggleSave = { onToggleSavePost(activePost, activePost.id in savedPostIds) },
            onAddComment = { text -> onAddComment(activePost.id, text) },
            onDeleteComment = { commentId -> onDeleteComment(activePost.id, commentId) },
            onDismiss = {
                fallbackPost = null
                onClosePost()
            }
        )
    }
}
