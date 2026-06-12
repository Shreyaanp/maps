package work.shreyaan.dwell

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.json.JSONObject

data class GoogleAccount(
    val idToken: String,
    val email: String,
    val displayName: String,
    val googleSubject: String,
)

object GoogleAuth {
    @SuppressLint("CredentialManagerSignInWithGoogle")
    suspend fun signIn(context: Context): GoogleAccount {
        val clientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
        require(clientId.isNotBlank()) { "Google sign-in is not configured." }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            CredentialManager.create(context).getCredential(
                context = context,
                request = request,
            )
        } catch (e: GetCredentialCancellationException) {
            throw GoogleAuthException("Google sign-in cancelled.", e)
        } catch (e: NoCredentialException) {
            throw GoogleAuthException("No Google account is available on this device.", e)
        } catch (e: GetCredentialException) {
            throw GoogleAuthException("Google sign-in failed.", e)
        }

        val credential = result.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw GoogleAuthException("Google did not return a usable ID token.")
        }

        val googleCredential = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: GoogleIdTokenParsingException) {
            throw GoogleAuthException("Google returned an unreadable ID token.", e)
        }

        val claims = parseJwtPayload(googleCredential.idToken)
        return GoogleAccount(
            idToken = googleCredential.idToken,
            email = claims?.optString("email").orEmpty().ifBlank { googleCredential.id },
            displayName = googleCredential.displayName.orEmpty(),
            googleSubject = claims?.optString("sub").orEmpty(),
        )
    }

    private fun parseJwtPayload(idToken: String): JSONObject? {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            JSONObject(String(decoded, Charsets.UTF_8))
        }.getOrNull()
    }
}

class GoogleAuthException(
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
