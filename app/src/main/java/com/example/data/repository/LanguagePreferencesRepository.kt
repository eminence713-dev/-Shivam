package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.storeDataStore by preferencesDataStore(name = "shivam_mart_prefs")

class LanguagePreferencesRepository(private val appContext: Context) {
    private val langKey = stringPreferencesKey("selected_language")
    private val apkDownloadUrlKey = stringPreferencesKey("free_apk_download_url")
    private val ownerPasswordHashKey = stringPreferencesKey("owner_password_sha256")
    private val initialCatalogSeededKey = booleanPreferencesKey("initial_catalog_seeded_v1")

    val languageFlow: Flow<AppLanguage> = appContext.storeDataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLanguage.HINDI.name -> AppLanguage.HINDI
            else -> AppLanguage.ENGLISH
        }
    }

    val apkDownloadUrlFlow: Flow<String> = appContext.storeDataStore.data.map { prefs ->
        prefs[apkDownloadUrlKey].orEmpty()
    }

    suspend fun setLanguage(language: AppLanguage) {
        appContext.storeDataStore.edit { prefs ->
            prefs[langKey] = language.name
        }
    }

    suspend fun setApkDownloadUrl(url: String) {
        appContext.storeDataStore.edit { prefs ->
            prefs[apkDownloadUrlKey] = url.trim()
        }
    }

    suspend fun isInitialCatalogSeeded(): Boolean {
        return appContext.storeDataStore.data.first()[initialCatalogSeededKey] == true
    }

    suspend fun markInitialCatalogSeeded() {
        appContext.storeDataStore.edit { prefs ->
            prefs[initialCatalogSeededKey] = true
        }
    }

    suspend fun verifyOwnerPassword(candidatePassword: String): Boolean {
        val prefs = appContext.storeDataStore.data.first()
        val storedHash = prefs[ownerPasswordHashKey] ?: sha256(DEFAULT_OWNER_PASSWORD)
        return sha256(candidatePassword.trim()) == storedHash
    }

    suspend fun changeOwnerPassword(currentPassword: String, newPassword: String): Result<Unit> {
        val trimmedNew = newPassword.trim()
        if (trimmedNew.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        if (!verifyOwnerPassword(currentPassword)) {
            return Result.failure(IllegalArgumentException("Current owner password is incorrect."))
        }
        appContext.storeDataStore.edit { prefs ->
            prefs[ownerPasswordHashKey] = sha256(trimmedNew)
        }
        return Result.success(Unit)
    }

    companion object {
        const val DEFAULT_OWNER_PASSWORD = "shivam123"

        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("shivam_riya_salt::$input".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
