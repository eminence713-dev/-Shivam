package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomerOrder
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.InventoryMovement
import com.example.data.model.OfflineSale
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductCost
import com.example.data.model.StoreSettings
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

enum class ReportDateRange(val daysBack: Int?, val labelEn: String, val labelHi: String) {
    TODAY(0, "Today", "आज"),
    LAST_7_DAYS(6, "Last 7 Days", "पिछले 7 दिन"),
    LAST_30_DAYS(29, "Last 30 Days", "पिछले 30 दिन"),
    ALL_TIME(null, "All Time", "कुल समय")
}

data class SalesAnalyticsSummary(
    val onlineGrossRevenueInr: Long,
    val offlineGrossSubtotalInr: Long,
    val totalDiscountsInr: Long,
    val netCollectedAndActiveRevenueInr: Long,
    val cancelledOrdersCount: Int,
    val cancelledOrdersValueInr: Long,
    val refundedValueInr: Long,
    val totalExpensesInr: Long,
    val recordedCostOfGoodsSoldInr: Long,
    val costCoveredNetRevenueInr: Long,
    val trueGrossProfitOnCostedItemsInr: Long,
    val uncostedSoldUnitsCount: Long,
    val dateWiseTotals: List<Pair<String, Long>>,
    val productWiseSales: List<Triple<String, Long, Long>>,
    val categoryWiseSales: List<Pair<ProductCategory, Long>>
)

class AdminViewModel(
    private val repository: StoreRepository,
    private val langPrefs: LanguagePreferencesRepository
) : ViewModel() {

    val allProductsState: StateFlow<UiState<List<Product>>> = repository.observeAllProductsForOwner()
        .map<List<Product>, UiState<List<Product>>> { list ->
            UiState.Success(list.sortedByDescending { it.updatedAtMillis })
        }
        .catch { e ->
            Log.w(TAG, "Error observing owner products", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load products"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val productCosts: StateFlow<Map<String, ProductCost>> = repository.observeProductCostsForOwner()
        .catch { e ->
            Log.w(TAG, "Error observing product costs", e)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val allOrdersState: StateFlow<UiState<List<CustomerOrder>>> = repository.observeAllOrdersForOwner()
        .map<List<CustomerOrder>, UiState<List<CustomerOrder>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAtMillis })
        }
        .catch { e ->
            Log.w(TAG, "Error observing all orders", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load orders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val offlineSalesState: StateFlow<UiState<List<OfflineSale>>> = repository.observeOfflineSalesForOwner()
        .map<List<OfflineSale>, UiState<List<OfflineSale>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAtMillis })
        }
        .catch { e ->
            Log.w(TAG, "Error observing offline sales", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load counter sales"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val expensesState: StateFlow<UiState<List<Expense>>> = repository.observeExpensesForOwner()
        .map<List<Expense>, UiState<List<Expense>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAtMillis })
        }
        .catch { e ->
            Log.w(TAG, "Error observing expenses", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load expenses"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val movementsState: StateFlow<UiState<List<InventoryMovement>>> = repository.observeInventoryMovementsForOwner()
        .map<List<InventoryMovement>, UiState<List<InventoryMovement>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAtMillis }.take(80))
        }
        .catch { e ->
            Log.w(TAG, "Error observing stock movements", e)
            emit(UiState.Error(e.localizedMessage ?: "Failed to load stock movements"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _lastRecordedOfflineSale = MutableStateFlow<OfflineSale?>(null)
    val lastRecordedOfflineSale: StateFlow<OfflineSale?> = _lastRecordedOfflineSale.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearLastOfflineSale() {
        _lastRecordedOfflineSale.value = null
    }

    fun changeOwnerPassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            langPrefs.changeOwnerPassword(currentPassword, newPassword).fold(
                onSuccess = {
                    _statusBanner.value = "Owner password updated successfully!"
                    onSuccess()
                },
                onFailure = { e ->
                    _statusBanner.value = e.localizedMessage ?: "Could not update owner password."
                }
            )
        }
    }

    fun saveProduct(product: Product, costPriceInr: Long, supplierNote: String, onSaved: () -> Unit = {}) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val res = repository.saveProductWithCost(product, costPriceInr, supplierNote)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _statusBanner.value = "Product saved successfully."
                    onSaved()
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to save product." }
            )
        }
    }

    fun setPublished(productId: String, published: Boolean) {
        viewModelScope.launch {
            repository.setProductPublished(productId, published).onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to update visibility."
            }
        }
    }

    fun updateStockManually(product: Product, newStock: Long) {
        viewModelScope.launch {
            repository.updateProductStockManually(product, newStock).fold(
                onSuccess = { _statusBanner.value = "Stock updated for ${product.nameEn}." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Stock update failed." }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId).fold(
                onSuccess = { _statusBanner.value = "Product deleted." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Delete failed." }
            )
        }
    }

    fun hideAllDemoProducts(products: List<Product>) {
        viewModelScope.launch {
            repository.hideAllDemoProducts(products).fold(
                onSuccess = { count -> _statusBanner.value = "Hidden $count starter sample items." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Could not hide sample items." }
            )
        }
    }

    fun seedDemoProducts() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val res = repository.seedDemoProducts()
            _isBusy.value = false
            res.fold(
                onSuccess = { count -> _statusBanner.value = "Loaded $count starter shop items." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Could not load starter items." }
            )
        }
    }

    fun updateOrder(
        orderId: String,
        newStatus: OrderStatus,
        newPaymentStatus: PaymentStatus,
        cancellationReason: String = "",
        generateInvoice: Boolean = false
    ) {
        viewModelScope.launch {
            repository.updateOrderByOwner(
                orderId = orderId,
                newOrderStatus = newStatus,
                newPaymentStatus = newPaymentStatus,
                cancellationReason = cancellationReason,
                generateInvoiceIfMissing = generateInvoice
            ).fold(
                onSuccess = { _statusBanner.value = "Order updated to ${newStatus.labelEn}." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Order update failed." }
            )
        }
    }

    fun recordOfflineSale(
        customerName: String,
        customerPhone: String,
        items: List<OrderLineItem>,
        discountInr: Long,
        paymentMethod: String
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val res = repository.recordOfflineSale(customerName, customerPhone, items, discountInr, paymentMethod)
            _isBusy.value = false
            res.fold(
                onSuccess = { sale ->
                    _lastRecordedOfflineSale.value = sale
                    _statusBanner.value = "Counter bill ${sale.invoiceNumber} saved!"
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to record counter sale." }
            )
        }
    }

    fun addExpense(title: String, category: ExpenseCategory, amountInr: Long, notes: String, onSaved: () -> Unit = {}) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val res = repository.addExpense(title, category, amountInr, notes)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _statusBanner.value = "Expense recorded."
                    onSaved()
                },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to add expense." }
            )
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    fun saveSettings(settings: StoreSettings) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val res = repository.saveStoreSettings(settings)
            _isBusy.value = false
            res.fold(
                onSuccess = { _statusBanner.value = "Store settings & policies updated." },
                onFailure = { e -> _statusBanner.value = e.localizedMessage ?: "Failed to save settings." }
            )
        }
    }

    fun computeAnalytics(
        range: ReportDateRange,
        orders: List<CustomerOrder>,
        offlineSales: List<OfflineSale>,
        expenses: List<Expense>,
        costMap: Map<String, ProductCost>
    ): SalesAnalyticsSummary {
        val todayEpoch = StoreRepository.currentEpochDay()
        val minEpochDay = range.daysBack?.let { todayEpoch - it }

        fun inRangeMillis(millis: Long): Boolean {
            if (minEpochDay == null) return true
            val safeMillis = if (millis > 0L) millis else System.currentTimeMillis()
            return (safeMillis / 86_400_000L) >= minEpochDay
        }

        val filteredOrders = orders.filter { inRangeMillis(it.createdAtMillis) }
        val filteredOffline = offlineSales.filter {
            minEpochDay == null || it.saleDateEpochDay >= minEpochDay
        }
        val filteredExpenses = expenses.filter {
            minEpochDay == null || it.expenseDateEpochDay >= minEpochDay
        }

        val activeOrders = filteredOrders.filter { it.statusEnum() != OrderStatus.CANCELLED }
        val cancelledOrders = filteredOrders.filter { it.statusEnum() == OrderStatus.CANCELLED }
        val refundedOrders = filteredOrders.filter { it.paymentStatusEnum() == PaymentStatus.REFUNDED }

        val onlineSubtotal = activeOrders.sumOf { it.subtotalInr }
        val onlineDiscounts = activeOrders.sumOf { it.discountInr }
        val offlineSubtotal = filteredOffline.sumOf { it.subtotalInr }
        val offlineDiscounts = filteredOffline.sumOf { it.discountInr }
        val totalDiscounts = onlineDiscounts + offlineDiscounts

        val netRevenue = activeOrders.sumOf { (it.subtotalInr - it.discountInr).coerceAtLeast(0L) } +
            filteredOffline.sumOf { it.totalPaidInr }

        var cogsInr = 0L
        var costCoveredRevInr = 0L
        var uncostedUnits = 0L

        val productAgg = mutableMapOf<String, Pair<Long, Long>>()
        val categoryAgg = mutableMapOf<ProductCategory, Long>()
        val dayFmt = SimpleDateFormat("dd MMM", Locale.US)
        val dateAgg = linkedMapOf<String, Long>()

        activeOrders.forEach { order ->
            val dayKey = dayFmt.format(order.createdAtDate())
            dateAgg[dayKey] = (dateAgg[dayKey] ?: 0L) + (order.subtotalInr - order.discountInr).coerceAtLeast(0L)

            order.parsedItems().forEach { line ->
                val prev = productAgg[line.productName] ?: (0L to 0L)
                productAgg[line.productName] = (prev.first + line.quantity) to (prev.second + line.lineTotalInr)
                val cat = ProductCategory.fromKey(line.category)
                categoryAgg[cat] = (categoryAgg[cat] ?: 0L) + line.lineTotalInr

                val unitCost = costMap[line.productId]?.costPriceInr ?: 0L
                if (unitCost > 0L) {
                    cogsInr += unitCost * line.quantity
                    costCoveredRevInr += line.lineTotalInr
                } else {
                    uncostedUnits += line.quantity
                }
            }
        }

        filteredOffline.forEach { sale ->
            val dayKey = dayFmt.format(sale.createdAtDate())
            dateAgg[dayKey] = (dateAgg[dayKey] ?: 0L) + sale.totalPaidInr

            cogsInr += sale.totalCostInr
            costCoveredRevInr += sale.costCoveredRevenueInr

            sale.parsedItems().forEach { line ->
                val prev = productAgg[line.productName] ?: (0L to 0L)
                productAgg[line.productName] = (prev.first + line.quantity) to (prev.second + line.lineTotalInr)
                val cat = ProductCategory.fromKey(line.category)
                categoryAgg[cat] = (categoryAgg[cat] ?: 0L) + line.lineTotalInr

                val unitCost = if (line.productId.isNotBlank()) costMap[line.productId]?.costPriceInr ?: 0L else 0L
                if (!sale.allCostsRecorded && unitCost <= 0L) {
                    uncostedUnits += line.quantity
                }
            }
        }

        val trueGrossProfit = costCoveredRevInr - cogsInr

        return SalesAnalyticsSummary(
            onlineGrossRevenueInr = onlineSubtotal,
            offlineGrossSubtotalInr = offlineSubtotal,
            totalDiscountsInr = totalDiscounts,
            netCollectedAndActiveRevenueInr = netRevenue,
            cancelledOrdersCount = cancelledOrders.size,
            cancelledOrdersValueInr = cancelledOrders.sumOf { it.totalPayableInr },
            refundedValueInr = refundedOrders.sumOf { it.totalPayableInr },
            totalExpensesInr = filteredExpenses.sumOf { it.amountInr },
            recordedCostOfGoodsSoldInr = cogsInr,
            costCoveredNetRevenueInr = costCoveredRevInr,
            trueGrossProfitOnCostedItemsInr = trueGrossProfit,
            uncostedSoldUnitsCount = uncostedUnits,
            dateWiseTotals = dateAgg.entries.map { it.key to it.value },
            productWiseSales = productAgg.entries
                .sortedByDescending { it.value.second }
                .map { Triple(it.key, it.value.first, it.value.second) },
            categoryWiseSales = ProductCategory.entries.map { cat ->
                cat to (categoryAgg[cat] ?: 0L)
            }.sortedByDescending { it.second }
        )
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
