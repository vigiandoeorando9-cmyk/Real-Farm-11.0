package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.domain.FarmAccountUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FarmAccountUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredentialAvailable : GoogleSignInResult
    data class Failure(val message: String, val cause: Throwable? = null) : GoogleSignInResult
}

fun FirebaseUser.toFarmAccountUser(): FarmAccountUser = FarmAccountUser(
    uid = uid,
    email = email ?: "$uid@farmbrasil.local",
    displayName = displayName?.takeIf { it.isNotBlank() } ?: "Produtor Rural BR",
    emailVerified = isEmailVerified,
    avatarStyle = "palha",
    photoUrl = photoUrl?.toString(),
)

private val LOCAL_OFFLINE_USER = FarmAccountUser(
    uid = "local_offline_farmer",
    email = "modo.offline@farmbrasil.local",
    displayName = "Produtor Local",
    emailVerified = false,
    avatarStyle = "palha",
    photoUrl = null,
)

class GoogleAuthManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    val isFirebaseConfigured: Boolean = runCatching {
        FirebaseApp.getApps(appContext).isNotEmpty() && resolveWebClientId() != null
    }.getOrDefault(false)

    private val auth: FirebaseAuth? by lazy {
        if (isFirebaseConfigured) runCatching { Firebase.auth }.getOrNull() else null
    }

    private val offlineUserFlow = MutableStateFlow<FarmAccountUser?>(
        if (!isFirebaseConfigured) LOCAL_OFFLINE_USER else null
    )

    private fun resolveWebClientId(): String? {
        return runCatching { appContext.getString(R.string.default_web_client_id) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    fun currentUser(): FarmAccountUser? =
        if (isFirebaseConfigured) auth?.currentUser?.toFarmAccountUser() ?: offlineUserFlow.value else offlineUserFlow.value

    fun observeAuthState(): Flow<FarmAccountUser?> {
        if (!isFirebaseConfigured) return offlineUserFlow
        val firebaseAuth = auth ?: return offlineUserFlow
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { fa ->
                trySend(fa.currentUser?.toFarmAccountUser() ?: offlineUserFlow.value)
            }
            firebaseAuth.addAuthStateListener(listener)
            trySend(firebaseAuth.currentUser?.toFarmAccountUser() ?: offlineUserFlow.value)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    fun enterOfflineMode(): FarmAccountUser {
        offlineUserFlow.value = LOCAL_OFFLINE_USER
        return LOCAL_OFFLINE_USER
    }

    suspend fun attemptAutoSignIn(activityContext: Context): GoogleSignInResult {
        if (!isFirebaseConfigured) {
            return GoogleSignInResult.Success(enterOfflineMode())
        }
        auth?.currentUser?.let { return GoogleSignInResult.Success(it.toFarmAccountUser()) }
        val serverClientId = resolveWebClientId()
            ?: return GoogleSignInResult.NoCredentialAvailable
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return executeCredentialRequest(activityContext, request)
    }

    suspend fun signInWithGoogle(activityContext: Context): GoogleSignInResult {
        if (!isFirebaseConfigured) {
            return GoogleSignInResult.Failure(
                "Firebase não está configurado (google-services.json ausente). Use o Modo Offline para jogar localmente."
            )
        }
        val serverClientId = resolveWebClientId()
            ?: return GoogleSignInResult.Failure("Configuração Google Sign-In ausente (default_web_client_id).")
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        return executeCredentialRequest(activityContext, request)
    }

    private suspend fun executeCredentialRequest(
        activityContext: Context,
        request: GetCredentialRequest,
    ): GoogleSignInResult {
        val firebaseAuth = auth
            ?: return GoogleSignInResult.Failure("Serviço de autenticação Firebase indisponível.")
        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return GoogleSignInResult.Failure("Credencial incompatível retornada pelo provedor Google.")
            }
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCred).await()
            val user = authResult.user
                ?: return GoogleSignInResult.Failure("Firebase não retornou o usuário autenticado.")
            GoogleSignInResult.Success(user.toFarmAccountUser())
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w("GoogleAuthManager", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.NoCredentialAvailable
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthManager", "Credential exception during sign-in", e)
            GoogleSignInResult.Failure("Falha no login Google: ${e.localizedMessage ?: "tente novamente"}", e)
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Unexpected error during Google sign-in", e)
            GoogleSignInResult.Failure("Erro ao autenticar com o Google: ${e.localizedMessage ?: "desconhecido"}", e)
        }
    }

    suspend fun signOut() {
        if (!isFirebaseConfigured) {
            offlineUserFlow.value = null
            return
        }
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("GoogleAuthManager", "clearCredentialState skipped: ${e.message}")
            }
            auth?.signOut()
        }
    }
}
