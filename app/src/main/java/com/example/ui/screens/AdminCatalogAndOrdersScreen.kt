package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.InventoryMovement
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductCost
import com.example.data.model.StoreSettings
import com.example.ui.components.ProductVisualImage
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.SalesAnalyticsSummary
import com.example.util.AppStrings
import com.example.util.ImageCompressor
import com.example.util.InvoicePrintHelper
import com.example.util.WhatsAppHelper
import com.example.util.formatInr
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminSection(val titleEn: String, val titleHi: String) {
    OVERVIEW("Owner Dashboard", "मालिक डैशबोर्ड"),
    PRODUCTS("Products & Stock", "उत्पाद और स्टॉक"),
    ORDERS("Online Orders", "ऑनलाइन ऑर्डर"),
    POS_COUNTER("Counter Sale & Bill", "दुकान काउंटर बिल"),
    SALES_PROFIT("Sales & Profit Reports", "बिक्री और मुनाफा रिपोर्ट"),
    EXPENSES("Shop Expenses", "दुकान खर्च"),
    SETTINGS("Store Settings & UPI", "स्टोर सेटिंग्स और UPI"),
    SETUP_GUIDE("Setup & Hosting Guide", "सेटअप और सुरक्षा गाइड");

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.HINDI) titleHi else titleEn
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOverviewSection(
    products: List<Product>,
    orders: List<CustomerOrder>,
    storeSettings: StoreSettings,
    todaySummary: SalesAnalyticsSummary,
    lang: AppLanguage,
    isBusy: Boolean,
    onNavigate: (AdminSection) -> Unit,
    onHideAllDemos: () -> Unit,
    onSeedDemos: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val langPrefs = remember(context) { com.example.data.repository.LanguagePreferencesRepository(context.applicationContext) }
    val savedShareUrl = langPrefs.apkDownloadUrlFlow.collectAsStateWithLifecycle(initialValue = "").value
    var linkDraft by remember(savedShareUrl) { mutableStateOf(savedShareUrl) }

    val pendingOrders = orders.count { it.statusEnum() == OrderStatus.PENDING }
    val lowOrOutProducts = products.count { it.stockCount <= it.lowStockThreshold }
    val publishedDemoCount = products.count { it.isDemo && it.isPublished }
    val publishedProducts = remember(products) { products.filter { it.isPublished && !it.isOutOfStock } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KPI Summary Row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminStatCard(
                label = if (lang == AppLanguage.HINDI) "आज की कुल बिक्री" else "Today's Net Sales",
                value = formatInr(todaySummary.netCollectedAndActiveRevenueInr),
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                label = if (lang == AppLanguage.HINDI) "नए पेंडिंग ऑर्डर" else "Pending Orders",
                value = "$pendingOrders",
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                label = if (lang == AppLanguage.HINDI) "कम / समाप्त स्टॉक" else "Low / Out Stock",
                value = "$lowOrOutProducts",
                modifier = Modifier.weight(1f)
            )
        }

        // ₹0 Free WhatsApp Customer Share & QR Card (Zero Play Store Hassle)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, WhatsAppGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "ग्राहकों को मुफ़्त भेजें (₹0 खर्च • बिना Play Store)"
                    } else {
                        "Free Customer Sharing (₹0 Cost • No Play Store Needed)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "अपना मुफ़्त APK लिंक (Google Drive) या शॉप लिंक नीचे सेव करें और 1-क्लिक में ग्राहकों को WhatsApp पर दुकान या आज की रेट-लिस्ट भेजें:"
                    } else {
                        "Save your free Google Drive APK link or shop URL below to generate a counter QR code and share your shop or live price list on WhatsApp in 1 tap:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = linkDraft,
                        onValueChange = { linkDraft = it },
                        label = {
                            Text(
                                if (lang == AppLanguage.HINDI) "मुफ़्त शॉप / Google Drive APK लिंक (ऐच्छिक)"
                                else "Free Shop / Google Drive APK Link (Optional)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("free_share_link_input")
                    )
                    Button(
                        onClick = { scope.launch { langPrefs.setApkDownloadUrl(linkDraft) } },
                        modifier = Modifier.height(52.dp).testTag("save_share_link_button")
                    ) {
                        Text(if (lang == AppLanguage.HINDI) "सेव" else "Save")
                    }
                }

                if (savedShareUrl.startsWith("http")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        com.example.ui.components.QrMatrixView(payload = savedShareUrl, qrSize = 112.dp)
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "काउंटर QR तैयार है! ग्राहक अपने फोन कैमरे से स्कैन करके सीधे आपकी दुकान खोल/डाउनलोड कर सकते हैं।"
                            } else {
                                "Counter QR Ready! Walk-in customers can scan this with their camera to open/download your shop."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val linkLine = if (savedShareUrl.isNotBlank()) "\n📲 दुकान एक क्लिक में खोलें: $savedShareUrl" else ""
                            val inviteText = buildString {
                                appendLine("🛍️ *श्री शिवम रेडीमेड & रिया फैंसी शूज़ मार्ट*")
                                appendLine("📍 बड़ा बाज़ार, कल्याणपुर, मोतिहारी")
                                appendLine("✨ साड़ी, कुर्ती, फ्रॉक, शर्ट, जींस और फैंसी जूते-चप्पल अब घर बैठे देखें और ऑर्डर करें!")
                                if (linkLine.isNotBlank()) appendLine(linkLine)
                                appendLine("📞 WhatsApp ऑर्डर: ${storeSettings.whatsappNumber}")
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, inviteText)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Shop on WhatsApp")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier.testTag("whatsapp_share_shop_invite_button")
                    ) {
                        Text(if (lang == AppLanguage.HINDI) "WhatsApp पर दुकान शेयर करें" else "Share Shop Invite on WhatsApp")
                    }

                    OutlinedButton(
                        onClick = {
                            val topItems = publishedProducts.take(12)
                            val catalogMsg = buildString {
                                appendLine("🛍️ *आज का नया कलेक्शन — श्री शिवम रेडीमेड & रिया फैंसी शूज़ मार्ट*")
                                appendLine("📍 बड़ा बाज़ार, कल्याणपुर • 📞 ${storeSettings.whatsappNumber}")
                                appendLine("────────────────")
                                if (topItems.isEmpty()) {
                                    appendLine("साड़ी • कुर्ती • शर्ट • जींस • जूते-चप्पल उपलब्ध हैं।")
                                } else {
                                    topItems.forEachIndexed { idx, p ->
                                        appendLine("${idx + 1}. ${p.displayName(lang)} — ₹${p.sellingPriceInr}")
                                    }
                                }
                                if (savedShareUrl.isNotBlank()) {
                                    appendLine("────────────────")
                                    appendLine("📲 पूरी दुकान फोटो सहित खोलें: $savedShareUrl")
                                }
                                appendLine("✅ ऑर्डर करने के लिए आइटम का नाम लिखकर इसी नंबर पर WhatsApp करें!")
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, catalogMsg)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Catalogue on WhatsApp")
                            )
                        },
                        border = BorderStroke(1.5.dp, WhatsAppGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                        modifier = Modifier.testTag("whatsapp_share_catalog_button")
                    ) {
                        Text(if (lang == AppLanguage.HINDI) "WhatsApp पर रेट-लिस्ट भेजें" else "Share Price List on WhatsApp")
                    }
                }
            }
        }

        // Demo Items Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            border = BorderStroke(1.dp, AntiqueGold)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) "डेमो सैंपल उत्पाद नियंत्रण" else "Demo Catalogue Controls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (publishedDemoCount > 0) {
                        "$publishedDemoCount clearly labelled [Demo Sample] items are currently visible for testing. Hide them in one tap once real shop photos are added."
                    } else {
                        "No demo items are currently published on the customer storefront."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (publishedDemoCount > 0) {
                        Button(
                            onClick = onHideAllDemos,
                            enabled = !isBusy,
                            modifier = Modifier.testTag("hide_all_demos_button")
                        ) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (lang == AppLanguage.HINDI) "सभी डेमो छुपाएँ" else "Hide All Demo Items")
                        }
                    }
                    OutlinedButton(
                        onClick = onSeedDemos,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("seed_demos_button")
                    ) {
                        Text(if (lang == AppLanguage.HINDI) "डेमो आइटम लोड करें" else "Reload 10 Demo Samples")
                    }
                }
            }
        }

        Text(
            text = if (lang == AppLanguage.HINDI) "त्वरित दुकान संचालन" else "Store Operations",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        val actions = listOf(
            Triple(AdminSection.PRODUCTS, AppStrings.manageProducts.get(lang), Icons.Filled.Inventory2),
            Triple(AdminSection.ORDERS, "${AppStrings.viewOrders.get(lang)} ($pendingOrders)", Icons.AutoMirrored.Filled.ReceiptLong),
            Triple(AdminSection.POS_COUNTER, AppStrings.offlineSale.get(lang), Icons.Filled.PointOfSale),
            Triple(AdminSection.SALES_PROFIT, AppStrings.dailySales.get(lang), Icons.Filled.BarChart),
            Triple(AdminSection.EXPENSES, AppStrings.expenses.get(lang), Icons.Filled.AccountBalanceWallet),
            Triple(AdminSection.SETTINGS, AppStrings.storeSettings.get(lang), Icons.Filled.Settings),
            Triple(AdminSection.SETUP_GUIDE, AppStrings.setupGuide.get(lang), Icons.AutoMirrored.Filled.MenuBook)
        )

        actions.forEach { (section, label, icon) ->
            AdminActionTile(
                title = label,
                subtitle = section.title(if (lang == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH),
                icon = icon,
                onClick = { onNavigate(section) },
                testTag = "admin_nav_${section.name.lowercase()}"
            )
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminProductsSection(
    products: List<Product>,
    productCosts: Map<String, ProductCost>,
    movements: List<InventoryMovement>,
    lang: AppLanguage,
    isBusy: Boolean,
    onSaveProduct: (Product, Long, String, () -> Unit) -> Unit,
    onTogglePublished: (String, Boolean) -> Unit,
    onUpdateStock: (Product, Long) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var showAuditLog by remember { mutableStateOf(false) }
    var categoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var search by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { isCreatingNew = true },
                modifier = Modifier.weight(1f).testTag("admin_add_product_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.HINDI) "नया उत्पाद जोड़ें" else "Add Product")
            }
            OutlinedButton(
                onClick = { showAuditLog = !showAuditLog },
                modifier = Modifier.testTag("toggle_stock_history_button")
            ) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (showAuditLog) "Products" else "Stock Log")
            }
        }

        Spacer(Modifier.height(10.dp))

        if (showAuditLog) {
            Text(
                text = "Inventory Movement Audit Log (${movements.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(movements, key = { it.id }) { mov ->
                    val dt = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(mov.createdAtDate())
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mov.productName, fontWeight = FontWeight.SemiBold)
                                Text("${mov.reason} • Ref: ${mov.referenceId.take(14)}", style = MaterialTheme.typography.bodySmall)
                                Text(dt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${if (mov.delta >= 0) "+" else ""}${mov.delta} (${mov.previousStock}→${mov.newStock})",
                                fontWeight = FontWeight.Bold,
                                color = if (mov.delta >= 0) VerifiedGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search inventory...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("All (${products.size})") }
                    )
                }
                items(ProductCategory.entries) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                        label = { Text(cat.label(lang)) }
                    )
                }
            }

            val filtered = products.filter { p ->
                (categoryFilter == null || p.categoryEnum() == categoryFilter) &&
                    (search.isBlank() || p.nameEn.contains(search.trim(), ignoreCase = true) || p.nameHi.contains(search.trim(), ignoreCase = true))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { prod ->
                    val cost = productCosts[prod.id]?.costPriceInr ?: 0L
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProductVisualImage(
                                    product = prod,
                                    lang = lang,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.displayName(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Sell: ${formatInr(prod.sellingPriceInr)} | Cost: ${if (cost > 0) formatInr(cost) else "Not set"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${prod.categoryEnum().label(lang)} • Sold: ${prod.soldCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Switch(
                                        checked = prod.isPublished,
                                        onCheckedChange = { onTogglePublished(prod.id, it) }
                                    )
                                    Text(
                                        text = if (prod.isPublished) "Live" else "Hidden",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Stock:", style = MaterialTheme.typography.labelLarge)
                                    IconButton(
                                        onClick = { if (prod.stockCount > 0) onUpdateStock(prod, prod.stockCount - 1) }
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease stock")
                                    }
                                    Text(
                                        text = "${prod.stockCount}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (prod.stockCount <= prod.lowStockThreshold) WarningAmber else VerifiedGreen
                                    )
                                    IconButton(
                                        onClick = { onUpdateStock(prod, prod.stockCount + 1) }
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase stock")
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingProduct = prod }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit product", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deletingProduct = prod }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreatingNew || editingProduct != null) {
        val initial = editingProduct ?: Product(
            stockCount = 10L,
            isPublished = true,
            isFeatured = true
        )
        val initialCost = editingProduct?.let { productCosts[it.id]?.costPriceInr } ?: 0L
        val initialNote = editingProduct?.let { productCosts[it.id]?.supplierNote }.orEmpty()
        ProductEditorDialog(
            initialProduct = initial,
            initialCostInr = initialCost,
            initialSupplierNote = initialNote,
            isBusy = isBusy,
            onDismiss = {
                isCreatingNew = false
                editingProduct = null
            },
            onSave = { updatedProd, costInr, note ->
                onSaveProduct(updatedProd, costInr, note) {
                    isCreatingNew = false
                    editingProduct = null
                }
            }
        )
    }

    deletingProduct?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Product Permanently?") },
            text = { Text("Delete '${target.nameEn}' from inventory? To temporarily remove it from the shop without losing history, switch Live to Hidden instead.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target.id)
                        deletingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductEditorDialog(
    initialProduct: Product,
    initialCostInr: Long,
    initialSupplierNote: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product, Long, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameEn by remember { mutableStateOf(initialProduct.nameEn) }
    var nameHi by remember { mutableStateOf(initialProduct.nameHi) }
    var category by remember { mutableStateOf(initialProduct.categoryEnum()) }
    var descEn by remember { mutableStateOf(initialProduct.descriptionEn) }
    var descHi by remember { mutableStateOf(initialProduct.descriptionHi) }
    var sellingPrice by remember { mutableStateOf(if (initialProduct.sellingPriceInr > 0) "${initialProduct.sellingPriceInr}" else "") }
    var originalPrice by remember { mutableStateOf(if (initialProduct.originalPriceInr > 0) "${initialProduct.originalPriceInr}" else "") }
    var costPrice by remember { mutableStateOf(if (initialCostInr > 0) "$initialCostInr" else "") }
    var supplierNote by remember { mutableStateOf(initialSupplierNote) }
    var stockCount by remember { mutableStateOf("${initialProduct.stockCount}") }
    var lowThreshold by remember { mutableStateOf("${initialProduct.lowStockThreshold}") }
    var sizesCsv by remember { mutableStateOf(initialProduct.sizes.joinToString(", ")) }
    var colorsCsv by remember { mutableStateOf(initialProduct.colors.joinToString(", ")) }
    var imageUrl by remember { mutableStateOf(initialProduct.imageUrl) }
    var imageBase64 by remember { mutableStateOf(initialProduct.imageBase64) }
    var isPublished by remember { mutableStateOf(initialProduct.isPublished) }
    var isFeatured by remember { mutableStateOf(initialProduct.isFeatured) }
    var isDemo by remember { mutableStateOf(initialProduct.isDemo) }
    var formError by remember { mutableStateOf<String?>(null) }
    var photoStatus by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoStatus = "Compressing photo..."
            scope.launch {
                ImageCompressor.compressUriToBase64(context, uri).fold(
                    onSuccess = { b64 ->
                        imageBase64 = b64
                        photoStatus = "Shop photo ready (${b64.length / 1024} KB compressed)."
                    },
                    onFailure = { e ->
                        photoStatus = "Could not process photo: ${e.localizedMessage}"
                    }
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialProduct.id.isBlank()) "Add Store Product" else "Edit Product",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("pick_product_photo_button")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageBase64.isNotBlank()) "Replace Uploaded Shop Photo" else "Pick Shop Photo from Phone")
                }

                if (photoStatus != null) {
                    Text(photoStatus.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "Photo sync: Selected photos are compressed (~140 KB JPEG) for fast live loading across all customer phones.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Or HTTPS Image URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Category:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelEn) }
                        )
                    }
                }

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Product Name (English) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameHi,
                    onValueChange = { nameHi = it },
                    label = { Text("उत्पाद का नाम (Hindi - Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Selling ₹ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = originalPrice,
                        onValueChange = { originalPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("MRP ₹ (Opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Purchase Cost ₹ (Private)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockCount,
                        onValueChange = { stockCount = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = lowThreshold,
                    onValueChange = { lowThreshold = it.filter { c -> c.isDigit() } },
                    label = { Text("Low-Stock Alert Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sizesCsv,
                    onValueChange = { sizesCsv = it },
                    label = { Text("Sizes (comma separated, e.g. M, L, XL or 6, 7, 8)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorsCsv,
                    onValueChange = { colorsCsv = it },
                    label = { Text("Colours (comma separated, e.g. Maroon, Gold)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description (English)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplierNote,
                    onValueChange = { supplierNote = it },
                    label = { Text("Private Supplier / Wholesale Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublished, onCheckedChange = { isPublished = it })
                    Text("Published Live")
                    Spacer(Modifier.width(12.dp))
                    Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    Text("Featured")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDemo, onCheckedChange = { isDemo = it })
                    Text("Mark as [Demo Sample] item")
                }

                if (formError != null) {
                    Text(formError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !isBusy,
                        onClick = {
                            val sell = sellingPrice.toLongOrNull() ?: 0L
                            val finalNameEn = nameEn.trim().ifEmpty { nameHi.trim() }
                            if (finalNameEn.isEmpty()) {
                                formError = "कृपया उत्पाद का नाम लिखें (Please enter product name)."
                                return@Button
                            }
                            if (sell <= 0L) {
                                formError = "कृपया सही बिक्री मूल्य लिखें (Enter selling price > ₹0)."
                                return@Button
                            }
                            val prod = initialProduct.copy(
                                nameEn = finalNameEn,
                                nameHi = nameHi.trim(),
                                category = category.name,
                                descriptionEn = descEn.trim(),
                                descriptionHi = descHi.trim(),
                                sellingPriceInr = sell,
                                originalPriceInr = originalPrice.toLongOrNull() ?: 0L,
                                stockCount = stockCount.toLongOrNull() ?: 0L,
                                lowStockThreshold = lowThreshold.toLongOrNull() ?: 3L,
                                sizes = sizesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                colors = colorsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                imageUrl = imageUrl.trim(),
                                imageBase64 = imageBase64,
                                isPublished = isPublished,
                                isFeatured = isFeatured,
                                isDemo = isDemo
                            )
                            onSave(prod, costPrice.toLongOrNull() ?: 0L, supplierNote.trim())
                        }
                    ) {
                        if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Save Product")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOrdersSection(
    orders: List<CustomerOrder>,
    settings: StoreSettings,
    lang: AppLanguage,
    onUpdateOrder: (String, OrderStatus, PaymentStatus, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var cancellingOrder by remember { mutableStateOf<CustomerOrder?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All (${orders.size})") }
                )
            }
            items(OrderStatus.entries) { st ->
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { statusFilter = if (statusFilter == st) null else st },
                    label = { Text(st.labelEn) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        val visible = orders.filter { statusFilter == null || it.statusEnum() == statusFilter }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders in this filter.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.id }) { order ->
                    val dt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(order.createdAtDate())
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.orderReference, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(dt, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${order.customerName} • Ph: ${order.customerPhone}", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Address: ${order.deliveryAddress}${if (order.landmark.isNotBlank()) " (${order.landmark})" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(order.itemSummary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Total: ${formatInr(order.totalPayableInr)} (Items ${formatInr(order.subtotalInr)} + Del ${formatInr(order.deliveryChargeInr)}) • ${order.paymentMethod}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider()
                            Text("Order Status:", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OrderStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = order.statusEnum() == st,
                                        onClick = {
                                            if (st == OrderStatus.CANCELLED && order.statusEnum() != OrderStatus.CANCELLED) {
                                                cancellingOrder = order
                                                cancelReason = ""
                                            } else if (st != order.statusEnum()) {
                                                onUpdateOrder(order.id, st, order.paymentStatusEnum(), "", true)
                                            }
                                        },
                                        label = { Text(st.labelEn) }
                                    )
                                }
                            }

                            Text("Payment Verification (Manual Owner Check):", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PaymentStatus.entries.forEach { pst ->
                                    FilterChip(
                                        selected = order.paymentStatusEnum() == pst,
                                        onClick = {
                                            if (pst != order.paymentStatusEnum()) {
                                                onUpdateOrder(order.id, order.statusEnum(), pst, order.cancellationReason, false)
                                            }
                                        },
                                        label = { Text(pst.labelEn) }
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        InvoicePrintHelper.printInvoice(context, settings, InvoicePrintHelper.fromCustomerOrder(order))
                                    }
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(AppStrings.printBill.get(lang))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val msg = "नमस्ते ${order.customerName}, Shri Shivam Readymade से आपके ऑर्डर ${order.orderReference} (${order.statusEnum().labelEn}) के संबंध में संदेश।"
                                        WhatsAppHelper.openWhatsAppChat(context, order.customerPhone, msg)
                                    },
                                    border = BorderStroke(1.dp, WhatsAppGreen),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen)
                                ) {
                                    Text("WhatsApp Customer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    cancellingOrder?.let { target ->
        AlertDialog(
            onDismissRequest = { cancellingOrder = null },
            title = { Text("Cancel Order & Restore Stock?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cancelling ${target.orderReference} will atomically restore all deducted item quantities back to shop inventory.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateOrder(
                            target.id,
                            OrderStatus.CANCELLED,
                            target.paymentStatusEnum(),
                            cancelReason.ifBlank { "Cancelled by store owner" },
                            false
                        )
                        cancellingOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel & Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancellingOrder = null }) { Text("Keep Order") }
            }
        )
    }
}
