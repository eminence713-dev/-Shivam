package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.DemoCatalog
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption(val labelEn: String, val labelHi: String) {
    NEWEST("Newest First", "नए उत्पाद पहले"),
    DISCOUNT_HIGH_LOW("Discount: High to Low", "सबसे ज़्यादा छूट (%)"),
    PRICE_LOW_HIGH("Price: Low to High", "कीमत: कम से ज़्यादा"),
    PRICE_HIGH_LOW("Price: High to Low", "कीमत: ज़्यादा से कम");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn
}

class StorefrontViewModel(
    private val repository: StoreRepository,
    private val langPrefs: LanguagePreferencesRepository,
    private val currentUserId: String,
    private val isAuthorizedOwner: Boolean
) : ViewModel() {

    val language: StateFlow<AppLanguage> = langPrefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.HINDI)

    val wishlistIds: StateFlow<Set<String>> = langPrefs.wishlistIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val storeSettings: StateFlow<StoreSettings> = repository.observeStoreSettings()
        .catch { e ->
            Log.w(TAG, "Error observing store settings", e)
            emit(StoreSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreSettings())

    val publishedProductsState: StateFlow<UiState<List<Product>>> = combine(
        repository.observePublishedProducts(),
        repository.observeHasCloudStoreConfig()
    ) { cloudProducts, hasCloudConfig ->
        val displayList = if (cloudProducts.isNotEmpty() || hasCloudConfig) {
            cloudProducts
        } else {
            // Day-0 preview catalog until shop owner signs in once to seed/publish cloud products
            DemoCatalog.buildSeedEntries().map { it.product.copy(id = it.id) }
        }
        val sorted = displayList.sortedWith(
            compareBy<Product> { it.isDemo }
                .thenByDescending { maxOf(it.updatedAtMillis, it.createdAtMillis) }
        )
        UiState.Success(sorted) as UiState<List<Product>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing published products, showing starter catalog", e)
            val fallback = DemoCatalog.buildSeedEntries().map { it.product.copy(id = it.id) }
            emit(UiState.Success(fallback))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val cloudOrdersFlow = if (currentUserId.isNotBlank() && currentUserId != "guest") {
        repository.observeCustomerOrders(currentUserId)
            .catch { e ->
                Log.w(TAG, "Error observing cloud customer orders", e)
                emit(emptyList())
            }
    } else {
        flowOf(emptyList())
    }

    val customerOrdersState: StateFlow<UiState<List<CustomerOrder>>> = combine(
        langPrefs.localCustomerOrdersFlow,
        cloudOrdersFlow
    ) { localOrders, cloudOrders ->
        val byId = LinkedHashMap<String, CustomerOrder>()
        // Cloud orders take precedence when signed in so live owner status updates appear automatically
        cloudOrders.forEach { ord -> byId[ord.id] = ord }
        localOrders.forEach { ord ->
            if (!byId.containsKey(ord.id)) {
                byId[ord.id] = ord
            }
        }
        UiState.Success(byId.values.sortedByDescending { it.createdAtMillis }) as UiState<List<CustomerOrder>>
    }
        .catch { e ->
            Log.w(TAG, "Error combining customer orders", e)
            emit(UiState.Success(emptyList()))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val pendingOwnerOrdersCount: StateFlow<Int> = (
        if (isAuthorizedOwner) {
            repository.observeAllOrdersForOwner()
                .map { orders -> orders.count { it.statusEnum() == OrderStatus.PENDING } }
                .catch { emit(0) }
        } else {
            emptyFlow()
        }
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ProductCategory?>(null)
    val selectedCategory: StateFlow<ProductCategory?> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _showWishlistOnly = MutableStateFlow(false)
    val showWishlistOnly: StateFlow<Boolean> = _showWishlistOnly.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    private val _lastConfirmedOrder = MutableStateFlow<CustomerOrder?>(null)
    val lastConfirmedOrder: StateFlow<CustomerOrder?> = _lastConfirmedOrder.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var activeCheckoutIdempotencyId: String? = null

    init {
        if (isAuthorizedOwner) {
            viewModelScope.launch {
                runCatching {
                    repository.ensureInitialCatalogSeededForOwner()
                }.onFailure { e ->
                    Log.w(TAG, "Error checking/seeding initial cloud catalog", e)
                }
            }
        }
    }

    fun toggleLanguage() {
        val next = if (language.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        viewModelScope.launch { langPrefs.setLanguage(next) }
    }

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            val added = langPrefs.toggleWishlist(product.id)
            val lang = language.value
            _toastMessage.value = if (added) {
                if (lang == AppLanguage.HINDI) "विशलिस्ट में जोड़ा गया ♥" else "Added to Wishlist ♥"
            } else {
                if (lang == AppLanguage.HINDI) "विशलिस्ट से हटाया गया" else "Removed from Wishlist"
            }
        }
    }

    fun setShowWishlistOnly(onlyWishlist: Boolean) {
        _showWishlistOnly.value = onlyWishlist
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ProductCategory?) {
        _selectedCategory.value = category
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    suspend fun verifyOwnerPassword(password: String): Boolean {
        return langPrefs.verifyOwnerPassword(password)
    }

    fun addToCart(
        product: Product,
        quantity: Int,
        selectedSize: String,
        selectedColor: String
    ): Boolean {
        val lang = language.value
        if (product.stockCount <= 0L) {
            _toastMessage.value = if (lang == AppLanguage.HINDI) "यह उत्पाद स्टॉक में नहीं है।" else "This item is currently out of stock."
            return false
        }
        val currentTotalForProduct = _cartItems.value
            .filter { it.product.id == product.id }
            .sumOf { it.quantity }

        if (currentTotalForProduct + quantity > product.stockCount.toInt()) {
            _toastMessage.value = if (lang == AppLanguage.HINDI) {
                "अधिकतम उपलब्ध स्टॉक (${product.stockCount}) से अधिक नहीं जोड़ सकते।"
            } else {
                "Cannot exceed available stock (${product.stockCount} available)."
            }
            return false
        }

        val itemKey = "${product.id}_${selectedSize}_${selectedColor}"
        _cartItems.update { current ->
            val existing = current.firstOrNull { it.key == itemKey }
            if (existing != null) {
                current.map {
                    if (it.key == itemKey) it.copy(product = product, quantity = it.quantity + quantity) else it
                }
            } else {
                current + CartItem(
                    product = product,
                    quantity = quantity,
                    selectedSize = selectedSize,
                    selectedColor = selectedColor
                )
            }
        }
        activeCheckoutIdempotencyId = null
        _toastMessage.value = if (lang == AppLanguage.HINDI) "कार्ट में जोड़ा गया!" else "Added to cart!"
        return true
    }

    fun updateCartQuantity(itemKey: String, newQty: Int, latestProducts: List<Product>) {
        if (newQty <= 0) {
            removeFromCart(itemKey)
            return
        }
        val target = _cartItems.value.firstOrNull { it.key == itemKey } ?: return
        val liveProduct = latestProducts.firstOrNull { it.id == target.product.id } ?: target.product
        val otherVariantsQty = _cartItems.value
            .filter { it.product.id == liveProduct.id && it.key != itemKey }
            .sumOf { it.quantity }

        if (otherVariantsQty + newQty > liveProduct.stockCount.toInt()) {
            val lang = language.value
            _toastMessage.value = if (lang == AppLanguage.HINDI) {
                "केवल ${liveProduct.stockCount} पीस स्टॉक में उपलब्ध हैं।"
            } else {
                "Only ${liveProduct.stockCount} units available in stock."
            }
            return
        }

        _cartItems.update { list ->
            list.map { if (it.key == itemKey) it.copy(product = liveProduct, quantity = newQty) else it }
        }
        activeCheckoutIdempotencyId = null
    }

    fun removeFromCart(itemKey: String) {
        _cartItems.update { list -> list.filterNot { it.key == itemKey } }
        activeCheckoutIdempotencyId = null
    }

    fun syncCartWithLiveCatalog(liveProducts: List<Product>): List<String> {
        val byId = liveProducts.associateBy { it.id }
        val warnings = mutableListOf<String>()
        _cartItems.update { current ->
            current.mapNotNull { cartItem ->
                val fresh = byId[cartItem.product.id]
                when {
                    fresh == null || !fresh.isPublished || fresh.stockCount <= 0L -> {
                        warnings.add("${cartItem.product.nameEn} is no longer in stock and was removed.")
                        null
                    }
                    cartItem.quantity > fresh.stockCount.toInt() -> {
                        warnings.add("${fresh.nameEn} quantity adjusted to available stock (${fresh.stockCount}).")
                        cartItem.copy(product = fresh, quantity = fresh.stockCount.toInt())
                    }
                    else -> cartItem.copy(product = fresh)
                }
            }
        }
        return warnings
    }

    fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        landmark: String,
        paymentMethod: PaymentMethodType,
        onOrderConfirmed: (CustomerOrder) -> Unit
    ) {
        if (_isPlacingOrder.value) return
        val items = _cartItems.value
        if (items.isEmpty()) return

        val orderDocId = activeCheckoutIdempotencyId
            ?: "ord_${UUID.randomUUID().toString().replace("-", "").take(20)}".also {
                activeCheckoutIdempotencyId = it
            }

        _isPlacingOrder.value = true
        _checkoutError.value = null

        viewModelScope.launch {
            val result = repository.placeCustomerOrder(
                customerName = customerName,
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                landmark = landmark,
                paymentMethod = paymentMethod,
                cartItems = items,
                deterministicOrderId = orderDocId
            )
            _isPlacingOrder.value = false
            result.fold(
                onSuccess = { confirmedOrder ->
                    langPrefs.saveLocalCustomerOrder(confirmedOrder)
                    _lastConfirmedOrder.value = confirmedOrder
                    _cartItems.value = emptyList()
                    activeCheckoutIdempotencyId = null
                    onOrderConfirmed(confirmedOrder)
                },
                onFailure = { err ->
                    activeCheckoutIdempotencyId = null
                    _checkoutError.value = err.localizedMessage ?: "Could not place order. Please check stock and try again."
                }
            )
        }
    }

    private companion object {
        const val TAG = "StorefrontVM"
    }
}
