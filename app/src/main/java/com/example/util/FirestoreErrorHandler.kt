package com.example.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?
) {
    val auth = runCatching { Firebase.auth }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfoJson = JSONArray().apply {
        currentUser?.providerData?.forEach { profile ->
            put(
                JSONObject().apply {
                    put("providerId", profile.providerId)
                    put("displayName", profile.displayName)
                    put("email", profile.email)
                    put("photoUrl", profile.photoUrl?.toString())
                }
            )
        }
    }

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("isAnonymous", currentUser?.isAnonymous)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", providerInfoJson)
    }

    val errInfoJson = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val serializedPayload = errInfoJson.toString()
    Log.e("FirestoreError", serializedPayload, error)
}
