package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.StoreSettings
import com.example.ui.components.JharokhaGoldDivider
import com.example.ui.components.StoreLogoCrest
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.WhatsAppGreen
import com.example.util.AppStrings
import com.example.util.AuthErrorClassifier
import com.example.util.AuthFailureKind
import com.example.util.AuthUiError
import com.example.util.WhatsAppHelper
import com.example.util.findActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

suspend fun performSilentAutoSignIn(
    context: Context,
    credentialManager: CredentialManager
): Boolean {
    if (Firebase.auth.currentUser != null) return true
    val activity = context.findActivity() ?: return false
    val clientId = runCatching { context.getString(R.string.default_web_client_id) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return false

    // Strictly one option (GetGoogleIdOption) per request for silent startup check
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    return try {
        val result = withTimeoutOrNull(3500L) {
            credentialManager.getCredential(activity, request)
        } ?: return false

        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Firebase.auth.signInWithCredential(authCredential).await()
            true
        } else {
            false
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d("Auth", "Silent auto sign-in skipped: ${e.message}")
        false
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    autoSignInJob: Job?,
    onAuthSuccess: () -> Unit,
    onAuthFailure: (AuthUiError) -> Unit,
    scope: CoroutineScope
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
    if (clientId == null) {
        onAuthFailure(AuthErrorClassifier.missingClientIdError())
        return
    }

    val activity = context.findActivity()
    if (activity == null) {
        onAuthFailure(AuthErrorClassifier.missingActivityError())
        return
    }

    // Strictly one option (GetSignInWithGoogleOption) per interactive request for API 36 responsiveness
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            // Ensure no background CredentialManager call overlaps with interactive sign-in
            autoSignInJob?.cancelAndJoin()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthFailure(
                    AuthUiError(
                        kind = AuthFailureKind.CONFIG_ERROR,
                        titleEn = "Unexpected Credential Type",
                        titleHi = "अप्रत्याशित क्रेडेंशियल प्रकार",
                        messageEn = "Google Sign-In returned an unsupported credential format (${credential.type}). Try 'Sign in via Browser Redirect' below.",
                        messageHi = "Google साइन-इन से अमान्य क्रेडेंशियल प्राप्त हुआ। कृपया नीचे ब्राउज़र लॉगिन विकल्प चुनें।",
                        suggestBrowserFallback = true
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthFailure(AuthErrorClassifier.classify(e))
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In via CredentialManager failed", e)
            onAuthFailure(AuthErrorClassifier.classify(e))
        }
    }
}

fun onGoogleBrowserRedirectSignInClicked(
    context: Context,
    autoSignInJob: Job?,
    onAuthSuccess: () -> Unit,
    onAuthFailure: (AuthUiError) -> Unit,
    scope: CoroutineScope
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthFailure(AuthErrorClassifier.missingActivityError())
        return
    }

    scope.launch {
        try {
            autoSignInJob?.cancelAndJoin()
            val pendingTask = Firebase.auth.pendingAuthResult
            if (pendingTask != null) {
                pendingTask.await()
            } else {
                val provider = OAuthProvider.newBuilder("google.com")
                    .setScopes(listOf("email", "profile"))
                    .addCustomParameter("prompt", "select_account")
                    .build()
                Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Browser Redirect Sign-In failed", e)
            onAuthFailure(AuthErrorClassifier.classify(e))
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthWelcomeScreen(
    lang: AppLanguage,
    requestedOwnerLogin: Boolean,
    onRequestedOwnerLoginChange: (Boolean) -> Unit,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingSession by remember { mutableStateOf(true) }
    var isSigningInPrimary by remember { mutableStateOf(false) }
    var isSigningInBrowser by remember { mutableStateOf(false) }
    var uiError by remember { mutableStateOf<AuthUiError?>(null) }
    var autoSignInJob by remember { mutableStateOf<Job?>(null) }
    var showMoreOptions by rememberSaveable { mutableStateOf(requestedOwnerLogin) }

    val anyLoading = isSigningInPrimary || isSigningInBrowser
    val activeError = uiError
    val shouldRevealExtraOptions = showMoreOptions || requestedOwnerLogin || activeError?.suggestBrowserFallback == true

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            isCheckingSession = false
            onAuthSuccess()
            return@LaunchedEffect
        }
        // Resume any pending browser OAuth redirect result first
        val pending = Firebase.auth.pendingAuthResult
        if (pending != null) {
            isSigningInBrowser = true
            isCheckingSession = false
            try {
                pending.await()
                isSigningInBrowser = false
                onAuthSuccess()
                return@LaunchedEffect
            } catch (e: Exception) {
                isSigningInBrowser = false
                uiError = AuthErrorClassifier.classify(e)
                return@LaunchedEffect
            }
        }

        val job = scope.launch {
            val signedIn = performSilentAutoSignIn(context, credentialManager)
            isCheckingSession = false
            if (signedIn) {
                onAuthSuccess()
            }
        }
        autoSignInJob = job
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, AntiqueGold.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = onToggleLanguage,
                            modifier = Modifier.testTag("auth_language_button")
                        ) {
                            Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (lang == AppLanguage.ENGLISH) "हिंदी में देखें" else "Switch to English")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    StoreLogoCrest(size = 84.dp)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = AppStrings.storeTitleTop.get(lang),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = AppStrings.storeSubtitle.get(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))
                    JharokhaGoldDivider()
                    Spacer(Modifier.height(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.img_store_hero_1793076838918),
                        contentDescription = "Storefront interior showcase",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "कोई पासवर्ड या फॉर्म नहीं — बस एक क्लिक में दुकान खोलें, कपड़े व जूते-चप्पल दाम के साथ देखें और ऑर्डर करें!"
                        } else {
                            "No password or signup form — tap once below to open the shop, view prices, and place your order!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    if (isCheckingSession && !anyLoading) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "दुकान खुल रही है..." else "Opening shop...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeError != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = activeError.title(lang),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = activeError.message(lang),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Primary 1-Click Shop Entry Button (Google Sign-In via CredentialManager)
                    Button(
                        onClick = {
                            isCheckingSession = false
                            isSigningInPrimary = true
                            uiError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                autoSignInJob = autoSignInJob,
                                onAuthSuccess = {
                                    isSigningInPrimary = false
                                    onAuthSuccess()
                                },
                                onAuthFailure = { err ->
                                    isSigningInPrimary = false
                                    uiError = err
                                },
                                scope = scope
                            )
                        },
                        enabled = !anyLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningInPrimary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "दुकान खुल रही है..." else "Opening Shop...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(Icons.Filled.Storefront, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) {
                                    "दुकान खोलें • 1-Click (Google)"
                                } else {
                                    "Open Shop • 1-Click (Google)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val defaultSettings = StoreSettings()
                            val msg = WhatsAppHelper.buildCartEnquiryMessage(
                                settings = defaultSettings,
                                cartItems = emptyList(),
                                nextStepLabel = "Store Enquiry",
                                lang = lang
                            )
                            WhatsAppHelper.openWhatsAppChat(context, defaultSettings.whatsappNumber, msg) { err ->
                                uiError = AuthUiError(
                                    kind = AuthFailureKind.CONFIG_ERROR,
                                    titleEn = "WhatsApp Unavailable",
                                    titleHi = "व्हाट्सऐप उपलब्ध नहीं",
                                    messageEn = err,
                                    messageHi = err
                                )
                            }
                        },
                        enabled = !anyLoading,
                        border = BorderStroke(1.5.dp, WhatsAppGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_whatsapp_button")
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${AppStrings.askOnWhatsApp.get(lang)} (9162685162)")
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quiet expandable toggle for Owner Login / Browser Fallback so customers see a clean 1-click UI
                    TextButton(
                        onClick = { showMoreOptions = !shouldRevealExtraOptions },
                        enabled = !anyLoading,
                        modifier = Modifier.testTag("toggle_more_auth_options")
                    ) {
                        Icon(
                            imageVector = if (shouldRevealExtraOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "दुकान मालिक / ब्राउज़र लॉगिन विकल्प"
                            } else {
                                "Shop Owner / Browser Login Options"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    AnimatedVisibility(visible = shouldRevealExtraOptions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    isCheckingSession = false
                                    isSigningInBrowser = true
                                    uiError = null
                                    onGoogleBrowserRedirectSignInClicked(
                                        context = context,
                                        autoSignInJob = autoSignInJob,
                                        onAuthSuccess = {
                                            isSigningInBrowser = false
                                            onAuthSuccess()
                                        },
                                        onAuthFailure = { err ->
                                            isSigningInBrowser = false
                                            uiError = err
                                        },
                                        scope = scope
                                    )
                                },
                                enabled = !anyLoading,
                                colors = if (activeError?.suggestBrowserFallback == true) {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("google_browser_redirect_button")
                            ) {
                                if (isSigningInBrowser) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == AppLanguage.HINDI) "ब्राउज़र रीडायरेक्ट जारी है..." else "Redirecting to Google...",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == AppLanguage.HINDI) {
                                            "ब्राउज़र द्वारा Google लॉगिन (Redirect)"
                                        } else {
                                            "Sign in via Browser Redirect (Google)"
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !anyLoading) {
                                        onRequestedOwnerLoginChange(!requestedOwnerLogin)
                                    }
                                    .testTag("owner_login_intent_toggle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Checkbox(
                                        checked = requestedOwnerLogin,
                                        onCheckedChange = { onRequestedOwnerLoginChange(it) },
                                        enabled = !anyLoading
                                    )
                                    Icon(
                                        Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == AppLanguage.HINDI) {
                                            "मालिक डैशबोर्ड लॉगिन जाँचें (केवल अधिकृत मालिक)"
                                        } else {
                                            "Verify Owner Dashboard Access (Owner Only)"
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Bada Bazar, Kalyanpur, Motihari, Bihar • राजू कुमार गुप्ता",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
