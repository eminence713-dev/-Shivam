package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.ui.components.QrMatrixView
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.UiState
import com.example.util.AppStrings
import com.example.util.InvoicePrintHelper
import com.example.util.UpiPaymentHelper
import com.example.util.WhatsAppHelper
import com.example.util.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CartAndCheckoutTab(
    cartItems: List<CartItem>,
    latestProducts: List<Product>,
    settings: StoreSettings,
    defaultCustomerName: String,
    isPlacingOrder: Boolean,
    checkoutError: String?,
    lang: AppLanguage,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSyncLiveStock: () -> List<String>,
    onPlaceOrder: (String, String, String, String, PaymentMethodType) -> Unit,
    onBrowseShop: () -> Unit
) {
    val context = LocalContext.current
    var customerName by rememberSaveable { mutableStateOf(defaultCustomerName) }
    var customerPhone by rememberSaveable { mutableStateOf("") }
    var deliveryAddress by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var selectedPayment by rememberSaveable {
        mutableStateOf(
            if (settings.codEnabled) PaymentMethodType.COD else PaymentMethodType.UPI_QR
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var stockSyncWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(latestProducts) {
        if (latestProducts.isNotEmpty() && cartItems.isNotEmpty()) {
            val warnings = onSyncLiveStock()
            if (warnings.isNotEmpty()) stockSyncWarnings = warnings
        }
    }

    if (cartItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (lang == AppLanguage.HINDI) "आपका कार्ट खाली है" else "Your shopping cart is empty",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBrowseShop, modifier = Modifier.testTag("empty_cart_shop_button")) {
                    Text(AppStrings.shopNow.get(lang))
                }
            }
        }
        return
    }

    val subtotal = cartItems.sumOf { it.lineSubtotalInr }
    val deliveryFee = when {
        !settings.policiesConfirmed -> 0L
        settings.freeDeliveryThresholdInr > 0L && subtotal >= settings.freeDeliveryThresholdInr -> 0L
        else -> settings.deliveryChargeInr.coerceAtLeast(0L)
    }
    val finalPayable = subtotal + deliveryFee

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (stockSyncWarnings.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stockSyncWarnings.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = if (lang == AppLanguage.HINDI) "शॉपिंग कार्ट (${cartItems.sumOf { it.quantity }} पीस)" else "Shopping Cart (${cartItems.sumOf { it.quantity }} items)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            cartItems.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.displayName(lang),
                                style = MaterialTheme.typography.titleSmall
                            )
                            val opts = listOf(item.selectedSize, item.selectedColor)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (opts.isNotBlank()) {
                                Text(opts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${formatInr(item.product.sellingPriceInr)} × ${item.quantity} = ${formatInr(item.lineSubtotalInr)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateQuantity(item.key, item.quantity - 1) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                            }
                            Text("${item.quantity}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onUpdateQuantity(item.key, item.quantity + 1) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase")
                            }
                            IconButton(onClick = { onRemoveItem(item.key) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Bill Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lang == AppLanguage.HINDI) "वस्तुओं का कुल (Subtotal)" else "Items Subtotal")
                        Text(formatInr(subtotal), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lang == AppLanguage.HINDI) "डिलीवरी शुल्क" else "Delivery Charge")
                        Text(
                            text = if (!settings.policiesConfirmed) {
                                if (lang == AppLanguage.HINDI) "दुकान द्वारा पुष्टि बाकी" else "Awaiting owner confirmation"
                            } else if (deliveryFee == 0L) {
                                "₹0 (Free)"
                            } else {
                                formatInr(deliveryFee)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (lang == AppLanguage.HINDI) "कुल देय राशि" else "Final Payable Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatInr(finalPayable),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Delivery & Payment Form
            Text(
                text = if (lang == AppLanguage.HINDI) "डिलीवरी विवरण (खाता पंजीकरण आवश्यक नहीं)" else "Delivery & Payment Details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    validationError = null
                },
                label = { Text(if (lang == AppLanguage.HINDI) "ग्राहक का पूरा नाम *" else "Customer Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = {
                    customerPhone = it.filter { ch -> ch.isDigit() }.take(10)
                    validationError = null
                },
                label = { Text(if (lang == AppLanguage.HINDI) "मोबाइल नंबर (10 अंक) *" else "Mobile Number (10 digits) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
            )

            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = {
                    deliveryAddress = it
                    validationError = null
                },
                label = { Text(if (lang == AppLanguage.HINDI) "पूरा डिलीवरी पता (गाँव / वार्ड / मोहल्ला) *" else "Full Delivery Address (Kalyanpur / Nearby Area) *") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
            )

            OutlinedTextField(
                value = landmark,
                onValueChange = { landmark = it },
                label = { Text(if (lang == AppLanguage.HINDI) "लैंडमार्क / निर्देश (वैकल्पिक)" else "Landmark or Delivery Instructions (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_landmark_input")
            )

            // Payment Method Selection
            Text(
                text = if (lang == AppLanguage.HINDI) "भुगतान का तरीका चुनें:" else "Select Payment Method:",
                style = MaterialTheme.typography.titleSmall
            )

            if (settings.codEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (selectedPayment == PaymentMethodType.COD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPayment = PaymentMethodType.COD }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        RadioButton(
                            selected = selectedPayment == PaymentMethodType.COD,
                            onClick = { selectedPayment = PaymentMethodType.COD }
                        )
                        Column {
                            Text(PaymentMethodType.COD.label(lang), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (lang == AppLanguage.HINDI) "सामान मिलने पर नकद भुगतान करें" else "Pay cash when items are delivered / collected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (settings.isUpiConfiguredAndActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (selectedPayment == PaymentMethodType.UPI_QR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPayment = PaymentMethodType.UPI_QR }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        RadioButton(
                            selected = selectedPayment == PaymentMethodType.UPI_QR,
                            onClick = { selectedPayment = PaymentMethodType.UPI_QR }
                        )
                        Column {
                            Text(PaymentMethodType.UPI_QR.label(lang), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Verified Payee: ${settings.merchantPayeeName} (${settings.merchantUpiId})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (lang == AppLanguage.HINDI) {
                                "UPI QR भुगतान: दुकानदार द्वारा सत्यापित मर्चेंट UPI ID जोड़े जाने के बाद सक्रिय होगा।"
                            } else {
                                "UPI QR Payment: Disabled until the shop owner configures a verified merchant UPI ID in Store Settings."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val errToDisplay = validationError ?: checkoutError
            if (errToDisplay != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errToDisplay,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val cleanPhone = customerPhone.trim()
                    when {
                        customerName.trim().length < 2 -> validationError = AppStrings.errNameRequired.get(lang)
                        !cleanPhone.matches(Regex("^[6-9][0-9]{9}$")) -> validationError = AppStrings.errPhoneInvalid.get(lang)
                        deliveryAddress.trim().length < 5 -> validationError = AppStrings.errAddressRequired.get(lang)
                        settings.minOrderAmountInr > 0 && subtotal < settings.minOrderAmountInr -> {
                            validationError = "Minimum order amount is ${formatInr(settings.minOrderAmountInr)}."
                        }
                        else -> {
                            validationError = null
                            onPlaceOrder(customerName, cleanPhone, deliveryAddress, landmark, selectedPayment)
                        }
                    }
                },
                enabled = !isPlacingOrder && (settings.codEnabled || settings.isUpiConfiguredAndActive),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("place_order_button")
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("${AppStrings.placeOrder.get(lang)} • ${formatInr(finalPayable)}")
                }
            }

            // WhatsApp alternative with explicit non-saved disclaimer
            OutlinedButton(
                onClick = {
                    val msg = WhatsAppHelper.buildCartEnquiryMessage(
                        settings = settings,
                        cartItems = cartItems,
                        nextStepLabel = "Discuss delivery & confirm on WhatsApp",
                        lang = lang
                    )
                    WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                },
                border = BorderStroke(1.5.dp, WhatsAppGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cart_whatsapp_button")
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(AppStrings.orderOnWhatsApp.get(lang))
            }

            Text(
                text = AppStrings.whatsappOrderDisclaimer.get(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun OrderConfirmationCard(
    order: CustomerOrder,
    settings: StoreSettings,
    lang: AppLanguage,
    onViewMyOrders: () -> Unit,
    onContinueShopping: () -> Unit
) {
    val context = LocalContext.current
    var upiLaunchNote by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, AntiqueGold)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = VerifiedGreen,
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) "ऑर्डर सफलतापूर्वक दर्ज हो गया!" else "Order Saved & Confirmed!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Order Ref: ${order.orderReference}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.itemSummary} • Total: ${formatInr(order.totalPayableInr)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status: ${order.statusEnum().label(lang)} | Payment: ${order.paymentStatusEnum().label(lang)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (order.paymentMethodEnum() == PaymentMethodType.UPI_QR && settings.isUpiConfiguredAndActive) {
                    val upiUri = remember(order.id, settings.merchantUpiId) {
                        UpiPaymentHelper.buildVerifiedUpiUri(
                            merchantUpiId = settings.merchantUpiId,
                            merchantPayeeName = settings.merchantPayeeName,
                            amountInr = order.totalPayableInr,
                            orderReference = order.orderReference
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (lang == AppLanguage.HINDI) "सत्यापित दुकानदार को UPI भुगतान करें" else "Pay Verified Merchant via UPI QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recipient: ${settings.merchantPayeeName}\nUPI ID: ${settings.merchantUpiId}\nAmount Due: ${formatInr(order.totalPayableInr)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    QrMatrixView(payload = upiUri, qrSize = 190.dp)
                    Text(
                        text = "Always verify recipient name (${settings.merchantPayeeName}) in your UPI app before entering PIN on your bank screen. Never share your UPI PIN with anyone. Payment remains 'Pending Verification' until confirmed by the shop owner.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            UpiPaymentHelper.launchUpiApp(context, upiUri) {
                                upiLaunchNote = "No UPI app detected on this device. Scan the QR code above from another phone."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open UPI App (${formatInr(order.totalPayableInr)})")
                    }
                    if (upiLaunchNote != null) {
                        Text(upiLaunchNote.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            InvoicePrintHelper.printInvoice(
                                context,
                                settings,
                                InvoicePrintHelper.fromCustomerOrder(order)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.printBill.get(lang))
                    }
                    OutlinedButton(
                        onClick = {
                            val msg = "Hello ${settings.storeNameEn}, I placed saved online Order Ref: ${order.orderReference} (${formatInr(order.totalPayableInr)}). Please confirm delivery timing."
                            WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                        },
                        border = BorderStroke(1.dp, WhatsAppGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp Update")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onViewMyOrders, modifier = Modifier.weight(1f)) {
                        Text(AppStrings.navOrders.get(lang))
                    }
                    Button(onClick = onContinueShopping, modifier = Modifier.weight(1f)) {
                        Text(AppStrings.shopNow.get(lang))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOrdersTab(
    ordersState: UiState<List<CustomerOrder>>,
    settings: StoreSettings,
    lang: AppLanguage
) {
    val context = LocalContext.current
    var refSearch by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (lang == AppLanguage.HINDI) "मेरे सुरक्षित ऑर्डर और ट्रैकिंग" else "My Orders & Safe Status Tracking",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (lang == AppLanguage.HINDI) "केवल आपके Google खाते से किए गए निजी ऑर्डर यहाँ दिखाई देते हैं।" else "Strictly isolated to your signed-in account — other customers' orders are never exposed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = refSearch,
            onValueChange = { refSearch = it },
            label = { Text(if (lang == AppLanguage.HINDI) "ऑर्डर रेफरेंस (SSR-...) खोजें" else "Filter by Order Ref (SSR-...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        when (ordersState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Text(ordersState.message, color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                val list = ordersState.data.filter {
                    refSearch.isBlank() || it.orderReference.contains(refSearch.trim(), ignoreCase = true)
                }
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            Text(if (lang == AppLanguage.HINDI) "अभी कोई ऑर्डर नहीं है" else "No orders found yet")
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(list, key = { it.id }) { order ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                                .format(order.createdAt?.toDate() ?: Date())
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(order.orderReference, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(order.itemSummary, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Total: ${formatInr(order.totalPayableInr)} • ${order.paymentMethodEnum().label(lang)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = if (order.statusEnum() == OrderStatus.CANCELLED) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Order: ${order.statusEnum().label(lang)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            color = if (order.paymentStatusEnum() == PaymentStatus.VERIFIED_PAID) VerifiedGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = order.paymentStatusEnum().label(lang),
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    if (order.cancellationReason.isNotBlank()) {
                                        Text("Note: ${order.cancellationReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                                                val msg = "Hello, checking status for Order Ref: ${order.orderReference}."
                                                WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                                            }
                                        ) {
                                            Text("WhatsApp Help")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
