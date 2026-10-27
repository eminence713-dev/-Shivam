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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
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
    onToggleLanguage: () -> Unit,
    onOpenCart: () -> Unit,
    onSignOut: () -> Unit,
    onOwnerAccessCheck: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreLogoCrest(size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) storeNameHi else "श्री $storeNameEn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) "बड़ा बाज़ार, कल्याणपुर, मोतिहारी" else "Bada Bazar, Kalyanpur, Motihari",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            FilledTonalButton(
                onClick = onToggleLanguage,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("language_switch_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = stringResource(R.string.cd_language_switch),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (lang == AppLanguage.ENGLISH) "हिंदी" else "EN",
                    style = MaterialTheme.typography.labelLarge
                )
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
            if (onOwnerAccessCheck != null) {
                IconButton(
                    onClick = onOwnerAccessCheck,
                    modifier = Modifier.testTag("owner_access_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = if (lang == AppLanguage.HINDI) "मालिक डैशबोर्ड एक्सेस जानकारी" else "Owner Dashboard Access Info",
                        tint = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Sign out",
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

        // Demo Sample badge — ensures prototype items are never misrepresented as live stock
        if (product.isDemo) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(bottomEnd = 10.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = AppStrings.demoBadge.get(lang),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Stock badge
        val (stockText, stockBg) = when {
            product.isOutOfStock -> AppStrings.outOfStock.get(lang) to MaterialTheme.colorScheme.error
            product.isLowStock -> "${AppStrings.lowStock.get(lang)} (${product.stockCount})" to WarningAmber
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
fun ProductGridCard(
    product: Product,
    lang: AppLanguage,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ProductVisualImage(
                product = product,
                lang = lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.categoryEnum().label(lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
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
                    Column {
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
