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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
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
    onExitToStorefront: () -> Unit,
    onLockOwner: () -> Unit
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
                        text = if (lang == AppLanguage.HINDI) "मालिक पैनल (पासवर्ड सुरक्षित)" else "Password-Protected Owner Access • Raju Kumar Gupta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                androidx.compose.material3.FilledTonalButton(
                    onClick = onExitToStorefront,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .testTag("admin_preview_shop_button")
                ) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "दुकान देखें" else "View Shop",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                IconButton(
                    onClick = onLockOwner,
                    modifier = Modifier.testTag("admin_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.cd_lock_owner_dashboard),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                lang = lang,
                isBusy = isBusy,
                onSave = { adminViewModel.saveSettings(it) },
                onChangeOwnerPassword = { currentPwd, newPwd, onDone ->
                    adminViewModel.changeOwnerPassword(currentPwd, newPwd, onDone)
                }
            )
            AdminSection.SETUP_GUIDE -> AdminSetupAndGuideSection()
        }
    }
}

@Composable
fun AdminStoreSettingsSection(
    settings: StoreSettings,
    lang: AppLanguage,
    isBusy: Boolean,
    onSave: (StoreSettings) -> Unit,
    onChangeOwnerPassword: (String, String, () -> Unit) -> Unit
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
            Text(if (lang == AppLanguage.HINDI) "स्टोर सेटिंग्स सेव करें" else "Save Store Settings")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Owner Password Management Card
        var currentPwd by remember { mutableStateOf("") }
        var newPwd by remember { mutableStateOf("") }
        var showPwd by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, AntiqueGold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) "मालिक पासवर्ड बदलें (Owner Password)" else "Change Owner Dashboard Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) {
                        "ग्राहकों के लिए ऐप बिना किसी लॉगिन के सीधे खुलता है। केवल यह मालिक पैनल आपके गुप्त पासवर्ड से सुरक्षित रहता है।"
                    } else {
                        "Customers open the shop directly without any login. Only this Owner Dashboard is protected by your secret password."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentPwd,
                    onValueChange = { currentPwd = it },
                    label = { Text(if (lang == AppLanguage.HINDI) "पुराना पासवर्ड" else "Current Password") },
                    singleLine = true,
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                imageVector = if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPwd) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_current_password_input")
                )
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it },
                    label = { Text(if (lang == AppLanguage.HINDI) "नया पासवर्ड (कम से कम 4 अक्षर)" else "New Password (min 4 characters)") },
                    singleLine = true,
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("settings_new_password_input")
                )
                Button(
                    onClick = {
                        onChangeOwnerPassword(currentPwd, newPwd) {
                            currentPwd = ""
                            newPwd = ""
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("change_owner_password_button")
                ) {
                    Text(if (lang == AppLanguage.HINDI) "नया पासवर्ड सेव करें" else "Update Owner Password")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun AdminSetupAndGuideSection() {
    val sections = listOf(
        "1. Direct Shop Open for Customers (Zero Login Hassle)" to
            "When customers open the app, it goes directly into Shri Shivam Readymade & Riya Fancy Shoes Mart with zero login or Google account popups. Customers can browse all 10 categories, add to cart, place orders, and send itemized orders directly to your WhatsApp (9162685162).",
        "2. Password-Protected Owner Dashboard" to
            "Only the Owner Dashboard (Lock icon at top-right) requires your Owner Password. You can change your password anytime inside Store Settings -> Change Owner Dashboard Password, and tap the Lock icon in the Owner Dashboard header to lock it back when done.",
        "3. Free APK Distribution via GitHub / WhatsApp (₹0 Cost)" to
            "Your GitHub repository automatically compiles 'Shivam-Riya-Mart.apk' under GitHub Releases (latest-apk). Paste that download link in Owner Dashboard -> Free Customer Sharing to create a Counter QR code and 1-tap WhatsApp invite message.",
        "4. Adding Real Shop Photos & Stock" to
            "1) Open Owner Dashboard -> Hide All Demo Items.\n2) Tap Products & Stock -> Add Product.\n3) Tap 'Pick Shop Photo from Phone' to select photos of your sarees, kurtis, shirts, and shoes.",
        "5. Safe UPI QR Setup & Payment Verification" to
            "1) Open Store Settings & UPI.\n2) Enter your Merchant UPI ID (e.g., merchant@upi) and Payee Name.\n3) Scan the ₹1 test QR with another phone to verify your shop name appears.\n4) Always verify payment in your bank/UPI Business app before marking 'Verified Paid'.",
        "6. Online Orders vs Walk-in Counter Sales" to
            "Customer orders placed in the app generate an SSR-... reference and printable bill, plus a 1-tap WhatsApp order message to your number. Walk-in shop customers can be billed in 'Counter Sale & Bill' (POS-... invoice)."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Owner Operations, Password Security & Free Sharing Guide",
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
