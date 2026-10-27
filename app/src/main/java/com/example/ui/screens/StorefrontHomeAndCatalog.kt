package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.ui.components.JharokhaGoldDivider
import com.example.ui.components.ProductGridCard
import com.example.ui.components.ProductVisualImage
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DeepSindoor
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.UiState
import com.example.util.AppStrings
import com.example.util.WhatsAppHelper
import com.example.util.formatInr

@Composable
fun StorefrontHomeTab(
    settings: StoreSettings,
    productsState: UiState<List<Product>>,
    lang: AppLanguage,
    onSelectCategory: (ProductCategory?) -> Unit,
    onOpenSearchCatalog: (String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit
) {
    val context = LocalContext.current
    var homeSearch by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner + Search
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_hero_1793076838918),
                    contentDescription = "Storefront banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DeepSindoor.copy(alpha = 0.55f),
                                    DeepSindoor.copy(alpha = 0.90f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.HINDI) settings.storeNameHi else settings.storeNameEn,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        val offerText = if (lang == AppLanguage.HINDI) settings.promoBannerHi else settings.promoBannerEn
                        if (offerText.isNotBlank()) {
                            Text(
                                text = offerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBF2DC),
                                maxLines = 2
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onSelectCategory(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AntiqueGold,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("shop_now_button")
                            ) {
                                Text(AppStrings.shopNow.get(lang))
                            }
                            OutlinedButton(
                                onClick = {
                                    val msg = WhatsAppHelper.buildCartEnquiryMessage(
                                        settings = settings,
                                        cartItems = emptyList(),
                                        nextStepLabel = "General Store Enquiry",
                                        lang = lang
                                    )
                                    WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                                },
                                border = BorderStroke(1.5.dp, Color.White),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.testTag("home_whatsapp_button")
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Search Bar
        item {
            OutlinedTextField(
                value = homeSearch,
                onValueChange = {
                    homeSearch = it
                    if (it.length >= 2) onOpenSearchCatalog(it)
                },
                placeholder = { Text(AppStrings.searchHint.get(lang)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_search_input")
            )
        }

        // 3. All 10 Category Shortcuts
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "उत्पाद श्रेणियाँ (10)" else "Shop by Category (10)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = AppStrings.viewProducts.get(lang),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clickable { onSelectCategory(null) }
                            .padding(6.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProductCategory.entries) { cat ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { onSelectCategory(cat) }
                                .testTag("category_chip_${cat.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Checkroom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = cat.label(lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        item { JharokhaGoldDivider(modifier = Modifier.padding(horizontal = 24.dp)) }

        // 4. Featured, New Arrivals & Popular Sections
        when (productsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = productsState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is UiState.Success -> {
                val products = productsState.data
                if (products.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) "अभी कोई प्रकाशित उत्पाद नहीं है।" else "No published products yet.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "दुकानदार उत्पादों की समीक्षा कर प्रकाशित करेंगे। जानकारी के लिए व्हाट्सऐप पर संपर्क करें।"
                                    } else {
                                        "The shop owner will review and publish available inventory shortly. Contact us on WhatsApp for enquiries."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    val featured = products.filter { it.isFeatured }.ifEmpty { products.take(4) }
                    val newArrivals = products.take(6)
                    val popularReal = products.filter { it.soldCount > 0L }.sortedByDescending { it.soldCount }.take(6)

                    item {
                        ProductHorizontalSection(
                            title = AppStrings.featuredHeading.get(lang),
                            items = featured,
                            lang = lang,
                            onOpenProduct = onOpenProduct,
                            onQuickAdd = onQuickAdd
                        )
                    }

                    item {
                        ProductHorizontalSection(
                            title = AppStrings.newArrivalsHeading.get(lang),
                            items = newArrivals,
                            lang = lang,
                            onOpenProduct = onOpenProduct,
                            onQuickAdd = onQuickAdd
                        )
                    }

                    if (popularReal.isNotEmpty()) {
                        item {
                            ProductHorizontalSection(
                                title = AppStrings.popularHeading.get(lang),
                                items = popularReal,
                                lang = lang,
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd
                            )
                        }
                    }
                }
            }
        }

        // 5. Transparent Delivery, Payment & Contact Info Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = AppStrings.deliveryPolicyTitle.get(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (lang == AppLanguage.HINDI) settings.deliveryCoverageHi else settings.deliveryCoverageEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (!settings.policiesConfirmed) {
                            AppStrings.awaitingOwnerConfirmation.get(lang)
                        } else {
                            val delText = if (settings.deliveryChargeInr == 0L) "Free Local Delivery" else "Delivery Fee: ${formatInr(settings.deliveryChargeInr)}"
                            val freeNote = if (settings.freeDeliveryThresholdInr > 0L) " (Free above ${formatInr(settings.freeDeliveryThresholdInr)})" else ""
                            "$delText$freeNote"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (lang == AppLanguage.HINDI) settings.returnPolicyHi else settings.returnPolicyEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val paymentModes = buildList {
                        if (settings.codEnabled) add(if (lang == AppLanguage.HINDI) "कैश ऑन डिलीवरी (COD)" else "Cash on Delivery (COD)")
                        if (settings.isUpiConfiguredAndActive) add("Verified UPI QR")
                        add("WhatsApp Order Assistance")
                    }.joinToString(" • ")
                    Text(
                        text = "Available Modes: $paymentModes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHorizontalSection(
    title: String,
    items: List<Product>,
    lang: AppLanguage,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { prod ->
                ProductGridCard(
                    product = prod,
                    lang = lang,
                    onClick = { onOpenProduct(prod) },
                    onQuickAdd = { onQuickAdd(prod) },
                    modifier = Modifier.width(210.dp)
                )
            }
        }
    }
}

@Composable
fun CatalogAndSearchTab(
    productsState: UiState<List<Product>>,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    sortOption: SortOption,
    lang: AppLanguage,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 560.dp -> 3
            else -> 2
        }

        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(AppStrings.searchHint.get(lang)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("catalog_search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text(AppStrings.allCategories.get(lang)) }
                    )
                }
                items(ProductCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label(lang)) }
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SortOption.entries) { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { onSelectSort(opt) },
                        label = { Text(opt.label(lang)) }
                    )
                }
            }

            when (productsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(productsState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val q = searchQuery.trim().lowercase()
                    val filtered = remember(productsState.data, q, selectedCategory, sortOption) {
                        productsState.data
                            .filter { p ->
                                (selectedCategory == null || p.categoryEnum() == selectedCategory) &&
                                    (q.isEmpty() ||
                                        p.nameEn.lowercase().contains(q) ||
                                        p.nameHi.lowercase().contains(q) ||
                                        p.descriptionEn.lowercase().contains(q) ||
                                        p.categoryEnum().labelEn.lowercase().contains(q))
                            }
                            .let { list ->
                                when (sortOption) {
                                    SortOption.NEWEST -> list
                                    SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.sellingPriceInr }
                                    SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.sellingPriceInr }
                                }
                            }
                    }

                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "कोई उत्पाद नहीं मिला" else "No matching products found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (lang == AppLanguage.HINDI) "फ़िल्टर बदलें या सभी श्रेणियाँ देखें।" else "Try clearing filters or searching another category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    onSearchChange("")
                                    onSelectCategory(null)
                                }
                            ) {
                                Text(if (lang == AppLanguage.HINDI) "फ़िल्टर रीसेट करें" else "Reset Filters")
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "${filtered.size} ${if (lang == AppLanguage.HINDI) "उत्पाद उपलब्ध" else "items available"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(filtered, key = { it.id }) { prod ->
                                ProductGridCard(
                                    product = prod,
                                    lang = lang,
                                    onClick = { onOpenProduct(prod) },
                                    onQuickAdd = { onQuickAdd(prod) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    settings: StoreSettings,
    lang: AppLanguage,
    onBack: () -> Unit,
    onAddToCart: (Int, String, String) -> Unit,
    onBuyNow: (Int, String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var selectedSize by remember(product.id) { mutableStateOf(product.sizes.firstOrNull().orEmpty()) }
    var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull().orEmpty()) }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    val maxQty = product.stockCount.toInt().coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = product.categoryEnum().label(lang),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp)
        ) {
            ProductVisualImage(
                product = product,
                lang = lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (product.isDemo) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "यह परीक्षण हेतु डेमो सैंपल उत्पाद है। वास्तविक दुकान स्टॉक की पुष्टि व्हाट्सऐप पर करें।"
                            } else {
                                "This is a clearly labelled demonstration sample item for testing."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = product.displayName(lang),
                style = MaterialTheme.typography.headlineSmall
            )

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatInr(product.sellingPriceInr),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.originalPriceInr > product.sellingPriceInr) {
                    Text(
                        text = formatInr(product.originalPriceInr),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            Text(
                text = if (product.isOutOfStock) {
                    AppStrings.outOfStock.get(lang)
                } else {
                    "${AppStrings.inStock.get(lang)}: ${product.stockCount} pcs"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (product.isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            val desc = product.displayDescription(lang)
            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (product.sizes.isNotEmpty()) {
                Text(
                    text = if (lang == AppLanguage.HINDI) "साइज़ चुनें:" else "Select Size:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.sizes.forEach { sz ->
                        FilterChip(
                            selected = selectedSize == sz,
                            onClick = { selectedSize = sz },
                            label = { Text(sz) }
                        )
                    }
                }
            }

            if (product.colors.isNotEmpty()) {
                Text(
                    text = if (lang == AppLanguage.HINDI) "रंग चुनें:" else "Select Colour:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.colors.forEach { clr ->
                        FilterChip(
                            selected = selectedColor == clr,
                            onClick = { selectedColor = clr },
                            label = { Text(clr) }
                        )
                    }
                }
            }

            // Quantity selector
            if (!product.isOutOfStock) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "मात्रा (Qty):" else "Quantity:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        enabled = quantity > 1
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { if (quantity < maxQty) quantity++ },
                        enabled = quantity < maxQty
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onAddToCart(quantity, selectedSize, selectedColor) },
                    enabled = !product.isOutOfStock,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_to_cart_button")
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.addToCart.get(lang))
                }
                Button(
                    onClick = { onBuyNow(quantity, selectedSize, selectedColor) },
                    enabled = !product.isOutOfStock,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_now_button")
                ) {
                    Text(AppStrings.buyNow.get(lang))
                }
            }

            OutlinedButton(
                onClick = {
                    val msg = WhatsAppHelper.buildSingleProductMessage(
                        settings = settings,
                        product = product,
                        quantity = quantity,
                        size = selectedSize,
                        color = selectedColor,
                        lang = lang
                    )
                    WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                },
                border = BorderStroke(1.5.dp, WhatsAppGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("product_whatsapp_button")
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(AppStrings.orderOnWhatsApp.get(lang))
            }
        }
    }
}
