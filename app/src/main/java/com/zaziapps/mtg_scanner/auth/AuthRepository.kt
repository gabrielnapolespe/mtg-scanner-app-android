package com.zaziapps.mtg_scanner.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.ClearCredentialException
import com.zaziapps.mtg_scanner.R

/**
 * Repository responsible for handling user authentication flows using Google ID and Firebase.
 * @param context Context | The application or activity context.
 * @param credentialManager CredentialManager | API manager to handle the native Google Sign-In bottom sheet.
 * @param firebaseAuth FirebaseAuth | The Firebase authentication instance to manage user sessions.
 */
class AuthRepository(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Triggers the Google Sign-In UI sheet and signs into Firebase using the retrieved ID token.
     * @param activity Activity | The host activity context required by CredentialManager to display the dialog UI.
     * @return Result<Boolean> | Success(true) if logged in, Success(false) if canceled by user, or Failure(Exception).
     */
    suspend fun loginWithGoogle(activity: Activity): Result<Boolean> {
        return try {
            // 1. Configure the Google ID token option with the web client ID ("869350428277-tkb59fn2uhuv55ubal9orv4b03fppops.apps.googleusercontent.com")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            // 2. Build the credential request with the configured option
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 3. Request credentials from Android Credential Manager using the required Activity instance
            val result = credentialManager.getCredential(context = activity, request = request)
            val credential = result.credential

            // 4. Safely extract or parse the credential depending on whether it is direct or wrapped as a CustomCredential
            val googleIdTokenCredential = try {
                credential as? GoogleIdTokenCredential
                    ?: if (credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL") {
                        // Unpack the generic CustomCredential back into a GoogleIdTokenCredential using its Bundle data
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else {
                        null
                    }
            } catch (e: Exception) {
                null
            }

            // 5. Authenticate with Firebase using the finalized Google ID token object
            if (googleIdTokenCredential != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                Result.success(true)
            } else {
                Result.failure(Exception("Could not extract a valid Google credential"))
            }

        } catch (e: GetCredentialCancellationException) {
            // The user intentionally closed or dismissed the dialog; this is an expected workflow, not a systemic failure
            Result.success(false)
        } catch (e: Exception) {
            // Catch any other exceptions such as network failures, missing SHA-1 configs, or wrong client IDs
            Result.failure(e)
        }
    }

    /**
     * Clears the user session globally by signing out from Firebase and clearing the Android Credential state.
     * @param onComplete Function1<Boolean, Unit> | Callback confirming whether the complete sign-out procedure finished.
     */
    fun logout(onComplete: (Boolean) -> Unit = {}) {
        // Sign out from Firebase instance
        firebaseAuth.signOut()

        val clearCredentialStateRequest = ClearCredentialStateRequest()

        // Clear the active account state inside Android's Credential Manager
        credentialManager.clearCredentialStateAsync(
            clearCredentialStateRequest,
            null, // optional cancellationSignal
            ContextCompat.getMainExecutor(context), // Run callback on the main application UI thread
            object : CredentialManagerCallback<Void?, ClearCredentialException> {
                override fun onResult(result: Void?) {
                    // Sign-out completed successfully
                    onComplete(true)
                }

                override fun onError(e: ClearCredentialException) {
                    // Error encountered while clearing the credential state
                    e.printStackTrace()
                    onComplete(false)
                }
            }
        )
    }
}
