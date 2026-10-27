package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.Product
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.StoreRepository
import com.example.ui.components.StoreTopBar
import com.example.ui.screens.AdminDashboardHub
import com.example.ui.screens.CartAndCheckoutTab
import com.example.ui.screens.CatalogAndSearchTab
import com.example.ui.screens.CustomerOrdersTab
import com.example.ui.screens.OrderConfirmationCard
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.StorefrontHomeTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StorefrontViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.AppStrings
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.launch

enum class PrimaryTab {
    HOME,
    CATALOG,
    CART,
    MY_ORDERS,
    OWNER_ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ShivamMartApp()
            }
        }
    }
}

@Composable
fun ShivamMartApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val langPrefs = remember(appContext) { LanguagePreferencesRepository(appContext) }
    val repository = remember(appContext) { StoreRepository(appContext) }

    val storefrontVm: StorefrontViewModel = viewModel(
        key = "shivam_mart_storefront",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StorefrontViewModel(
                    repository = repository,
                    langPrefs = langPrefs
                ) as T
            }
        }
    )

    val lang by storefrontVm.language.collectAsStateWithLifecycle()
    val settings by storefrontVm.storeSettings.collectAsStateWithLifecycle()
    val productsState by storefrontVm.publishedProductsState.collectAsStateWithLifecycle()
    val ordersState by storefrontVm.customerOrdersState.collectAsStateWithLifecycle()
    val searchQuery by storefrontVm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by storefrontVm.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by storefrontVm.sortOption.collectAsStateWithLifecycle()
    val cartItems by storefrontVm.cartItems.collectAsStateWithLifecycle()
    val isPlacingOrder by storefrontVm.isPlacingOrder.collectAsStateWithLifecycle()
    val checkoutError by storefrontVm.checkoutError.collectAsStateWithLifecycle()
    val toastMessage by storefrontVm.toastMessage.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(PrimaryTab.HOME) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmedOrderBanner by remember { mutableStateOf<CustomerOrder?>(null) }
    var isOwnerUnlocked by rememberSaveable { mutableStateOf(false) }
    var showOwnerPasswordDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            storefrontVm.clearToast()
        }
    }

    if (showOwnerPasswordDialog) {
        OwnerPasswordDialog(
            lang = lang,
            onDismiss = { showOwnerPasswordDialog = false },
            onVerifyPassword = { enteredPwd, onError ->
                scope.launch {
                    val valid = storefrontVm.verifyOwnerPassword(enteredPwd)
                    if (valid) {
                        isOwnerUnlocked = true
                        showOwnerPasswordDialog = false
                        selectedProductId = null
                        confirmedOrderBanner = null
                        activeTab = PrimaryTab.OWNER_ADMIN
                    } else {
                        onError(
                            if (lang == AppLanguage.HINDI) {
                                "गलत पासवर्ड! केवल दुकान मालिक के लिए अनुमति है।"
                            } else {
                                "Incorrect password! Access is restricted to the shop owner."
                            }
                        )
                    }
                }
            }
        )
    }

    val liveProducts = (productsState as? UiState.Success<List<Product>>)?.data.orEmpty()
    val selectedProduct = remember(selectedProductId, liveProducts) {
        selectedProductId?.let { id -> liveProducts.firstOrNull { it.id == id } }
    }
    val totalCartCount = cartItems.sumOf { it.quantity }

    BackHandler(enabled = confirmedOrderBanner != null || selectedProduct != null || activeTab != PrimaryTab.HOME) {
        when {
            confirmedOrderBanner != null -> confirmedOrderBanner = null
            selectedProduct != null -> selectedProductId = null
            activeTab != PrimaryTab.HOME -> activeTab = PrimaryTab.HOME
        }
    }

    data class NavDest(val tab: PrimaryTab, val label: String, val icon: ImageVector, val badge: Int = 0)
    val destinations = buildList {
        add(NavDest(PrimaryTab.HOME, AppStrings.navHome.get(lang), Icons.Filled.Home))
        add(NavDest(PrimaryTab.CATALOG, AppStrings.navCategories.get(lang), Icons.Filled.Category))
        add(NavDest(PrimaryTab.CART, AppStrings.navCart.get(lang), Icons.Filled.ShoppingCart, totalCartCount))
        add(NavDest(PrimaryTab.MY_ORDERS, AppStrings.navOrders.get(lang), Icons.AutoMirrored.Filled.ReceiptLong))
        if (isOwnerUnlocked) {
            add(NavDest(PrimaryTab.OWNER_ADMIN, AppStrings.navAdmin.get(lang), Icons.Filled.AdminPanelSettings))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                StoreTopBar(
                    storeNameEn = settings.storeNameEn,
                    storeNameHi = settings.storeNameHi,
                    lang = lang,
                    cartCount = totalCartCount,
                    isOwnerUnlocked = isOwnerUnlocked,
                    onToggleLanguage = { storefrontVm.toggleLanguage() },
                    onOpenCart = {
                        selectedProductId = null
                        confirmedOrderBanner = null
                        activeTab = PrimaryTab.CART
                    },
                    onOwnerClick = {
                        if (isOwnerUnlocked) {
                            selectedProductId = null
                            confirmedOrderBanner = null
                            activeTab = PrimaryTab.OWNER_ADMIN
                        } else {
                            showOwnerPasswordDialog = true
                        }
                    }
                )
            },
            floatingActionButton = {
                if (activeTab in listOf(PrimaryTab.HOME, PrimaryTab.CATALOG) && selectedProduct == null) {
                    FloatingActionButton(
                        onClick = {
                            val msg = WhatsAppHelper.buildCartEnquiryMessage(
                                settings = settings,
                                cartItems = cartItems,
                                nextStepLabel = "Store Enquiry",
                                lang = lang
                            )
                            WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                        },
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("whatsapp_fab")
                    ) {
                        Text("WA", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            NavigationBarItem(
                                selected = activeTab == dest.tab && selectedProduct == null,
                                onClick = {
                                    selectedProductId = null
                                    confirmedOrderBanner = null
                                    activeTab = dest.tab
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.badge > 0) {
                                                Badge { Text("${dest.badge}") }
                                            }
                                        }
                                    ) {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("bottom_nav_${dest.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        destinations.forEach { dest ->
                            NavigationRailItem(
                                selected = activeTab == dest.tab && selectedProduct == null,
                                onClick = {
                                    selectedProductId = null
                                    confirmedOrderBanner = null
                                    activeTab = dest.tab
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dest.badge > 0) Badge { Text("${dest.badge}") }
                                        }
                                    ) {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        confirmedOrderBanner != null -> {
                            OrderConfirmationCard(
                                order = confirmedOrderBanner!!,
                                settings = settings,
                                lang = lang,
                                onViewMyOrders = {
                                    confirmedOrderBanner = null
                                    activeTab = PrimaryTab.MY_ORDERS
                                },
                                onContinueShopping = {
                                    confirmedOrderBanner = null
                                    activeTab = PrimaryTab.HOME
                                }
                            )
                        }
                        selectedProduct != null -> {
                            ProductDetailScreen(
                                product = selectedProduct,
                                settings = settings,
                                lang = lang,
                                onBack = { selectedProductId = null },
                                onAddToCart = { qty, sz, clr ->
                                    storefrontVm.addToCart(selectedProduct, qty, sz, clr)
                                },
                                onBuyNow = { qty, sz, clr ->
                                    if (storefrontVm.addToCart(selectedProduct, qty, sz, clr)) {
                                        selectedProductId = null
                                        activeTab = PrimaryTab.CART
                                    }
                                }
                            )
                        }
                        activeTab == PrimaryTab.HOME -> {
                            StorefrontHomeTab(
                                settings = settings,
                                productsState = productsState,
                                lang = lang,
                                onSelectCategory = { cat ->
                                    storefrontVm.selectCategory(cat)
                                    activeTab = PrimaryTab.CATALOG
                                },
                                onOpenSearchCatalog = { q ->
                                    storefrontVm.updateSearchQuery(q)
                                    activeTab = PrimaryTab.CATALOG
                                },
                                onOpenProduct = { prod -> selectedProductId = prod.id },
                                onQuickAdd = { prod ->
                                    storefrontVm.addToCart(
                                        product = prod,
                                        quantity = 1,
                                        selectedSize = prod.sizes.firstOrNull().orEmpty(),
                                        selectedColor = prod.colors.firstOrNull().orEmpty()
                                    )
                                }
                            )
                        }
                        activeTab == PrimaryTab.CATALOG -> {
                            CatalogAndSearchTab(
                                productsState = productsState,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                sortOption = sortOption,
                                lang = lang,
                                onSearchChange = { storefrontVm.updateSearchQuery(it) },
                                onSelectCategory = { storefrontVm.selectCategory(it) },
                                onSelectSort = { storefrontVm.updateSortOption(it) },
                                onOpenProduct = { prod -> selectedProductId = prod.id },
                                onQuickAdd = { prod ->
                                    storefrontVm.addToCart(
                                        product = prod,
                                        quantity = 1,
                                        selectedSize = prod.sizes.firstOrNull().orEmpty(),
                                        selectedColor = prod.colors.firstOrNull().orEmpty()
                                    )
                                }
                            )
                        }
                        activeTab == PrimaryTab.CART -> {
                            CartAndCheckoutTab(
                                cartItems = cartItems,
                                latestProducts = liveProducts,
                                settings = settings,
                                isPlacingOrder = isPlacingOrder,
                                checkoutError = checkoutError,
                                lang = lang,
                                onUpdateQuantity = { key, nextQ ->
                                    storefrontVm.updateCartQuantity(key, nextQ, liveProducts)
                                },
                                onRemoveItem = { key -> storefrontVm.removeFromCart(key) },
                                onSyncLiveStock = { storefrontVm.syncCartWithLiveCatalog(liveProducts) },
                                onPlaceOrder = { name, phone, addr, lmk, method ->
                                    storefrontVm.placeOrder(name, phone, addr, lmk, method) { savedOrder ->
                                        confirmedOrderBanner = savedOrder
                                    }
                                },
                                onBrowseShop = { activeTab = PrimaryTab.CATALOG }
                            )
                        }
                        activeTab == PrimaryTab.MY_ORDERS -> {
                            CustomerOrdersTab(
                                ordersState = ordersState,
                                settings = settings,
                                lang = lang
                            )
                        }
                        activeTab == PrimaryTab.OWNER_ADMIN && isOwnerUnlocked -> {
                            val adminVm: AdminViewModel = viewModel(
                                key = "shivam_mart_admin",
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return AdminViewModel(
                                            repository = repository,
                                            langPrefs = langPrefs
                                        ) as T
                                    }
                                }
                            )
                            AdminDashboardHub(
                                adminViewModel = adminVm,
                                storeSettings = settings,
                                lang = lang,
                                onExitToStorefront = { activeTab = PrimaryTab.HOME },
                                onLockOwner = {
                                    isOwnerUnlocked = false
                                    activeTab = PrimaryTab.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerPasswordDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onVerifyPassword: (String, (String) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (lang == AppLanguage.HINDI) "मालिक पैनल पासवर्ड" else "Owner Dashboard Access",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "केवल दुकान मालिक के लिए। कृपया अपना पासवर्ड दर्ज करें:"
                    } else {
                        "For shop owner only. Enter your Owner Password to unlock:"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = {
                        Text(if (lang == AppLanguage.HINDI) "मालिक पासवर्ड" else "Owner Password")
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_password_input")
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("owner_password_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = if (lang == AppLanguage.HINDI) "कृपया पासवर्ड लिखें।" else "Please enter password."
                    } else {
                        onVerifyPassword(password) { err -> errorMessage = err }
                    }
                },
                modifier = Modifier.testTag("owner_password_submit_button")
            ) {
                Text(if (lang == AppLanguage.HINDI) "खोलें (Unlock)" else "Unlock")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("owner_password_cancel_button")
            ) {
                Text(if (lang == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        },
        modifier = Modifier.testTag("owner_password_dialog")
    )
}
