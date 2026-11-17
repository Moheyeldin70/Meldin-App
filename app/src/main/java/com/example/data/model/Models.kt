package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ContentType {
    LONG_VIDEO,
    SHORT_VIDEO,
    IMAGE,
    ARTICLE;

    companion object {
        fun fromRaw(raw: String?): ContentType =
            entries.firstOrNull { it.name == raw } ?: ARTICLE
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val type: String = ContentType.ARTICLE.name,
    val title: String = "",
    val description: String = "",
    val bodyText: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val category: String = "",
    val durationSeconds: Long = 0L,
    val languageTag: String = "en",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): ContentType = ContentType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPost(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val type: String = ContentType.ARTICLE.name,
    val title: String = "",
    val description: String = "",
    val authorName: String = "",
    val thumbnailUrl: String = "",
    val mediaUrl: String = "",
    val category: String = "",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val savedAt: Timestamp? = null
) {
    fun resolvedType(): ContentType = ContentType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class PrivateItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = ContentType.ARTICLE.name,
    val title: String = "",
    val description: String = "",
    val bodyText: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val category: String = "",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): ContentType = ContentType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class AdminSecurityConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val passwordHash: String = "",
    val salt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
