package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.StoreSettings
import com.example.data.repository.StoreRepository
import com.example.ui.components.StoreLogoCrest
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DeepSindoor
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

object AuthManager {

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
        Firebase.auth.signOut()
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

@Composable
fun AuthScreen(
    lang: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val defaultSettings = remember { StoreSettings() }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(context)
        isCheckingAutoSignIn = false
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StoreLogoCrest(size = 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == AppLanguage.HINDI) "शिवम & रिया मार्ट" else "Shivam & Riya Mart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lang == AppLanguage.HINDI) "बड़ा बाज़ार, कल्याणपुर" else "Bada Bazar, Kalyanpur",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onToggleLanguage,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("auth_language_switch_button")
                ) {
                    Icon(Icons.Outlined.Translate, contentDescription = "Switch Language", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (lang == AppLanguage.HINDI) "English" else "हिंदी")
                }
            }

            // Hero Storefront Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_hero_1793076838918),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    DeepSindoor.copy(alpha = 0.55f),
                                    DeepSindoor.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Surface(
                            color = AntiqueGold,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "ऑनलाइन शॉपिंग ऐप" else "OFFICIAL ONLINE STORE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) defaultSettings.storeNameHi else defaultSettings.storeNameEn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "साड़ियाँ • कुर्तियाँ • शर्ट्स • जींस • जूते और सैंडल — लाइव स्टॉक और घर बैठे ऑर्डर!"
                            } else {
                                "Sarees • Kurtis • Menswear • Footwear — Live shop prices & instant online ordering!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFBF2DC)
                        )
                    }
                }
            }

            // Main Flipkart-Style Sign-In Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.55f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "दुकान खोलने के लिए Google से जुड़ें"
                        } else {
                            "Continue with Google to Shop Live"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "बस एक टैप करें — दुकान के सभी नए उत्पाद, छूट (Discount), और अपने ऑर्डर की लाइव स्थिति तुरंत देखें।"
                        } else {
                            "One tap with your phone's Google account gives you live catalog updates, discounts, and real-time order tracking."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrustBenefitRow(
                            icon = Icons.Filled.Storefront,
                            text = if (lang == AppLanguage.HINDI) "दुकान के असली दाम और लाइव स्टॉक तुरंत देखें" else "Real-time shop catalogue, MRP savings & live stock"
                        )
                        TrustBenefitRow(
                            icon = Icons.Filled.LocalShipping,
                            text = if (lang == AppLanguage.HINDI) "कैश ऑन डिलीवरी (COD) और लाइव ऑर्डर ट्रैकिंग" else "Cash on Delivery (COD) & step-by-step live order tracking"
                        )
                        TrustBenefitRow(
                            icon = Icons.Filled.Verified,
                            text = if (lang == AppLanguage.HINDI) "प्रो. राजू कुमार गुप्ता • कल्याणपुर का भरोसेमंद प्रतिष्ठान" else " Shivam Assured Quality • Prop. Raju Kumar Gupta"
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .testTag("auth_error_message")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isSigningIn) return@Button
                            isSigningIn = true
                            errorMessage = null
                            scope.launch {
                                val res = AuthManager.signInInteractiveWithGoogle(context)
                                isSigningIn = false
                                res.onFailure { err ->
                                    if (err !is GetCredentialCancellationException) {
                                        errorMessage = if (lang == AppLanguage.HINDI) {
                                            "Google साइन-इन पूरा नहीं हो सका। कृपया फ़ोन में Google अकाउंट लॉगिन जांचें और पुनः प्रयास करें।"
                                        } else {
                                            "Could not complete Google Sign-In. Please ensure a Google account is active on this device and try again."
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSigningIn && !isCheckingAutoSignIn,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn || isCheckingAutoSignIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (lang == AppLanguage.HINDI) "दुकान खुल रही है..." else "Signing in..."
                            )
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "Google से दुकान खोलें (1-Tap)" else "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    OutlinedButton(
                        onClick = {
                            val msg = WhatsAppHelper.buildCartEnquiryMessage(
                                settings = defaultSettings,
                                cartItems = emptyList(),
                                nextStepLabel = "Store Enquiry",
                                lang = lang
                            )
                            WhatsAppHelper.openWhatsAppChat(context, defaultSettings.whatsappNumber, msg)
                        },
                        border = BorderStroke(1.5.dp, WhatsAppGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_whatsapp_button")
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (lang == AppLanguage.HINDI) {
                                "व्हाट्सऐप पर सीधे संपर्क करें (9162685162)"
                            } else {
                                "Enquire on WhatsApp (9162685162)"
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            tint = VerifiedGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "100% सुरक्षित • कोई पासवर्ड याद रखने की ज़रूरत नहीं"
                            } else {
                                "100% Secure Google Sign-In • No password required for customers"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustBenefitRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = VerifiedGreen.copy(alpha = 0.14f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VerifiedGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
