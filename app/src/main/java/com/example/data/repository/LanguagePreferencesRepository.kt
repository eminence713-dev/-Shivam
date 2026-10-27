package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storeDataStore by preferencesDataStore(name = "shivam_mart_prefs")

class LanguagePreferencesRepository(private val appContext: Context) {
    private val langKey = stringPreferencesKey("selected_language")
    private val apkDownloadUrlKey = stringPreferencesKey("free_apk_download_url")

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
}
