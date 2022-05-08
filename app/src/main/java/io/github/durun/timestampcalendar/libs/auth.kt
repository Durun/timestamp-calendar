package io.github.durun.timestampcalendar.libs

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential

class MyAuth(private val context: Context) {
    companion object {
        private const val TAG = "MyAuth"
        private val scopes = listOf(
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/calendar"
        )
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .apply { scopes.forEach { requestScopes(Scope(it)) } }
            .requestEmail()
            .build()
    }

    private val client = GoogleSignIn.getClient(context, options)
    val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(context, scopes).also {
        it.selectedAccount = lastSignedInAccount()?.account
    }

    fun isSignedIn(): Boolean = credential.selectedAccount != null
    private fun lastSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /**
     * @return GoogleへログインするためのIntent
     */
    fun signInIntent(): Intent = client.signInIntent

    /**
     * @param data signInIntentをstartし、帰ってきたresultのdata
     */
    fun handleSignInResult(data: Intent) {
        runCatching {
            val account: GoogleSignInAccount =
                GoogleSignIn.getSignedInAccountFromIntent(data).result
            credential.selectedAccount = account.account
        }.onFailure {
            Log.w(TAG, "Sign in failed: ${it.message}")
        }
    }

    fun signInLastAccount() {
        val account = lastSignedInAccount()
        if (account != null) {
            credential.selectedAccount = account.account
        }
    }

    fun signOut() {
        client.signOut()
            .addOnCompleteListener {
                credential.selectedAccount = null
                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
            }
    }
}

