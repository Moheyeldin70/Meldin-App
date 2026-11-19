package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

private const val TAG = "AuthHelper"
private const val EXPECTED_DEBUG_SHA1 = "B4:46:DA:DC:29:93:E4:56:C1:A4:71:37:39:80:7E:96:0E:9D:55:4D"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private fun resolveWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (resId == 0) return null
    return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun getAppSigningSha1(context: Context): String? = runCatching {
    val pm = context.packageManager
    val pkg = context.packageName
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo
        if (signingInfo?.hasMultipleSigners() == true) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo?.signingCertificateHistory
        }
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
    }
    val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
    val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
    digest.joinToString(":") { "%02X".format(it) }
}.getOrNull()

private fun isConfigurationOrPlayServicesError(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lower = message.lowercase()
    if (lower.contains("activity is cancelled by the user") ||
        lower.contains("user canceled") ||
        lower.contains("user cancelled")
    ) {
        return false
    }
    return lower.contains("28444") ||
        lower.contains("developer") ||
        lower.contains("sha") ||
        lower.contains("reauth") ||
        lower.contains("[16]") ||
        lower.contains("16:") ||
        lower.contains("10:") ||
        lower.contains("token") ||
        lower.contains("config") ||
        lower.contains("network") ||
        lower.contains("internal") ||
        lower.contains("play services") ||
        lower.contains("failed") ||
        lower.contains("error")
}

private fun isReauthError(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    val lower = message.lowercase()
    return lower.contains("reauth") || lower.contains("[16]") || lower.contains("16:")
}

private fun formatSignInFailureMessage(context: Context, e: Exception): String {
    val raw = e.localizedMessage?.takeIf { it.isNotBlank() }
        ?: (e as? GetCredentialException)?.errorMessage?.toString()?.takeIf { it.isNotBlank() }
        ?: "Google Sign-In failed (${e::class.java.simpleName})"
    val lower = raw.lowercase()
    val actualSha1 = getAppSigningSha1(context)
    return when {
        e is NoCredentialException ->
            "No Google account credential available on this device. Ensure a Google account is signed in and Google Play Services is updated. ($raw)"
        isReauthError(raw) || lower.contains("28444") || lower.contains("developer") || lower.contains("10:") || lower.contains("sha") -> {
            when {
                actualSha1 != null && !actualSha1.equals(EXPECTED_DEBUG_SHA1, ignoreCase = true) ->
                    "$raw\n\nThis APK's signing SHA-1 ($actualSha1) does not match the registered Firebase SHA-1 ($EXPECTED_DEBUG_SHA1). Push debug.keystore.base64 to GitHub or add this SHA-1 in Firebase Console."
                actualSha1 != null ->
                    "$raw\n\nAPK SHA-1 matches ($actualSha1). Please ensure your device has a screen lock/PIN set, clear Google Play Services cache, or confirm this Google account is allowed on the project's OAuth consent screen."
                else ->
                    "$raw\n\nCheck that the APK signing SHA-1 matches Firebase Console and your Google account is signed in on the device."
            }
        }
        else -> raw
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return
    }
    val hostContext: Context = context.findActivity() ?: context
    scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val cred = credentialManager.getCredential(hostContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

private suspend fun signInWithGoogleIdToken(
    cred: androidx.credentials.Credential,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(
            GoogleAuthProvider.getCredential(token, null)
        ).await()
        onAuthSuccess()
    } else {
        onAuthError("Unexpected credential type returned by Google Sign-In")
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to resolve Activity for Credential Manager")
        return
    }
    scope.launch {
        try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val cred = credentialManager.getCredential(activity, request).credential
            signInWithGoogleIdToken(cred, onAuthSuccess, onAuthError)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Primary Google Sign-In flow cancelled/reauth: ${e.message}", e)
            if (isReauthError(e.message)) {
                // Retry once after clearing stale Credential Manager state using a separate single-option GetGoogleIdOption request
                try {
                    runCatching {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    }
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val fallbackCred = credentialManager.getCredential(activity, fallbackRequest).credential
                    signInWithGoogleIdToken(fallbackCred, onAuthSuccess, onAuthError)
                } catch (fallbackEx: GetCredentialCancellationException) {
                    Log.w(TAG, "Fallback Google ID Sign-In also cancelled/reauth: ${fallbackEx.message}", fallbackEx)
                    if (isConfigurationOrPlayServicesError(fallbackEx.message)) {
                        onAuthError(formatSignInFailureMessage(activity, fallbackEx))
                    } else {
                        onAuthCancelled()
                    }
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback Google ID Sign-In failed", fallbackEx)
                    onAuthError(formatSignInFailureMessage(activity, fallbackEx))
                }
            } else if (isConfigurationOrPlayServicesError(e.message)) {
                onAuthError(formatSignInFailureMessage(activity, e))
            } else {
                onAuthCancelled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(formatSignInFailureMessage(activity, e))
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

object SecurityHashUtil {
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        return sha256(password, salt) == expectedHash
    }
}
