package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.repository.StoreRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Locale

object AuthManager {

    fun isFirebaseAvailable(context: Context): Boolean =
        FirebaseApp.getApps(context.applicationContext).isNotEmpty()

    private fun Context.findActivityContext(): Context {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return this
    }

    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return null
        return context.getString(resId).takeIf { it.isNotBlank() }
    }

    suspend fun attemptAutoSignIn(context: Context): Boolean {
        if (!isFirebaseAvailable(context)) return false
        val webClientId = resolveWebClientId(context) ?: return false
        val activityContext = context.findActivityContext()
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            handleCredentialResult(result.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d("Auth", "No previously authorized Google credential found for silent sign-in.")
            false
        } catch (e: GetCredentialCancellationException) {
            Log.d("Auth", "Silent auto sign-in dismissed.")
            false
        } catch (e: Exception) {
            Log.d("Auth", "Silent auto sign-in skipped: ${e.message}")
            false
        }
    }

    suspend fun signInInteractiveWithGoogle(context: Context): Result<Unit> {
        if (!isFirebaseAvailable(context)) {
            return Result.failure(
                IllegalStateException("Cloud configuration is not active in this build.")
            )
        }
        val webClientId = resolveWebClientId(context)
            ?: return Result.failure(
                IllegalStateException("Google Sign-In configuration missing (default_web_client_id).")
            )
        val activityContext = context.findActivityContext()
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val ok = handleCredentialResult(result.credential)
            if (ok) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "User cancelled Google Sign-In", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Credential Manager error during Google Sign-In", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("Auth", "Firebase Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    private suspend fun handleCredentialResult(credential: androidx.credentials.Credential): Boolean {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            Firebase.auth.signInWithCredential(firebaseCredential).await()
            return true
        }
        return false
    }

    suspend fun signOut(context: Context) {
        if (isFirebaseAvailable(context)) {
            Firebase.auth.signOut()
        }
        runCatching {
            CredentialManager.create(context.findActivityContext())
                .clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { e ->
            Log.w("Auth", "Could not clear CredentialManager state on sign-out", e)
        }
    }

    fun hasVerifiedOwnerToken(user: FirebaseUser?): Boolean {
        if (user == null || !user.isEmailVerified) return false
        val emailLower = user.email?.lowercase(Locale.US).orEmpty()
        return emailLower in StoreRepository.VERIFIED_OWNER_EMAILS
    }
}
