package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.localization.LocalMeldinStrings

@Composable
fun ExploreScreen(
    postsState: UiState<List<Post>>,
    savedPostIds: Set<String>,
    selectedTab: ContentType,
    searchQuery: String,
    selectedCategory: String?,
    onSelectTab: (ContentType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenPost: (Post) -> Unit,
    onToggleSave: (Post, Boolean) -> Unit,
    onNavigateToPrivateStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalMeldinStrings.current
    var showSearchBar by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.meldin_logo),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            IconButton(
                onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) onSearchQueryChange("")
                },
                modifier = Modifier.testTag("toggle_search_button")
            ) {
                Icon(
                    imageVector = if (showSearchBar) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = strings.searchHint,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("explore_search_input")
            )
        }

        // 4 Content Format Tabs: Long Videos, Short Videos, Images, Articles
        val tabs = ContentType.entries
        val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEach { tab ->
                val label = when (tab) {
                    ContentType.LONG_VIDEO -> strings.longVideos
                    ContentType.SHORT_VIDEO -> strings.shortVideos
                    ContentType.IMAGE -> strings.images
                    ContentType.ARTICLE -> strings.articles
                }
                val icon = when (tab) {
                    ContentType.LONG_VIDEO -> Icons.Filled.Movie
                    ContentType.SHORT_VIDEO -> Icons.Filled.SmartDisplay
                    ContentType.IMAGE -> Icons.Filled.Image
                    ContentType.ARTICLE -> Icons.AutoMirrored.Filled.Article
                }
                val isSelected = tab == selectedTab
                Tab(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        when (postsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = postsState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is UiState.Success -> {
                val tabPosts = remember(postsState.data, selectedTab) {
                    postsState.data.filter { it.resolvedType() == selectedTab }
                }
                val categories = remember(tabPosts) {
                    tabPosts.map { it.category }.filter { it.isNotBlank() }.distinct()
                }
                val filteredPosts = remember(tabPosts, searchQuery, selectedCategory) {
                    tabPosts.filter { post ->
                        val matchesCat = selectedCategory == null || post.category == selectedCategory
                        val q = searchQuery.trim().lowercase()
                        val matchesQuery = q.isEmpty() ||
                            post.title.lowercase().contains(q) ||
                            post.description.lowercase().contains(q) ||
                            post.category.lowercase().contains(q)
                        matchesCat && matchesQuery
                    }
                }

                if (categories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onSelectCategory(null) },
                                label = { Text(strings.allCategories) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { onSelectCategory(category) },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = Pair(selectedTab, filteredPosts),
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    label = "tab_feed_transition"
                ) { (tab, posts) ->
                    if (posts.isEmpty()) {
                        EmptyExploreState(
                            tab = tab,
                            onNavigateToPrivateStudio = onNavigateToPrivateStudio
                        )
                    } else {
                        when (tab) {
                            ContentType.LONG_VIDEO -> LongVideoFeed(
                                posts = posts,
                                savedIds = savedPostIds,
                                onOpenPost = onOpenPost,
                                onToggleSave = onToggleSave
                            )
                            ContentType.SHORT_VIDEO -> ShortVideoReelFeed(
                                posts = posts,
                                savedIds = savedPostIds,
                                onOpenPost = onOpenPost,
                                onToggleSave = onToggleSave
                            )
                            ContentType.IMAGE -> ImageGalleryFeed(
                                posts = posts,
                                savedIds = savedPostIds,
                                onOpenPost = onOpenPost,
                                onToggleSave = onToggleSave
                            )
                            ContentType.ARTICLE -> ArticleEditorialFeed(
                                posts = posts,
                                savedIds = savedPostIds,
                                onOpenPost = onOpenPost,
                                onToggleSave = onToggleSave
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongVideoFeed(
    posts: List<Post>,
    savedIds: Set<String>,
    onOpenPost: (Post) -> Unit,
    onToggleSave: (Post, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            val isSaved = post.id in savedIds
            Card(
                onClick = { onOpenPost(post) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_card_${post.id}")
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        MeldinImage(
                            url = post.thumbnailUrl.ifBlank { post.mediaUrl },
                            contentDescription = post.title,
                            fallbackType = ContentType.LONG_VIDEO,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                        if (post.durationSeconds > 0L) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = formatDuration(post.durationSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = post.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = post.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        PostCardFooter(
                            post = post,
                            isSaved = isSaved,
                            onToggleSave = { onToggleSave(post, isSaved) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortVideoReelFeed(
    posts: List<Post>,
    savedIds: Set<String>,
    onOpenPost: (Post) -> Unit,
    onToggleSave: (Post, Boolean) -> Unit
) {
    val strings = LocalMeldinStrings.current
    val pagerState = rememberPagerState(pageCount = { posts.size })

    VerticalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        pageSpacing = 14.dp,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val post = posts[page]
        val isSaved = post.id in savedIds

        Card(
            onClick = { onOpenPost(post) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxSize()
                .testTag("short_reel_${post.id}")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MeldinImage(
                    url = post.thumbnailUrl.ifBlank { post.mediaUrl },
                    contentDescription = post.title,
                    fallbackType = ContentType.SHORT_VIDEO,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )

                // Center Play Callout
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = strings.shortVideos,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Side Action Rail
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ReelRailMetric(
                        icon = Icons.Filled.FavoriteBorder,
                        label = "${post.likesCount}"
                    )
                    ReelRailMetric(
                        icon = Icons.Filled.ChatBubbleOutline,
                        label = "${post.commentsCount}"
                    )
                    IconButton(
                        onClick = { onToggleSave(post, isSaved) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = strings.save,
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                // Bottom Overlay Metadata
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, end = 80.dp, bottom = 22.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = post.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = post.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelRailMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun ImageGalleryFeed(
    posts: List<Post>,
    savedIds: Set<String>,
    onOpenPost: (Post) -> Unit,
    onToggleSave: (Post, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            val isSaved = post.id in savedIds
            Card(
                onClick = { onOpenPost(post) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_card_${post.id}")
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 5f)
                    ) {
                        MeldinImage(
                            url = post.mediaUrl.ifBlank { post.thumbnailUrl },
                            contentDescription = post.title,
                            fallbackType = ContentType.IMAGE,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { onToggleSave(post, isSaved) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = post.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleEditorialFeed(
    posts: List<Post>,
    savedIds: Set<String>,
    onOpenPost: (Post) -> Unit,
    onToggleSave: (Post, Boolean) -> Unit
) {
    val strings = LocalMeldinStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posts, key = { it.id }) { post ->
            val isSaved = post.id in savedIds
            val wordCount = post.bodyText.split(Regex("\\s+")).count { it.isNotBlank() }
            val readMins = ((wordCount / 180) + 1).coerceAtLeast(1)

            Card(
                onClick = { onOpenPost(post) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("article_card_${post.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val cover = post.thumbnailUrl.ifBlank { post.mediaUrl }
                    if (cover.isNotBlank()) {
                        MeldinImage(
                            url = cover,
                            contentDescription = post.title,
                            fallbackType = ContentType.ARTICLE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = post.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "$readMins ${strings.readTimeMin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (post.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = post.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    PostCardFooter(
                        post = post,
                        isSaved = isSaved,
                        onToggleSave = { onToggleSave(post, isSaved) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCardFooter(
    post: Post,
    isSaved: Boolean,
    onToggleSave: () -> Unit
) {
    val strings = LocalMeldinStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = post.authorName.ifBlank { strings.verifiedPublisher },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "${post.likesCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "${post.commentsCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onToggleSave,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isSaved) strings.saved else strings.save,
                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyExploreState(
    tab: ContentType,
    onNavigateToPrivateStudio: () -> Unit
) {
    val strings = LocalMeldinStrings.current
    val icon = when (tab) {
        ContentType.LONG_VIDEO -> Icons.Filled.PlayCircle
        ContentType.SHORT_VIDEO -> Icons.Filled.SmartDisplay
        ContentType.IMAGE -> Icons.Filled.Image
        ContentType.ARTICLE -> Icons.AutoMirrored.Filled.Article
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = strings.emptyFeedTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.emptyFeedSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onNavigateToPrivateStudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(strings.goToPrivateStudio)
            }
        }
    }
}
