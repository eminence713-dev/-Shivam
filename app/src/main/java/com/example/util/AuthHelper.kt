package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.model.AppLanguage
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthWebException

enum class AuthFailureKind {
    CANCELLED,
    NO_DEVICE_ACCOUNT,
    POPUP_INTERRUPTED,
    CONFIG_ERROR,
    NETWORK_OR_OTHER
}

data class AuthUiError(
    val kind: AuthFailureKind,
    val titleEn: String,
    val titleHi: String,
    val messageEn: String,
    val messageHi: String,
    val suggestBrowserFallback: Boolean = false
) {
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.HINDI) titleHi else titleEn
    fun message(lang: AppLanguage): String = if (lang == AppLanguage.HINDI) messageHi else messageEn
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

object AuthErrorClassifier {

    fun missingClientIdError(): AuthUiError = AuthUiError(
        kind = AuthFailureKind.CONFIG_ERROR,
        titleEn = "Firebase Configuration Missing",
        titleHi = "Firebase कॉन्फ़िगरेशन अनुपलब्ध",
        messageEn = "Google Web Client ID (default_web_client_id) was not found in app/google-services.json. Please ensure Firebase Google Sign-In is provisioned for this app.",
        messageHi = "ऐप में Google Web Client ID (default_web_client_id) नहीं मिली। कृपया सुनिश्चित करें कि Firebase Google Sign-In सक्रिय है।",
        suggestBrowserFallback = true
    )

    fun missingActivityError(): AuthUiError = AuthUiError(
        kind = AuthFailureKind.CONFIG_ERROR,
        titleEn = "Unable to Open Sign-In Window",
        titleHi = "साइन-इन विंडो खोलने में असमर्थ",
        messageEn = "Could not attach Google Sign-In to an active Android Activity window. Please reopen the app and try again.",
        messageHi = "सक्रिय स्क्रीन से जुड़ने में समस्या हुई। कृपया ऐप दोबारा खोलकर प्रयास करें।",
        suggestBrowserFallback = false
    )

    fun classify(throwable: Throwable): AuthUiError {
        val rawMsg = throwable.localizedMessage?.trim().orEmpty()
        val lowerMsg = rawMsg.lowercase()

        return when {
            throwable is FirebaseAuthException -> {
                val code = throwable.errorCode
                if (code == "ERROR_WEB_CONTEXT_CANCELED" || code == "ERROR_USER_CANCELLED") {
                    AuthUiError(
                        kind = AuthFailureKind.CANCELLED,
                        titleEn = "Browser Sign-In Cancelled",
                        titleHi = "ब्राउज़र साइन-इन रद्द हुआ",
                        messageEn = "The Google sign-in browser window was closed before completing login. Please try again.",
                        messageHi = "लॉगिन पूरा होने से पहले ब्राउज़र विंडो बंद कर दी गई। कृपया पुनः प्रयास करें।",
                        suggestBrowserFallback = false
                    )
                } else {
                    AuthUiError(
                        kind = AuthFailureKind.CONFIG_ERROR,
                        titleEn = "Firebase Authentication Error ($code)",
                        titleHi = "Firebase प्रमाणीकरण त्रुटि ($code)",
                        messageEn = rawMsg.ifBlank { "Firebase rejected the authentication credential ($code). Verify Google Sign-In is enabled in Firebase Authentication." },
                        messageHi = rawMsg.ifBlank { "Firebase प्रमाणीकरण में त्रुटि ($code) हुई। कृपया पुनः प्रयास करें।" },
                        suggestBrowserFallback = true
                    )
                }
            }

            throwable is GetCredentialCancellationException ||
                lowerMsg.contains("canceled") ||
                lowerMsg.contains("cancelled") ||
                lowerMsg.contains("user closed") -> AuthUiError(
                kind = AuthFailureKind.CANCELLED,
                titleEn = "Sign-In Cancelled",
                titleHi = "साइन-इन रद्द किया गया",
                messageEn = "Google Sign-In was closed before selecting an account. Tap 'Sign in with Google' to try again, or use 'Sign in via Browser Redirect' below if no account appeared on this device.",
                messageHi = "खाता चुनने से पहले Google साइन-इन बंद हो गया। पुनः 'Sign in with Google' दबाएँ, या नीचे दिए गए 'ब्राउज़र द्वारा Google लॉगिन' विकल्प का उपयोग करें।",
                suggestBrowserFallback = true
            )

            throwable is NoCredentialException ||
                lowerMsg.contains("no credentials available") ||
                lowerMsg.contains("cannot find a matching credential") -> AuthUiError(
                kind = AuthFailureKind.NO_DEVICE_ACCOUNT,
                titleEn = "No Google Account Found on Device",
                titleHi = "डिवाइस पर कोई Google खाता नहीं मिला",
                messageEn = "No Google account is signed into Android Settings on this phone/emulator. Tap 'Sign in via Browser Redirect' below to sign in right away, or add a Google account in Android Settings → Accounts.",
                messageHi = "इस फोन/एम्युलेटर की सेटिंग्स में कोई Google खाता लॉगिन नहीं है। तुरंत लॉगिन करने के लिए नीचे 'ब्राउज़र द्वारा Google लॉगिन' दबाएँ, या फोन Settings → Accounts में Google खाता जोड़ें।",
                suggestBrowserFallback = true
            )

            throwable is GetCredentialInterruptedException ||
                throwable is FirebaseAuthWebException ||
                throwable is GetCredentialCustomException ||
                throwable is GetCredentialUnknownException -> AuthUiError(
                kind = AuthFailureKind.POPUP_INTERRUPTED,
                titleEn = "Sign-In Prompt Interrupted",
                titleHi = "साइन-इन पॉपअप बाधित हुआ",
                messageEn = "The Google Sign-In popup or browser redirect was interrupted (${rawMsg.ifBlank { "please retry" }}). Tap 'Sign in with Google' or 'Sign in via Browser Redirect' to try again.",
                messageHi = "Google साइन-इन विंडो बाधित हो गई। कृपया पुनः प्रयास करें या ब्राउज़र लॉगिन विकल्प चुनें।",
                suggestBrowserFallback = true
            )

            throwable is GetCredentialProviderConfigurationException ||
                throwable is GetCredentialUnsupportedException -> AuthUiError(
                kind = AuthFailureKind.CONFIG_ERROR,
                titleEn = "Google Play Services Configuration Issue",
                titleHi = "Google Play Services कॉन्फ़िगरेशन समस्या",
                messageEn = "Credential Manager is unavailable or misconfigured on this device (${rawMsg.ifBlank { "provider unavailable" }}). Use 'Sign in via Browser Redirect' below to authenticate directly with Firebase.",
                messageHi = "इस डिवाइस पर Credential Manager उपलब्ध नहीं है। कृपया नीचे दिए गए 'ब्राउज़र द्वारा Google लॉगिन' बटन का उपयोग करें।",
                suggestBrowserFallback = true
            )

            throwable is FirebaseNetworkException || lowerMsg.contains("network") -> AuthUiError(
                kind = AuthFailureKind.NETWORK_OR_OTHER,
                titleEn = "Network Connection Error",
                titleHi = "इंटरनेट कनेक्शन त्रुटि",
                messageEn = "Could not reach Google / Firebase servers. Please check your internet connection and try again.",
                messageHi = "इंटरनेट कनेक्शन में समस्या है। कृपया नेटवर्क जांचकर पुनः प्रयास करें।",
                suggestBrowserFallback = false
            )

            else -> AuthUiError(
                kind = AuthFailureKind.NETWORK_OR_OTHER,
                titleEn = "Sign-In Failed",
                titleHi = "साइन-इन विफल रहा",
                messageEn = rawMsg.ifBlank { "Unable to complete Google Sign-In. Please try again or use the Browser Redirect option below." },
                messageHi = rawMsg.ifBlank { "Google साइन-इन पूरा नहीं हो सका। कृपया पुनः प्रयास करें या नीचे ब्राउज़र लॉगिन चुनें।" },
                suggestBrowserFallback = true
            )
        }
    }
}
