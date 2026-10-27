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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
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
import androidx.credentials.exceptions.GetCredentialCancellationException
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
    val firebaseReady = remember(appContext) { AuthManager.isFirebaseAvailable(appContext) }
    val langPrefs = remember(appContext) { LanguagePreferencesRepository(appContext) }
    val repository = remember(appContext) { StoreRepository(appContext) }

    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(if (firebaseReady) Firebase.auth.currentUser else null)
    }

    DisposableEffect(firebaseReady) {
        if (!firebaseReady) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose {
                Firebase.auth.removeAuthStateListener(listener)
            }
        }
    }

    // Customers open directly into the Flipkart-style storefront with ZERO login wall!
    ShivamMartStoreShell(
        user = currentUser,
        repository = repository,
        langPrefs = langPrefs
    )
}

@Composable
private fun ShivamMartStoreShell(
    user: FirebaseUser?,
    repository: StoreRepository,
    langPrefs: LanguagePreferencesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uidKey = user?.uid.orEmpty()
    var isRegisteredAdminDoc by remember(uidKey) { mutableStateOf(false) }
    val hasOwnerEmailToken = remember(uidKey, user?.email, user?.isEmailVerified) {
        AuthManager.hasVerifiedOwnerToken(user)
    }

    LaunchedEffect(uidKey, hasOwnerEmailToken) {
        if (uidKey.isNotBlank() && !hasOwnerEmailToken) {
            isRegisteredAdminDoc = repository.isUserRegisteredAdmin(uidKey)
        } else {
            isRegisteredAdminDoc = false
        }
    }

    val isCloudAuthorizedOwner = hasOwnerEmailToken || isRegisteredAdminDoc
    // When cloud is configured, owner writes require verified owner Google auth; in CI passthrough mode, owner password alone unlocks local dashboard
    val canAccessOwnerDashboard = if (repository.isCloudConfigured) {
        isCloudAuthorizedOwner
    } else {
        true
    }

    val storefrontVm: StorefrontViewModel = viewModel(
        key = "shivam_mart_storefront_${uidKey}_${isCloudAuthorizedOwner}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StorefrontViewModel(
                    repository = repository,
                    langPrefs = langPrefs,
                    currentUserId = uidKey,
                    isAuthorizedOwner = isCloudAuthorizedOwner
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

    var activeTab by rememberSaveable { mutableStateOf(PrimaryTab.HOME) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmedOrderBanner by remember { mutableStateOf<CustomerOrder?>(null) }
    var isOwnerUnlocked by rememberSaveable { mutableStateOf(false) }
    var showOwnerAccessDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(canAccessOwnerDashboard) {
        if (!canAccessOwnerDashboard && isOwnerUnlocked) {
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

    if (showOwnerAccessDialog) {
        OwnerAccessDialog(
            lang = lang,
            isCloudConfigured = repository.isCloudConfigured,
            signedInUser = user,
            isCloudAuthorizedOwner = isCloudAuthorizedOwner,
            onDismiss = { showOwnerAccessDialog = false },
            onVerifyPassword = { enteredPwd ->
                storefrontVm.verifyOwnerPassword(enteredPwd)
            },
            onSignOutAccount = {
                scope.launch { AuthManager.signOut(context) }
            },
            onUnlockedSuccess = {
                isOwnerUnlocked = true
                showOwnerAccessDialog = false
                selectedProductId = null
                confirmedOrderBanner = null
                activeTab = PrimaryTab.OWNER_ADMIN
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
        if (isOwnerUnlocked && canAccessOwnerDashboard) {
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
                    isOwnerUnlocked = isOwnerUnlocked && canAccessOwnerDashboard,
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
                        if (isOwnerUnlocked && canAccessOwnerDashboard) {
                            selectedProductId = null
                            confirmedOrderBanner = null
                            activeTab = PrimaryTab.OWNER_ADMIN
                        } else {
                            showOwnerAccessDialog = true
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
                                defaultCustomerName = "",
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
                        activeTab == PrimaryTab.OWNER_ADMIN && isOwnerUnlocked && canAccessOwnerDashboard -> {
                            val adminVm: AdminViewModel = viewModel(
                                key = "shivam_mart_admin_${uidKey.ifBlank { "local_owner" }}",
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
private fun OwnerAccessDialog(
    lang: AppLanguage,
    isCloudConfigured: Boolean,
    signedInUser: FirebaseUser?,
    isCloudAuthorizedOwner: Boolean,
    onDismiss: () -> Unit,
    onVerifyPassword: suspend (String) -> Boolean,
    onSignOutAccount: () -> Unit,
    onUnlockedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isPasswordVerified by remember { mutableStateOf(false) }
    var isSigningInOwner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Automatically proceed once password is verified and cloud owner authorization is active (or local mode)
    LaunchedEffect(isPasswordVerified, isCloudConfigured, isCloudAuthorizedOwner) {
        if (isPasswordVerified && (!isCloudConfigured || isCloudAuthorizedOwner)) {
            onUnlockedSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isPasswordVerified) Icons.Filled.CloudSync else Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (!isPasswordVerified) {
                    if (lang == AppLanguage.HINDI) "मालिक पैनल पासवर्ड" else "Shop Owner Login"
                } else {
                    if (lang == AppLanguage.HINDI) "लाइव क्लाउड सिंक (एक बार जोड़ें)" else "Connect Owner Cloud Account"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isPasswordVerified) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "ग्राहकों के लिए कोई लॉगिन ज़रूरी नहीं है। यह सेक्शन केवल दुकान मालिक (राजू कुमार गुप्ता) के लिए है:"
                        } else {
                            "Customers do not need to log in. This password is strictly for the Shop Owner:"
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
                } else {
                    Text(
                        text = if (lang == AppLanguage.HINDI) {
                            "पासवर्ड सही है! सभी ग्राहकों के फोन में नया सामान लाइव दिखाने के लिए अपना मालिक Gmail अकाउंट (rajuguptas123@gmail.com या eminence713@gmail.com) चुनें:"
                        } else {
                            "Password verified! Select your verified Owner Google account (rajuguptas123@gmail.com or eminence713@gmail.com) to publish live products to all customer phones:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (signedInUser != null && !isCloudAuthorizedOwner) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "अभी ${signedInUser.email} जुड़ा है, जो मालिक खाता नहीं है।"
                                    } else {
                                        "Signed in as ${signedInUser.email}, which is not an authorized owner email."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(
                                    onClick = onSignOutAccount,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (lang == AppLanguage.HINDI) "दूसरा अकाउंट चुनें" else "Switch Google Account")
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            if (isSigningInOwner) return@Button
                            isSigningInOwner = true
                            errorMessage = null
                            scope.launch {
                                val res = AuthManager.signInInteractiveWithGoogle(context)
                                isSigningInOwner = false
                                res.onFailure { err ->
                                    if (err !is GetCredentialCancellationException) {
                                        errorMessage = if (lang == AppLanguage.HINDI) {
                                            "Google अकाउंट जुड़ नहीं सका। कृपया फोन में इंटरनेट और Gmail अकाउंट जांचें।"
                                        } else {
                                            "Could not sign in with Google. Please verify internet and Google account on device."
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSigningInOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_google_sign_in_button")
                    ) {
                        if (isSigningInOwner) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (lang == AppLanguage.HINDI) "जुड़ रहा है..." else "Connecting...")
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (lang == AppLanguage.HINDI) {
                                    "मालिक Google अकाउंट जोड़ें"
                                } else {
                                    "Connect Owner Google Account"
                                }
                            )
                        }
                    }
                }

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
            if (!isPasswordVerified) {
                Button(
                    onClick = {
                        if (password.isBlank()) {
                            errorMessage = if (lang == AppLanguage.HINDI) "कृपया पासवर्ड लिखें।" else "Please enter password."
                        } else {
                            scope.launch {
                                val valid = onVerifyPassword(password)
                                if (valid) {
                                    errorMessage = null
                                    if (!isCloudConfigured || isCloudAuthorizedOwner) {
                                        onUnlockedSuccess()
                                    } else {
                                        isPasswordVerified = true
                                    }
                                } else {
                                    errorMessage = if (lang == AppLanguage.HINDI) {
                                        "गलत पासवर्ड! केवल दुकान मालिक के लिए अनुमति है।"
                                    } else {
                                        "Incorrect password! Access is restricted to the shop owner."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("owner_password_submit_button")
                ) {
                    Text(if (lang == AppLanguage.HINDI) "खोलें (Unlock)" else "Unlock")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("owner_password_cancel_button")
            ) {
                Text(if (lang == AppLanguage.HINDI) "बंद करें" else "Cancel")
            }
        },
        modifier = Modifier.testTag("owner_password_dialog")
    )
}
