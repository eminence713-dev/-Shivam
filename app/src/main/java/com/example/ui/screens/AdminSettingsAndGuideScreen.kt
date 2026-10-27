package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.Expense
import com.example.data.model.InventoryMovement
import com.example.data.model.OfflineSale
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.ui.components.QrMatrixView
import com.example.ui.theme.AntiqueGold
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ReportDateRange
import com.example.ui.viewmodel.UiState
import com.example.util.UpiPaymentHelper

@Composable
fun AdminDashboardHub(
    adminViewModel: AdminViewModel,
    storeSettings: StoreSettings,
    lang: AppLanguage,
    onExitToStorefront: () -> Unit
) {
    var currentSection by rememberSaveable { mutableStateOf(AdminSection.OVERVIEW) }
    var selectedRange by rememberSaveable { mutableStateOf(ReportDateRange.TODAY) }

    BackHandler {
        if (currentSection != AdminSection.OVERVIEW) {
            currentSection = AdminSection.OVERVIEW
        } else {
            onExitToStorefront()
        }
    }

    val allProductsState by adminViewModel.allProductsState.collectAsStateWithLifecycle()
    val productCosts by adminViewModel.productCosts.collectAsStateWithLifecycle()
    val allOrdersState by adminViewModel.allOrdersState.collectAsStateWithLifecycle()
    val offlineSalesState by adminViewModel.offlineSalesState.collectAsStateWithLifecycle()
    val expensesState by adminViewModel.expensesState.collectAsStateWithLifecycle()
    val movementsState by adminViewModel.movementsState.collectAsStateWithLifecycle()
    val isBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
    val banner by adminViewModel.statusBanner.collectAsStateWithLifecycle()
    val lastOfflineSale by adminViewModel.lastRecordedOfflineSale.collectAsStateWithLifecycle()

    val products = (allProductsState as? UiState.Success<List<Product>>)?.data.orEmpty()
    val orders = (allOrdersState as? UiState.Success<List<CustomerOrder>>)?.data.orEmpty()
    val offlineSales = (offlineSalesState as? UiState.Success<List<OfflineSale>>)?.data.orEmpty()
    val expenses = (expensesState as? UiState.Success<List<Expense>>)?.data.orEmpty()
    val movements = (movementsState as? UiState.Success<List<InventoryMovement>>)?.data.orEmpty()

    val todaySummary = remember(orders, offlineSales, expenses, productCosts) {
        adminViewModel.computeAnalytics(ReportDateRange.TODAY, orders, offlineSales, expenses, productCosts)
    }
    val selectedSummary = remember(selectedRange, orders, offlineSales, expenses, productCosts) {
        adminViewModel.computeAnalytics(selectedRange, orders, offlineSales, expenses, productCosts)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Admin Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentSection != AdminSection.OVERVIEW) currentSection = AdminSection.OVERVIEW
                        else onExitToStorefront()
                    },
                    modifier = Modifier.testTag("admin_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSection.title(lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Owner-Verified Admin Access • Raju Kumar Gupta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                androidx.compose.material3.FilledTonalButton(
                    onClick = onExitToStorefront,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("admin_preview_shop_button")
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "दुकान देखें" else "View Shop",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
            }
        }

        if (banner != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(banner.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { adminViewModel.clearBanner() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss notification")
                    }
                }
            }
        }

        if (allProductsState is UiState.Loading && products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        when (currentSection) {
            AdminSection.OVERVIEW -> AdminOverviewSection(
                products = products,
                orders = orders,
                storeSettings = storeSettings,
                todaySummary = todaySummary,
                lang = lang,
                isBusy = isBusy,
                onNavigate = { currentSection = it },
                onHideAllDemos = { adminViewModel.hideAllDemoProducts(products) },
                onSeedDemos = { adminViewModel.seedDemoProducts() }
            )
            AdminSection.PRODUCTS -> AdminProductsSection(
                products = products,
                productCosts = productCosts,
                movements = movements,
                lang = lang,
                isBusy = isBusy,
                onSaveProduct = { prod, cost, note, onDone -> adminViewModel.saveProduct(prod, cost, note, onDone) },
                onTogglePublished = { id, pub -> adminViewModel.setPublished(id, pub) },
                onUpdateStock = { prod, next -> adminViewModel.updateStockManually(prod, next) },
                onDeleteProduct = { id -> adminViewModel.deleteProduct(id) }
            )
            AdminSection.ORDERS -> AdminOrdersSection(
                orders = orders,
                settings = storeSettings,
                lang = lang,
                onUpdateOrder = { id, st, pst, reason, inv ->
                    adminViewModel.updateOrder(id, st, pst, reason, inv)
                }
            )
            AdminSection.POS_COUNTER -> AdminPosCounterSection(
                products = products,
                offlineSales = offlineSales,
                lastSavedSale = lastOfflineSale,
                settings = storeSettings,
                lang = lang,
                isBusy = isBusy,
                onRecordSale = { cName, cPhone, items, disc, mode ->
                    adminViewModel.recordOfflineSale(cName, cPhone, items, disc, mode)
                },
                onDismissLastSale = { adminViewModel.clearLastOfflineSale() }
            )
            AdminSection.SALES_PROFIT -> AdminSalesAndProfitSection(
                selectedRange = selectedRange,
                onSelectRange = { selectedRange = it },
                summary = selectedSummary,
                products = products,
                orders = orders,
                offlineSales = offlineSales,
                lang = lang
            )
            AdminSection.EXPENSES -> AdminExpensesSection(
                expenses = expenses,
                lang = lang,
                isBusy = isBusy,
                onAddExpense = { t, c, a, n, done -> adminViewModel.addExpense(t, c, a, n, done) },
                onDeleteExpense = { id -> adminViewModel.deleteExpense(id) }
            )
            AdminSection.SETTINGS -> AdminStoreSettingsSection(
                settings = storeSettings,
                isBusy = isBusy,
                onSave = { adminViewModel.saveSettings(it) }
            )
            AdminSection.SETUP_GUIDE -> AdminSetupAndGuideSection()
        }
    }
}

@Composable
fun AdminStoreSettingsSection(
    settings: StoreSettings,
    isBusy: Boolean,
    onSave: (StoreSettings) -> Unit
) {
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var whatsapp by remember(settings) { mutableStateOf(settings.whatsappNumber) }
    var addressEn by remember(settings) { mutableStateOf(settings.addressEn) }
    var addressHi by remember(settings) { mutableStateOf(settings.addressHi) }
    var promoEn by remember(settings) { mutableStateOf(settings.promoBannerEn) }
    var promoHi by remember(settings) { mutableStateOf(settings.promoBannerHi) }
    var policiesConfirmed by remember(settings) { mutableStateOf(settings.policiesConfirmed) }
    var deliveryCharge by remember(settings) { mutableStateOf("${settings.deliveryChargeInr}") }
    var freeThreshold by remember(settings) { mutableStateOf("${settings.freeDeliveryThresholdInr}") }
    var minOrder by remember(settings) { mutableStateOf("${settings.minOrderAmountInr}") }
    var coverageEn by remember(settings) { mutableStateOf(settings.deliveryCoverageEn) }
    var returnEn by remember(settings) { mutableStateOf(settings.returnPolicyEn) }
    var codEnabled by remember(settings) { mutableStateOf(settings.codEnabled) }
    var upiEnabled by remember(settings) { mutableStateOf(settings.upiEnabled) }
    var merchantUpiId by remember(settings) { mutableStateOf(settings.merchantUpiId) }
    var merchantPayeeName by remember(settings) { mutableStateOf(settings.merchantPayeeName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Store Contact & Promotional Banner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Shop Phone") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp Number") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(value = addressEn, onValueChange = { addressEn = it }, label = { Text("Store Address (English)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = addressHi, onValueChange = { addressHi = it }, label = { Text("दुकान का पता (Hindi)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = promoEn, onValueChange = { promoEn = it }, label = { Text("Homepage Offer Banner (English)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = promoHi, onValueChange = { promoHi = it }, label = { Text("होमपेज ऑफर बैनर (Hindi)") }, modifier = Modifier.fillMaxWidth())

        HorizontalDivider()
        Text("Delivery Charges & Return Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Confirm Delivery & Return Policies Live", fontWeight = FontWeight.SemiBold)
                Text("Keep OFF while awaiting confirmation; turn ON to apply exact INR delivery fees at checkout.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = policiesConfirmed, onCheckedChange = { policiesConfirmed = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = deliveryCharge,
                onValueChange = { deliveryCharge = it.filter { c -> c.isDigit() } },
                label = { Text("Delivery ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = freeThreshold,
                onValueChange = { freeThreshold = it.filter { c -> c.isDigit() } },
                label = { Text("Free Above ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minOrder,
                onValueChange = { minOrder = it.filter { c -> c.isDigit() } },
                label = { Text("Min Order ₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(value = coverageEn, onValueChange = { coverageEn = it }, label = { Text("Delivery Coverage Areas") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = returnEn, onValueChange = { returnEn = it }, label = { Text("Return & Exchange Policy") }, minLines = 2, modifier = Modifier.fillMaxWidth())

        HorizontalDivider()
        Text("Safe Payment Configuration (COD & Verified UPI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Cash on Delivery (COD)", fontWeight = FontWeight.SemiBold)
            Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Verified Merchant UPI QR", fontWeight = FontWeight.SemiBold)
                Text("Requires your genuine merchant UPI ID below. Never enter a personal password or PIN.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = upiEnabled, onCheckedChange = { upiEnabled = it })
        }

        OutlinedTextField(
            value = merchantUpiId,
            onValueChange = { merchantUpiId = it.trim() },
            label = { Text("Verified Merchant UPI ID (e.g. yourshop@okaxis)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_upi_id_input")
        )
        OutlinedTextField(
            value = merchantPayeeName,
            onValueChange = { merchantPayeeName = it },
            label = { Text("Verified Merchant Payee Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (upiEnabled && merchantUpiId.contains("@")) {
            val testUri = UpiPaymentHelper.buildVerifiedUpiUri(merchantUpiId, merchantPayeeName, 1L, "TEST-VERIFY")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Test Scan ₹1 QR Before Saving Live:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    QrMatrixView(payload = testUri, qrSize = 150.dp)
                }
            }
        }

        Button(
            onClick = {
                onSave(
                    settings.copy(
                        phone = phone,
                        whatsappNumber = whatsapp,
                        addressEn = addressEn,
                        addressHi = addressHi,
                        promoBannerEn = promoEn,
                        promoBannerHi = promoHi,
                        policiesConfirmed = policiesConfirmed,
                        deliveryChargeInr = deliveryCharge.toLongOrNull() ?: 0L,
                        freeDeliveryThresholdInr = freeThreshold.toLongOrNull() ?: 0L,
                        minOrderAmountInr = minOrder.toLongOrNull() ?: 0L,
                        deliveryCoverageEn = coverageEn,
                        returnPolicyEn = returnEn,
                        codEnabled = codEnabled,
                        upiEnabled = upiEnabled && merchantUpiId.contains("@"),
                        merchantUpiId = merchantUpiId,
                        merchantPayeeName = merchantPayeeName
                    )
                )
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button")
        ) {
            Text("Save Store Settings")
        }
    }
}

@Composable
fun AdminSetupAndGuideSection() {
    val sections = listOf(
        "1. Platform Note: Installable Android App (.apk / .aab)" to
            "This project is built on Google AI Studio's native Android (Kotlin + Jetpack Compose) runtime rather than a browser service-worker PWA. You can generate an installable Android APK/AAB directly from the AI Studio Settings menu to install on Raju Kumar Gupta's phone and share with Kalyanpur customers, or publish to Google Play. If you also want a public browser website on a custom domain later, connect a web frontend to this same Firebase Firestore database.",
        "2. Owner Admin Access & Google Security" to
            "Admin privileges are strictly locked in Cloud Firestore Security Rules to verified Google Sign-In for rajuguptas123@gmail.com (and authorized setup account). Keep 2-Step Verification turned ON on your Google account and never share your phone lock PIN.",
        "3. Replacing Demo Products with Real Shop Photos" to
            "1) Open Owner Dashboard -> Hide All Demo Items.\n2) Tap Products & Stock -> Add Product.\n3) Tap 'Pick Shop Photo from Phone' to select photos of your real sarees, kurtis, shirts, and shoes. Photos are compressed (~140 KB) and stored in Cloud Firestore immediately. For large multi-photo galleries later, you can paste HTTPS image URLs from Firebase Storage or Cloudinary.",
        "4. Safe UPI QR Setup & Payment Verification" to
            "1) Open Store Settings & UPI.\n2) Enter your bank/PhonePe/Paytm Business Merchant UPI ID (e.g., merchant@upi) and Payee Name.\n3) Scan the ₹1 test QR with another phone to verify your shop name appears.\n4) IMPORTANT: Screenshots can be faked! Every UPI order starts as 'Pending Verification'—only mark 'Verified Paid' after checking your own UPI Business app or bank SMS.",
        "5. Confirming Delivery Charges & Return Policy" to
            "Until confirmed, checkout shows 'Awaiting owner confirmation'. Once you decide Kalyanpur delivery charges (e.g., ₹0 in Bada Bazar, ₹30 nearby villages), open Store Settings -> switch ON 'Confirm Delivery & Return Policies Live' and save.",
        "6. Online Orders vs Walk-in Counter Sales" to
            "Online customer orders appear in 'Online Orders' with an SSR-... reference. Walk-in shop customers should be billed in 'Counter Sale & Bill' (POS-... invoice). Both deduct product stock automatically without double-counting revenue.",
        "7. Honest Profit Calculation" to
            "Enter your private wholesale purchase cost ('Purchase Cost ₹') when adding each product. Customers can NEVER read '/product_costs'. Reports calculate Gross Margin strictly on items with recorded purchase cost.",
        "8. Regular CSV Backup & APK Distribution" to
            "Open 'Sales & Profit Reports' -> tap 'Export CSV' weekly to save a copy of your products, online orders, and counter bills to Google Drive or WhatsApp. Export your signed APK/AAB via AI Studio -> Settings -> Export."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Owner Operations, Security & Deployment Guide",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        sections.forEach { (heading, body) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(body, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
