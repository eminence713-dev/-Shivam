package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentStatus
import com.example.data.model.ProductCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private val Context.storeDataStore by preferencesDataStore(name = "shivam_mart_prefs")

class LanguagePreferencesRepository(private val appContext: Context) {
    private val langKey = stringPreferencesKey("selected_language")
    private val apkDownloadUrlKey = stringPreferencesKey("free_apk_download_url")
    private val ownerPasswordHashKey = stringPreferencesKey("owner_password_sha256")
    private val initialCatalogSeededKey = booleanPreferencesKey("initial_catalog_seeded_v1")
    private val wishlistIdsKey = stringSetPreferencesKey("customer_wishlist_ids")
    private val localCustomerOrdersKey = stringPreferencesKey("local_customer_orders_json_v1")

    val languageFlow: Flow<AppLanguage> = appContext.storeDataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLanguage.HINDI.name -> AppLanguage.HINDI
            else -> AppLanguage.ENGLISH
        }
    }

    val apkDownloadUrlFlow: Flow<String> = appContext.storeDataStore.data.map { prefs ->
        prefs[apkDownloadUrlKey].orEmpty()
    }

    val wishlistIdsFlow: Flow<Set<String>> = appContext.storeDataStore.data.map { prefs ->
        prefs[wishlistIdsKey] ?: emptySet()
    }

    val localCustomerOrdersFlow: Flow<List<CustomerOrder>> = appContext.storeDataStore.data.map { prefs ->
        decodeOrdersJson(prefs[localCustomerOrdersKey].orEmpty())
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

    suspend fun toggleWishlist(productId: String): Boolean {
        if (productId.isBlank()) return false
        var added = false
        appContext.storeDataStore.edit { prefs ->
            val current = prefs[wishlistIdsKey] ?: emptySet()
            prefs[wishlistIdsKey] = if (productId in current) {
                added = false
                current - productId
            } else {
                added = true
                current + productId
            }
        }
        return added
    }

    suspend fun saveLocalCustomerOrder(order: CustomerOrder) {
        appContext.storeDataStore.edit { prefs ->
            val existing = decodeOrdersJson(prefs[localCustomerOrdersKey].orEmpty())
            val merged = (listOf(order) + existing.filterNot { it.id == order.id })
                .sortedByDescending { it.createdAtMillis }
                .take(40)
            prefs[localCustomerOrdersKey] = encodeOrdersJson(merged)
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

    private fun encodeOrdersJson(orders: List<CustomerOrder>): String {
        val arr = JSONArray()
        orders.forEach { order ->
            val itemsArr = JSONArray()
            order.lineItems.forEach { line ->
                itemsArr.put(
                    JSONObject().apply {
                        put("productId", line.productId)
                        put("productName", line.productName)
                        put("category", line.category)
                        put("quantity", line.quantity)
                        put("unitPriceInr", line.unitPriceInr)
                        put("lineTotalInr", line.lineTotalInr)
                        put("selectedSize", line.selectedSize)
                        put("selectedColor", line.selectedColor)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", order.id)
                    put("orderReference", order.orderReference)
                    put("customerId", order.customerId)
                    put("customerName", order.customerName)
                    put("customerPhone", order.customerPhone)
                    put("deliveryAddress", order.deliveryAddress)
                    put("landmark", order.landmark)
                    put("items", itemsArr)
                    put("itemSummary", order.itemSummary)
                    put("totalQuantity", order.totalQuantity)
                    put("subtotalInr", order.subtotalInr)
                    put("deliveryChargeInr", order.deliveryChargeInr)
                    put("discountInr", order.discountInr)
                    put("totalPayableInr", order.totalPayableInr)
                    put("deliveryFeeConfirmed", order.deliveryFeeConfirmed)
                    put("paymentMethod", order.paymentMethod)
                    put("paymentStatus", order.paymentStatus)
                    put("orderStatus", order.orderStatus)
                    put("stockDeducted", order.stockDeducted)
                    put("cancellationReason", order.cancellationReason)
                    put("invoiceNumber", order.invoiceNumber)
                    put("createdAtMillis", order.createdAtMillis)
                    put("updatedAtMillis", order.updatedAtMillis)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeOrdersJson(raw: String): List<CustomerOrder> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val itemsArr = obj.optJSONArray("items")
                    val lines = buildList {
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val lo = itemsArr.optJSONObject(j) ?: continue
                                add(
                                    OrderLineItem(
                                        productId = lo.optString("productId", ""),
                                        productName = lo.optString("productName", ""),
                                        category = lo.optString("category", ProductCategory.SAREES.name),
                                        quantity = lo.optLong("quantity", 1L),
                                        unitPriceInr = lo.optLong("unitPriceInr", 0L),
                                        lineTotalInr = lo.optLong("lineTotalInr", 0L),
                                        selectedSize = lo.optString("selectedSize", ""),
                                        selectedColor = lo.optString("selectedColor", "")
                                    )
                                )
                            }
                        }
                    }
                    add(
                        CustomerOrder(
                            id = obj.optString("id", ""),
                            orderReference = obj.optString("orderReference", ""),
                            customerId = obj.optString("customerId", "guest"),
                            customerName = obj.optString("customerName", ""),
                            customerPhone = obj.optString("customerPhone", ""),
                            deliveryAddress = obj.optString("deliveryAddress", ""),
                            landmark = obj.optString("landmark", ""),
                            lineItems = lines,
                            itemSummary = obj.optString("itemSummary", ""),
                            totalQuantity = obj.optLong("totalQuantity", 1L),
                            subtotalInr = obj.optLong("subtotalInr", 0L),
                            deliveryChargeInr = obj.optLong("deliveryChargeInr", 0L),
                            discountInr = obj.optLong("discountInr", 0L),
                            totalPayableInr = obj.optLong("totalPayableInr", 0L),
                            deliveryFeeConfirmed = obj.optBoolean("deliveryFeeConfirmed", false),
                            paymentMethod = obj.optString("paymentMethod", PaymentMethodType.COD.name),
                            paymentStatus = obj.optString("paymentStatus", PaymentStatus.PENDING.name),
                            orderStatus = obj.optString("orderStatus", OrderStatus.PENDING.name),
                            stockDeducted = obj.optBoolean("stockDeducted", false),
                            cancellationReason = obj.optString("cancellationReason", ""),
                            invoiceNumber = obj.optString("invoiceNumber", ""),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
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
