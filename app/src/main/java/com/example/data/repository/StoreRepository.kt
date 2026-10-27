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
import com.example.data.model.ProductCost
import com.example.data.model.StoreSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class StoreRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    companion object {
        val AUTHORIZED_OWNER_EMAILS = setOf(
            "rajuguptas123@gmail.com",
            "eminence713@gmail.com"
        )

        fun isVerifiedOwnerEmail(email: String?, isEmailVerified: Boolean): Boolean {
            if (!isEmailVerified) return false
            val normalized = email?.trim()?.lowercase(Locale.ROOT) ?: return false
            return normalized in AUTHORIZED_OWNER_EMAILS
        }

        fun isVerifiedOwnerEmail(user: FirebaseUser?): Boolean {
            if (user == null) return false
            return isVerifiedOwnerEmail(user.email, user.isEmailVerified)
        }

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

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing store records.")
    }

    suspend fun checkIsOwnerAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        if (isVerifiedOwnerEmail(user)) return true
        val path = "admins/${user.uid}"
        return try {
            val snap = db.collection("admins").document(user.uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    fun observeStoreSettings(): Flow<StoreSettings> {
        val path = "store_settings/public_config"
        return db.collection("store_settings").document("public_config")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(StoreSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: StoreSettings()
                } else {
                    StoreSettings()
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("store_settings").document("public_config")
        return try {
            docRef.set(settings.toFirestoreMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Customer catalog query: explicitly filters isPublished == true to align with firestore.rules
    fun observePublishedProducts(): Flow<List<Product>> {
        val path = "products"
        return db.collection("products")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // Owner catalog query: observes both published and unpublished products
    fun observeAllProductsForOwner(): Flow<List<Product>> {
        val path = "products"
        return db.collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeProductCostsForOwner(): Flow<Map<String, ProductCost>> {
        val path = "product_costs"
        return db.collection("product_costs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProductCost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .associateBy { it.productId.ifBlank { it.id } }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun saveProductWithCost(
        product: Product,
        costPriceInr: Long,
        supplierNote: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val isNew = product.id.isBlank()
        val productId = if (isNew) {
            "prod_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        } else {
            product.id
        }
        val productRef = db.collection("products").document(productId)
        val costRef = db.collection("product_costs").document(productId)

        return try {
            db.runTransaction { tx ->
                val existingSnap = tx.get(productRef)
                val previousStock = if (existingSnap.exists()) {
                    existingSnap.getLong("stockCount") ?: 0L
                } else {
                    0L
                }
                val newStock = product.stockCount.coerceIn(0L, 100000L)

                if (!existingSnap.exists()) {
                    tx.set(productRef, product.toCreateMap(uid))
                } else {
                    tx.update(productRef, product.toOwnerUpdateMap())
                }

                val costPayload = mutableMapOf<String, Any>(
                    "productId" to productId,
                    "costPriceInr" to costPriceInr.coerceIn(0L, 500000L),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (supplierNote.isNotBlank()) {
                    costPayload["supplierNote"] = supplierNote.trim().take(200)
                }
                tx.set(costRef, costPayload)

                if (!existingSnap.exists() || previousStock != newStock) {
                    val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    val movRef = db.collection("inventory_movements").document(movId)
                    tx.set(
                        movRef,
                        mapOf(
                            "productId" to productId,
                            "productName" to product.nameEn.trim().take(140),
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
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, productRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProductStockManually(product: Product, newStockCount: Long): Result<Unit> {
        val uid = requireUserId()
        if (product.id.isBlank()) return Result.failure(IllegalArgumentException("Invalid product ID"))
        val productRef = db.collection("products").document(product.id)
        val clampedStock = newStockCount.coerceIn(0L, 100000L)

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(productRef)
                if (!snap.exists()) error("Product no longer exists")
                val prevStock = snap.getLong("stockCount") ?: 0L
                if (prevStock == clampedStock) return@runTransaction

                tx.update(
                    productRef,
                    mapOf(
                        "stockCount" to clampedStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val movRef = db.collection("inventory_movements").document(movId)
                tx.set(
                    movRef,
                    mapOf(
                        "productId" to product.id,
                        "productName" to (snap.getString("nameEn") ?: product.nameEn).take(140),
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
            handleFirestoreError(e, OperationType.UPDATE, productRef.path)
            Result.failure(e)
        }
    }

    suspend fun setProductPublished(productId: String, isPublished: Boolean): Result<Unit> {
        requireUserId()
        val ref = db.collection("products").document(productId)
        return try {
            ref.update(
                mapOf(
                    "isPublished" to isPublished,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun hideAllDemoProducts(products: List<Product>): Result<Int> {
        requireUserId()
        val publishedDemos = products.filter { it.isDemo && it.isPublished && it.id.isNotBlank() }
        if (publishedDemos.isEmpty()) return Result.success(0)
        return try {
            val batch = db.batch()
            publishedDemos.forEach { demo ->
                val ref = db.collection("products").document(demo.id)
                batch.update(
                    ref,
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
        requireUserId()
        val prodRef = db.collection("products").document(productId)
        val costRef = db.collection("product_costs").document(productId)
        return try {
            val batch = db.batch()
            batch.delete(prodRef)
            batch.delete(costRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, prodRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDemoProducts(): Result<Int> {
        val uid = requireUserId()
        val entries = DemoCatalog.buildSeedEntries()
        return try {
            val batch = db.batch()
            entries.forEach { entry ->
                val prodRef = db.collection("products").document(entry.id)
                val costRef = db.collection("product_costs").document(entry.id)
                val movRef = db.collection("inventory_movements").document("mov_seed_${entry.id}")
                batch.set(prodRef, entry.product.toCreateMap(uid))
                batch.set(
                    costRef,
                    mapOf(
                        "productId" to entry.id,
                        "costPriceInr" to entry.sampleCostInr,
                        "supplierNote" to "Demo sample cost for testing reports",
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    movRef,
                    mapOf(
                        "productId" to entry.id,
                        "productName" to entry.product.nameEn.take(140),
                        "previousStock" to 0L,
                        "newStock" to entry.product.stockCount,
                        "delta" to entry.product.stockCount,
                        "reason" to "INITIAL_STOCK",
                        "referenceId" to entry.id,
                        "actorId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(entries.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "products")
            Result.failure(e)
        }
    }

    // Atomic customer order creation: validates live server prices & stock, deducts stock, and writes order + movements
    suspend fun placeCustomerOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        cartItems: List<CartItem>,
        deterministicOrderId: String? = null
    ): Result<CustomerOrder> {
        val uid = requireUserId()
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }
        val cleanPhone = customerPhone.filter { it.isDigit() }.takeLast(10)
        if (!cleanPhone.matches(Regex("^[6-9][0-9]{9}$"))) {
            return Result.failure(IllegalArgumentException("Valid 10-digit Indian mobile number required"))
        }

        val orderId = deterministicOrderId?.takeIf { it.isNotBlank() }
            ?: "ord_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val orderRef = db.collection("orders").document(orderId)
        val settingsRef = db.collection("store_settings").document("public_config")

        val requestedQtyByProduct: Map<String, Int> = cartItems
            .groupBy { it.product.id }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        return try {
            val createdOrder = db.runTransaction { tx ->
                // 1. All reads FIRST
                val existingOrderSnap = tx.get(orderRef)
                if (existingOrderSnap.exists()) {
                    error("Duplicate order submission detected. Please check My Orders.")
                }

                val settingsSnap = tx.get(settingsRef)
                val settings = if (settingsSnap.exists()) {
                    settingsSnap.toObject(StoreSettings::class.java) ?: StoreSettings()
                } else {
                    StoreSettings()
                }

                if (paymentMethod == PaymentMethodType.COD && !settings.codEnabled) {
                    error("Cash on Delivery is currently disabled by the shop owner.")
                }
                if (paymentMethod == PaymentMethodType.UPI_QR && !settings.isUpiConfiguredAndActive) {
                    error("UPI QR payment is awaiting verified merchant setup by the shop owner.")
                }

                val productSnaps = requestedQtyByProduct.keys.associateWith { prodId ->
                    tx.get(db.collection("products").document(prodId))
                }

                // 2. Validate stock & build server-verified line items
                val verifiedLineItems = mutableListOf<OrderLineItem>()
                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val snap = productSnaps[prodId]
                    if (snap == null || !snap.exists() || snap.getBoolean("isPublished") != true) {
                        error("One or more items are no longer available in the store.")
                    }
                    val availableStock = snap.getLong("stockCount") ?: 0L
                    val prodName = snap.getString("nameEn") ?: "Item"
                    if (totalReqQty <= 0 || totalReqQty.toLong() > availableStock) {
                        error("Insufficient stock for $prodName (Available: $availableStock).")
                    }
                }

                cartItems.forEach { cartItem ->
                    val snap = productSnaps.getValue(cartItem.product.id)
                    val unitPrice = snap.getLong("sellingPriceInr") ?: cartItem.product.sellingPriceInr
                    val nameEn = snap.getString("nameEn") ?: cartItem.product.nameEn
                    val category = snap.getString("category") ?: cartItem.product.category
                    val qty = cartItem.quantity.toLong()
                    verifiedLineItems.add(
                        OrderLineItem(
                            productId = cartItem.product.id,
                            productName = nameEn.take(120),
                            category = category,
                            quantity = qty,
                            unitPriceInr = unitPrice,
                            lineTotalInr = unitPrice * qty,
                            selectedSize = cartItem.selectedSize.take(40),
                            selectedColor = cartItem.selectedColor.take(40)
                        )
                    )
                }

                val subtotalInr = verifiedLineItems.sumOf { it.lineTotalInr }
                if (settings.minOrderAmountInr > 0 && subtotalInr < settings.minOrderAmountInr) {
                    error("Minimum order amount is ₹${settings.minOrderAmountInr}.")
                }

                val deliveryChargeInr = if (!settings.policiesConfirmed) {
                    0L
                } else if (settings.freeDeliveryThresholdInr > 0 && subtotalInr >= settings.freeDeliveryThresholdInr) {
                    0L
                } else {
                    settings.deliveryChargeInr.coerceAtLeast(0L)
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

                // 3. Writes: deduct stock on each product, write order, write inventory movements
                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val snap = productSnaps.getValue(prodId)
                    val prevStock = snap.getLong("stockCount") ?: 0L
                    val prevSold = snap.getLong("soldCount") ?: 0L
                    val newStock = prevStock - totalReqQty.toLong()
                    val prodName = (snap.getString("nameEn") ?: "Item").take(140)
                    val prodRef = db.collection("products").document(prodId)

                    tx.update(
                        prodRef,
                        mapOf(
                            "stockCount" to newStock,
                            "soldCount" to (prevSold + totalReqQty.toLong()),
                            "lastOrderId" to orderId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )

                    val movId = "mov_${orderId}_${prodId.takeLast(8)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        .take(120)
                    val movRef = db.collection("inventory_movements").document(movId)
                    tx.set(
                        movRef,
                        mapOf(
                            "productId" to prodId,
                            "productName" to prodName,
                            "previousStock" to prevStock,
                            "newStock" to newStock,
                            "delta" to (-totalReqQty.toLong()),
                            "reason" to "ONLINE_ORDER",
                            "referenceId" to orderId,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val itemMaps = verifiedLineItems.map { it.toMap() }
                val orderPayload = mutableMapOf<String, Any>(
                    "orderReference" to orderReference,
                    "customerId" to uid,
                    "customerName" to customerName.trim().take(100),
                    "customerPhone" to cleanPhone,
                    "deliveryAddress" to deliveryAddress.trim().take(400),
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
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (landmark.isNotBlank()) {
                    orderPayload["landmark"] = landmark.trim().take(200)
                }

                tx.set(orderRef, orderPayload)

                CustomerOrder(
                    id = orderId,
                    orderReference = orderReference,
                    customerId = uid,
                    customerName = customerName.trim().take(100),
                    customerPhone = cleanPhone,
                    deliveryAddress = deliveryAddress.trim().take(400),
                    landmark = landmark.trim().take(200),
                    items = itemMaps,
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
                    stockDeducted = true
                )
            }.await()
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    // Strictly scoped to customerId == uid to enforce zero cross-customer PII access
    fun observeCustomerOrders(customerId: String): Flow<List<CustomerOrder>> {
        val path = "orders"
        return db.collection("orders")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getOrderById(orderId: String): Result<CustomerOrder> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            val snap = docRef.get().await()
            val order = snap.toObject(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: error("Order not found")
            Result.success(order)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAllOrdersForOwner(): Flow<List<CustomerOrder>> {
        val path = "orders"
        return db.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // Owner updates order status, payment verification, invoice assignment, and atomic restock on cancellation
    suspend fun updateOrderByOwner(
        orderId: String,
        newOrderStatus: OrderStatus,
        newPaymentStatus: PaymentStatus,
        cancellationReason: String = "",
        generateInvoiceIfMissing: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val orderRef = db.collection("orders").document(orderId)

        return try {
            db.runTransaction { tx ->
                val orderSnap = tx.get(orderRef)
                if (!orderSnap.exists()) error("Order not found")
                val currentOrder = orderSnap.toObject(CustomerOrder::class.java) ?: error("Invalid order record")

                val shouldRestock = newOrderStatus == OrderStatus.CANCELLED && currentOrder.stockDeducted
                val lineItemsByProduct = if (shouldRestock) {
                    currentOrder.parsedItems()
                        .filter { it.productId.isNotBlank() }
                        .groupBy { it.productId }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }
                } else {
                    emptyMap()
                }

                // Read all affected products BEFORE any write
                val productSnaps = lineItemsByProduct.keys.associateWith { prodId ->
                    tx.get(db.collection("products").document(prodId))
                }

                var updatedStockDeducted = currentOrder.stockDeducted
                if (shouldRestock) {
                    lineItemsByProduct.forEach { (prodId, qtyToRestore) ->
                        val prodSnap = productSnaps[prodId]
                        if (prodSnap != null && prodSnap.exists()) {
                            val prevStock = prodSnap.getLong("stockCount") ?: 0L
                            val prevSold = prodSnap.getLong("soldCount") ?: 0L
                            val newStock = prevStock + qtyToRestore
                            val newSold = (prevSold - qtyToRestore).coerceAtLeast(0L)
                            val prodName = (prodSnap.getString("nameEn") ?: "Item").take(140)
                            val prodRef = db.collection("products").document(prodId)

                            tx.update(
                                prodRef,
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
                                db.collection("inventory_movements").document(movId),
                                mapOf(
                                    "productId" to prodId,
                                    "productName" to prodName,
                                    "previousStock" to prevStock,
                                    "newStock" to newStock,
                                    "delta" to qtyToRestore,
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

                val updatePayload = mutableMapOf<String, Any>(
                    "orderStatus" to newOrderStatus.name,
                    "paymentStatus" to newPaymentStatus.name,
                    "stockDeducted" to updatedStockDeducted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cancellationReason.isNotBlank()) {
                    updatePayload["cancellationReason"] = cancellationReason.trim().take(300)
                }
                if (invoiceNum.isNotBlank()) {
                    updatePayload["invoiceNumber"] = invoiceNum
                }

                tx.update(orderRef, updatePayload)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    // Record walk-in counter sale separately from online orders (preventing duplicate revenue counting)
    suspend fun recordOfflineSale(
        customerName: String,
        customerPhone: String,
        lineItems: List<OrderLineItem>,
        discountInr: Long,
        paymentMethod: String
    ): Result<OfflineSale> {
        val uid = requireUserId()
        if (lineItems.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one item"))
        val saleId = "sale_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val saleRef = db.collection("offline_sales").document(saleId)

        val catalogProductIds = lineItems.map { it.productId }.filter { it.isNotBlank() }.distinct()

        return try {
            val savedSale = db.runTransaction { tx ->
                // 1. All reads FIRST (products + private costs)
                val prodSnaps = catalogProductIds.associateWith { id ->
                    tx.get(db.collection("products").document(id))
                }
                val costSnaps = catalogProductIds.associateWith { id ->
                    tx.get(db.collection("product_costs").document(id))
                }

                // Validate stock for linked catalog products
                val qtyByProduct = lineItems
                    .filter { it.productId.isNotBlank() }
                    .groupBy { it.productId }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }

                qtyByProduct.forEach { (prodId, reqQty) ->
                    val pSnap = prodSnaps[prodId]
                    if (pSnap == null || !pSnap.exists()) {
                        error("Selected product no longer exists.")
                    }
                    val avail = pSnap.getLong("stockCount") ?: 0L
                    val name = pSnap.getString("nameEn") ?: "Item"
                    if (reqQty > avail) {
                        error("Only $avail units available in stock for $name.")
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

                // Deduct stock & log movements
                qtyByProduct.forEach { (prodId, reqQty) ->
                    val pSnap = prodSnaps.getValue(prodId)
                    val prevStock = pSnap.getLong("stockCount") ?: 0L
                    val prevSold = pSnap.getLong("soldCount") ?: 0L
                    val newStock = prevStock - reqQty
                    val pName = (pSnap.getString("nameEn") ?: "Item").take(140)
                    tx.update(
                        db.collection("products").document(prodId),
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
                        db.collection("inventory_movements").document(movId),
                        mapOf(
                            "productId" to prodId,
                            "productName" to pName,
                            "previousStock" to prevStock,
                            "newStock" to newStock,
                            "delta" to (-reqQty),
                            "reason" to "OFFLINE_SALE",
                            "referenceId" to saleId,
                            "actorId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val invoiceNumber = generateInvoiceNumber("POS")
                val itemSummary = lineItems.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(580)
                val itemMaps = lineItems.map { it.toMap() }
                val safeMethod = if (paymentMethod in setOf("CASH", "UPI", "CARD")) paymentMethod else "CASH"
                val epochDay = currentEpochDay()

                val payload = mutableMapOf<String, Any>(
                    "invoiceNumber" to invoiceNumber,
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
                )
                if (customerName.isNotBlank()) payload["customerName"] = customerName.trim().take(100)
                if (customerPhone.isNotBlank()) payload["customerPhone"] = customerPhone.trim().take(20)

                tx.set(saleRef, payload)

                OfflineSale(
                    id = saleId,
                    invoiceNumber = invoiceNumber,
                    customerName = customerName.trim().take(100),
                    customerPhone = customerPhone.trim().take(20),
                    items = itemMaps,
                    itemSummary = itemSummary,
                    subtotalInr = subtotalInr,
                    discountInr = safeDiscount,
                    totalPaidInr = totalPaidInr,
                    totalCostInr = totalCostInr,
                    costCoveredRevenueInr = costCoveredRevenueInr,
                    allCostsRecorded = allRecorded,
                    paymentMethod = safeMethod,
                    recordedBy = uid,
                    saleDateEpochDay = epochDay
                )
            }.await()
            Result.success(savedSale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, saleRef.path)
            Result.failure(e)
        }
    }

    fun observeOfflineSalesForOwner(): Flow<List<OfflineSale>> {
        val path = "offline_sales"
        return db.collection("offline_sales")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(OfflineSale::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeExpensesForOwner(): Flow<List<Expense>> {
        val path = "expenses"
        return db.collection("expenses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Expense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountInr: Long,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val expId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = db.collection("expenses").document(expId)
        val payload = mutableMapOf<String, Any>(
            "title" to title.trim().take(140),
            "category" to category.name,
            "amountInr" to amountInr.coerceIn(1L, 5000000L),
            "recordedBy" to uid,
            "expenseDateEpochDay" to currentEpochDay(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (notes.isNotBlank()) payload["notes"] = notes.trim().take(300)

        return try {
            ref.set(payload).await()
            Result.success(expId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("expenses").document(expenseId)
        return try {
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    fun observeInventoryMovementsForOwner(): Flow<List<InventoryMovement>> {
        val path = "inventory_movements"
        return db.collection("inventory_movements")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(InventoryMovement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }
}
