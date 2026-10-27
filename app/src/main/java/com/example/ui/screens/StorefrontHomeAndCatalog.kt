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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.ui.components.JharokhaGoldDivider
import com.example.ui.components.ProductGridCard
import com.example.ui.components.ProductVisualImage
import com.example.ui.components.ShivamAssuredBadge
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.DeepSindoor
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.UiState
import com.example.util.AppStrings
import com.example.util.WhatsAppHelper
import com.example.util.formatInr

private fun categoryIcon(cat: ProductCategory): ImageVector = when (cat) {
    ProductCategory.SAREES -> Icons.Filled.AutoAwesome
    ProductCategory.KURTIS -> Icons.Filled.DryCleaning
    ProductCategory.LADIES_FROCKS -> Icons.Filled.WorkspacePremium
    ProductCategory.SHIRTS,
    ProductCategory.TSHIRTS,
    ProductCategory.JEANS,
    ProductCategory.TROUSERS -> Icons.Filled.Checkroom
    ProductCategory.SHOES,
    ProductCategory.SANDALS,
    ProductCategory.SLIPPERS -> Icons.Filled.Hiking
}

@Composable
fun StorefrontHomeTab(
    settings: StoreSettings,
    productsState: UiState<List<Product>>,
    wishlistIds: Set<String> = emptySet(),
    lang: AppLanguage,
    onSelectCategory: (ProductCategory?) -> Unit,
    onOpenSearchCatalog: (String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit = {},
    onQuickAdd: (Product) -> Unit
) {
    val context = LocalContext.current
    var homeSearch by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Flipkart-Style Delivery Location Strip + Instant Search Bar
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "डिलीवरी क्षेत्र: ${settings.deliveryCoverageHi}"
                            } else {
                                "Deliver to: ${settings.deliveryCoverageEn}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = VerifiedGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "लाइव सिंक ●" else "LIVE SYNC ●",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VerifiedGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = homeSearch,
                        onValueChange = {
                            homeSearch = it
                            if (it.length >= 2) onOpenSearchCatalog(it)
                        },
                        placeholder = { Text(AppStrings.searchHint.get(lang)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }
        }

        // 2. Flipkart-Style Circular Category Icon Strip (All 10 Categories)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(ProductCategory.entries) { cat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCategory(cat) }
                            .padding(vertical = 4.dp)
                            .testTag("category_chip_${cat.name}")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.5.dp, AntiqueGold.copy(alpha = 0.65f)),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = cat.label(lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = cat.label(lang),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. Festive Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_hero_1793076838918),
                    contentDescription = "Storefront banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DeepSindoor.copy(alpha = 0.50f),
                                    DeepSindoor.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Surface(
                            color = AntiqueGold,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "कल्याणपुर स्पेशल ऑफ़र" else "FESTIVE & DAILY DEALS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) settings.storeNameHi else settings.storeNameEn,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                        val offerText = if (lang == AppLanguage.HINDI) settings.promoBannerHi else settings.promoBannerEn
                        if (offerText.isNotBlank()) {
                            Text(
                                text = offerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBF2DC),
                                maxLines = 2
                            )
                        }
                        Spacer(Modifier.height(10.dp))
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

        // 4. Live Catalog Sections (Top Deals, New Arrivals, All Live Products 2-Column Feed)
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
                                        "दुकानदार उत्पादों को जोड़ते ही वे यहाँ तुरंत लाइव दिखेंगे।"
                                    } else {
                                        "New items added by the shop owner appear here automatically in real time."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    val topDeals = products
                        .filter { it.discountPercent > 0 }
                        .sortedByDescending { it.discountPercent }
                        .take(6)
                    val newArrivals = products.take(6)

                    if (topDeals.isNotEmpty()) {
                        item {
                            ProductHorizontalSection(
                                title = if (lang == AppLanguage.HINDI) "🔥 सबसे बड़ी बचत (Top Deals)" else "🔥 Top Deals & Discounts",
                                items = topDeals,
                                wishlistIds = wishlistIds,
                                lang = lang,
                                onOpenProduct = onOpenProduct,
                                onToggleWishlist = onToggleWishlist,
                                onQuickAdd = onQuickAdd
                            )
                        }
                    }

                    item {
                        ProductHorizontalSection(
                            title = AppStrings.newArrivalsHeading.get(lang),
                            items = newArrivals,
                            wishlistIds = wishlistIds,
                            lang = lang,
                            onOpenProduct = onOpenProduct,
                            onToggleWishlist = onToggleWishlist,
                            onQuickAdd = onQuickAdd
                        )
                    }

                    item { JharokhaGoldDivider(modifier = Modifier.padding(horizontal = 24.dp)) }

                    // Full Flipkart-style 2-Column Live Store Feed right on Home!
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) {
                                    "सभी लाइव उत्पाद (${products.size})"
                                } else {
                                    "All Live Shop Products (${products.size})"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = AppStrings.viewProducts.get(lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clickable { onSelectCategory(null) }
                            )
                        }
                    }

                    val rows = products.chunked(2)
                    items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { prod ->
                                ProductGridCard(
                                    product = prod,
                                    lang = lang,
                                    isWishlisted = prod.id in wishlistIds,
                                    onToggleWishlist = { onToggleWishlist(prod) },
                                    onClick = { onOpenProduct(prod) },
                                    onQuickAdd = { onQuickAdd(prod) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
                }
            }
        }
    }
}

@Composable
private fun ProductHorizontalSection(
    title: String,
    items: List<Product>,
    wishlistIds: Set<String>,
    lang: AppLanguage,
    onOpenProduct: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
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
                    isWishlisted = prod.id in wishlistIds,
                    onToggleWishlist = { onToggleWishlist(prod) },
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
    wishlistIds: Set<String> = emptySet(),
    showWishlistOnly: Boolean = false,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    sortOption: SortOption,
    lang: AppLanguage,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onToggleShowWishlistOnly: (Boolean) -> Unit = {},
    onOpenProduct: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit = {},
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
                        selected = showWishlistOnly,
                        onClick = { onToggleShowWishlistOnly(!showWishlistOnly) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showWishlistOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (lang == AppLanguage.HINDI) "विशलिस्ट (${wishlistIds.size})" else "Wishlist (${wishlistIds.size})"
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == null && !showWishlistOnly,
                        onClick = {
                            onToggleShowWishlistOnly(false)
                            onSelectCategory(null)
                        },
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
                    val filtered = remember(productsState.data, q, selectedCategory, sortOption, showWishlistOnly, wishlistIds) {
                        productsState.data
                            .filter { p ->
                                (!showWishlistOnly || p.id in wishlistIds) &&
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
                                    SortOption.DISCOUNT_HIGH_LOW -> list.sortedByDescending { it.discountPercent }
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
                                    onToggleShowWishlistOnly(false)
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
                                    isWishlisted = prod.id in wishlistIds,
                                    onToggleWishlist = { onToggleWishlist(prod) },
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
    isWishlisted: Boolean = false,
    lang: AppLanguage,
    onBack: () -> Unit,
    onToggleWishlist: () -> Unit = {},
    onAddToCart: (Int, String, String) -> Unit,
    onBuyNow: (Int, String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var selectedSize by remember(product.id) { mutableStateOf(product.sizes.firstOrNull().orEmpty()) }
    var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull().orEmpty()) }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    val maxQty = product.stockCount.toInt().coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable Product Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = product.categoryEnum().label(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ShivamAssuredBadge(lang = lang, modifier = Modifier.padding(end = 12.dp))
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
                    isWishlisted = isWishlisted,
                    onToggleWishlist = onToggleWishlist,
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
                Text(
                    text = product.displayName(lang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Flipkart-style Special Price Block
                if (product.discountPercent > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalOffer,
                            contentDescription = null,
                            tint = VerifiedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) "विशेष दुकान मूल्य (Special Price)" else "Special Shop Price",
                            style = MaterialTheme.typography.labelLarge,
                            color = VerifiedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (product.discountPercent > 0) {
                        Text(
                            text = "↓${product.discountPercent}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = VerifiedGreen
                        )
                    }
                    if (product.originalPriceInr > product.sellingPriceInr) {
                        Text(
                            text = formatInr(product.originalPriceInr),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = formatInr(product.sellingPriceInr),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (product.isOutOfStock) {
                        AppStrings.outOfStock.get(lang)
                    } else {
                        "${AppStrings.inStock.get(lang)}: ${product.stockCount} pcs"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (product.isOutOfStock) MaterialTheme.colorScheme.error else VerifiedGreen
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
                    HorizontalDivider()
                    Text(
                        text = if (lang == AppLanguage.HINDI) "साइज़ चुनें (Select Size):" else "Select Size:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                        text = if (lang == AppLanguage.HINDI) "रंग चुनें (Select Colour):" else "Select Colour:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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

                if (!product.isOutOfStock) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (lang == AppLanguage.HINDI) "मात्रा (Qty):" else "Quantity:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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

                // Flipkart-style Delivery & Store Promise Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (lang == AppLanguage.HINDI) "✔ कैश ऑन डिलीवरी (COD) उपलब्ध" else "✔ Cash on Delivery (COD) Available",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "📍 बड़ा बाज़ार, कल्याणपुर दुकान पर आसान साइज़ व रंग जांच सुविधा"
                            } else {
                                "📍 Easy size & colour check at Bada Bazar, Kalyanpur store"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
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

        // Sticky Flipkart-Style Dual Action Bar at Bottom
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                OutlinedButton(
                    onClick = { onAddToCart(quantity, selectedSize, selectedColor) },
                    enabled = !product.isOutOfStock,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_to_cart_button")
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.addToCart.get(lang), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onBuyNow(quantity, selectedSize, selectedColor) },
                    enabled = !product.isOutOfStock,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("buy_now_button")
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.buyNow.get(lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
