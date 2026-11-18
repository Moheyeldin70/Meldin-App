package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import com.example.data.auth.onGoogleSignInClicked
import com.example.data.auth.signOut
import com.example.data.model.AdminSecurityConfig
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.localization.LocalMeldinStrings
import com.example.localization.SupportedLanguages
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkPrimaryOrange
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightPrimaryOrange
import com.example.ui.theme.ThemePreference

@Composable
fun AccountScreen(
    userName: String,
    userEmail: String?,
    userPhotoUrl: String?,
    isAdmin: Boolean,
    isAdminUnlocked: Boolean,
    adminConfig: AdminSecurityConfig?,
    postsState: UiState<List<Post>>,
    isBusy: Boolean,
    themePreference: ThemePreference,
    currentLanguageCode: String,
    onChangeTheme: (ThemePreference) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onVerifyAdminPassword: (String, () -> Unit) -> Unit,
    onSetAdminPassword: (String, Boolean, (String) -> Unit) -> Unit,
    onLockAdmin: () -> Unit,
    onPublishPost: (ContentType, String, String, String, String, String, String, Long, String, () -> Unit) -> Unit,
    onUpdatePost: (String, ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onPublishStarterPack: () -> Unit,
    onAccountSwitched: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalMeldinStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAdminDashboard by remember { mutableStateOf(false) }
    var switchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("account_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.cancel
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = strings.account,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // User Profile Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!userPhotoUrl.isNullOrBlank()) {
                    MeldinImage(
                        url = userPhotoUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAdmin) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = strings.verifiedPublisher,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Theme Mode Switcher (Palette 3752 Light & Palette 4191 Dark)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.appearance,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(LightBackground)
                                .border(1.dp, LightPrimaryOrange, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                                .border(1.dp, DarkPrimaryOrange, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        Triple(ThemePreference.SYSTEM, strings.themeSystem, Icons.Filled.SettingsBrightness),
                        Triple(ThemePreference.LIGHT, strings.themeLight, Icons.Filled.LightMode),
                        Triple(ThemePreference.DARK, strings.themeDark, Icons.Filled.DarkMode)
                    )
                    options.forEachIndexed { idx, (pref, label, _) ->
                        SegmentedButton(
                            selected = themePreference == pref,
                            onClick = { onChangeTheme(pref) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("theme_${pref.name.lowercase()}")
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Multilingual Selector
        Surface(
            onClick = { showLanguageDialog = true },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_language_picker")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                        val activeLang = SupportedLanguages.firstOrNull { it.code == currentLanguageCode }
                        Text(
                            text = "${activeLang?.nativeName ?: "English"} (${activeLang?.englishName ?: "English"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = strings.edit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Password-Protected Admin Dashboard Entry
        Surface(
            onClick = { showAdminDashboard = true },
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_admin_dashboard")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAdminUnlocked) Icons.Filled.LockOpen else Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.adminDashboard,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isAdmin) strings.adminLockSubtitle else strings.adminAccessRestricted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Sign-In Options
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.signInOptions, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.googleSignInNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (switchError != null) {
                    Text(
                        text = switchError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = {
                        switchError = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = onAccountSwitched,
                            onAuthError = { switchError = it },
                            scope = scope
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_google_account_button")
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.switchAccount)
                }

                Button(
                    onClick = {
                        onLockAdmin()
                        signOut(credentialManager = credentialManager, scope = scope)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.signOut)
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentCode = currentLanguageCode,
            onSelect = { code ->
                onSelectLanguage(code)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAdminDashboard) {
        AdminDashboardDialog(
            isAdmin = isAdmin,
            isUnlocked = isAdminUnlocked,
            adminConfig = adminConfig,
            postsState = postsState,
            isBusy = isBusy,
            onVerifyPassword = onVerifyAdminPassword,
            onSetPassword = onSetAdminPassword,
            onLock = onLockAdmin,
            onPublishPost = onPublishPost,
            onUpdatePost = onUpdatePost,
            onDeletePost = onDeletePost,
            onPublishStarterPack = onPublishStarterPack,
            onDismiss = { showAdminDashboard = false }
        )
    }
}

@Composable
fun LanguagePickerDialog(
    currentCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalMeldinStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.chooseLanguage) },
        text = {
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(SupportedLanguages, key = { it.code }) { lang ->
                    val selected = lang.code == currentCode
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang.code) }
                            .testTag("lang_option_${lang.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun AdminDashboardDialog(
    isAdmin: Boolean,
    isUnlocked: Boolean,
    adminConfig: AdminSecurityConfig?,
    postsState: UiState<List<Post>>,
    isBusy: Boolean,
    onVerifyPassword: (String, () -> Unit) -> Unit,
    onSetPassword: (String, Boolean, (String) -> Unit) -> Unit,
    onLock: () -> Unit,
    onPublishPost: (ContentType, String, String, String, String, String, String, Long, String, () -> Unit) -> Unit,
    onUpdatePost: (String, ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onPublishStarterPack: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalMeldinStrings.current
    var showPublisherEditor by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                            }
                            Text(strings.adminDashboard, style = MaterialTheme.typography.titleLarge)
                        }
                        if (isAdmin && isUnlocked) {
                            Row {
                                IconButton(onClick = { showChangePasswordDialog = true }) {
                                    Icon(Icons.Filled.Key, contentDescription = strings.changePassword)
                                }
                                IconButton(onClick = onLock) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Lock")
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isAdmin && isUnlocked) {
                    FloatingActionButton(
                        onClick = {
                            editingPost = null
                            showPublisherEditor = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("admin_publish_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = strings.publishNewContent)
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isAdmin) {
                    // Non-admin explanation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = strings.adminAccessRestricted,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.adminRestrictedDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (!isUnlocked) {
                    AdminPasswordGate(
                        hasExistingPassword = adminConfig != null,
                        isBusy = isBusy,
                        onVerifyPassword = onVerifyPassword,
                        onSetPassword = { pwd, onErr -> onSetPassword(pwd, false, onErr) }
                    )
                } else {
                    // Unlocked Admin Publishing Studio
                    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // Summary Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AdminMetricCard(
                                    label = strings.totalPublished,
                                    count = posts.size,
                                    modifier = Modifier.weight(1f)
                                )
                                AdminMetricCard(
                                    label = strings.longVideos,
                                    count = posts.count { it.resolvedType() == ContentType.LONG_VIDEO },
                                    modifier = Modifier.weight(1f)
                                )
                                AdminMetricCard(
                                    label = strings.shortVideos,
                                    count = posts.count { it.resolvedType() == ContentType.SHORT_VIDEO },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editingPost = null
                                        showPublisherEditor = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_new_post_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.publishNewContent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                if (posts.isEmpty()) {
                                    OutlinedButton(
                                        onClick = onPublishStarterPack,
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("publish_starter_pack_button")
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.publishStarterPack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        items(posts, key = { it.id }) { post ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MeldinImage(
                                        url = post.thumbnailUrl.ifBlank { post.mediaUrl },
                                        contentDescription = post.title,
                                        fallbackType = post.resolvedType(),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        ContentTypeBadge(type = post.resolvedType())
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = post.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${post.category} • ${post.likesCount} ♥ • ${post.commentsCount} 💬",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingPost = post
                                        showPublisherEditor = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = strings.edit)
                                    }
                                    IconButton(onClick = { onDeletePost(post.id) }) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublisherEditor) {
        ContentEditorDialog(
            dialogTitle = if (editingPost == null) strings.publishNewContent else strings.editContent,
            submitLabel = if (editingPost == null) strings.publish else strings.update,
            initialType = editingPost?.resolvedType() ?: ContentType.LONG_VIDEO,
            initialTitle = editingPost?.title ?: "",
            initialCategory = editingPost?.category ?: "Featured",
            initialDescription = editingPost?.description ?: "",
            initialBodyText = editingPost?.bodyText ?: "",
            initialMediaUrl = editingPost?.mediaUrl ?: "",
            initialThumbnailUrl = editingPost?.thumbnailUrl ?: "",
            initialDurationSeconds = editingPost?.durationSeconds ?: 0L,
            showLanguageSelector = editingPost == null,
            isBusy = isBusy,
            onDismiss = { showPublisherEditor = false },
            onSubmit = { type, title, category, desc, body, media, thumb, duration, langTag ->
                val existing = editingPost
                if (existing == null) {
                    onPublishPost(type, title, desc, body, media, thumb, category, duration, langTag) {
                        showPublisherEditor = false
                    }
                } else {
                    onUpdatePost(existing.id, type, title, desc, body, media, thumb, category, duration) {
                        showPublisherEditor = false
                    }
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var newPwd by remember { mutableStateOf("") }
        var confirmPwd by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(strings.changePassword) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPwd,
                        onValueChange = { newPwd = it; errorMsg = null },
                        label = { Text(strings.enterAdminPassword) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPwd,
                        onValueChange = { confirmPwd = it; errorMsg = null },
                        label = { Text(strings.confirmPassword) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        newPwd.length < 4 -> errorMsg = strings.passwordTooShortError
                        newPwd != confirmPwd -> errorMsg = strings.passwordMismatchError
                        else -> onSetPassword(newPwd, true) { errorMsg = it }.also {
                            if (errorMsg == null) showChangePasswordDialog = false
                        }
                    }
                }) {
                    Text(strings.update)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun AdminPasswordGate(
    hasExistingPassword: Boolean,
    isBusy: Boolean,
    onVerifyPassword: (String, () -> Unit) -> Unit,
    onSetPassword: (String, (String) -> Unit) -> Unit
) {
    val strings = LocalMeldinStrings.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasExistingPassword) strings.adminDashboard else strings.createAdminPassword,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.adminLockSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = null },
            label = { Text(strings.enterAdminPassword) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_password_input")
        )

        if (!hasExistingPassword) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMsg = null },
                label = { Text(strings.confirmPassword) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_confirm_input")
            )
        }

        if (errorMsg != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (!hasExistingPassword) {
                    when {
                        password.length < 4 -> errorMsg = strings.passwordTooShortError
                        password != confirmPassword -> errorMsg = strings.passwordMismatchError
                        else -> onSetPassword(password) { errorMsg = it }
                    }
                } else {
                    onVerifyPassword(password) {
                        errorMsg = strings.wrongPasswordError
                    }
                }
            },
            enabled = !isBusy && password.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_unlock_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (hasExistingPassword) strings.unlockDashboard else strings.setPasswordAndUnlock)
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
