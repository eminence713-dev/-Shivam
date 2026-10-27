package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Locale

@Composable
fun CartAndCheckoutTab(
    cartItems: List<CartItem>,
    latestProducts: List<Product>,
    settings: StoreSettings,
    defaultCustomerName: String = "",
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

    val totalUnits = cartItems.sumOf { it.quantity }
    val totalMrp = cartItems.sumOf { it.lineOriginalTotalInr }
    val subtotal = cartItems.sumOf { it.lineSubtotalInr }
    val totalMrpSavings = (totalMrp - subtotal).coerceAtLeast(0L)
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
                text = if (lang == AppLanguage.HINDI) "मेरा कार्ट ($totalUnits पीस)" else "My Cart ($totalUnits items)",
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (item.lineSavingsInr > 0L) {
                                Text(
                                    text = if (lang == AppLanguage.HINDI) {
                                        "छूट बचत: ${formatInr(item.lineSavingsInr)}"
                                    } else {
                                        "Discount saved: ${formatInr(item.lineSavingsInr)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VerifiedGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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

            // Flipkart-Style Price Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (lang == AppLanguage.HINDI) "मूल्य विवरण (Price Details)" else "Price Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lang == AppLanguage.HINDI) "कुल एमआरपी ($totalUnits पीस)" else "Price ($totalUnits items)")
                        Text(formatInr(totalMrp))
                    }
                    if (totalMrpSavings > 0L) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (lang == AppLanguage.HINDI) "छूट (Discount)" else "Discount")
                            Text(
                                text = "− ${formatInr(totalMrpSavings)}",
                                color = VerifiedGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (lang == AppLanguage.HINDI) "डिलीवरी शुल्क" else "Delivery Charges")
                        Text(
                            text = if (!settings.policiesConfirmed) {
                                if (lang == AppLanguage.HINDI) "दुकान द्वारा पुष्टि" else "Confirmed on call"
                            } else if (deliveryFee == 0L) {
                                "FREE"
                            } else {
                                formatInr(deliveryFee)
                            },
                            color = if (deliveryFee == 0L) VerifiedGreen else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (lang == AppLanguage.HINDI) "कुल देय राशि" else "Total Amount",
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
                    if (totalMrpSavings > 0L) {
                        Surface(
                            color = VerifiedGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) {
                                    "🎉 इस ऑर्डर पर आपकी कुल बचत ${formatInr(totalMrpSavings)} है!"
                                } else {
                                    "🎉 You will save ${formatInr(totalMrpSavings)} on this order!"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = VerifiedGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Delivery & Payment Form
            Text(
                text = if (lang == AppLanguage.HINDI) "डिलीवरी पता और भुगतान विवरण" else "Delivery Address & Payment",
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

            Text(
                text = if (lang == AppLanguage.HINDI) "भुगतान का तरीका चुनें:" else "Select Payment Method:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("place_order_button")
            ) {
                if (isPlacingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "${AppStrings.placeOrder.get(lang)} • ${formatInr(finalPayable)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
fun OrderTrackingStepper(
    status: OrderStatus,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (status == OrderStatus.CANCELLED) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (lang == AppLanguage.HINDI) "यह ऑर्डर रद्द कर दिया गया है (Cancelled)" else "Order Cancelled",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }

    val steps = listOf(
        OrderStatus.PENDING to (if (lang == AppLanguage.HINDI) "बुक" else "Placed"),
        OrderStatus.CONFIRMED to (if (lang == AppLanguage.HINDI) "कन्फर्म" else "Confirmed"),
        OrderStatus.PACKED to (if (lang == AppLanguage.HINDI) "पैक" else "Packed"),
        OrderStatus.OUT_FOR_DELIVERY to (if (lang == AppLanguage.HINDI) "रास्ते में" else "Out"),
        OrderStatus.DELIVERED to (if (lang == AppLanguage.HINDI) "डिलीवर" else "Delivered")
    )
    val activeIdx = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            steps.forEachIndexed { idx, (_, _) ->
                val reached = idx <= activeIdx
                Surface(
                    shape = CircleShape,
                    color = if (reached) VerifiedGreen else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (reached) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (idx < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (idx < activeIdx) VerifiedGreen else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            steps.forEachIndexed { idx, (_, stepLabel) ->
                val reached = idx <= activeIdx
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (idx == activeIdx) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached) VerifiedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = VerifiedGreen,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = if (lang == AppLanguage.HINDI) "ऑर्डर दुकानदार के पास लाइव पहुँच गया!" else "Order Placed & Sent Live to Shop!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Order Ref: ${order.orderReference}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OrderTrackingStepper(status = order.statusEnum(), lang = lang)
                Text(
                    text = "${order.itemSummary} • Total: ${formatInr(order.totalPayableInr)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
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

                Button(
                    onClick = {
                        val msg = WhatsAppHelper.buildSavedOrderWhatsAppMessage(order, settings, lang)
                        WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_send_whatsapp_button")
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (lang == AppLanguage.HINDI) {
                            "व्हाट्सऐप पर दुकानदार को तुरंत सूचित करें (${settings.whatsappNumber})"
                        } else {
                            "Send Instant Alert on WhatsApp (${settings.whatsappNumber})"
                        }
                    )
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
                    OutlinedButton(onClick = onViewMyOrders, modifier = Modifier.weight(1f)) {
                        Text(AppStrings.navOrders.get(lang))
                    }
                }

                Button(onClick = onContinueShopping, modifier = Modifier.fillMaxWidth()) {
                    Text(AppStrings.shopNow.get(lang))
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
            text = if (lang == AppLanguage.HINDI) "मेरे ऑर्डर और लाइव ट्रैकिंग (My Orders)" else "My Orders & Live Tracking",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (lang == AppLanguage.HINDI) {
                "जैसे ही दुकानदार आपका ऑर्डर कन्फर्म या पैक करेंगे, स्थिति यहाँ अपने आप लाइव अपडेट होगी।"
            } else {
                "Order status updates here automatically in real time as the shop confirms, packs, and delivers."
            },
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
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(if (lang == AppLanguage.HINDI) "अभी कोई ऑर्डर नहीं है" else "No orders found yet")
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(list, key = { it.id }) { order ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                                .format(order.createdAtDate())
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            order.orderReference,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall)
                                    }

                                    // Flipkart-Style 5-Step Live Progress Stepper
                                    OrderTrackingStepper(
                                        status = order.statusEnum(),
                                        lang = lang
                                    )

                                    Text(order.itemSummary, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Total: ${formatInr(order.totalPayableInr)} • ${order.paymentMethodEnum().label(lang)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = if (order.paymentStatusEnum() == PaymentStatus.VERIFIED_PAID) {
                                                VerifiedGreen.copy(alpha = 0.16f)
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            },
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
                                        Text(
                                            "Note: ${order.cancellationReason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                InvoicePrintHelper.printInvoice(
                                                    context,
                                                    settings,
                                                    InvoicePrintHelper.fromCustomerOrder(order)
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(AppStrings.printBill.get(lang))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val msg = WhatsAppHelper.buildSavedOrderWhatsAppMessage(order, settings, lang)
                                                WhatsAppHelper.openWhatsAppChat(context, settings.whatsappNumber, msg)
                                            }
                                        ) {
                                            Text(if (lang == AppLanguage.HINDI) "व्हाट्सऐप सहायता" else "WhatsApp Help")
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
