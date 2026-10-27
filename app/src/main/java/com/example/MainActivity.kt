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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.Product
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.StoreRepository
import com.example.ui.components.StoreLogoCrest
import com.example.ui.components.StoreTopBar
import com.example.ui.screens.AdminDashboardHub
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.CartAndCheckoutTab
import com.example.ui.screens.CatalogAndSearchTab
import com.example.ui.screens.CustomerOrdersTab
import com.example.ui.screens.OrderConfirmationCard
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.StorefrontHomeTab
import com.example.ui.screens.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.RoleVerificationState
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
                StoreRootAuthGate()
            }
        }
    }
}

@Composable
fun StoreRootAuthGate() {
    val context = LocalContext.current
    val langPrefs = remember(context) { LanguagePreferencesRepository(context.applicationContext) }
    val preAuthLang by langPrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var requestedOwnerLogin by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val activeUser = currentUser
    if (activeUser == null) {
        AuthWelcomeScreen(
            lang = preAuthLang,
            requestedOwnerLogin = requestedOwnerLogin,
            onRequestedOwnerLoginChange = { requestedOwnerLogin = it },
            onToggleLanguage = {
                val next = if (preAuthLang == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
                scope.launch { langPrefs.setLanguage(next) }
            },
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AuthenticatedStoreShell(
            user = activeUser,
            langPrefs = langPrefs,
            requestedOwnerLogin = requestedOwnerLogin,
            onConsumeRequestedOwnerLogin = { requestedOwnerLogin = false },
            onSignedOut = {
                requestedOwnerLogin = false
                currentUser = null
            },
            onSwitchToOwnerAccount = {
                requestedOwnerLogin = true
                currentUser = null
            }
        )
    }
}

@Composable
private fun AuthenticatedStoreShell(
    user: FirebaseUser,
    langPrefs: LanguagePreferencesRepository,
    requestedOwnerLogin: Boolean,
    onConsumeRequestedOwnerLogin: () -> Unit,
    onSignedOut: () -> Unit,
    onSwitchToOwnerAccount: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(appContext) { StoreRepository(appContext) }

    val storefrontVm: StorefrontViewModel = viewModel(
        key = "storefront_${user.uid}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StorefrontViewModel(
                    repository = repository,
                    langPrefs = langPrefs,
                    currentUserId = user.uid,
                    defaultCustomerName = user.displayName.orEmpty()
                ) as T
            }
        }
    )

    val lang by storefrontVm.language.collectAsStateWithLifecycle()
    val isOwnerAdmin by storefrontVm.isOwnerAdmin.collectAsStateWithLifecycle()
    val roleVerificationState by storefrontVm.roleVerificationState.collectAsStateWithLifecycle()
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
    var handledInitialRoleRoutingUid by rememberSaveable { mutableStateOf<String?>(null) }
    var showUnauthorizedOwnerDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(roleVerificationState, user.uid, requestedOwnerLogin) {
        when (val roleState = roleVerificationState) {
            is RoleVerificationState.Checking -> Unit
            is RoleVerificationState.AuthorizedOwner -> {
                if (handledInitialRoleRoutingUid != user.uid) {
                    handledInitialRoleRoutingUid = user.uid
                    // Automatically open Owner Dashboard upon authorized owner Google login
                    activeTab = PrimaryTab.OWNER_ADMIN
                }
                if (requestedOwnerLogin) {
                    activeTab = PrimaryTab.OWNER_ADMIN
                    onConsumeRequestedOwnerLogin()
                }
            }
            is RoleVerificationState.CustomerOnly -> {
                if (handledInitialRoleRoutingUid != user.uid) {
                    handledInitialRoleRoutingUid = user.uid
                    if (activeTab == PrimaryTab.OWNER_ADMIN) {
                        activeTab = PrimaryTab.HOME
                    }
                }
                if (activeTab == PrimaryTab.OWNER_ADMIN) {
                    activeTab = PrimaryTab.HOME
                }
                if (requestedOwnerLogin) {
                    showUnauthorizedOwnerDialog = true
                    onConsumeRequestedOwnerLogin()
                }
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

    if (roleVerificationState is RoleVerificationState.Checking) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { pad ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(24.dp)
                    .testTag("role_verification_loading"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StoreLogoCrest(size = 72.dp)
                    Spacer(Modifier.height(18.dp))
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "खाता और मालिक डैशबोर्ड अनुमति जाँची जा रही है..."
                        } else {
                            "Verifying Google account & store permissions..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    if (showUnauthorizedOwnerDialog && !isOwnerAdmin) {
        val currentEmail = user.email?.takeIf { it.isNotBlank() } ?: "Google Account"
        AlertDialog(
            onDismissRequest = { showUnauthorizedOwnerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LockPerson,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "अनधिकृत मालिक खाता (केवल ग्राहक एक्सेस)"
                    } else {
                        "Owner Dashboard Restricted"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "आप वर्तमान में $currentEmail से साइन इन हैं।\n\nसुरक्षा नियमों के अनुसार मालिक डैशबोर्ड केवल अधिकृत दुकान मालिक के Google खाते के लिए उपलब्ध है। अन्य सभी खातों के लिए केवल ग्राहक स्टोरफ्रंट खुला है।"
                    } else {
                        "You are signed in as $currentEmail.\n\nOwner Dashboard features (inventory, POS billing, orders & store settings) are strictly restricted to the authorized shop owner Google account. You can continue shopping as a customer or switch to the authorized owner Google account."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnauthorizedOwnerDialog = false
                        signOutUser(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = onSwitchToOwnerAccount,
                            scope = scope
                        )
                    },
                    modifier = Modifier.testTag("switch_owner_account_button")
                ) {
                    Text(
                        if (lang == AppLanguage.HINDI) "अधिकृत खाते से लॉगिन करें" else "Switch Google Account"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnauthorizedOwnerDialog = false },
                    modifier = Modifier.testTag("continue_as_customer_button")
                ) {
                    Text(
                        if (lang == AppLanguage.HINDI) "ग्राहक के रूप में जारी रखें" else "Continue as Customer"
                    )
                }
            },
            modifier = Modifier.testTag("unauthorized_owner_dialog")
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
        if (isOwnerAdmin) {
            add(NavDest(PrimaryTab.OWNER_ADMIN, AppStrings.navAdmin.get(lang), Icons.Filled.AdminPanelSettings))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    StoreTopBar(
                        storeNameEn = settings.storeNameEn,
                        storeNameHi = settings.storeNameHi,
                        lang = lang,
                        cartCount = totalCartCount,
                        onToggleLanguage = { storefrontVm.toggleLanguage() },
                        onOpenCart = {
                            selectedProductId = null
                            confirmedOrderBanner = null
                            activeTab = PrimaryTab.CART
                        },
                        onSignOut = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignedOut,
                                scope = scope
                            )
                        },
                        onOwnerAccessCheck = if (!isOwnerAdmin) {
                            { showUnauthorizedOwnerDialog = true }
                        } else {
                            null
                        }
                    )
                }
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
                                defaultCustomerName = storefrontVm.defaultCustomerName,
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
                        activeTab == PrimaryTab.OWNER_ADMIN && isOwnerAdmin -> {
                            val adminVm: AdminViewModel = viewModel(
                                key = "admin_${user.uid}",
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return AdminViewModel(repository) as T
                                    }
                                }
                            )
                            AdminDashboardHub(
                                adminViewModel = adminVm,
                                storeSettings = settings,
                                lang = lang,
                                onExitToStorefront = { activeTab = PrimaryTab.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
