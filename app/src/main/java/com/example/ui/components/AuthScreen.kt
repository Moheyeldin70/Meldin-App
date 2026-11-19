package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.onGoogleSignInClicked
import com.example.localization.LocalMeldinStrings
import com.example.localization.SupportedLanguages
import com.example.ui.theme.ThemePreference

@Composable
fun AuthScreen(
    themePreference: ThemePreference,
    currentLanguageCode: String,
    onChangeTheme: (ThemePreference) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val strings = LocalMeldinStrings.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language + Theme Quick Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val langName = SupportedLanguages.firstOrNull { it.code == currentLanguageCode }?.nativeName ?: "English"
                FilledTonalButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.testTag("auth_language_button")
                ) {
                    Icon(Icons.Filled.Language, contentDescription = strings.languageLabel, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(langName, style = MaterialTheme.typography.labelLarge)
                }

                val nextTheme = when (themePreference) {
                    ThemePreference.SYSTEM -> ThemePreference.DARK
                    ThemePreference.DARK -> ThemePreference.LIGHT
                    ThemePreference.LIGHT -> ThemePreference.SYSTEM
                }
                val themeIcon = if (themePreference == ThemePreference.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode
                val themeLabel = when (themePreference) {
                    ThemePreference.SYSTEM -> strings.themeSystem
                    ThemePreference.LIGHT -> strings.themeLight
                    ThemePreference.DARK -> strings.themeDark
                }
                FilledTonalButton(
                    onClick = { onChangeTheme(nextTheme) },
                    modifier = Modifier.testTag("auth_theme_button")
                ) {
                    Icon(themeIcon, contentDescription = strings.appearance, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(themeLabel, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.meldin_logo),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Feature summary cards
                FeatureRow(
                    icon = Icons.Filled.PlayCircle,
                    title = "${strings.longVideos} • ${strings.shortVideos} • ${strings.images} • ${strings.articles}"
                )
                Spacer(modifier = Modifier.height(10.dp))
                FeatureRow(
                    icon = Icons.Filled.Bookmarks,
                    title = "${strings.savedTab} & ${strings.comments}"
                )
                Spacer(modifier = Modifier.height(10.dp))
                FeatureRow(
                    icon = Icons.Filled.Lock,
                    title = "${strings.privateStudioTab} & ${strings.adminDashboard}"
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        val cm = credentialManager
                        if (cm == null) {
                            errorMessage = "Google Credential Manager is unavailable on this device."
                            return@Button
                        }
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = cm,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.googleSignInNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
