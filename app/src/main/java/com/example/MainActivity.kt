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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
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
    val lang by langPrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.HINDI)

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(
            lang = lang,
            onToggleLanguage = {
                val next = if (lang == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
                scope.launch { langPrefs.setLanguage(next) }
            }
        )
    } else {
        AuthenticatedShivamMartShell(
            user = signedInUser,
            langPrefs = langPrefs,
            onSignOut = {
                scope.launch { AuthManager.signOut(context) }
            }
        )
    }
}

@Composable
private fun AuthenticatedShivamMartShell(
    user: FirebaseUser,
    langPrefs: LanguagePreferencesRepository,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val repository = remember(appContext) { StoreRepository(appContext) }

    var isRegisteredAdminDoc by remember(user.uid) { mutableStateOf(false) }
    val hasOwnerEmailToken = remember(user.uid, user.email, user.isEmailVerified) {
        AuthManager.hasVerifiedOwnerToken(user)
    }

    LaunchedEffect(user.uid, hasOwnerEmailToken) {
        if (!hasOwnerEmailToken) {
            isRegisteredAdminDoc = repository.isUserRegisteredAdmin(user.uid)
        }
    }

    val isAuthorizedOwner = hasOwnerEmailToken || isRegisteredAdminDoc

    val storefrontVm: StorefrontViewModel = viewModel(
        key = "shivam_mart_storefront_${user.uid}_${isAuthorizedOwner}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StorefrontViewModel(
                    repository = repository,
                    langPrefs = langPrefs,
                    currentUserId = user.uid,
                    isAuthorizedOwner = isAuthorizedOwner
                ) as T
            }
        }
    )

    val lang by storefrontVm.language.collectAsStateWithLifecycle()
    val wishlistIds by storefrontVm.wishlistIds.collectAsStateWithLifecycle()
    val showWishlistOnly by storefrontVm.showWishlistOnly.collectAsStateWithLifecycle()
    val settings by storefrontVm.storeSettings.collectAsStateWithLifecycle()
    val productsState by storefrontVm.publishedProductsState.collectAsStateWithLifecycle()
    val ordersState by storefrontVm.customerOrdersState.collectAsStateWithLifecycle()
    val pendingOwnerOrdersCount by storefrontVm.pendingOwnerOrdersCount.collectAsStateWithLifecycle()
    val searchQuery by storefrontVm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by storefrontVm.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by storefrontVm.sortOption.collectAsStateWithLifecycle()
    val cartItems by storefrontVm.cartItems.collectAsStateWithLifecycle()
    val isPlacingOrder by storefrontVm.isPlacingOrder.collectAsStateWithLifecycle()
    val checkoutError by storefrontVm.checkoutError.collectAsStateWithLifecycle()
    val toastMessage by storefrontVm.toastMessage.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable(user.uid) { mutableStateOf(PrimaryTab.HOME) }
    var selectedProductId by rememberSaveable(user.uid) { mutableStateOf<String?>(null) }
    var confirmedOrderBanner by remember(user.uid) { mutableStateOf<CustomerOrder?>(null) }
    var isOwnerUnlocked by rememberSaveable(user.uid) { mutableStateOf(isAuthorizedOwner) }
    var showOwnerPasswordDialog by rememberSaveable(user.uid) { mutableStateOf(false) }
    var showAccountSheetDialog by rememberSaveable(user.uid) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isAuthorizedOwner) {
        if (isAuthorizedOwner) {
            isOwnerUnlocked = true
        } else {
            isOwnerUnlocked = false
            if (activeTab == PrimaryTab.OWNER_ADMIN) {
                activeTab = PrimaryTab.HOME
            }
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            storefrontVm.clearToast()
        }
    }

    if (showOwnerPasswordDialog && isAuthorizedOwner) {
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

    if (showAccountSheetDialog) {
        AccountProfileDialog(
            user = user,
            isAuthorizedOwner = isAuthorizedOwner,
            lang = lang,
            onDismiss = { showAccountSheetDialog = false },
            onOpenOwnerPanel = {
                showAccountSheetDialog = false
                if (isOwnerUnlocked) {
                    selectedProductId = null
                    confirmedOrderBanner = null
                    activeTab = PrimaryTab.OWNER_ADMIN
                } else {
                    showOwnerPasswordDialog = true
                }
            },
            onSignOut = {
                showAccountSheetDialog = false
                onSignOut()
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
        if (isAuthorizedOwner && isOwnerUnlocked) {
            add(
                NavDest(
                    tab = PrimaryTab.OWNER_ADMIN,
                    label = AppStrings.navAdmin.get(lang),
                    icon = Icons.Filled.AdminPanelSettings,
                    badge = pendingOwnerOrdersCount
                )
            )
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
                    wishlistCount = wishlistIds.size,
                    showOwnerButton = isAuthorizedOwner,
                    isOwnerUnlocked = isOwnerUnlocked,
                    pendingOwnerOrdersCount = pendingOwnerOrdersCount,
                    onToggleLanguage = { storefrontVm.toggleLanguage() },
                    onOpenWishlist = {
                        selectedProductId = null
                        confirmedOrderBanner = null
                        storefrontVm.setShowWishlistOnly(true)
                        activeTab = PrimaryTab.CATALOG
                    },
                    onOpenCart = {
                        selectedProductId = null
                        confirmedOrderBanner = null
                        activeTab = PrimaryTab.CART
                    },
                    onOwnerClick = {
                        if (isAuthorizedOwner) {
                            if (isOwnerUnlocked) {
                                selectedProductId = null
                                confirmedOrderBanner = null
                                activeTab = PrimaryTab.OWNER_ADMIN
                            } else {
                                showOwnerPasswordDialog = true
                            }
                        }
                    },
                    onAccountClick = { showAccountSheetDialog = true }
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
                                    if (dest.tab == PrimaryTab.CATALOG) {
                                        storefrontVm.setShowWishlistOnly(false)
                                    }
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
                                    if (dest.tab == PrimaryTab.CATALOG) {
                                        storefrontVm.setShowWishlistOnly(false)
                                    }
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
                                isWishlisted = selectedProduct.id in wishlistIds,
                                lang = lang,
                                onBack = { selectedProductId = null },
                                onToggleWishlist = { storefrontVm.toggleWishlist(selectedProduct) },
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
                                wishlistIds = wishlistIds,
                                lang = lang,
                                onSelectCategory = { cat ->
                                    storefrontVm.setShowWishlistOnly(false)
                                    storefrontVm.selectCategory(cat)
                                    activeTab = PrimaryTab.CATALOG
                                },
                                onOpenSearchCatalog = { q ->
                                    storefrontVm.setShowWishlistOnly(false)
                                    storefrontVm.updateSearchQuery(q)
                                    activeTab = PrimaryTab.CATALOG
                                },
                                onOpenProduct = { prod -> selectedProductId = prod.id },
                                onToggleWishlist = { prod -> storefrontVm.toggleWishlist(prod) },
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
                                wishlistIds = wishlistIds,
                                showWishlistOnly = showWishlistOnly,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                sortOption = sortOption,
                                lang = lang,
                                onSearchChange = { storefrontVm.updateSearchQuery(it) },
                                onSelectCategory = { storefrontVm.selectCategory(it) },
                                onSelectSort = { storefrontVm.updateSortOption(it) },
                                onToggleShowWishlistOnly = { storefrontVm.setShowWishlistOnly(it) },
                                onOpenProduct = { prod -> selectedProductId = prod.id },
                                onToggleWishlist = { prod -> storefrontVm.toggleWishlist(prod) },
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
                                defaultCustomerName = user.displayName.orEmpty(),
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
                                onBrowseShop = {
                                    storefrontVm.setShowWishlistOnly(false)
                                    activeTab = PrimaryTab.CATALOG
                                }
                            )
                        }
                        activeTab == PrimaryTab.MY_ORDERS -> {
                            CustomerOrdersTab(
                                ordersState = ordersState,
                                settings = settings,
                                lang = lang
                            )
                        }
                        activeTab == PrimaryTab.OWNER_ADMIN && isAuthorizedOwner && isOwnerUnlocked -> {
                            val adminVm: AdminViewModel = viewModel(
                                key = "shivam_mart_admin_${user.uid}",
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
private fun AccountProfileDialog(
    user: FirebaseUser,
    isAuthorizedOwner: Boolean,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (lang == AppLanguage.HINDI) "मेरा खाता (My Account)" else "My Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!user.displayName.isNullOrBlank()) {
                    Text(
                        text = user.displayName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = user.email.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                if (isAuthorizedOwner) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "✔ सत्यापित दुकान मालिक खाता (Raju Kumar Gupta) — आप नया उत्पाद जोड़ सकते हैं और ऑनलाइन ऑर्डर मैनेज कर सकते हैं।"
                        } else {
                            "✔ Verified Shop Owner Account — Full cloud catalog & live orders access enabled."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onOpenOwnerPanel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_open_owner_button")
                    ) {
                        Text(if (lang == AppLanguage.HINDI) "मालिक डैशबोर्ड खोलें" else "Open Owner Dashboard")
                    }
                } else {
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "ग्राहक खाता सक्रिय है। आपके सभी ऑर्डर और बिल क्लाउड पर सुरक्षित रहते हैं।"
                        } else {
                            "Customer shopping account active. Your orders and bills are synced to your account."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(
                        text = if (lang == AppLanguage.HINDI) "  अकाउंट बदलें / लॉग आउट" else "  Switch Account / Sign Out"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.HINDI) "बंद करें" else "Close")
            }
        }
    )
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
