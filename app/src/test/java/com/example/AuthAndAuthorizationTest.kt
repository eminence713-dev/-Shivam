package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.repository.StoreRepository
import com.example.util.AuthErrorClassifier
import com.example.util.AuthFailureKind
import com.example.util.findActivity
import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAndAuthorizationTest {

    @Test
    fun `firebase google web client id and named database id resources are generated`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientId = context.getString(R.string.default_web_client_id)
        val dbId = context.getString(R.string.firestore_database_id)

        assertTrue("default_web_client_id must be generated from google-services.json", clientId.endsWith(".apps.googleusercontent.com"))
        assertEquals("ai-studio-android-shivamri-f9c3a184-78c1-4e3a-b629-471c923e0404", dbId)
    }

    @Test
    fun `findActivity unwraps nested ContextWrapper chains safely`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertNull("Application context should not unwrap to an Activity", appContext.findActivity())

        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val rawActivity = activityController.get()
        val wrappedOnce = ContextWrapper(rawActivity)
        val wrappedTwice = ContextWrapper(wrappedOnce)

        assertEquals(rawActivity, wrappedTwice.findActivity())
    }

    @Test
    fun `owner authorization strictly grants verified owner emails and restricts other accounts`() {
        // Authorized store owner account (verified)
        assertTrue(StoreRepository.isVerifiedOwnerEmail("rajuguptas123@gmail.com", isEmailVerified = true))
        assertTrue(StoreRepository.isVerifiedOwnerEmail("  RAJUGUPTAS123@GMAIL.COM ", isEmailVerified = true))
        assertTrue(StoreRepository.isVerifiedOwnerEmail("eminence713@gmail.com", isEmailVerified = true))

        // Unverified email with owner address MUST be rejected
        assertFalse(StoreRepository.isVerifiedOwnerEmail("rajuguptas123@gmail.com", isEmailVerified = false))

        // Regular customer Google accounts MUST be restricted from Owner Dashboard
        assertFalse(StoreRepository.isVerifiedOwnerEmail("customer.kalyanpur@gmail.com", isEmailVerified = true))
        assertFalse(StoreRepository.isVerifiedOwnerEmail("", isEmailVerified = true))
        assertFalse(StoreRepository.isVerifiedOwnerEmail(null, isEmailVerified = true))
    }

    @Test
    fun `AuthErrorClassifier produces bilingual diagnostics for cancellation, missing account, popup, and config errors`() {
        val cancelErr = AuthErrorClassifier.classify(GetCredentialCancellationException("User cancelled selector"))
        assertEquals(AuthFailureKind.CANCELLED, cancelErr.kind)
        assertTrue(cancelErr.suggestBrowserFallback)
        assertTrue(cancelErr.title(AppLanguage.ENGLISH).contains("Cancelled", ignoreCase = true))
        assertTrue(cancelErr.title(AppLanguage.HINDI).isNotBlank())

        val noCredErr = AuthErrorClassifier.classify(NoCredentialException("No credentials available"))
        assertEquals(AuthFailureKind.NO_DEVICE_ACCOUNT, noCredErr.kind)
        assertTrue(noCredErr.suggestBrowserFallback)
        assertTrue(noCredErr.message(AppLanguage.ENGLISH).contains("Browser Redirect"))

        val popupErr = AuthErrorClassifier.classify(GetCredentialInterruptedException("Interrupted"))
        assertEquals(AuthFailureKind.POPUP_INTERRUPTED, popupErr.kind)
        assertTrue(popupErr.suggestBrowserFallback)

        val configErr = AuthErrorClassifier.classify(GetCredentialProviderConfigurationException("Provider missing"))
        assertEquals(AuthFailureKind.CONFIG_ERROR, configErr.kind)
        assertTrue(configErr.suggestBrowserFallback)

        val webCancelErr = AuthErrorClassifier.classify(
            FirebaseAuthException("ERROR_WEB_CONTEXT_CANCELED", "The web operation was canceled by the user.")
        )
        assertEquals(AuthFailureKind.CANCELLED, webCancelErr.kind)
        assertFalse(webCancelErr.suggestBrowserFallback)

        val missingClient = AuthErrorClassifier.missingClientIdError()
        assertNotNull(missingClient)
        assertEquals(AuthFailureKind.CONFIG_ERROR, missingClient.kind)
    }
}
