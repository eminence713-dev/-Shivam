package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.CustomerOrderEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryMovementEntity
import com.example.data.local.OfflineSaleEntity
import com.example.data.local.ProductCostEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ShivamMartDatabase
import com.example.data.local.StoreCodec
import com.example.data.local.StoreSettingsEntity
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class StoreRepository(
    private val database: ShivamMartDatabase
) {
    private val dao = database.storeDao()

    constructor(context: Context) : this(ShivamMartDatabase.getInstance(context.applicationContext))

    companion object {
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

    suspend fun ensureInitialCatalogSeeded(prefs: LanguagePreferencesRepository? = null) {
        val alreadySeeded = prefs?.isInitialCatalogSeeded() ?: false
        if (!alreadySeeded && dao.countProducts() == 0) {
            seedDemoProducts()
            prefs?.markInitialCatalogSeeded()
        } else if (!alreadySeeded) {
            prefs?.markInitialCatalogSeeded()
        }
    }

    fun observeStoreSettings(): Flow<StoreSettings> =
        dao.observeSettings().map { entity -> entity?.toModel() ?: StoreSettings() }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> = try {
        dao.upsertSettings(StoreSettingsEntity.fromModel(settings))
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observePublishedProducts(): Flow<List<Product>> =
        dao.observePublishedProducts().map { list -> list.map { it.toModel() } }

    fun observeAllProductsForOwner(): Flow<List<Product>> =
        dao.observeAllProducts().map { list -> list.map { it.toModel() } }

    fun observeProductCostsForOwner(): Flow<Map<String, ProductCost>> =
        dao.observeProductCosts().map { list ->
            list.map { it.toModel() }.associateBy { it.productId }
        }

    suspend fun saveProductWithCost(
        product: Product,
        costPriceInr: Long,
        supplierNote: String = ""
    ): Result<String> {
        val isNew = product.id.isBlank()
        val productId = if (isNew) {
            "prod_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        } else {
            product.id
        }
        val now = System.currentTimeMillis()

        return try {
            database.withTransaction {
                val existing = dao.getProductById(productId)
                val previousStock = existing?.stockCount ?: 0L
                val newStock = product.stockCount.coerceIn(0L, 100000L)
                val preservedCreatedAt = existing?.createdAtMillis ?: now

                val entity = ProductEntity.fromModel(
                    p = product.copy(createdAtMillis = preservedCreatedAt),
                    idOverride = productId,
                    nowMillis = now
                )
                dao.upsertProduct(entity)

                dao.upsertProductCost(
                    ProductCostEntity(
                        productId = productId,
                        costPriceInr = costPriceInr.coerceIn(0L, 500000L),
                        supplierNote = supplierNote.trim().take(200),
                        updatedBy = "owner",
                        updatedAtMillis = now
                    )
                )

                if (existing == null || previousStock != newStock) {
                    val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                    dao.insertInventoryMovement(
                        InventoryMovementEntity(
                            id = movId,
                            productId = productId,
                            productName = product.nameEn.trim().take(140),
                            previousStock = previousStock,
                            newStock = newStock,
                            delta = newStock - previousStock,
                            reason = if (existing == null) "INITIAL_STOCK" else "MANUAL_UPDATE",
                            referenceId = productId,
                            actorId = "owner",
                            createdAtMillis = now
                        )
                    )
                }
            }
            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProductStockManually(product: Product, newStockCount: Long): Result<Unit> {
        if (product.id.isBlank()) return Result.failure(IllegalArgumentException("Invalid product ID"))
        val clampedStock = newStockCount.coerceIn(0L, 100000L)
        val now = System.currentTimeMillis()

        return try {
            database.withTransaction {
                val existing = dao.getProductById(product.id) ?: error("Product no longer exists")
                val prevStock = existing.stockCount
                if (prevStock == clampedStock) return@withTransaction

                dao.upsertProduct(existing.copy(stockCount = clampedStock, updatedAtMillis = now))
                val movId = "mov_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                dao.insertInventoryMovement(
                    InventoryMovementEntity(
                        id = movId,
                        productId = product.id,
                        productName = existing.nameEn.take(140),
                        previousStock = prevStock,
                        newStock = clampedStock,
                        delta = clampedStock - prevStock,
                        reason = "MANUAL_UPDATE",
                        referenceId = product.id,
                        actorId = "owner",
                        createdAtMillis = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setProductPublished(productId: String, isPublished: Boolean): Result<Unit> = try {
        database.withTransaction {
            val existing = dao.getProductById(productId) ?: error("Product not found")
            dao.upsertProduct(existing.copy(isPublished = isPublished, updatedAtMillis = System.currentTimeMillis()))
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun hideAllDemoProducts(products: List<Product>): Result<Int> {
        val publishedDemos = products.filter { it.isDemo && it.isPublished && it.id.isNotBlank() }
        if (publishedDemos.isEmpty()) return Result.success(0)
        val now = System.currentTimeMillis()
        return try {
            database.withTransaction {
                publishedDemos.forEach { demo ->
                    val existing = dao.getProductById(demo.id)
                    if (existing != null) {
                        dao.upsertProduct(existing.copy(isPublished = false, updatedAtMillis = now))
                    }
                }
            }
            Result.success(publishedDemos.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = try {
        database.withTransaction {
            dao.deleteProductById(productId)
            dao.deleteProductCostById(productId)
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun seedDemoProducts(): Result<Int> {
        val entries = DemoCatalog.buildSeedEntries()
        val now = System.currentTimeMillis()
        return try {
            database.withTransaction {
                val productEntities = entries.map { entry ->
                    ProductEntity.fromModel(entry.product, idOverride = entry.id, nowMillis = entry.product.updatedAtMillis)
                }
                val costEntities = entries.map { entry ->
                    ProductCostEntity(
                        productId = entry.id,
                        costPriceInr = entry.sampleCostInr,
                        supplierNote = "Starter sample cost",
                        updatedBy = "owner",
                        updatedAtMillis = now
                    )
                }
                val movEntities = entries.map { entry ->
                    InventoryMovementEntity(
                        id = "mov_seed_${entry.id}",
                        productId = entry.id,
                        productName = entry.product.nameEn.take(140),
                        previousStock = 0L,
                        newStock = entry.product.stockCount,
                        delta = entry.product.stockCount,
                        reason = "INITIAL_STOCK",
                        referenceId = entry.id,
                        actorId = "owner",
                        createdAtMillis = now
                    )
                }
                dao.upsertProducts(productEntities)
                dao.upsertProductCosts(costEntities)
                dao.insertInventoryMovements(movEntities)
            }
            Result.success(entries.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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

        val orderId = deterministicOrderId?.takeIf { it.isNotBlank() }
            ?: "ord_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val now = System.currentTimeMillis()

        val requestedQtyByProduct: Map<String, Int> = cartItems
            .groupBy { it.product.id }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        return try {
            val createdOrder = database.withTransaction {
                if (dao.getOrderById(orderId) != null) {
                    error("Duplicate order submission detected. Please check My Orders.")
                }

                val settings = dao.getSettingsOnce()?.toModel() ?: StoreSettings()
                if (paymentMethod == PaymentMethodType.COD && !settings.codEnabled) {
                    error("Cash on Delivery is currently disabled by the shop owner.")
                }
                if (paymentMethod == PaymentMethodType.UPI_QR && !settings.isUpiConfiguredAndActive) {
                    error("UPI QR payment is awaiting verified merchant setup by the shop owner.")
                }

                val productEntities = requestedQtyByProduct.keys.associateWith { prodId ->
                    dao.getProductById(prodId)
                }

                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val entity = productEntities[prodId]
                    if (entity == null || !entity.isPublished) {
                        error("One or more items are no longer available in the store.")
                    }
                    if (totalReqQty <= 0 || totalReqQty.toLong() > entity.stockCount) {
                        error("Insufficient stock for ${entity.nameEn} (Available: ${entity.stockCount}).")
                    }
                }

                val verifiedLineItems = cartItems.map { cartItem ->
                    val entity = productEntities.getValue(cartItem.product.id)!!
                    val unitPrice = entity.sellingPriceInr
                    val qty = cartItem.quantity.toLong()
                    OrderLineItem(
                        productId = cartItem.product.id,
                        productName = entity.nameEn.take(120),
                        category = entity.category,
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

                requestedQtyByProduct.forEach { (prodId, totalReqQty) ->
                    val entity = productEntities.getValue(prodId)!!
                    val prevStock = entity.stockCount
                    val newStock = prevStock - totalReqQty.toLong()
                    dao.upsertProduct(
                        entity.copy(
                            stockCount = newStock,
                            soldCount = entity.soldCount + totalReqQty.toLong(),
                            lastOrderId = orderId,
                            updatedAtMillis = now
                        )
                    )
                    val movId = "mov_${orderId}_${prodId.takeLast(8)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        .take(120)
                    dao.insertInventoryMovement(
                        InventoryMovementEntity(
                            id = movId,
                            productId = prodId,
                            productName = entity.nameEn.take(140),
                            previousStock = prevStock,
                            newStock = newStock,
                            delta = -totalReqQty.toLong(),
                            reason = "ONLINE_ORDER",
                            referenceId = orderId,
                            actorId = "customer",
                            createdAtMillis = now
                        )
                    )
                }

                val orderEntity = CustomerOrderEntity(
                    id = orderId,
                    orderReference = orderReference,
                    customerId = "local_customer",
                    customerName = customerName.trim().take(100),
                    customerPhone = cleanPhone,
                    deliveryAddress = deliveryAddress.trim().take(400),
                    landmark = landmark.trim().take(200),
                    itemsEncoded = StoreCodec.encodeLineItems(verifiedLineItems),
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
                    cancellationReason = "",
                    invoiceNumber = "",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                dao.upsertOrder(orderEntity)
                orderEntity.toModel()
            }
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCustomerOrders(): Flow<List<CustomerOrder>> =
        dao.observeAllOrders().map { list -> list.map { it.toModel() } }

    fun observeAllOrdersForOwner(): Flow<List<CustomerOrder>> =
        dao.observeAllOrders().map { list -> list.map { it.toModel() } }

    suspend fun updateOrderByOwner(
        orderId: String,
        newOrderStatus: OrderStatus,
        newPaymentStatus: PaymentStatus,
        cancellationReason: String = "",
        generateInvoiceIfMissing: Boolean = false
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        return try {
            database.withTransaction {
                val currentEntity = dao.getOrderById(orderId) ?: error("Order not found")
                val currentOrder = currentEntity.toModel()

                val shouldRestock = newOrderStatus == OrderStatus.CANCELLED && currentOrder.stockDeducted
                var updatedStockDeducted = currentOrder.stockDeducted

                if (shouldRestock) {
                    val lineItemsByProduct = currentOrder.parsedItems()
                        .filter { it.productId.isNotBlank() }
                        .groupBy { it.productId }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }

                    lineItemsByProduct.forEach { (prodId, qtyToRestore) ->
                        val prodEntity = dao.getProductById(prodId)
                        if (prodEntity != null) {
                            val prevStock = prodEntity.stockCount
                            val newStock = prevStock + qtyToRestore
                            val newSold = (prodEntity.soldCount - qtyToRestore).coerceAtLeast(0L)
                            dao.upsertProduct(
                                prodEntity.copy(
                                    stockCount = newStock,
                                    soldCount = newSold,
                                    updatedAtMillis = now
                                )
                            )
                            val movId = "mov_restock_${orderId}_${prodId.takeLast(6)}_${UUID.randomUUID().toString().take(4)}"
                                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                .take(120)
                            dao.insertInventoryMovement(
                                InventoryMovementEntity(
                                    id = movId,
                                    productId = prodId,
                                    productName = prodEntity.nameEn.take(140),
                                    previousStock = prevStock,
                                    newStock = newStock,
                                    delta = qtyToRestore,
                                    reason = "ORDER_CANCELLED_RESTOCK",
                                    referenceId = orderId,
                                    actorId = "owner",
                                    createdAtMillis = now
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

                dao.upsertOrder(
                    currentEntity.copy(
                        orderStatus = newOrderStatus.name,
                        paymentStatus = newPaymentStatus.name,
                        stockDeducted = updatedStockDeducted,
                        cancellationReason = if (cancellationReason.isNotBlank()) cancellationReason.trim().take(300) else currentEntity.cancellationReason,
                        invoiceNumber = invoiceNum,
                        updatedAtMillis = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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
        if (lineItems.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one item"))
        val saleId = "sale_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()
        val catalogProductIds = lineItems.map { it.productId }.filter { it.isNotBlank() }.distinct()

        return try {
            val savedSale = database.withTransaction {
                val prodEntities = catalogProductIds.associateWith { id -> dao.getProductById(id) }
                val costEntities = catalogProductIds.associateWith { id -> dao.getProductCostById(id) }

                val qtyByProduct = lineItems
                    .filter { it.productId.isNotBlank() }
                    .groupBy { it.productId }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }

                qtyByProduct.forEach { (prodId, reqQty) ->
                    val pEntity = prodEntities[prodId] ?: error("Selected product no longer exists.")
                    if (reqQty > pEntity.stockCount) {
                        error("Only ${pEntity.stockCount} units available in stock for ${pEntity.nameEn}.")
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
                        costEntities[line.productId]?.costPriceInr ?: 0L
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
                    val pEntity = prodEntities.getValue(prodId)!!
                    val prevStock = pEntity.stockCount
                    val newStock = prevStock - reqQty
                    dao.upsertProduct(
                        pEntity.copy(
                            stockCount = newStock,
                            soldCount = pEntity.soldCount + reqQty,
                            updatedAtMillis = now
                        )
                    )
                    val movId = "mov_${saleId}_${prodId.takeLast(8)}"
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                        .take(120)
                    dao.insertInventoryMovement(
                        InventoryMovementEntity(
                            id = movId,
                            productId = prodId,
                            productName = pEntity.nameEn.take(140),
                            previousStock = prevStock,
                            newStock = newStock,
                            delta = -reqQty,
                            reason = "OFFLINE_SALE",
                            referenceId = saleId,
                            actorId = "owner",
                            createdAtMillis = now
                        )
                    )
                }

                val invoiceNumber = generateInvoiceNumber("POS")
                val itemSummary = lineItems.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(580)
                val safeMethod = if (paymentMethod in setOf("CASH", "UPI", "CARD")) paymentMethod else "CASH"
                val epochDay = currentEpochDay()

                val saleEntity = OfflineSaleEntity(
                    id = saleId,
                    invoiceNumber = invoiceNumber,
                    customerName = customerName.trim().take(100),
                    customerPhone = customerPhone.trim().take(20),
                    itemsEncoded = StoreCodec.encodeLineItems(lineItems),
                    itemSummary = itemSummary,
                    subtotalInr = subtotalInr,
                    discountInr = safeDiscount,
                    totalPaidInr = totalPaidInr,
                    totalCostInr = totalCostInr,
                    costCoveredRevenueInr = costCoveredRevenueInr,
                    allCostsRecorded = allRecorded,
                    paymentMethod = safeMethod,
                    recordedBy = "owner",
                    saleDateEpochDay = epochDay,
                    createdAtMillis = now
                )
                dao.insertOfflineSale(saleEntity)
                saleEntity.toModel()
            }
            Result.success(savedSale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeOfflineSalesForOwner(): Flow<List<OfflineSale>> =
        dao.observeOfflineSales().map { list -> list.map { it.toModel() } }

    fun observeExpensesForOwner(): Flow<List<Expense>> =
        dao.observeExpenses().map { list -> list.map { it.toModel() } }

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountInr: Long,
        notes: String
    ): Result<String> {
        val expId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()
        return try {
            dao.insertExpense(
                ExpenseEntity(
                    id = expId,
                    title = title.trim().take(140),
                    category = category.name,
                    amountInr = amountInr.coerceIn(1L, 5000000L),
                    notes = notes.trim().take(300),
                    recordedBy = "owner",
                    expenseDateEpochDay = currentEpochDay(),
                    createdAtMillis = now
                )
            )
            Result.success(expId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = try {
        dao.deleteExpenseById(expenseId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeInventoryMovementsForOwner(): Flow<List<InventoryMovement>> =
        dao.observeInventoryMovements().map { list -> list.map { it.toModel() } }
}
