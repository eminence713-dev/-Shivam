package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.DemoCatalog
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.InventoryMovement
import com.example.data.model.OfflineSale
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductCost
import com.example.data.model.StoreSettings
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class StoreRepository private constructor(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?,
    @Suppress("UNUSED_PARAMETER") internalMarker: Boolean
) {
    constructor(
        db: FirebaseFirestore,
        auth: FirebaseAuth = Firebase.auth
    ) : this(db = db, auth = auth, internalMarker = true)

    constructor(context: Context) : this(
        db = if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } else {
            null
        },
        auth = if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
            Firebase.auth
        } else {
            null
        },
        internalMarker = true
    )

    val isCloudConfigured: Boolean
        get() = db != null

    companion object {
        val VERIFIED_OWNER_EMAILS = setOf(
            "rajuguptas123@gmail.com",
            "eminence713@gmail.com"
        )

        fun currentEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

        fun generateOrderReference(): String {
            val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(Date())
            val suffix = UUID.randomUUID().toString().replace("-", "").take(5).uppercase(Locale.US)
            return "SSR-$datePart-$suffix"
        }

        fun generateInvoiceNumber(prefix: String = "INV"): String {
            val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(Date())
            val suffix = UUID.randomUUID().toString().replace("-", "").take(5).uppercase(Locale.US)
            return "$prefix-$datePart-$suffix"
        }
    }

    private fun requireUid(): String =
        checkNotNull(auth?.currentUser?.uid) { "Please sign in with the verified Shop Owner Google account to sync cloud changes." }

    suspend fun isUserRegisteredAdmin(uid: String): Boolean {
        val database = db ?: return false
        if (uid.isBlank()) return false
        return try {
            val snap = database.collection("admins").document(uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Seeds initial 10 starter items into Cloud Firestore ONLY when signed in as the verified shop owner
     * and the cloud `/products` collection is completely empty.
     */
    suspend fun ensureInitialCatalogSeededForOwner() {
        val database = db ?: return
        val user = auth?.currentUser ?: return
        val emailLower = user.email?.lowercase(Locale.US).orEmpty()
        val isTokenOwner = user.isEmailVerified && emailLower in VERIFIED_OWNER_EMAILS
        if (!isTokenOwner && !isUserRegisteredAdmin(user.uid)) return

        try {
            val existing = database.collection("products").limit(1).get().await()
            if (existing.isEmpty) {
                seedDemoProducts()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "products")
        }
    }

    fun observeStoreSettings(): Flow<StoreSettings> {
        val database = db ?: return LocalFallbackStore.observeSettings()
        return database.collection("store_settings")
            .document("public_config")
            .snapshots()
            .map { snap -> if (snap.exists()) snap.toStoreSettings() else StoreSettings() }
            .catch { e ->
                handleFirestoreError(e, OperationType.GET, "store_settings/public_config")
                throw e
            }
    }

    fun observeHasCloudStoreConfig(): Flow<Boolean> {
        val database = db ?: return kotlinx.coroutines.flow.flowOf(false)
        return database.collection("store_settings")
            .document("public_config")
            .snapshots()
            .map { snap -> snap.exists() }
            .catch { emit(false) }
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> {
        val database = db ?: return LocalFallbackStore.saveSettings(settings)
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val docRef = database.collection("store_settings").document("public_config")
        val payload = mapOf<String, Any?>(
            "storeNameEn" to settings.storeNameEn.trim().ifBlank { "Shri Shivam Readymade & Riya Fancy Shoes Mart" }.take(150),
            "storeNameHi" to settings.storeNameHi.trim().ifBlank { "श्री शिवम रेडीमेड एंड रिया फैंसी शूज़ मार्ट" }.take(150),
            "ownerName" to settings.ownerName.trim().ifBlank { "Raju Kumar Gupta" }.take(100),
            "phone" to settings.phone.trim().ifBlank { "9162685162" }.take(20),
            "whatsappNumber" to settings.whatsappNumber.trim().ifBlank { "9162685162" }.take(20),
            "email" to settings.email.trim().ifBlank { "rajuguptas123@gmail.com" }.take(120),
            "addressEn" to settings.addressEn.trim().ifBlank { "Bada Bazar, Kalyanpur, Motihari, Bihar, India" }.take(250),
            "addressHi" to settings.addressHi.trim().ifBlank { "बड़ा बाज़ार, कल्याणपुर, मोतिहारी, बिहार, भारत" }.take(250),
            "promoBannerEn" to settings.promoBannerEn.trim().take(300),
            "promoBannerHi" to settings.promoBannerHi.trim().take(300),
            "deliveryCoverageEn" to settings.deliveryCoverageEn.trim().take(300),
            "deliveryCoverageHi" to settings.deliveryCoverageHi.trim().take(300),
            "deliveryChargeInr" to settings.deliveryChargeInr.coerceIn(0L, 10_000L),
            "freeDeliveryThresholdInr" to settings.freeDeliveryThresholdInr.coerceIn(0L, 1_000_000L),
            "minOrderAmountInr" to settings.minOrderAmountInr.coerceIn(0L, 1_000_000L),
            "expectedDeliveryEn" to settings.expectedDeliveryEn.trim().take(300),
            "expectedDeliveryHi" to settings.expectedDeliveryHi.trim().take(300),
            "returnPolicyEn" to settings.returnPolicyEn.trim().take(600),
            "returnPolicyHi" to settings.returnPolicyHi.trim().take(600),
            "policiesConfirmed" to settings.policiesConfirmed,
            "codEnabled" to settings.codEnabled,
            "upiEnabled" to settings.upiEnabled,
            "merchantUpiId" to settings.merchantUpiId.trim().take(100),
            "merchantPayeeName" to settings.merchantPayeeName.trim().take(120),
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            database.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    tx.update(docRef, payload)
                } else {
                    tx.set(docRef, payload)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "store_settings/public_config")
            Result.failure(e)
        }
    }

    fun observePublishedProducts(): Flow<List<Product>> {
        val database = db ?: return LocalFallbackStore.observePublishedProducts()
        return database.collection("products")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toProductOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "products")
                throw e
            }
    }

    fun observeAllProductsForOwner(): Flow<List<Product>> {
        val database = db ?: return LocalFallbackStore.observeAllProducts()
        return database.collection("products")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toProductOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "products")
                throw e
            }
    }

    fun observeProductCostsForOwner(): Flow<Map<String, ProductCost>> {
        val database = db ?: return LocalFallbackStore.observeCosts()
        return database.collection("product_costs")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toProductCostOrNull() }.associateBy { it.productId }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "product_costs")
                throw e
            }
    }

    suspend fun saveProductWithCost(
        product: Product,
        costPriceInr: Long,
        supplierNote: String = ""
    ): Result<String> {
        val database = db ?: return LocalFallbackStore.saveProductWithCost(product, costPriceInr, supplierNote)
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val isNew = product.id.isBlank()
        val productId = if (isNew) {
            "prod_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        } else {
            product.id
        }
        val prodRef = database.collection("products").document(productId)
        val costRef = database.collection("product_costs").document(productId)

        val cleanNameEn = product.nameEn.trim().take(140)
        if (cleanNameEn.isEmpty()) {
            return Result.failure(IllegalArgumentException("Product English name is required."))
        }
        val cleanSelling = product.sellingPriceInr.coerceIn(1L, 500_000L)
        val cleanMrp = product.originalPriceInr.coerceIn(0L, 500_000L)
        val newStock = product.stockCount.coerceIn(0L, 100_000L)
        val cleanLowStock = product.lowStockThreshold.coerceIn(0L, 1_000L)
        val cleanSizes = product.sizes.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15)
        val cleanColors = product.colors.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15)
        val cleanCategory = ProductCategory.fromKey(product.category).name

        return try {
            database.runTransaction { tx ->
                val existingSnap = tx.get(prodRef)
                val existingCostSnap = tx.get(costRef)
                val previousStock = existingSnap.getLong("stockCount") ?: 0L
                val existingSold = existingSnap.getLong("soldCount") ?: product.soldCount.coerceAtLeast(0L)

                val mutableProductFields = mapOf<String, Any?>(
                    "nameEn" to cleanNameEn,
                    "nameHi" to product.nameHi.trim().take(140),
                    "category" to cleanCategory,
                    "descriptionEn" to product.descriptionEn.trim().take(1000),
                    "descriptionHi" to product.descriptionHi.trim().take(1000),
                    "sellingPriceInr" to cleanSelling,
                    "originalPriceInr" to cleanMrp,
                    "stockCount" to newStock,
                    "lowStockThreshold" to cleanLowStock,
                    "sizes" to cleanSizes,
                    "colors" to cleanColors,
                    "variantNotes" to product.variantNotes.trim().take(300),
                    "sampleImageKey" to product.sampleImageKey.trim().take(60),
                    "imageUrl" to product.imageUrl.trim().take(600),
                    "imageBase64" to product.imageBase64.take(350_000),
                    "isPublished" to product.isPublished,
                    "isFeatured" to product.isFeatured,
                    "isDemo" to product.isDemo,
                    "soldCount" to existingSold,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                if (existingSnap.exists()) {
                    tx.update(prodRef, mutableProductFields)
                } else {
                    val createFields = mutableProductFields + mapOf(
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(prodRef, createFields)
                }

                val costMutable = mapOf<String, Any?>(
                    "costPriceInr" to costPriceInr.coerceIn(0L, 500_000L),
                    "supplierNote" to supplierNote.trim().take(200),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                if (existingCostSnap.exists()) {
                    tx.update(costRef, costMutable)
                } else {
                    tx.set(costRef, costMutable + mapOf("productId" to productId))
                }

                if (!existingSnap.exists() || previousStock != newStock) {
                    val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    val movRef = database.collection("inventory_movements").document(movId)
                    tx.set(
                        movRef,
                        mapOf(
                            "productId" to productId,
                            "productName" to cleanNameEn,
                            "previousStock" to previousStock,
                            "newStock" to newStock,
                            "delta" to (newStock - previousStock),
                            "reason" to if (!existingSnap.exists()) "INITIAL_STOCK" else "MANUAL_UPDATE",
                            "referenceId" to productId,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, "products/$productId")
            Result.failure(e)
        }
    }

    suspend fun updateProductStockManually(product: Product, newStockCount: Long): Result<Unit> {
        val database = db ?: return LocalFallbackStore.updateStockManually(product, newStockCount)
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        if (product.id.isBlank()) return Result.failure(IllegalArgumentException("Invalid product ID"))
        val clampedStock = newStockCount.coerceIn(0L, 100_000L)
        val prodRef = database.collection("products").document(product.id)

        return try {
            database.runTransaction { tx ->
                val snap = tx.get(prodRef)
                if (!snap.exists()) error("Product no longer exists")
                val prevStock = snap.getLong("stockCount") ?: 0L
                if (prevStock == clampedStock) return@runTransaction
                val prodName = snap.getString("nameEn")?.take(140).orEmpty().ifBlank { product.nameEn.take(140) }

                tx.update(
                    prodRef,
                    mapOf(
                        "stockCount" to clampedStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                tx.set(
                    database.collection("inventory_movements").document(movId),
                    mapOf(
                        "productId" to product.id,
                        "productName" to prodName,
                        "previousStock" to prevStock,
                        "newStock" to clampedStock,
                        "delta" to (clampedStock - prevStock),
                        "reason" to "MANUAL_UPDATE",
                        "referenceId" to product.id,
                        "actorId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "products/${product.id}")
            Result.failure(e)
        }
    }

    suspend fun setProductPublished(productId: String, isPublished: Boolean): Result<Unit> {
        val database = db ?: return LocalFallbackStore.setPublished(productId, isPublished)
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            database.collection("products").document(productId)
                .update(
                    mapOf(
                        "isPublished" to isPublished,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "products/$productId")
            Result.failure(e)
        }
    }

    suspend fun hideAllDemoProducts(products: List<Product>): Result<Int> {
        val database = db ?: return LocalFallbackStore.hideAllDemoProducts(products)
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val publishedDemos = products.filter { it.isDemo && it.isPublished && it.id.isNotBlank() }
        if (publishedDemos.isEmpty()) return Result.success(0)
        return try {
            val batch = database.batch()
            publishedDemos.forEach { demo ->
                batch.update(
                    database.collection("products").document(demo.id),
                    mapOf(
                        "isPublished" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(publishedDemos.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "products")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val database = db ?: return LocalFallbackStore.deleteProduct(productId)
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val batch = database.batch()
            batch.delete(database.collection("products").document(productId))
            batch.delete(database.collection("product_costs").document(productId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "products/$productId")
            Result.failure(e)
        }
    }

    suspend fun seedDemoProducts(): Result<Int> {
        val database = db ?: return LocalFallbackStore.seedDemoProducts()
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val entries = DemoCatalog.buildSeedEntries()
        return try {
            val existingProdSnaps = entries.associate { entry ->
                entry.id to database.collection("products").document(entry.id).get().await()
            }
            val existingCostSnaps = entries.associate { entry ->
                entry.id to database.collection("product_costs").document(entry.id).get().await()
            }

            val batch = database.batch()
            entries.forEach { entry ->
                val p = entry.product
                val prodRef = database.collection("products").document(entry.id)
                val costRef = database.collection("product_costs").document(entry.id)
                val prodSnap = existingProdSnaps.getValue(entry.id)
                val costSnap = existingCostSnaps.getValue(entry.id)

                val prodMutable = mapOf<String, Any?>(
                    "nameEn" to p.nameEn.take(140),
                    "nameHi" to p.nameHi.take(140),
                    "category" to p.category,
                    "descriptionEn" to p.descriptionEn.take(1000),
                    "descriptionHi" to p.descriptionHi.take(1000),
                    "sellingPriceInr" to p.sellingPriceInr,
                    "originalPriceInr" to p.originalPriceInr,
                    "stockCount" to p.stockCount,
                    "lowStockThreshold" to p.lowStockThreshold,
                    "sizes" to p.sizes.take(15),
                    "colors" to p.colors.take(15),
                    "sampleImageKey" to p.sampleImageKey,
                    "isPublished" to true,
                    "isFeatured" to p.isFeatured,
                    "isDemo" to true,
                    "soldCount" to (prodSnap.getLong("soldCount") ?: 0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                if (prodSnap.exists()) {
                    batch.update(prodRef, prodMutable)
                } else {
                    batch.set(
                        prodRef,
                        prodMutable + mapOf(
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val movId = "mov_seed_${entry.id}_${UUID.randomUUID().toString().take(4)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    batch.set(
                        database.collection("inventory_movements").document(movId),
                        mapOf(
                            "productId" to entry.id,
                            "productName" to p.nameEn.take(140),
                            "previousStock" to 0L,
                            "newStock" to p.stockCount,
                            "delta" to p.stockCount,
                            "reason" to "INITIAL_STOCK",
                            "referenceId" to entry.id,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val costMutable = mapOf<String, Any?>(
                    "costPriceInr" to entry.sampleCostInr,
                    "supplierNote" to "Starter sample cost",
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                if (costSnap.exists()) {
                    batch.update(costRef, costMutable)
                } else {
                    batch.set(costRef, costMutable + mapOf("productId" to entry.id))
                }
            }
            batch.commit().await()
            Result.success(entries.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    suspend fun placeCustomerOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        cartItems: List<CartItem>,
        deterministicOrderId: String? = null
    ): Result<CustomerOrder> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }
        val cleanPhone = customerPhone.filter { it.isDigit() }.takeLast(10)
        if (!cleanPhone.matches(Regex("^[6-9][0-9]{9}$"))) {
            return Result.failure(IllegalArgumentException("Valid 10-digit Indian mobile number required"))
        }
        if (customerName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Please enter customer full name"))
        }
        if (deliveryAddress.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Please enter full delivery address"))
        }

        val orderId = deterministicOrderId?.takeIf { it.isNotBlank() }
            ?: "ord_${UUID.randomUUID().toString().replace("-", "").take(20)}"

        val database = db ?: return LocalFallbackStore.placeCustomerOrder(
            customerName = customerName,
            customerPhone = cleanPhone,
            deliveryAddress = deliveryAddress,
            landmark = landmark,
            paymentMethod = paymentMethod,
            cartItems = cartItems,
            orderId = orderId
        )

        val signedInUid = auth?.currentUser?.uid
        return if (signedInUid != null) {
            placeAuthenticatedAtomicOrder(
                database = database,
                uid = signedInUid,
                orderId = orderId,
                customerName = customerName,
                cleanPhone = cleanPhone,
                deliveryAddress = deliveryAddress,
                landmark = landmark,
                paymentMethod = paymentMethod,
                cartItems = cartItems
            )
        } else {
            placeGuestCloudOrder(
                database = database,
                orderId = orderId,
                customerName = customerName,
                cleanPhone = cleanPhone,
                deliveryAddress = deliveryAddress,
                landmark = landmark,
                paymentMethod = paymentMethod,
                cartItems = cartItems
            )
        }
    }

    private suspend fun placeGuestCloudOrder(
        database: FirebaseFirestore,
        orderId: String,
        customerName: String,
        cleanPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        cartItems: List<CartItem>
    ): Result<CustomerOrder> {
        val nowMillis = System.currentTimeMillis()
        val requestedQtyByProduct: Map<String, Int> = cartItems
            .groupBy { it.product.id }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        return try {
            val settingsSnap = database.collection("store_settings").document("public_config").get().await()
            val settings = if (settingsSnap.exists()) settingsSnap.toStoreSettings() else StoreSettings()
            if (paymentMethod == PaymentMethodType.COD && !settings.codEnabled) {
                error("Cash on Delivery is currently disabled by the shop owner.")
            }
            if (paymentMethod == PaymentMethodType.UPI_QR && !settings.isUpiConfiguredAndActive) {
                error("UPI QR payment is awaiting verified merchant setup by the shop owner.")
            }

            val resolvedProducts = mutableMapOf<String, Product>()
            for ((prodId, totalReqQty) in requestedQtyByProduct) {
                val snap = runCatching {
                    database.collection("products").document(prodId).get().await()
                }.getOrNull()
                val cloudProd = snap?.toProductOrNull()
                val fallbackStarter = cartItems.first { it.product.id == prodId }.product
                    .takeIf { cloudProd == null && !settingsSnap.exists() && it.isDemo }
                val effectiveProd = cloudProd ?: fallbackStarter
                    ?: error("One or more items are no longer available in the store.")

                if (!effectiveProd.isPublished) {
                    error("${effectiveProd.nameEn} is currently unavailable.")
                }
                if (totalReqQty <= 0 || totalReqQty > 100 || totalReqQty.toLong() > effectiveProd.stockCount) {
                    error("Insufficient stock for ${effectiveProd.nameEn} (Available: ${effectiveProd.stockCount}).")
                }
                resolvedProducts[prodId] = effectiveProd
            }

            val verifiedLineItems = cartItems.map { cartItem ->
                val prod = resolvedProducts.getValue(cartItem.product.id)
                val unitPrice = prod.sellingPriceInr
                val qty = cartItem.quantity.toLong()
                OrderLineItem(
                    productId = cartItem.product.id,
                    productName = prod.nameEn.take(120),
                    category = prod.category,
                    quantity = qty,
                    unitPriceInr = unitPrice,
                    lineTotalInr = unitPrice * qty,
                    selectedSize = cartItem.selectedSize.take(40),
                    selectedColor = cartItem.selectedColor.take(40)
                )
            }

            val subtotalInr = verifiedLineItems.sumOf { it.lineTotalInr }
            if (settings.minOrderAmountInr > 0 && subtotalInr < settings.minOrderAmountInr) {
                error("Minimum order amount is ₹${settings.minOrderAmountInr}.")
            }

            val deliveryChargeInr = when {
                !settings.policiesConfirmed -> 0L
                settings.freeDeliveryThresholdInr > 0 && subtotalInr >= settings.freeDeliveryThresholdInr -> 0L
                else -> settings.deliveryChargeInr.coerceAtLeast(0L)
            }
            val totalPayableInr = subtotalInr + deliveryChargeInr
            val totalQuantity = verifiedLineItems.sumOf { it.quantity }
            val orderReference = generateOrderReference()
            val itemSummary = verifiedLineItems.joinToString(", ") {
                buildString {
                    append(it.productName)
                    if (it.selectedSize.isNotBlank()) append(" (${it.selectedSize})")
                    append(" x${it.quantity}")
                }
            }.take(580)

            val itemMaps = verifiedLineItems.map { line ->
                mapOf(
                    "productId" to line.productId,
                    "productName" to line.productName,
                    "category" to line.category,
                    "quantity" to line.quantity,
                    "unitPriceInr" to line.unitPriceInr,
                    "lineTotalInr" to line.lineTotalInr,
                    "selectedSize" to line.selectedSize,
                    "selectedColor" to line.selectedColor
                )
            }

            val orderPayload = mapOf<String, Any?>(
                "orderReference" to orderReference,
                "customerId" to "guest",
                "customerName" to customerName.trim().take(100),
                "customerPhone" to cleanPhone,
                "deliveryAddress" to deliveryAddress.trim().take(400),
                "landmark" to landmark.trim().take(200),
                "items" to itemMaps,
                "itemSummary" to itemSummary,
                "totalQuantity" to totalQuantity,
                "subtotalInr" to subtotalInr,
                "deliveryChargeInr" to deliveryChargeInr,
                "discountInr" to 0L,
                "totalPayableInr" to totalPayableInr,
                "deliveryFeeConfirmed" to settings.policiesConfirmed,
                "paymentMethod" to paymentMethod.name,
                "paymentStatus" to PaymentStatus.PENDING.name,
                "orderStatus" to OrderStatus.PENDING.name,
                "stockDeducted" to false,
                "cancellationReason" to "",
                "invoiceNumber" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            database.collection("orders").document(orderId).set(orderPayload).await()

            Result.success(
                CustomerOrder(
                    id = orderId,
                    orderReference = orderReference,
                    customerId = "guest",
                    customerName = customerName.trim().take(100),
                    customerPhone = cleanPhone,
                    deliveryAddress = deliveryAddress.trim().take(400),
                    landmark = landmark.trim().take(200),
                    lineItems = verifiedLineItems,
                    itemSummary = itemSummary,
                    totalQuantity = totalQuantity,
                    subtotalInr = subtotalInr,
                    deliveryChargeInr = deliveryChargeInr,
                    discountInr = 0L,
                    totalPayableInr = totalPayableInr,
                    deliveryFeeConfirmed = settings.policiesConfirmed,
                    paymentMethod = paymentMethod.name,
                    paymentStatus = PaymentStatus.PENDING.name,
                    orderStatus = OrderStatus.PENDING.name,
                    stockDeducted = false,
                    createdAtMillis = nowMillis,
                    updatedAtMillis = nowMillis
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    private suspend fun placeAuthenticatedAtomicOrder(
        database: FirebaseFirestore,
        uid: String,
        orderId: String,
        customerName: String,
        cleanPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        cartItems: List<CartItem>
    ): Result<CustomerOrder> {
        val nowMillis = System.currentTimeMillis()
        val requestedQtyByProduct: Map<String, Int> = cartItems
            .groupBy { it.product.id }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        val orderRef = database.collection("orders").document(orderId)
        val settingsRef = database.collection("store_settings").document("public_config")

        return try {
            val createdOrder = database.runTransaction { tx ->
                val existingOrderSnap = tx.get(orderRef)
                if (existingOrderSnap.exists()) {
                    error("Duplicate order submission detected. Please check My Orders.")
                }

                val settingsSnap = tx.get(settingsRef)
                val settings = if (settingsSnap.exists()) settingsSnap.toStoreSettings() else StoreSettings()
                if (paymentMethod == PaymentMethodType.COD && !settings.codEnabled) {
                    error("Cash on Delivery is currently disabled by the shop owner.")
                }
                if (paymentMethod == PaymentMethodType.UPI_QR && !settings.isUpiConfiguredAndActive) {
                    error("UPI QR payment is awaiting verified merchant setup by the shop owner.")
                }

                val productSnaps = requestedQtyByProduct.keys.associateWith { prodId ->
                    tx.get(database.collection("products").document(prodId))
                }

                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val snap = productSnaps[prodId]
                    val prod = snap?.toProductOrNull()
                    if (prod == null || !prod.isPublished) {
                        error("One or more items are no longer available in the store.")
                    }
                    if (totalReqQty <= 0 || totalReqQty > 100 || totalReqQty.toLong() > prod.stockCount) {
                        error("Insufficient stock for ${prod.nameEn} (Available: ${prod.stockCount}).")
                    }
                }

                val verifiedLineItems = cartItems.map { cartItem ->
                    val prod = productSnaps.getValue(cartItem.product.id).toProductOrNull()!!
                    val unitPrice = prod.sellingPriceInr
                    val qty = cartItem.quantity.toLong()
                    OrderLineItem(
                        productId = cartItem.product.id,
                        productName = prod.nameEn.take(120),
                        category = prod.category,
                        quantity = qty,
                        unitPriceInr = unitPrice,
                        lineTotalInr = unitPrice * qty,
                        selectedSize = cartItem.selectedSize.take(40),
                        selectedColor = cartItem.selectedColor.take(40)
                    )
                }

                val subtotalInr = verifiedLineItems.sumOf { it.lineTotalInr }
                if (settings.minOrderAmountInr > 0 && subtotalInr < settings.minOrderAmountInr) {
                    error("Minimum order amount is ₹${settings.minOrderAmountInr}.")
                }

                val deliveryChargeInr = when {
                    !settings.policiesConfirmed -> 0L
                    settings.freeDeliveryThresholdInr > 0 && subtotalInr >= settings.freeDeliveryThresholdInr -> 0L
                    else -> settings.deliveryChargeInr.coerceAtLeast(0L)
                }

                val totalPayableInr = subtotalInr + deliveryChargeInr
                val totalQuantity = verifiedLineItems.sumOf { it.quantity }
                val orderReference = generateOrderReference()
                val itemSummary = verifiedLineItems.joinToString(", ") {
                    buildString {
                        append(it.productName)
                        if (it.selectedSize.isNotBlank()) append(" (${it.selectedSize})")
                        append(" x${it.quantity}")
                    }
                }.take(580)

                val itemMaps = verifiedLineItems.map { line ->
                    mapOf(
                        "productId" to line.productId,
                        "productName" to line.productName,
                        "category" to line.category,
                        "quantity" to line.quantity,
                        "unitPriceInr" to line.unitPriceInr,
                        "lineTotalInr" to line.lineTotalInr,
                        "selectedSize" to line.selectedSize,
                        "selectedColor" to line.selectedColor
                    )
                }

                val orderPayload = mapOf<String, Any?>(
                    "orderReference" to orderReference,
                    "customerId" to uid,
                    "customerName" to customerName.trim().take(100),
                    "customerPhone" to cleanPhone,
                    "deliveryAddress" to deliveryAddress.trim().take(400),
                    "landmark" to landmark.trim().take(200),
                    "items" to itemMaps,
                    "itemSummary" to itemSummary,
                    "totalQuantity" to totalQuantity,
                    "subtotalInr" to subtotalInr,
                    "deliveryChargeInr" to deliveryChargeInr,
                    "discountInr" to 0L,
                    "totalPayableInr" to totalPayableInr,
                    "deliveryFeeConfirmed" to settings.policiesConfirmed,
                    "paymentMethod" to paymentMethod.name,
                    "paymentStatus" to PaymentStatus.PENDING.name,
                    "orderStatus" to OrderStatus.PENDING.name,
                    "stockDeducted" to true,
                    "cancellationReason" to "",
                    "invoiceNumber" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                tx.set(orderRef, orderPayload)

                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val prod = productSnaps.getValue(prodId).toProductOrNull()!!
                    val prevStock = prod.stockCount
                    val newStock = prevStock - totalReqQty.toLong()
                    val prodRef = database.collection("products").document(prodId)
                    tx.update(
                        prodRef,
                        mapOf(
                            "stockCount" to newStock,
                            "soldCount" to (prod.soldCount + totalReqQty.toLong()),
                            "lastOrderId" to orderId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val movId = "mov_${orderId}_${prodId.takeLast(8)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        .take(120)
                    tx.set(
                        database.collection("inventory_movements").document(movId),
                        mapOf(
                            "productId" to prodId,
                            "productName" to prod.nameEn.take(140),
                            "previousStock" to prevStock,
                            "newStock" to newStock,
                            "delta" to -totalReqQty.toLong(),
                            "reason" to "ONLINE_ORDER",
                            "referenceId" to orderId,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                CustomerOrder(
                    id = orderId,
                    orderReference = orderReference,
                    customerId = uid,
                    customerName = customerName.trim().take(100),
                    customerPhone = cleanPhone,
                    deliveryAddress = deliveryAddress.trim().take(400),
                    landmark = landmark.trim().take(200),
                    lineItems = verifiedLineItems,
                    itemSummary = itemSummary,
                    totalQuantity = totalQuantity,
                    subtotalInr = subtotalInr,
                    deliveryChargeInr = deliveryChargeInr,
                    discountInr = 0L,
                    totalPayableInr = totalPayableInr,
                    deliveryFeeConfirmed = settings.policiesConfirmed,
                    paymentMethod = paymentMethod.name,
                    paymentStatus = PaymentStatus.PENDING.name,
                    orderStatus = OrderStatus.PENDING.name,
                    stockDeducted = true,
                    createdAtMillis = nowMillis,
                    updatedAtMillis = nowMillis
                )
            }.await()
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    fun observeCustomerOrders(customerId: String = auth?.currentUser?.uid.orEmpty()): Flow<List<CustomerOrder>> {
        val database = db ?: return emptyFlow()
        if (customerId.isBlank() || customerId == "guest") return emptyFlow()
        return database.collection("orders")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toCustomerOrderOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "orders")
                throw e
            }
    }

    suspend fun getOrderById(orderId: String): Result<CustomerOrder> {
        val database = db ?: return Result.failure(IllegalStateException("Cloud database not configured"))
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = database.collection("orders").document(orderId).get().await()
            val order = snap.toCustomerOrderOrNull() ?: error("Order not found")
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "orders/$orderId")
            Result.failure(e)
        }
    }

    fun observeAllOrdersForOwner(): Flow<List<CustomerOrder>> {
        val database = db ?: return LocalFallbackStore.observeOrders()
        return database.collection("orders")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toCustomerOrderOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "orders")
                throw e
            }
    }

    suspend fun updateOrderByOwner(
        orderId: String,
        newOrderStatus: OrderStatus,
        newPaymentStatus: PaymentStatus,
        cancellationReason: String = "",
        generateInvoiceIfMissing: Boolean = false
    ): Result<Unit> {
        val database = db ?: return LocalFallbackStore.updateOrderByOwner(
            orderId = orderId,
            newStatus = newOrderStatus,
            newPaymentStatus = newPaymentStatus,
            cancellationReason = cancellationReason,
            generateInvoice = generateInvoiceIfMissing
        )
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val orderRef = database.collection("orders").document(orderId)

        return try {
            database.runTransaction { tx ->
                // 1. ALL READS FIRST
                val orderSnap = tx.get(orderRef)
                val currentOrder = orderSnap.toCustomerOrderOrNull() ?: error("Order not found")
                val shouldDeductGuestStock = !currentOrder.stockDeducted &&
                    newOrderStatus in listOf(
                        OrderStatus.CONFIRMED,
                        OrderStatus.PACKED,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED
                    )
                val shouldRestock = newOrderStatus == OrderStatus.CANCELLED && currentOrder.stockDeducted

                val lineItemsByProduct = if (shouldDeductGuestStock || shouldRestock) {
                    currentOrder.parsedItems()
                        .filter { it.productId.isNotBlank() }
                        .groupBy { it.productId }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }
                } else {
                    emptyMap()
                }

                val prodSnaps = lineItemsByProduct.keys.associateWith { prodId ->
                    tx.get(database.collection("products").document(prodId))
                }

                // 2. WRITES
                var updatedStockDeducted = currentOrder.stockDeducted
                if (shouldDeductGuestStock) {
                    lineItemsByProduct.forEach { (prodId, qtyToDeduct) ->
                        val pSnap = prodSnaps[prodId]
                        if (pSnap != null && pSnap.exists()) {
                            val prevStock = pSnap.getLong("stockCount") ?: 0L
                            val prevSold = pSnap.getLong("soldCount") ?: 0L
                            val newStock = (prevStock - qtyToDeduct).coerceAtLeast(0L)
                            val newSold = (prevSold + qtyToDeduct).coerceAtMost(1_000_000L)
                            val prodName = pSnap.getString("nameEn")?.take(140).orEmpty().ifBlank { "Product" }

                            tx.update(
                                database.collection("products").document(prodId),
                                mapOf(
                                    "stockCount" to newStock,
                                    "soldCount" to newSold,
                                    "lastOrderId" to orderId,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            val movId = "mov_confirm_${orderId}_${prodId.takeLast(6)}_${UUID.randomUUID().toString().take(4)}"
                                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                .take(120)
                            tx.set(
                                database.collection("inventory_movements").document(movId),
                                mapOf(
                                    "productId" to prodId,
                                    "productName" to prodName,
                                    "previousStock" to prevStock,
                                    "newStock" to newStock,
                                    "delta" to (newStock - prevStock),
                                    "reason" to "ONLINE_ORDER",
                                    "referenceId" to orderId,
                                    "actorId" to uid,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                    updatedStockDeducted = true
                } else if (shouldRestock) {
                    lineItemsByProduct.forEach { (prodId, qtyToRestore) ->
                        val pSnap = prodSnaps[prodId]
                        if (pSnap != null && pSnap.exists()) {
                            val prevStock = pSnap.getLong("stockCount") ?: 0L
                            val prevSold = pSnap.getLong("soldCount") ?: 0L
                            val newStock = (prevStock + qtyToRestore).coerceAtMost(100_000L)
                            val newSold = (prevSold - qtyToRestore).coerceAtLeast(0L)
                            val prodName = pSnap.getString("nameEn")?.take(140).orEmpty().ifBlank { "Product" }

                            tx.update(
                                database.collection("products").document(prodId),
                                mapOf(
                                    "stockCount" to newStock,
                                    "soldCount" to newSold,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            val movId = "mov_restock_${orderId}_${prodId.takeLast(6)}_${UUID.randomUUID().toString().take(4)}"
                                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                .take(120)
                            tx.set(
                                database.collection("inventory_movements").document(movId),
                                mapOf(
                                    "productId" to prodId,
                                    "productName" to prodName,
                                    "previousStock" to prevStock,
                                    "newStock" to newStock,
                                    "delta" to (newStock - prevStock),
                                    "reason" to "ORDER_CANCELLED_RESTOCK",
                                    "referenceId" to orderId,
                                    "actorId" to uid,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                    updatedStockDeducted = false
                }

                val invoiceNum = if (currentOrder.invoiceNumber.isNotBlank()) {
                    currentOrder.invoiceNumber
                } else if (generateInvoiceIfMissing || newOrderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.DELIVERED)) {
                    generateInvoiceNumber("INV")
                } else {
                    ""
                }

                val nextCancelReason = if (cancellationReason.isNotBlank()) {
                    cancellationReason.trim().take(300)
                } else {
                    currentOrder.cancellationReason
                }

                tx.update(
                    orderRef,
                    mapOf<String, Any?>(
                        "orderStatus" to newOrderStatus.name,
                        "paymentStatus" to newPaymentStatus.name,
                        "stockDeducted" to updatedStockDeducted,
                        "cancellationReason" to nextCancelReason,
                        "invoiceNumber" to invoiceNum,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    suspend fun recordOfflineSale(
        customerName: String,
        customerPhone: String,
        lineItems: List<OrderLineItem>,
        discountInr: Long,
        paymentMethod: String
    ): Result<OfflineSale> {
        val database = db ?: return LocalFallbackStore.recordOfflineSale(
            customerName = customerName,
            customerPhone = customerPhone,
            lineItems = lineItems,
            discountInr = discountInr,
            paymentMethod = paymentMethod
        )
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        if (lineItems.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one item"))
        val saleId = "sale_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val nowMillis = System.currentTimeMillis()
        val catalogProductIds = lineItems.map { it.productId }.filter { it.isNotBlank() }.distinct()

        return try {
            val savedSale = database.runTransaction { tx ->
                val prodSnaps = catalogProductIds.associateWith { id ->
                    tx.get(database.collection("products").document(id))
                }
                val costSnaps = catalogProductIds.associateWith { id ->
                    tx.get(database.collection("product_costs").document(id))
                }

                val qtyByProduct = lineItems
                    .filter { it.productId.isNotBlank() }
                    .groupBy { it.productId }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }

                qtyByProduct.forEach { (prodId, reqQty) ->
                    val pSnap = prodSnaps[prodId]
                    if (pSnap == null || !pSnap.exists()) error("Selected product no longer exists.")
                    val avail = pSnap.getLong("stockCount") ?: 0L
                    val pName = pSnap.getString("nameEn").orEmpty()
                    if (reqQty > avail) {
                        error("Only $avail units available in stock for $pName.")
                    }
                }

                val subtotalInr = lineItems.sumOf { it.lineTotalInr }.coerceAtLeast(1L)
                val safeDiscount = discountInr.coerceIn(0L, subtotalInr)
                val totalPaidInr = (subtotalInr - safeDiscount).coerceAtLeast(0L)

                var totalCostInr = 0L
                var grossCostCoveredSubtotal = 0L
                var allRecorded = true

                lineItems.forEach { line ->
                    val unitCost = if (line.productId.isNotBlank()) {
                        costSnaps[line.productId]?.getLong("costPriceInr") ?: 0L
                    } else {
                        0L
                    }
                    if (unitCost > 0L) {
                        totalCostInr += unitCost * line.quantity
                        grossCostCoveredSubtotal += line.lineTotalInr
                    } else {
                        allRecorded = false
                    }
                }

                val costCoveredRevenueInr = if (subtotalInr > 0L && grossCostCoveredSubtotal > 0L) {
                    (totalPaidInr * grossCostCoveredSubtotal) / subtotalInr
                } else {
                    0L
                }

                qtyByProduct.forEach { (prodId, reqQty) ->
                    val pSnap = prodSnaps.getValue(prodId)
                    val prevStock = pSnap.getLong("stockCount") ?: 0L
                    val prevSold = pSnap.getLong("soldCount") ?: 0L
                    val newStock = prevStock - reqQty
                    val pName = pSnap.getString("nameEn")?.take(140).orEmpty().ifBlank { "Product" }

                    tx.update(
                        database.collection("products").document(prodId),
                        mapOf(
                            "stockCount" to newStock,
                            "soldCount" to (prevSold + reqQty),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val movId = "mov_${saleId}_${prodId.takeLast(8)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        .take(120)
                    tx.set(
                        database.collection("inventory_movements").document(movId),
                        mapOf(
                            "productId" to prodId,
                            "productName" to pName,
                            "previousStock" to prevStock,
                            "newStock" to newStock,
                            "delta" to -reqQty,
                            "reason" to "OFFLINE_SALE",
                            "referenceId" to saleId,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val invoiceNumber = generateInvoiceNumber("POS")
                val itemSummary = lineItems.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(580)
                val safeMethod = if (paymentMethod in setOf("CASH", "UPI", "CARD")) paymentMethod else "CASH"
                val epochDay = currentEpochDay()
                val itemMaps = lineItems.map { line ->
                    mapOf(
                        "productId" to line.productId,
                        "productName" to line.productName.take(120),
                        "category" to line.category,
                        "quantity" to line.quantity,
                        "unitPriceInr" to line.unitPriceInr,
                        "lineTotalInr" to line.lineTotalInr,
                        "selectedSize" to line.selectedSize.take(40),
                        "selectedColor" to line.selectedColor.take(40)
                    )
                }

                val salePayload = mapOf<String, Any?>(
                    "invoiceNumber" to invoiceNumber,
                    "customerName" to customerName.trim().take(100),
                    "customerPhone" to customerPhone.trim().take(20),
                    "items" to itemMaps,
                    "itemSummary" to itemSummary,
                    "subtotalInr" to subtotalInr,
                    "discountInr" to safeDiscount,
                    "totalPaidInr" to totalPaidInr,
                    "totalCostInr" to totalCostInr,
                    "costCoveredRevenueInr" to costCoveredRevenueInr,
                    "allCostsRecorded" to allRecorded,
                    "paymentMethod" to safeMethod,
                    "recordedBy" to uid,
                    "saleDateEpochDay" to epochDay,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                tx.set(database.collection("offline_sales").document(saleId), salePayload)

                OfflineSale(
                    id = saleId,
                    invoiceNumber = invoiceNumber,
                    customerName = customerName.trim().take(100),
                    customerPhone = customerPhone.trim().take(20),
                    lineItems = lineItems,
                    itemSummary = itemSummary,
                    subtotalInr = subtotalInr,
                    discountInr = safeDiscount,
                    totalPaidInr = totalPaidInr,
                    totalCostInr = totalCostInr,
                    costCoveredRevenueInr = costCoveredRevenueInr,
                    allCostsRecorded = allRecorded,
                    paymentMethod = safeMethod,
                    recordedBy = uid,
                    saleDateEpochDay = epochDay,
                    createdAtMillis = nowMillis
                )
            }.await()
            Result.success(savedSale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "offline_sales/$saleId")
            Result.failure(e)
        }
    }

    fun observeOfflineSalesForOwner(): Flow<List<OfflineSale>> {
        val database = db ?: return LocalFallbackStore.observeOfflineSales()
        return database.collection("offline_sales")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toOfflineSaleOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "offline_sales")
                throw e
            }
    }

    fun observeExpensesForOwner(): Flow<List<Expense>> {
        val database = db ?: return LocalFallbackStore.observeExpenses()
        return database.collection("expenses")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toExpenseOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "expenses")
                throw e
            }
    }

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountInr: Long,
        notes: String
    ): Result<String> {
        val database = db ?: return LocalFallbackStore.addExpense(title, category, amountInr, notes)
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val cleanTitle = title.trim().take(140)
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("Expense title must be at least 2 characters."))
        }
        val expId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val payload = mapOf<String, Any?>(
            "title" to cleanTitle,
            "category" to category.name,
            "amountInr" to amountInr.coerceIn(1L, 5_000_000L),
            "notes" to notes.trim().take(300),
            "recordedBy" to uid,
            "expenseDateEpochDay" to currentEpochDay(),
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            database.collection("expenses").document(expId).set(payload).await()
            Result.success(expId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "expenses/$expId")
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        val database = db ?: return LocalFallbackStore.deleteExpense(expenseId)
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            database.collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "expenses/$expenseId")
            Result.failure(e)
        }
    }

    fun observeInventoryMovementsForOwner(): Flow<List<InventoryMovement>> {
        val database = db ?: return LocalFallbackStore.observeMovements()
        return database.collection("inventory_movements")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toInventoryMovementOrNull() } }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "inventory_movements")
                throw e
            }
    }

    // --- Safe DocumentSnapshot Mappers using ServerTimestampBehavior.ESTIMATE ---

    private fun DocumentSnapshot.estimatedMillis(field: String): Long =
        getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.stringList(field: String): List<String> =
        (get(field) as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.parseOrderLineItems(): List<OrderLineItem> {
        val raw = get("items") as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            val m = entry as? Map<*, *> ?: return@mapNotNull null
            OrderLineItem(
                productId = m["productId"]?.toString().orEmpty(),
                productName = m["productName"]?.toString().orEmpty(),
                category = m["category"]?.toString().orEmpty().ifBlank { ProductCategory.SAREES.name },
                quantity = (m["quantity"] as? Number)?.toLong() ?: 1L,
                unitPriceInr = (m["unitPriceInr"] as? Number)?.toLong() ?: 0L,
                lineTotalInr = (m["lineTotalInr"] as? Number)?.toLong() ?: 0L,
                selectedSize = m["selectedSize"]?.toString().orEmpty(),
                selectedColor = m["selectedColor"]?.toString().orEmpty()
            )
        }
    }

    private fun DocumentSnapshot.toStoreSettings(): StoreSettings {
        val d = StoreSettings()
        return StoreSettings(
            id = id,
            storeNameEn = getString("storeNameEn") ?: d.storeNameEn,
            storeNameHi = getString("storeNameHi") ?: d.storeNameHi,
            ownerName = getString("ownerName") ?: d.ownerName,
            phone = getString("phone") ?: d.phone,
            whatsappNumber = getString("whatsappNumber") ?: d.whatsappNumber,
            email = getString("email") ?: d.email,
            addressEn = getString("addressEn") ?: d.addressEn,
            addressHi = getString("addressHi") ?: d.addressHi,
            promoBannerEn = getString("promoBannerEn") ?: d.promoBannerEn,
            promoBannerHi = getString("promoBannerHi") ?: d.promoBannerHi,
            deliveryCoverageEn = getString("deliveryCoverageEn") ?: d.deliveryCoverageEn,
            deliveryCoverageHi = getString("deliveryCoverageHi") ?: d.deliveryCoverageHi,
            deliveryChargeInr = getLong("deliveryChargeInr") ?: d.deliveryChargeInr,
            freeDeliveryThresholdInr = getLong("freeDeliveryThresholdInr") ?: d.freeDeliveryThresholdInr,
            minOrderAmountInr = getLong("minOrderAmountInr") ?: d.minOrderAmountInr,
            expectedDeliveryEn = getString("expectedDeliveryEn") ?: d.expectedDeliveryEn,
            expectedDeliveryHi = getString("expectedDeliveryHi") ?: d.expectedDeliveryHi,
            returnPolicyEn = getString("returnPolicyEn") ?: d.returnPolicyEn,
            returnPolicyHi = getString("returnPolicyHi") ?: d.returnPolicyHi,
            policiesConfirmed = getBoolean("policiesConfirmed") ?: d.policiesConfirmed,
            codEnabled = getBoolean("codEnabled") ?: d.codEnabled,
            upiEnabled = getBoolean("upiEnabled") ?: d.upiEnabled,
            merchantUpiId = getString("merchantUpiId") ?: d.merchantUpiId,
            merchantPayeeName = getString("merchantPayeeName") ?: d.merchantPayeeName,
            updatedBy = getString("updatedBy") ?: d.updatedBy,
            updatedAtMillis = estimatedMillis("updatedAt")
        )
    }

    private fun DocumentSnapshot.toProductOrNull(): Product? {
        if (!exists()) return null
        val nameEn = getString("nameEn") ?: return null
        return Product(
            id = id,
            nameEn = nameEn,
            nameHi = getString("nameHi").orEmpty(),
            category = getString("category") ?: ProductCategory.SAREES.name,
            descriptionEn = getString("descriptionEn").orEmpty(),
            descriptionHi = getString("descriptionHi").orEmpty(),
            sellingPriceInr = getLong("sellingPriceInr") ?: 0L,
            originalPriceInr = getLong("originalPriceInr") ?: 0L,
            stockCount = getLong("stockCount") ?: 0L,
            lowStockThreshold = getLong("lowStockThreshold") ?: 3L,
            sizes = stringList("sizes"),
            colors = stringList("colors"),
            variantNotes = getString("variantNotes").orEmpty(),
            sampleImageKey = getString("sampleImageKey").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            imageBase64 = getString("imageBase64").orEmpty(),
            isPublished = getBoolean("isPublished") ?: true,
            isFeatured = getBoolean("isFeatured") ?: false,
            isDemo = getBoolean("isDemo") ?: false,
            soldCount = getLong("soldCount") ?: 0L,
            lastOrderId = getString("lastOrderId").orEmpty(),
            createdBy = getString("createdBy").orEmpty(),
            createdAtMillis = estimatedMillis("createdAt"),
            updatedAtMillis = estimatedMillis("updatedAt")
        )
    }

    private fun DocumentSnapshot.toProductCostOrNull(): ProductCost? {
        if (!exists()) return null
        val prodId = getString("productId") ?: id
        return ProductCost(
            id = id,
            productId = prodId,
            costPriceInr = getLong("costPriceInr") ?: 0L,
            supplierNote = getString("supplierNote").orEmpty(),
            updatedBy = getString("updatedBy").orEmpty(),
            updatedAtMillis = estimatedMillis("updatedAt")
        )
    }

    private fun DocumentSnapshot.toCustomerOrderOrNull(): CustomerOrder? {
        if (!exists()) return null
        val orderRef = getString("orderReference") ?: return null
        return CustomerOrder(
            id = id,
            orderReference = orderRef,
            customerId = getString("customerId").orEmpty(),
            customerName = getString("customerName").orEmpty(),
            customerPhone = getString("customerPhone").orEmpty(),
            deliveryAddress = getString("deliveryAddress").orEmpty(),
            landmark = getString("landmark").orEmpty(),
            lineItems = parseOrderLineItems(),
            itemSummary = getString("itemSummary").orEmpty(),
            totalQuantity = getLong("totalQuantity") ?: 1L,
            subtotalInr = getLong("subtotalInr") ?: 0L,
            deliveryChargeInr = getLong("deliveryChargeInr") ?: 0L,
            discountInr = getLong("discountInr") ?: 0L,
            totalPayableInr = getLong("totalPayableInr") ?: 0L,
            deliveryFeeConfirmed = getBoolean("deliveryFeeConfirmed") ?: false,
            paymentMethod = getString("paymentMethod") ?: PaymentMethodType.COD.name,
            paymentStatus = getString("paymentStatus") ?: PaymentStatus.PENDING.name,
            orderStatus = getString("orderStatus") ?: OrderStatus.PENDING.name,
            stockDeducted = getBoolean("stockDeducted") ?: true,
            cancellationReason = getString("cancellationReason").orEmpty(),
            invoiceNumber = getString("invoiceNumber").orEmpty(),
            createdAtMillis = estimatedMillis("createdAt"),
            updatedAtMillis = estimatedMillis("updatedAt")
        )
    }

    private fun DocumentSnapshot.toOfflineSaleOrNull(): OfflineSale? {
        if (!exists()) return null
        val inv = getString("invoiceNumber") ?: return null
        return OfflineSale(
            id = id,
            invoiceNumber = inv,
            customerName = getString("customerName").orEmpty(),
            customerPhone = getString("customerPhone").orEmpty(),
            lineItems = parseOrderLineItems(),
            itemSummary = getString("itemSummary").orEmpty(),
            subtotalInr = getLong("subtotalInr") ?: 0L,
            discountInr = getLong("discountInr") ?: 0L,
            totalPaidInr = getLong("totalPaidInr") ?: 0L,
            totalCostInr = getLong("totalCostInr") ?: 0L,
            costCoveredRevenueInr = getLong("costCoveredRevenueInr") ?: 0L,
            allCostsRecorded = getBoolean("allCostsRecorded") ?: false,
            paymentMethod = getString("paymentMethod") ?: "CASH",
            recordedBy = getString("recordedBy").orEmpty(),
            saleDateEpochDay = getLong("saleDateEpochDay") ?: 0L,
            createdAtMillis = estimatedMillis("createdAt")
        )
    }

    private fun DocumentSnapshot.toExpenseOrNull(): Expense? {
        if (!exists()) return null
        val title = getString("title") ?: return null
        return Expense(
            id = id,
            title = title,
            category = getString("category") ?: ExpenseCategory.OTHER.name,
            amountInr = getLong("amountInr") ?: 0L,
            notes = getString("notes").orEmpty(),
            recordedBy = getString("recordedBy").orEmpty(),
            expenseDateEpochDay = getLong("expenseDateEpochDay") ?: 0L,
            createdAtMillis = estimatedMillis("createdAt")
        )
    }

    private fun DocumentSnapshot.toInventoryMovementOrNull(): InventoryMovement? {
        if (!exists()) return null
        val prodId = getString("productId") ?: return null
        return InventoryMovement(
            id = id,
            productId = prodId,
            productName = getString("productName").orEmpty(),
            previousStock = getLong("previousStock") ?: 0L,
            newStock = getLong("newStock") ?: 0L,
            delta = getLong("delta") ?: 0L,
            reason = getString("reason") ?: "MANUAL_UPDATE",
            referenceId = getString("referenceId").orEmpty(),
            actorId = getString("actorId").orEmpty(),
            createdAtMillis = estimatedMillis("createdAt")
        )
    }
}
