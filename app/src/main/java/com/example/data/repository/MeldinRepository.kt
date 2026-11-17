package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.auth.SecurityHashUtil
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminSecurityConfig
import com.example.data.model.Comment
import com.example.data.model.ContentType
import com.example.data.model.Post
import com.example.data.model.PrivateItem
import com.example.data.model.SavedPost
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MeldinRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val OWNER_ADMIN_EMAIL = "moheyeldinemad9@gmail.com"
        private const val ADMIN_VAULT_DOC = "admin_vault"
        private val ALL_CONTENT_TYPE_NAMES = ContentType.entries.map { it.name }
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun checkIsAdmin(uid: String, email: String?, isEmailVerified: Boolean): Boolean {
        if (email.equals(OWNER_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified) {
            return true
        }
        return try {
            val doc = db.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // Published Platform Posts
    // -------------------------------------------------------------------------
    fun observePublishedPosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereIn("type", ALL_CONTENT_TYPE_NAMES)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun publishPost(
        authorName: String,
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long,
        languageTag: String
    ): Result<Unit> {
        var docPath = "posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCategory = category.trim().ifEmpty { "General" }
            val cleanAuthor = authorName.trim().ifEmpty { "Meldin Admin" }.take(80)
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 160) { "Title must be 1-160 characters." }
            require(description.length <= 1000) { "Description must be <= 1000 characters." }
            require(bodyText.length <= 20000) { "Article body must be <= 20000 characters." }
            require(mediaUrl.length <= 350000) { "Media data exceeds limit." }
            require(thumbnailUrl.length <= 350000) { "Thumbnail data exceeds limit." }
            require(cleanCategory.length <= 50) { "Category must be 1-50 characters." }

            val docRef = db.collection("posts").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to cleanAuthor,
                "type" to type.name,
                "title" to cleanTitle,
                "description" to description.trim(),
                "category" to cleanCategory,
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "languageTag" to languageTag.take(10).ifBlank { "en" },
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (bodyText.isNotBlank()) payload["bodyText"] = bodyText.trim()
            if (mediaUrl.isNotBlank()) payload["mediaUrl"] = mediaUrl.trim()
            if (thumbnailUrl.isNotBlank()) payload["thumbnailUrl"] = thumbnailUrl.trim()

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String,
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long
    ): Result<Unit> {
        val docPath = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID required" }
            val cleanTitle = title.trim()
            val cleanCategory = category.trim().ifEmpty { "General" }
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 160) { "Title must be 1-160 characters." }

            val updates = mapOf(
                "type" to type.name,
                "title" to cleanTitle,
                "description" to description.trim().take(1000),
                "bodyText" to bodyText.trim().take(20000),
                "mediaUrl" to mediaUrl.trim().take(350000),
                "thumbnailUrl" to thumbnailUrl.trim().take(350000),
                "category" to cleanCategory.take(50),
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(docPath).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val docPath = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID required" }
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishStarterPack(authorName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().ifEmpty { "Meldin Studio" }.take(80)
            val batch = db.batch()
            val starters = listOf(
                mapOf(
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "type" to ContentType.LONG_VIDEO.name,
                    "title" to "Designing Calm Digital Spaces",
                    "description" to "An editorial documentary exploring warm minimalism, kinetic typography, and thoughtful soundscapes in modern mobile software.",
                    "mediaUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    "thumbnailUrl" to "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200&q=80",
                    "category" to "Design",
                    "durationSeconds" to 420L,
                    "languageTag" to "en",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "type" to ContentType.SHORT_VIDEO.name,
                    "title" to "60s Amber Sunset Timelapse",
                    "description" to "Golden hour light sweeping over desert ridges captured in vertical cinema format.",
                    "mediaUrl" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "thumbnailUrl" to "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=900&q=80",
                    "category" to "Cinema",
                    "durationSeconds" to 45L,
                    "languageTag" to "en",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "type" to ContentType.IMAGE.name,
                    "title" to "Terracotta Light Study No. 4",
                    "description" to "Architectural shadows meeting warm sandstone and burnt orange geometry at dusk.",
                    "mediaUrl" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80",
                    "thumbnailUrl" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                    "category" to "Photography",
                    "durationSeconds" to 0L,
                    "languageTag" to "en",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "type" to ContentType.ARTICLE.name,
                    "title" to "Why Multilingual Craft Matters More Than Ever",
                    "description" to "How right-to-left typography, localized pacing, and high-contrast warm palettes build truly global creative communities.",
                    "bodyText" to "Great digital publishing feels native no matter which language you read first.\n\nWhen a reader switches between English, Arabic, Japanese, or Chinese, layout rhythm should adapt with zero friction. Mirroring directional layouts for RTL scripts while preserving clean visual hierarchy turns translation into hospitality.\n\nOn Meldin, every story, short reel, image study, and long-form film shares one cohesive editorial rhythm—warm paper tones in daylight, deep espresso obsidian after sunset, and vibrant orange highlights guiding attention cleanly.",
                    "thumbnailUrl" to "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=1200&q=80",
                    "category" to "Editorial",
                    "durationSeconds" to 0L,
                    "languageTag" to "en",
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            starters.forEach { data ->
                val ref = db.collection("posts").document()
                batch.set(ref, data)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Comments & Likes
    // -------------------------------------------------------------------------
    fun observeComments(postId: String): Flow<List<Comment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Comment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(
        postId: String,
        authorName: String,
        authorPhotoUrl: String?,
        text: String
    ): Result<Unit> {
        val path = "posts/$postId/comments"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim()
            require(cleanText.isNotEmpty() && cleanText.length <= 1000) { "Comment must be 1-1000 characters." }

            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val payload = mutableMapOf<String, Any>(
                "postId" to postId,
                "userId" to uid,
                "authorName" to authorName.trim().ifEmpty { "Reader" }.take(80),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!authorPhotoUrl.isNullOrBlank()) {
                payload["authorPhotoUrl"] = authorPhotoUrl.take(500)
            }

            commentRef.set(payload).await()
            postRef.update(
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val path = "posts/$postId/comments/$commentId"
        return try {
            requireUserId()
            val postRef = db.collection("posts").document(postId)
            postRef.collection("comments").document(commentId).delete().await()
            val postSnap = postRef.get().await()
            val currentCount = postSnap.getLong("commentsCount") ?: 0L
            if (currentCount > 0L) {
                postRef.update(
                    mapOf(
                        "commentsCount" to (currentCount - 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeIsPostLiked(postId: String, userId: String): Flow<Boolean> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/likes/$userId"
        emitAll(
            db.collection("posts").document(postId).collection("likes").document(userId)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val path = "posts/$postId/likes"
        return try {
            val uid = requireUserId()
            val postRef = db.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(uid)

            db.runTransaction { tx ->
                val postSnap = tx.get(postRef)
                val currentLikes = postSnap.getLong("likesCount") ?: 0L
                if (currentlyLiked) {
                    tx.delete(likeRef)
                    val next = (currentLikes - 1L).coerceAtLeast(0L)
                    tx.update(
                        postRef,
                        mapOf(
                            "likesCount" to next,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        likeRef,
                        mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        postRef,
                        mapOf(
                            "likesCount" to (currentLikes + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Personal Saved Library
    // -------------------------------------------------------------------------
    fun observeSavedPosts(userId: String): Flow<List<SavedPost>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/saved_posts"
        emitAll(
            db.collection("users").document(userId).collection("saved_posts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.savedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleSavePost(post: Post, isCurrentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauth"}/saved_posts/${post.id}"
        return try {
            val activeUid = requireUserId()
            val ref = db.collection("users").document(activeUid)
                .collection("saved_posts").document(post.id)
            if (isCurrentlySaved) {
                ref.delete().await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "postId" to post.id,
                    "userId" to activeUid,
                    "type" to post.type,
                    "title" to post.title.take(160),
                    "description" to post.description.take(1000),
                    "authorName" to post.authorName.take(80),
                    "category" to post.category.take(50),
                    "durationSeconds" to post.durationSeconds.coerceIn(0L, 86400L),
                    "savedAt" to FieldValue.serverTimestamp()
                )
                if (post.thumbnailUrl.isNotBlank()) payload["thumbnailUrl"] = post.thumbnailUrl.take(350000)
                if (post.mediaUrl.isNotBlank()) payload["mediaUrl"] = post.mediaUrl.take(350000)
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Private Studio (Strictly Owner-Private Content)
    // -------------------------------------------------------------------------
    fun observePrivateItems(userId: String): Flow<List<PrivateItem>> = flow {
        val uid = requireUserId()
        val path = "users/$userId/private_items"
        emitAll(
            db.collection("users").document(userId).collection("private_items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PrivateItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPrivateItem(
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long
    ): Result<Unit> {
        var docPath = "users/unauth/private_items"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCategory = category.trim().ifEmpty { "Personal" }
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 160) { "Title must be 1-160 characters." }

            val docRef = db.collection("users").document(uid)
                .collection("private_items").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "type" to type.name,
                "title" to cleanTitle,
                "description" to description.trim().take(1000),
                "category" to cleanCategory.take(50),
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (bodyText.isNotBlank()) payload["bodyText"] = bodyText.trim().take(20000)
            if (mediaUrl.isNotBlank()) payload["mediaUrl"] = mediaUrl.trim().take(350000)
            if (thumbnailUrl.isNotBlank()) payload["thumbnailUrl"] = thumbnailUrl.trim().take(350000)

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePrivateItem(
        itemId: String,
        type: ContentType,
        title: String,
        description: String,
        bodyText: String,
        mediaUrl: String,
        thumbnailUrl: String,
        category: String,
        durationSeconds: Long
    ): Result<Unit> {
        var docPath = "users/unauth/private_items/$itemId"
        return try {
            val uid = requireUserId()
            require(itemId.isNotBlank()) { "Item ID required" }
            val cleanTitle = title.trim()
            val cleanCategory = category.trim().ifEmpty { "Personal" }
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 160) { "Title must be 1-160 characters." }

            val docRef = db.collection("users").document(uid)
                .collection("private_items").document(itemId)
            docPath = docRef.path

            val updates = mapOf(
                "type" to type.name,
                "title" to cleanTitle,
                "description" to description.trim().take(1000),
                "bodyText" to bodyText.trim().take(20000),
                "mediaUrl" to mediaUrl.trim().take(350000),
                "thumbnailUrl" to thumbnailUrl.trim().take(350000),
                "category" to cleanCategory.take(50),
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePrivateItem(itemId: String): Result<Unit> {
        var docPath = "users/unauth/private_items/$itemId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("private_items").document(itemId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Admin Vault Password Protection
    // -------------------------------------------------------------------------
    fun observeAdminSecurityConfig(userId: String): Flow<AdminSecurityConfig?> = flow {
        val uid = requireUserId()
        val path = "users/$userId/private_settings/$ADMIN_VAULT_DOC"
        emitAll(
            db.collection("users").document(userId)
                .collection("private_settings").document(ADMIN_VAULT_DOC)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            AdminSecurityConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveAdminPassword(rawPassword: String, hasExisting: Boolean): Result<Unit> {
        var docPath = "users/unauth/private_settings/$ADMIN_VAULT_DOC"
        return try {
            val uid = requireUserId()
            require(rawPassword.length >= 4) { "Password must be at least 4 characters." }
            val salt = SecurityHashUtil.generateSalt()
            val hash = SecurityHashUtil.sha256(rawPassword, salt)
            val docRef = db.collection("users").document(uid)
                .collection("private_settings").document(ADMIN_VAULT_DOC)
            docPath = docRef.path
            if (hasExisting) {
                docRef.update(
                    mapOf(
                        "passwordHash" to hash,
                        "salt" to salt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "passwordHash" to hash,
                        "salt" to salt,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
