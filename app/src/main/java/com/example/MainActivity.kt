package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.PreferencesManager
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.repository.MeldinRepository
import com.example.localization.LocalMeldinStrings
import com.example.localization.SupportedLanguages
import com.example.localization.getStringsForLanguage
import com.example.ui.components.AccountScreen
import com.example.ui.components.AuthScreen
import com.example.ui.components.ExploreScreen
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

enum class AppDestination {
    EXPLORE,
    LIBRARY,
    ACCOUNT
}

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
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val activePostId by viewModel.activePostId.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
    val isActivePostLiked by viewModel.isActivePostLiked.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(AppDestination.EXPLORE) }
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

    if (destination != AppDestination.EXPLORE) {
        BackHandler { destination = AppDestination.EXPLORE }
    }

    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val savedPostIds = remember(savedPostsState) {
        (savedPostsState as? UiState.Success)?.data?.map { it.postId }?.toSet() ?: emptySet()
    }

    val navItems = listOf(
        Triple(AppDestination.EXPLORE, strings.explore, Pair(Icons.Filled.Explore, Icons.Outlined.Explore)),
        Triple(AppDestination.LIBRARY, strings.library, Pair(Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks)),
        Triple(AppDestination.ACCOUNT, strings.account, Pair(Icons.Filled.Person, Icons.Outlined.Person))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        navItems.forEach { (dest, label, icons) ->
                            val selected = destination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { destination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
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
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        navItems.forEach { (dest, label, icons) ->
                            val selected = destination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { destination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = destination,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "destination_transition"
                    ) { activeDest ->
                        when (activeDest) {
                            AppDestination.EXPLORE -> {
                                ExploreScreen(
                                    postsState = postsState,
                                    savedPostIds = savedPostIds,
                                    selectedTab = selectedTab,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    onSelectTab = viewModel::selectTab,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onSelectCategory = viewModel::selectCategory,
                                    onOpenPost = { post ->
                                        fallbackPost = post
                                        viewModel.openPost(post.id)
                                    },
                                    onToggleSave = viewModel::toggleSavePost,
                                    onNavigateToPrivateStudio = {
                                        libraryInitialSubTab = 1
                                        destination = AppDestination.LIBRARY
                                    }
                                )
                            }
                            AppDestination.LIBRARY -> {
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
                                    onDeletePrivateItem = viewModel::deletePrivateItem
                                )
                            }
                            AppDestination.ACCOUNT -> {
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
                                    onAccountSwitched = {}
                                )
                            }
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
