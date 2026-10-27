package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WarningAmber
import com.example.util.AppStrings
import com.example.util.ImageCompressor
import com.example.util.QrCodeGenerator
import com.example.util.formatInr

@Composable
fun StoreLogoCrest(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, AntiqueGold),
        color = MaterialTheme.colorScheme.primary
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_launcher_crest_1793076836756),
            contentDescription = stringResource(R.string.cd_store_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun JharokhaGoldDivider(modifier: Modifier = Modifier) {
    val gold = AntiqueGold
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val midY = size.height / 2f
        val centerX = size.width / 2f
        drawLine(
            color = gold.copy(alpha = 0.45f),
            start = Offset(0f, midY),
            end = Offset(centerX - 18.dp.toPx(), midY),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(color = gold, radius = 3.5.dp.toPx(), center = Offset(centerX, midY))
        drawCircle(color = gold.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = Offset(centerX - 10.dp.toPx(), midY))
        drawCircle(color = gold.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = Offset(centerX + 10.dp.toPx(), midY))
        drawLine(
            color = gold.copy(alpha = 0.45f),
            start = Offset(centerX + 18.dp.toPx(), midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun StoreTopBar(
    storeNameEn: String,
    storeNameHi: String,
    lang: AppLanguage,
    cartCount: Int,
    wishlistCount: Int = 0,
    showOwnerButton: Boolean = false,
    isOwnerUnlocked: Boolean = false,
    pendingOwnerOrdersCount: Int = 0,
    onToggleLanguage: () -> Unit,
    onOpenWishlist: () -> Unit = {},
    onOpenCart: () -> Unit,
    onOwnerClick: () -> Unit,
    onAccountClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreLogoCrest(size = 40.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) storeNameHi else "श्री $storeNameEn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) "कल्याणपुर, मोतिहारी • लाइव स्टोर" else "Kalyanpur, Motihari • Live Store",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onToggleLanguage,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("language_switch_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = stringResource(R.string.cd_language_switch),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = if (lang == AppLanguage.HINDI) "EN" else "हिंदी",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(
                onClick = onOpenWishlist,
                modifier = Modifier.testTag("top_wishlist_button")
            ) {
                BadgedBox(
                    badge = {
                        if (wishlistCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Text("$wishlistCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (wishlistCount > 0) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            IconButton(
                onClick = onOpenCart,
                modifier = Modifier.testTag("top_cart_button")
            ) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Text("$cartCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = stringResource(R.string.cd_shopping_cart),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            if (showOwnerButton) {
                IconButton(
                    onClick = onOwnerClick,
                    modifier = Modifier.testTag("owner_access_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (pendingOwnerOrdersCount > 0) {
                                Badge { Text("$pendingOwnerOrdersCount") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isOwnerUnlocked) Icons.Outlined.AdminPanelSettings else Icons.Outlined.Lock,
                            contentDescription = stringResource(R.string.cd_owner_dashboard),
                            tint = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
            }
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier.testTag("top_account_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Account & Sign Out",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

fun categoryFallbackDrawable(category: ProductCategory): Int = when (category) {
    ProductCategory.SAREES,
    ProductCategory.KURTIS,
    ProductCategory.LADIES_FROCKS -> R.drawable.img_ethnic_collection_1793076839555
    ProductCategory.SHIRTS,
    ProductCategory.TSHIRTS,
    ProductCategory.JEANS,
    ProductCategory.TROUSERS,
    ProductCategory.SHOES,
    ProductCategory.SANDALS,
    ProductCategory.SLIPPERS -> R.drawable.img_footwear_menswear_1793076840531
}

@Composable
fun ProductVisualImage(
    product: Product,
    lang: AppLanguage,
    isWishlisted: Boolean = false,
    onToggleWishlist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(product.imageBase64) {
        ImageCompressor.decodeBase64Cached(product.imageBase64)
    }
    val fallbackRes = remember(product.category) {
        categoryFallbackDrawable(product.categoryEnum())
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = product.displayName(lang),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            product.imageUrl.startsWith("https://") -> {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.displayName(lang),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(fallbackRes),
                    error = painterResource(fallbackRes),
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = product.displayName(lang),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top-Left Flipkart-style % OFF Discount Badge
        if (product.discountPercent > 0) {
            Surface(
                color = VerifiedGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(bottomEnd = 10.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "${product.discountPercent}% OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Top-Right Wishlist Heart Button
        if (onToggleWishlist != null) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.90f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleWishlist)
                    .testTag("wishlist_toggle_${product.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                        tint = if (isWishlisted) Color(0xFFD32F2F) else Color(0xFF5C4B4E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom-End Stock Urgency Badge
        val (stockText, stockBg) = when {
            product.isOutOfStock -> AppStrings.outOfStock.get(lang) to MaterialTheme.colorScheme.error
            product.isLowStock -> {
                val label = if (lang == AppLanguage.HINDI) "केवल ${product.stockCount} बचे!" else "Only ${product.stockCount} left!"
                label to WarningAmber
            }
            else -> AppStrings.inStock.get(lang) to VerifiedGreen
        }
        Surface(
            color = stockBg.copy(alpha = 0.92f),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 10.dp),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Text(
                text = stockText,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun ShivamAssuredBadge(lang: AppLanguage, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFFBF2DC),
        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (lang == AppLanguage.HINDI) "शिवम एश्योर्ड" else "Shivam Assured",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    lang: AppLanguage,
    isWishlisted: Boolean = false,
    onToggleWishlist: (() -> Unit)? = null,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ProductVisualImage(
                product = product,
                lang = lang,
                isWishlisted = isWishlisted,
                onToggleWishlist = onToggleWishlist,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = product.categoryEnum().label(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ShivamAssuredBadge(lang = lang)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.displayName(lang),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatInr(product.sellingPriceInr),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (product.originalPriceInr > product.sellingPriceInr) {
                                Text(
                                    text = formatInr(product.originalPriceInr),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                        if (product.savingsInr > 0L) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) {
                                    "${formatInr(product.savingsInr)} की बचत"
                                } else {
                                    "Save ${formatInr(product.savingsInr)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = VerifiedGreen
                            )
                        } else {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "COD उपलब्ध" else "COD Available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (product.isOutOfStock) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !product.isOutOfStock, onClick = onQuickAdd)
                            .testTag("quick_add_${product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddShoppingCart,
                                contentDescription = AppStrings.addToCart.get(lang),
                                tint = if (product.isOutOfStock) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrMatrixView(
    payload: String,
    qrSize: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { QrCodeGenerator.encode(payload) }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, AntiqueGold),
        modifier = modifier.size(qrSize)
    ) {
        if (matrix == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                Text("UPI payload too long for QR preview", style = MaterialTheme.typography.bodySmall, color = Color.Black)
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                val n = matrix.size
                val cell = size.minDimension / n.toFloat()
                for (r in 0 until n) {
                    for (c in 0 until n) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell + 0.4f, cell + 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
