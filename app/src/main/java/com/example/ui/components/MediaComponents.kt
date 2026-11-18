package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.ContentType
import com.example.localization.LocalMeldinStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageCompressionHelper {
    suspend fun encodeUriToDataUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
            val maxDim = 720
            val ratio = minOf(
                maxDim.toFloat() / original.width.coerceAtLeast(1),
                maxDim.toFloat() / original.height.coerceAtLeast(1),
                1f
            )
            val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            var quality = 78
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 210_000 && quality > 35) {
                out.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun MeldinImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackType: ContentType = ContentType.IMAGE
) {
    val context = LocalContext.current
    if (url.startsWith("data:image")) {
        val bitmap = remember(url) {
            try {
                val base64Part = url.substringAfter("base64,", "")
                if (base64Part.isNotEmpty()) {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            MeldinMediaPlaceholder(fallbackType = fallbackType, modifier = modifier)
        }
    } else if (url.isNotBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                MeldinMediaPlaceholder(fallbackType = fallbackType, modifier = modifier)
            }
        )
    } else {
        MeldinMediaPlaceholder(fallbackType = fallbackType, modifier = modifier)
    }
}

@Composable
fun MeldinMediaPlaceholder(
    fallbackType: ContentType,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (fallbackType) {
        ContentType.LONG_VIDEO -> listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
        ContentType.SHORT_VIDEO -> listOf(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.primaryContainer
        )
        ContentType.IMAGE -> listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
        ContentType.ARTICLE -> listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        )
    }
    val icon = when (fallbackType) {
        ContentType.LONG_VIDEO -> Icons.Filled.PlayCircle
        ContentType.SHORT_VIDEO -> Icons.Filled.SmartDisplay
        ContentType.IMAGE -> Icons.Filled.Image
        ContentType.ARTICLE -> Icons.AutoMirrored.Filled.Article
    }

    Box(
        modifier = modifier.background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
fun MeldinVideoPlayer(
    mediaUrl: String,
    thumbnailUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    val strings = LocalMeldinStrings.current
    var isPlaying by remember(mediaUrl) { mutableStateOf(autoPlay) }
    var isPrepared by remember(mediaUrl) { mutableStateOf(false) }
    var hasError by remember(mediaUrl) { mutableStateOf(false) }
    var reloadNonce by remember { mutableStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(mediaUrl, reloadNonce) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (mediaUrl.isBlank() || hasError) {
            if (thumbnailUrl.isNotBlank()) {
                MeldinImage(
                    url = thumbnailUrl,
                    contentDescription = strings.videoPlaybackUnavailable,
                    modifier = Modifier.fillMaxSize(),
                    fallbackType = ContentType.LONG_VIDEO
                )
            } else {
                MeldinMediaPlaceholder(
                    fallbackType = ContentType.LONG_VIDEO,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (mediaUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.videoPlaybackUnavailable,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    hasError = false
                                    isPrepared = false
                                    reloadNonce++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.retry)
                            }
                            if (mediaUrl.startsWith("http")) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl))
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.openLink)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setOnPreparedListener { mp: MediaPlayer ->
                            isPrepared = true
                            mp.isLooping = true
                            if (autoPlay || isPlaying) {
                                start()
                                isPlaying = true
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            hasError = true
                            isPrepared = false
                            true
                        }
                        setVideoURI(Uri.parse(mediaUrl))
                    }
                },
                update = { view ->
                    if (view.tag != "$mediaUrl#$reloadNonce") {
                        view.tag = "$mediaUrl#$reloadNonce"
                        isPrepared = false
                        view.setVideoURI(Uri.parse(mediaUrl))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isPrepared && thumbnailUrl.isNotBlank()) {
                MeldinImage(
                    url = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    fallbackType = ContentType.LONG_VIDEO
                )
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            if (isPrepared && !isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            videoViewRef?.start()
                            isPlaying = true
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (isPrepared) {
                IconButton(
                    onClick = {
                        videoViewRef?.let { v ->
                            if (v.isPlaying) {
                                v.pause()
                                isPlaying = false
                            } else {
                                v.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ContentTypeBadge(type: ContentType, modifier: Modifier = Modifier) {
    val strings = LocalMeldinStrings.current
    val (label, bgColor, fgColor) = when (type) {
        ContentType.LONG_VIDEO -> Triple(
            strings.longVideos,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        ContentType.SHORT_VIDEO -> Triple(
            strings.shortVideos,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
        ContentType.IMAGE -> Triple(
            strings.images,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
        ContentType.ARTICLE -> Triple(
            strings.articles,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return ""
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
