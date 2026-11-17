package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.SecurityHashUtil
import com.example.data.model.AdminSecurityConfig
import com.example.data.model.Comment
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.PrivateItem
import com.example.data.model.SavedPost
import com.example.data.model.UiState
import com.example.data.repository.MeldinRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MeldinViewModel(
    private val repository: MeldinRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String?,
    val isEmailVerified: Boolean,
    val currentUserPhotoUrl: String?
) : ViewModel() {

    private companion object {
        const val TAG = "MeldinVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    // Selected feed tab (Long Videos, Short Videos, Images, Articles)
    private val _selectedTab = MutableStateFlow(ContentType.LONG_VIDEO)
    val selectedTab: StateFlow<ContentType> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Active post ID for detail sheet
    private val _activePostId = MutableStateFlow<String?>(null)
    val activePostId: StateFlow<String?> = _activePostId.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = repository.checkIsAdmin(currentUserId, currentUserEmail, isEmailVerified)
        }
    }

    val postsState: StateFlow<UiState<List<Post>>> = repository.observePublishedPosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing published posts", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load posts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val savedPostsState: StateFlow<UiState<List<SavedPost>>> = repository.observeSavedPosts(currentUserId)
        .map<List<SavedPost>, UiState<List<SavedPost>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing saved posts", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load saved library"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val privateItemsState: StateFlow<UiState<List<PrivateItem>>> = repository.observePrivateItems(currentUserId)
        .map<List<PrivateItem>, UiState<List<PrivateItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing private items", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load private studio"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val adminSecurityConfig: StateFlow<AdminSecurityConfig?> = repository.observeAdminSecurityConfig(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing admin security config", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val activeCommentsState: StateFlow<UiState<List<Comment>>> = _activePostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf<UiState<List<Comment>>>(UiState.Success(emptyList()))
            } else {
                repository.observeComments(postId)
                    .map<List<Comment>, UiState<List<Comment>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing comments", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load comments"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val isActivePostLiked: StateFlow<Boolean> = _activePostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf(false)
            } else {
                repository.observeIsPostLiked(postId, currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(false)
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    fun selectTab(type: ContentType) {
        _selectedTab.value = type
        _selectedCategory.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openPost(postId: String) {
        _activePostId.value = postId
    }

    fun closePost() {
        _activePostId.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    // -------------------------------------------------------------------------
    // User Engagement Actions
    // -------------------------------------------------------------------------
    fun toggleSavePost(post: Post, currentlySaved: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleSavePost(post, currentlySaved)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update library"
            }
        }
    }

    fun toggleLikeActivePost() {
        val postId = _activePostId.value ?: return
        val currentlyLiked = isActivePostLiked.value
        viewModelScope.launch {
            val res = repository.toggleLike(postId, currentlyLiked)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update like"
            }
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            val res = repository.addComment(postId, currentUserName, currentUserPhotoUrl, text)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to post comment"
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            val res = repository.deleteComment(postId, commentId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete comment"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private Studio Actions
    // -------------------------------------------------------------------------
    fun createPrivateItem(
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createPrivateItem(
                type, title, description, bodyText, mediaUrl, thumbnailUrl, category, durationSeconds
            )
            _isBusy.value = false
            if (res.isSuccess) {
                onComplete()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save private item"
            }
        }
    }

    fun updatePrivateItem(
        itemId: String,
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updatePrivateItem(
                itemId, type, title, description, bodyText, mediaUrl, thumbnailUrl, category, durationSeconds
            )
            _isBusy.value = false
            if (res.isSuccess) {
                onComplete()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update private item"
            }
        }
    }

    fun deletePrivateItem(itemId: String) {
        viewModelScope.launch {
            val res = repository.deletePrivateItem(itemId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete item"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Admin Vault & Publishing Actions
    // -------------------------------------------------------------------------
    fun setAndUnlockAdminPassword(password: String, hasExisting: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveAdminPassword(password, hasExisting)
            _isBusy.value = false
            if (res.isSuccess) {
                _isAdminUnlocked.value = true
            } else {
                onError(res.exceptionOrNull()?.localizedMessage ?: "Failed to save password")
            }
        }
    }

    fun verifyAndUnlockAdmin(password: String, onWrongPassword: () -> Unit) {
        val config = adminSecurityConfig.value ?: return
        if (SecurityHashUtil.verify(password, config.salt, config.passwordHash)) {
            _isAdminUnlocked.value = true
        } else {
            onWrongPassword()
        }
    }

    fun lockAdminDashboard() {
        _isAdminUnlocked.value = false
    }

    fun publishPost(
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long,
        languageTag: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishPost(
                currentUserName, type, title, description, bodyText, mediaUrl, thumbnailUrl,
                category, durationSeconds, languageTag
            )
            _isBusy.value = false
            if (res.isSuccess) {
                onComplete()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    fun updatePost(
        postId: String,
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updatePost(
                postId, type, title, description, bodyText, mediaUrl, thumbnailUrl, category, durationSeconds
            )
            _isBusy.value = false
            if (res.isSuccess) {
                onComplete()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update post"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val res = repository.deletePost(postId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete post"
            }
        }
    }

    fun publishStarterPack() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishStarterPack(currentUserName)
            _isBusy.value = false
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish starter showcase"
            }
        }
    }
}
