package com.example.data.repository

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Safe in-memory fallback store used strictly when an APK is built in CI passthrough mode
 * without `google-services.json` (`FirebaseApp.getApps(context).isEmpty()`), ensuring the app
 * never crashes and remains 100% usable offline/via WhatsApp.
 */
object LocalFallbackStore {
    private val _settings = MutableStateFlow(StoreSettings())
    private val _products = MutableStateFlow(
        DemoCatalog.buildSeedEntries().map { it.product.copy(id = it.id) }
    )
    private val _costs = MutableStateFlow(
        DemoCatalog.buildSeedEntries().associate { entry ->
            entry.id to ProductCost(
                id = entry.id,
                productId = entry.id,
                costPriceInr = entry.sampleCostInr,
                supplierNote = "Starter sample cost"
            )
        }
    )
    private val _orders = MutableStateFlow<List<CustomerOrder>>(emptyList())
    private val _offlineSales = MutableStateFlow<List<OfflineSale>>(emptyList())
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _movements = MutableStateFlow<List<InventoryMovement>>(emptyList())

    fun observeSettings(): Flow<StoreSettings> = _settings.asStateFlow()
    fun observePublishedProducts(): Flow<List<Product>> = _products.map { list -> list.filter { it.isPublished } }
    fun observeAllProducts(): Flow<List<Product>> = _products.asStateFlow()
    fun observeCosts(): Flow<Map<String, ProductCost>> = _costs.asStateFlow()
    fun observeOrders(): Flow<List<CustomerOrder>> = _orders.asStateFlow()
    fun observeOfflineSales(): Flow<List<OfflineSale>> = _offlineSales.asStateFlow()
    fun observeExpenses(): Flow<List<Expense>> = _expenses.asStateFlow()
    fun observeMovements(): Flow<List<InventoryMovement>> = _movements.asStateFlow()

    fun saveSettings(settings: StoreSettings): Result<Unit> {
        _settings.value = settings.copy(updatedAtMillis = System.currentTimeMillis())
        return Result.success(Unit)
    }

    fun saveProductWithCost(product: Product, costPriceInr: Long, supplierNote: String): Result<String> {
        val now = System.currentTimeMillis()
        val id = product.id.ifBlank { "prod_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val updated = product.copy(
            id = id,
            nameEn = product.nameEn.trim().take(140),
            category = ProductCategory.fromKey(product.category).name,
            updatedAtMillis = now
        )
        _products.update { current ->
            if (current.any { it.id == id }) current.map { if (it.id == id) updated else it }
            else listOf(updated) + current
        }
        _costs.update { current ->
            current + (id to ProductCost(id = id, productId = id, costPriceInr = costPriceInr, supplierNote = supplierNote, updatedAtMillis = now))
        }
        return Result.success(id)
    }

    fun updateStockManually(product: Product, newStock: Long): Result<Unit> {
        val clamped = newStock.coerceIn(0L, 100_000L)
        _products.update { list ->
            list.map { if (it.id == product.id) it.copy(stockCount = clamped, updatedAtMillis = System.currentTimeMillis()) else it }
        }
        return Result.success(Unit)
    }

    fun setPublished(productId: String, published: Boolean): Result<Unit> {
        _products.update { list ->
            list.map { if (it.id == productId) it.copy(isPublished = published, updatedAtMillis = System.currentTimeMillis()) else it }
        }
        return Result.success(Unit)
    }

    fun hideAllDemoProducts(products: List<Product>): Result<Int> {
        val ids = products.filter { it.isDemo && it.isPublished }.map { it.id }.toSet()
        _products.update { list ->
            list.map { if (it.id in ids) it.copy(isPublished = false) else it }
        }
        return Result.success(ids.size)
    }

    fun deleteProduct(productId: String): Result<Unit> {
        _products.update { list -> list.filterNot { it.id == productId } }
        _costs.update { map -> map - productId }
        return Result.success(Unit)
    }

    fun seedDemoProducts(): Result<Int> {
        val entries = DemoCatalog.buildSeedEntries()
        val existingIds = _products.value.map { it.id }.toSet()
        val toAdd = entries.filterNot { it.id in existingIds }.map { it.product.copy(id = it.id) }
        _products.update { current ->
            current.map { p -> if (p.isDemo) p.copy(isPublished = true) else p } + toAdd
        }
        return Result.success(entries.size)
    }

    fun placeCustomerOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        cartItems: List<CartItem>,
        orderId: String
    ): Result<CustomerOrder> {
        val now = System.currentTimeMillis()
        val settings = _settings.value
        val byId = _products.value.associateBy { it.id }
        val lines = cartItems.map { item ->
            val p = byId[item.product.id] ?: item.product
            val qty = item.quantity.toLong()
            OrderLineItem(
                productId = p.id,
                productName = p.nameEn.take(120),
                category = p.category,
                quantity = qty,
                unitPriceInr = p.sellingPriceInr,
                lineTotalInr = p.sellingPriceInr * qty,
                selectedSize = item.selectedSize.take(40),
                selectedColor = item.selectedColor.take(40)
            )
        }
        val subtotal = lines.sumOf { it.lineTotalInr }
        val delivery = when {
            !settings.policiesConfirmed -> 0L
            settings.freeDeliveryThresholdInr > 0 && subtotal >= settings.freeDeliveryThresholdInr -> 0L
            else -> settings.deliveryChargeInr.coerceAtLeast(0L)
        }
        val order = CustomerOrder(
            id = orderId,
            orderReference = StoreRepository.generateOrderReference(),
            customerId = "guest",
            customerName = customerName.trim().take(100),
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress.trim().take(400),
            landmark = landmark.trim().take(200),
            lineItems = lines,
            itemSummary = lines.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(580),
            totalQuantity = lines.sumOf { it.quantity },
            subtotalInr = subtotal,
            deliveryChargeInr = delivery,
            discountInr = 0L,
            totalPayableInr = subtotal + delivery,
            deliveryFeeConfirmed = settings.policiesConfirmed,
            paymentMethod = paymentMethod.name,
            paymentStatus = PaymentStatus.PENDING.name,
            orderStatus = OrderStatus.PENDING.name,
            stockDeducted = true,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val reqByProd = cartItems.groupBy { it.product.id }.mapValues { (_, l) -> l.sumOf { it.quantity }.toLong() }
        _products.update { list ->
            list.map { p ->
                val deduct = reqByProd[p.id] ?: 0L
                if (deduct > 0L) p.copy(stockCount = (p.stockCount - deduct).coerceAtLeast(0L), soldCount = p.soldCount + deduct)
                else p
            }
        }
        _orders.update { listOf(order) + it }
        return Result.success(order)
    }

    fun updateOrderByOwner(
        orderId: String,
        newStatus: OrderStatus,
        newPaymentStatus: PaymentStatus,
        cancellationReason: String,
        generateInvoice: Boolean
    ): Result<Unit> {
        _orders.update { list ->
            list.map { ord ->
                if (ord.id != orderId) ord
                else {
                    val inv = ord.invoiceNumber.ifBlank {
                        if (generateInvoice || newStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.DELIVERED)) {
                            StoreRepository.generateInvoiceNumber("INV")
                        } else ""
                    }
                    ord.copy(
                        orderStatus = newStatus.name,
                        paymentStatus = newPaymentStatus.name,
                        cancellationReason = cancellationReason.ifBlank { ord.cancellationReason },
                        invoiceNumber = inv,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                }
            }
        }
        return Result.success(Unit)
    }

    fun recordOfflineSale(
        customerName: String,
        customerPhone: String,
        lineItems: List<OrderLineItem>,
        discountInr: Long,
        paymentMethod: String
    ): Result<OfflineSale> {
        val subtotal = lineItems.sumOf { it.lineTotalInr }.coerceAtLeast(1L)
        val disc = discountInr.coerceIn(0L, subtotal)
        val paid = (subtotal - disc).coerceAtLeast(0L)
        val sale = OfflineSale(
            id = "sale_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            invoiceNumber = StoreRepository.generateInvoiceNumber("POS"),
            customerName = customerName.trim().take(100),
            customerPhone = customerPhone.trim().take(20),
            lineItems = lineItems,
            itemSummary = lineItems.joinToString(", ") { "${it.productName} x${it.quantity}" }.take(580),
            subtotalInr = subtotal,
            discountInr = disc,
            totalPaidInr = paid,
            paymentMethod = paymentMethod,
            saleDateEpochDay = StoreRepository.currentEpochDay(),
            createdAtMillis = System.currentTimeMillis()
        )
        _offlineSales.update { listOf(sale) + it }
        return Result.success(sale)
    }

    fun addExpense(title: String, category: ExpenseCategory, amountInr: Long, notes: String): Result<String> {
        val id = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val exp = Expense(
            id = id,
            title = title.trim().take(140),
            category = category.name,
            amountInr = amountInr.coerceAtLeast(1L),
            notes = notes.trim().take(300),
            expenseDateEpochDay = StoreRepository.currentEpochDay(),
            createdAtMillis = System.currentTimeMillis()
        )
        _expenses.update { listOf(exp) + it }
        return Result.success(id)
    }

    fun deleteExpense(expenseId: String): Result<Unit> {
        _expenses.update { list -> list.filterNot { it.id == expenseId } }
        return Result.success(Unit)
    }
}
