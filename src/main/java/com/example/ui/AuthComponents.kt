package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "PmaGoogleAuth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

/**
 * Attempts silent/returning-user sign-in via GetGoogleIdOption on app startup.
 * Fails quietly if no previously authorized Google credential exists yet.
 */
suspend fun attemptAutoSignIn(
  activity: Activity,
  auth: FirebaseAuth = Firebase.auth
) {
  if (auth.currentUser != null) return
  val clientId = try {
    activity.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "Missing default_web_client_id", e)
    return
  }
  if (clientId.isBlank()) return

  try {
    val credentialManager = CredentialManager.create(activity)
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
      auth.signInWithCredential(firebaseCred).await()
    }
  } catch (e: CancellationException) {
    throw e
  } catch (_: NoCredentialException) {
    // Expected for first-time users on this device
  } catch (_: GetCredentialCancellationException) {
    // User dismissed auto-prompt
  } catch (e: Exception) {
    Log.w(AUTH_TAG, "Silent auto sign-in skipped: ${e.message}")
  }
}

sealed interface SignInOutcome {
  data class Success(val user: FirebaseUser) : SignInOutcome
  data object Cancelled : SignInOutcome
  data class Error(val message: String) : SignInOutcome
}

suspend fun signInInteractiveWithGoogle(
  activity: Activity,
  language: AppLanguage,
  auth: FirebaseAuth = Firebase.auth
): SignInOutcome {
  val clientId = try {
    activity.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    return SignInOutcome.Error(
      if (language == AppLanguage.FR) "Configuration Google Sign-In introuvable (default_web_client_id)."
      else "Google Sign-In configuration missing (default_web_client_id)."
    )
  }

  return try {
    val credentialManager = CredentialManager.create(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()
    val result = credentialManager.getCredential(activity, request)
    val credential = result.credential
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
      val authCredential = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = auth.signInWithCredential(authCredential).await()
      val user = authResult.user
      if (user != null) SignInOutcome.Success(user)
      else SignInOutcome.Error(
        if (language == AppLanguage.FR) "Utilisateur Firebase introuvable après connexion."
        else "Firebase user missing after sign-in."
      )
    } else {
      SignInOutcome.Error(
        if (language == AppLanguage.FR) "Type d'identifiant Google inattendu."
        else "Unexpected credential type returned by Google."
      )
    }
  } catch (e: CancellationException) {
    throw e
  } catch (_: GetCredentialCancellationException) {
    SignInOutcome.Cancelled
  } catch (e: Exception) {
    Log.e(AUTH_TAG, "Interactive Google Sign-In failed", e)
    SignInOutcome.Error(
      e.localizedMessage
        ?: if (language == AppLanguage.FR) "Échec de la connexion Google." else "Google Sign-In failed."
    )
  }
}

suspend fun signOutWithGoogle(
  context: Context,
  auth: FirebaseAuth = Firebase.auth
) {
  auth.signOut()
  withContext(NonCancellable) {
    try {
      CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Failed clearing CredentialManager state", e)
    }
  }
}

@Composable
fun AuthScreen(
  theme: ThemeColors,
  language: AppLanguage,
  isCheckingSession: Boolean
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = theme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp),
        shape = RoundedCornerShape(24.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Image(
            painter = painterResource(id = R.drawable.white_leather_bf),
            contentDescription = if (language == AppLanguage.FR) "Logo B&F Parcours" else "B&F Journey Logo",
            modifier = Modifier
              .size(72.dp)
              .clip(RoundedCornerShape(18.dp))
              .border(1.5.dp, theme.accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (language == AppLanguage.FR) "B&F Parcours" else "B&F Journey",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (language == AppLanguage.FR)
              "Connectez votre compte Google pour synchroniser votre suivi en temps réel sur tous vos appareils."
            else
              "Sign in with your Google account to synchronize your care records in real time across all your devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.mutedText,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Sync benefits summary card
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.background,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              AuthFeatureRow(
                icon = Icons.Default.Sync,
                text = if (language == AppLanguage.FR)
                  "Rendez-vous, médications, notes et checklists synchronisés instantanément"
                else
                  "Appointments, medications, notes and checklists synced instantly",
                theme = theme
              )
              AuthFeatureRow(
                icon = Icons.Default.Devices,
                text = if (language == AppLanguage.FR)
                  "Utilisez le même compte Google sur chaque téléphone ou tablette"
                else
                  "Use the same Google account on every phone or tablet",
                theme = theme
              )
              AuthFeatureRow(
                icon = Icons.Default.Lock,
                text = if (language == AppLanguage.FR)
                  "Données médicales protégées et privées par compte"
                else
                  "Private maternal records strictly isolated per account",
                theme = theme
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          if (errorMessage != null) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFEF2F2),
              border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
              Text(
                text = errorMessage.orEmpty(),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB91C1C)
              )
            }
          }

          if (isCheckingSession) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 12.dp)
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = theme.accent, strokeWidth = 2.dp)
              Text(
                text = if (language == AppLanguage.FR) "Vérification de session..." else "Checking session...",
                style = MaterialTheme.typography.bodySmall,
                color = theme.mutedText
              )
            }
          } else {
            Button(
              onClick = {
                if (activity == null) {
                  errorMessage = if (language == AppLanguage.FR)
                    "Impossible d'ouvrir l'écran de connexion depuis ce contexte."
                  else
                    "Unable to launch Google Sign-In from this context."
                  return@Button
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                  when (val outcome = signInInteractiveWithGoogle(activity, language)) {
                    is SignInOutcome.Success -> {
                      isLoading = false
                    }
                    is SignInOutcome.Cancelled -> {
                      isLoading = false
                    }
                    is SignInOutcome.Error -> {
                      isLoading = false
                      errorMessage = outcome.message
                    }
                  }
                }
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = theme.accent,
                contentColor = Color.White
              )
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = Color.White,
                  strokeWidth = 2.5.dp
                )
              } else {
                Icon(
                  Icons.Default.CloudDone,
                  contentDescription = if (language == AppLanguage.FR) "Connexion Google" else "Google Sign-In",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = if (language == AppLanguage.FR) "Se connecter avec Google" else "Sign in with Google",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuthFeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  theme: ThemeColors
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = theme.accent.copy(alpha = 0.14f),
      modifier = Modifier.size(28.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
      }
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = theme.primaryText,
      lineHeight = 16.sp
    )
  }
}
