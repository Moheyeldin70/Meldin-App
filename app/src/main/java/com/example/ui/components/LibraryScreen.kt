package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.PrivateItem
import com.example.data.model.SavedPost
import com.example.data.model.UiState
import com.example.localization.LocalMeldinStrings
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    savedPostsState: UiState<List<SavedPost>>,
    privateItemsState: UiState<List<PrivateItem>>,
    allPosts: List<Post>,
    initialSubTab: Int = 0,
    isBusy: Boolean,
    onOpenPost: (Post) -> Unit,
    onUnsavePost: (SavedPost) -> Unit,
    onCreatePrivateItem: (ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onUpdatePrivateItem: (String, ContentType, String, String, String, String, String, String, Long, () -> Unit) -> Unit,
    onDeletePrivateItem: (String) -> Unit,
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalMeldinStrings.current
    var selectedSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var showCreateEditor by remember { mutableStateOf(false) }
    var editingPrivateItem by remember { mutableStateOf<PrivateItem?>(null) }
    var viewingPrivateItem by remember { mutableStateOf<PrivateItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.library,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier.testTag("library_account_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = strings.account,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("library_saved_tab")
                    ) {
                        Text(strings.savedTab, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("library_private_tab")
                    ) {
                        Text(strings.privateStudioTab, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (selectedSubTab == 0) {
                SavedLibraryList(
                    savedPostsState = savedPostsState,
                    allPosts = allPosts,
                    onOpenPost = onOpenPost,
                    onUnsavePost = onUnsavePost
                )
            } else {
                PrivateStudioList(
                    privateItemsState = privateItemsState,
                    onOpenItem = { viewingPrivateItem = it },
                    onEditItem = {
                        editingPrivateItem = it
                        showCreateEditor = true
                    },
                    onDeleteItem = onDeletePrivateItem
                )
            }
        }

        if (selectedSubTab == 1) {
            FloatingActionButton(
                onClick = {
                    editingPrivateItem = null
                    showCreateEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("create_private_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.createPrivateItem)
            }
        }
    }

    if (showCreateEditor) {
        ContentEditorDialog(
            dialogTitle = if (editingPrivateItem == null) strings.createPrivateItem else strings.editPrivateItem,
            submitLabel = if (editingPrivateItem == null) strings.save else strings.update,
            initialType = editingPrivateItem?.resolvedType() ?: ContentType.ARTICLE,
            initialTitle = editingPrivateItem?.title ?: "",
            initialCategory = editingPrivateItem?.category ?: "Personal",
            initialDescription = editingPrivateItem?.description ?: "",
            initialBodyText = editingPrivateItem?.bodyText ?: "",
            initialMediaUrl = editingPrivateItem?.mediaUrl ?: "",
            initialThumbnailUrl = editingPrivateItem?.thumbnailUrl ?: "",
            initialDurationSeconds = editingPrivateItem?.durationSeconds ?: 0L,
            showLanguageSelector = false,
            isBusy = isBusy,
            onDismiss = { showCreateEditor = false },
            onSubmit = { type, title, category, desc, body, media, thumb, duration, _ ->
                val existing = editingPrivateItem
                if (existing == null) {
                    onCreatePrivateItem(type, title, desc, body, media, thumb, category, duration) {
                        showCreateEditor = false
                    }
                } else {
                    onUpdatePrivateItem(existing.id, type, title, desc, body, media, thumb, category, duration) {
                        showCreateEditor = false
                    }
                }
            }
        )
    }

    viewingPrivateItem?.let { item ->
        PrivateItemDetailDialog(
            item = item,
            onEdit = {
                viewingPrivateItem = null
                editingPrivateItem = item
                showCreateEditor = true
            },
            onDismiss = { viewingPrivateItem = null }
        )
    }
}

@Composable
private fun SavedLibraryList(
    savedPostsState: UiState<List<SavedPost>>,
    allPosts: List<Post>,
    onOpenPost: (Post) -> Unit,
    onUnsavePost: (SavedPost) -> Unit
) {
    val strings = LocalMeldinStrings.current
    when (savedPostsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(savedPostsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val saved = savedPostsState.data
            if (saved.isEmpty()) {
                LibraryEmptyState(
                    icon = Icons.Filled.Bookmarks,
                    title = strings.emptySavedTitle,
                    subtitle = strings.emptySavedSubtitle
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(saved, key = { it.id }) { item ->
                        Card(
                            onClick = {
                                val livePost = allPosts.firstOrNull { it.id == item.postId } ?: Post(
                                    id = item.postId,
                                    authorName = item.authorName,
                                    type = item.type,
                                    title = item.title,
                                    description = item.description,
                                    thumbnailUrl = item.thumbnailUrl,
                                    mediaUrl = item.mediaUrl,
                                    category = item.category,
                                    durationSeconds = item.durationSeconds
                                )
                                onOpenPost(livePost)
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MeldinImage(
                                    url = item.thumbnailUrl.ifBlank { item.mediaUrl },
                                    contentDescription = item.title,
                                    fallbackType = item.resolvedType(),
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    ContentTypeBadge(type = item.resolvedType())
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.authorName.isNotBlank()) {
                                        Text(
                                            text = item.authorName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onUnsavePost(item) }) {
                                    Icon(
                                        imageVector = Icons.Filled.BookmarkRemove,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.primary
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

@Composable
private fun PrivateStudioList(
    privateItemsState: UiState<List<PrivateItem>>,
    onOpenItem: (PrivateItem) -> Unit,
    onEditItem: (PrivateItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val strings = LocalMeldinStrings.current
    when (privateItemsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(privateItemsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val itemsList = privateItemsState.data
            if (itemsList.isEmpty()) {
                LibraryEmptyState(
                    icon = Icons.Filled.Lock,
                    title = strings.emptyPrivateTitle,
                    subtitle = strings.emptyPrivateSubtitle
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(itemsList, key = { it.id }) { item ->
                        Card(
                            onClick = { onOpenItem(item) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MeldinImage(
                                    url = item.thumbnailUrl.ifBlank { item.mediaUrl },
                                    contentDescription = item.title,
                                    fallbackType = item.resolvedType(),
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ContentTypeBadge(type = item.resolvedType())
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = strings.privateBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Column {
                                    IconButton(onClick = { onEditItem(item) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = strings.edit)
                                    }
                                    IconButton(onClick = { onDeleteItem(item.id) }) {
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
}

@Composable
private fun LibraryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrivateItemDetailDialog(
    item: PrivateItem,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalMeldinStrings.current
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            ContentTypeBadge(type = item.resolvedType())
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = strings.edit)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                when (item.resolvedType()) {
                    ContentType.LONG_VIDEO -> {
                        MeldinVideoPlayer(
                            mediaUrl = item.mediaUrl,
                            thumbnailUrl = item.thumbnailUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    }
                    ContentType.SHORT_VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MeldinVideoPlayer(
                                mediaUrl = item.mediaUrl,
                                thumbnailUrl = item.thumbnailUrl,
                                modifier = Modifier
                                    .width(260.dp)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }
                    ContentType.IMAGE -> {
                        MeldinImage(
                            url = item.mediaUrl.ifBlank { item.thumbnailUrl },
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        )
                    }
                    ContentType.ARTICLE -> {
                        val cover = item.thumbnailUrl.ifBlank { item.mediaUrl }
                        if (cover.isNotBlank()) {
                            MeldinImage(
                                url = cover,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (item.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.bodyText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = item.bodyText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContentEditorDialog(
    dialogTitle: String,
    submitLabel: String,
    initialType: ContentType,
    initialTitle: String,
    initialCategory: String,
    initialDescription: String,
    initialBodyText: String,
    initialMediaUrl: String,
    initialThumbnailUrl: String,
    initialDurationSeconds: Long,
    showLanguageSelector: Boolean,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        type: ContentType,
        title: String,
        category: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        durationSeconds: Long,
        languageTag: String
    ) -> Unit
) {
    val strings = LocalMeldinStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }
    var bodyText by remember { mutableStateOf(initialBodyText) }
    var mediaUrl by remember { mutableStateOf(initialMediaUrl) }
    var thumbnailUrl by remember { mutableStateOf(initialThumbnailUrl) }
    var durationStr by remember {
        mutableStateOf(if (initialDurationSeconds > 0) initialDurationSeconds.toString() else "")
    }
    var languageTag by remember { mutableStateOf("en") }
    var isCompressingImage by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCompressingImage = true
                val encoded = ImageCompressionHelper.encodeUriToDataUri(context, uri)
                isCompressingImage = false
                if (encoded != null) {
                    if (type == ContentType.IMAGE) {
                        mediaUrl = encoded
                    } else {
                        thumbnailUrl = encoded
                    }
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUrl = uri.toString()
        }
    }

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
                            Text(
                                text = dialogTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Button(
                            onClick = {
                                onSubmit(
                                    type,
                                    title,
                                    category,
                                    description,
                                    bodyText,
                                    mediaUrl,
                                    thumbnailUrl,
                                    durationStr.toLongOrNull() ?: 0L,
                                    languageTag
                                )
                            },
                            enabled = title.isNotBlank() && !isBusy && !isCompressingImage,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("editor_submit_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(submitLabel)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Content Type Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ContentType.entries) { candidate ->
                        val label = when (candidate) {
                            ContentType.LONG_VIDEO -> strings.longVideos
                            ContentType.SHORT_VIDEO -> strings.shortVideos
                            ContentType.IMAGE -> strings.images
                            ContentType.ARTICLE -> strings.articles
                        }
                        FilterChip(
                            selected = type == candidate,
                            onClick = { type = candidate },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 160) title = it },
                    label = { Text(strings.titleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { if (it.length <= 50) category = it },
                    label = { Text(strings.categoryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 1000) description = it },
                    label = { Text(strings.descriptionLabel) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == ContentType.ARTICLE) {
                    OutlinedTextField(
                        value = bodyText,
                        onValueChange = { if (it.length <= 20000) bodyText = it },
                        label = { Text(strings.articleBodyLabel) },
                        minLines = 6,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == ContentType.LONG_VIDEO || type == ContentType.SHORT_VIDEO) {
                    OutlinedTextField(
                        value = durationStr,
                        onValueChange = { raw ->
                            if (raw.all { it.isDigit() } && raw.length <= 5) durationStr = raw
                        },
                        label = { Text(strings.durationLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = if (mediaUrl.startsWith("data:image")) "(Device Image Selected)" else mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text(strings.mediaUrlLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isCompressingImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isCompressingImage) strings.loading else strings.pickImageFromGallery,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (type == ContentType.LONG_VIDEO || type == ContentType.SHORT_VIDEO) {
                        OutlinedButton(
                            onClick = {
                                videoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.pickVideoFromGallery,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = if (thumbnailUrl.startsWith("data:image")) "(Device Thumbnail Selected)" else thumbnailUrl,
                    onValueChange = { thumbnailUrl = it },
                    label = { Text(strings.thumbnailUrlLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showLanguageSelector) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("en", "ar", "ja", "zh", "es", "fr")) { tag ->
                            FilterChip(
                                selected = languageTag == tag,
                                onClick = { languageTag = tag },
                                label = { Text(tag.uppercase()) }
                            )
                        }
                    }
                }
            }
        }
    }
}
