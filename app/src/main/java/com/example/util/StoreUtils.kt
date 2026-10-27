package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

object WhatsAppHelper {

    fun formatInternationalIndianNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.length == 10 && digits[0] in '6'..'9' -> "91$digits"
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.length >= 10 -> "91${digits.takeLast(10)}"
            else -> "919162685162"
        }
    }

    fun buildSavedOrderWhatsAppMessage(
        order: CustomerOrder,
        settings: StoreSettings,
        lang: AppLanguage
    ): String {
        return buildString {
            appendLine("🛍️ *नया ग्राहक ऑर्डर — ${settings.storeNameEn}*")
            appendLine("📋 *Order Ref:* ${order.orderReference}")
            appendLine("👤 *नाम (Customer):* ${order.customerName}")
            appendLine("📞 *मोबाइल:* ${order.customerPhone}")
            appendLine("📍 *पता (Address):* ${order.deliveryAddress}")
            if (order.landmark.isNotBlank()) {
                appendLine("🧭 *लैंडमार्क:* ${order.landmark}")
            }
            appendLine("────────────────")
            order.parsedItems().forEachIndexed { idx, item ->
                val opts = listOf(item.selectedSize, item.selectedColor)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                append("${idx + 1}. ${item.productName} × ${item.quantity}")
                if (opts.isNotBlank()) append(" [$opts]")
                appendLine(" = ${formatInr(item.lineTotalInr)}")
            }
            appendLine("────────────────")
            appendLine("💰 *कुल राशि (Total):* ${formatInr(order.totalPayableInr)}")
            appendLine("💳 *भुगतान (Payment):* ${order.paymentMethodEnum().label(lang)}")
            appendLine()
            appendLine(
                if (lang == AppLanguage.HINDI)
                    "कृपया मेरा ऑर्डर कन्फर्म करें और डिलीवरी का समय बताएँ। धन्यवाद!"
                else
                    "Please confirm my order and let me know the delivery timing. Thank you!"
            )
        }.trim()
    }

    fun buildCartEnquiryMessage(
        settings: StoreSettings,
        cartItems: List<CartItem>,
        nextStepLabel: String,
        lang: AppLanguage
    ): String {
        val subtotal = cartItems.sumOf { it.lineSubtotalInr }
        val deliveryLine = if (!settings.policiesConfirmed) {
            if (lang == AppLanguage.HINDI) "डिलीवरी शुल्क: दुकान द्वारा पुष्टि की जाएगी"
            else "Delivery Charge: To be confirmed by shop"
        } else if (settings.freeDeliveryThresholdInr > 0 && subtotal >= settings.freeDeliveryThresholdInr) {
            if (lang == AppLanguage.HINDI) "डिलीवरी शुल्क: निःशुल्क (₹0)" else "Delivery Charge: Free (₹0)"
        } else {
            val fee = formatInr(settings.deliveryChargeInr)
            if (lang == AppLanguage.HINDI) "डिलीवरी शुल्क: $fee" else "Delivery Charge: $fee"
        }

        return buildString {
            appendLine("नमस्ते / Hello ${settings.storeNameEn},")
            appendLine()
            if (cartItems.isEmpty()) {
                appendLine(
                    if (lang == AppLanguage.HINDI)
                        "मैं आपकी दुकान के कपड़ों और फुटवियर के बारे में जानकारी चाहता/चाहती हूँ।"
                    else
                        "I would like to enquire about clothing and footwear availability at Kalyanpur store."
                )
            } else {
                appendLine(if (lang == AppLanguage.HINDI) "मेरी पसंद की वस्तुएँ:" else "Selected Store Items:")
                cartItems.forEachIndexed { index, item ->
                    val opts = listOfNotNull(
                        item.selectedSize.takeIf { it.isNotBlank() }?.let { "Size: $it" },
                        item.selectedColor.takeIf { it.isNotBlank() }?.let { "Colour: $it" }
                    ).joinToString(", ")
                    append("${index + 1}. ${item.product.displayName(lang)} x ${item.quantity}")
                    if (opts.isNotBlank()) append(" [$opts]")
                    appendLine(" = ${formatInr(item.lineSubtotalInr)}")
                }
                appendLine()
                appendLine("Subtotal: ${formatInr(subtotal)}")
                appendLine(deliveryLine)
                appendLine("Chosen Next Step: $nextStepLabel")
            }
        }.trim()
    }

    fun buildSingleProductMessage(
        settings: StoreSettings,
        product: Product,
        quantity: Int,
        size: String,
        color: String,
        lang: AppLanguage
    ): String {
        val item = CartItem(product = product, quantity = quantity, selectedSize = size, selectedColor = color)
        return buildCartEnquiryMessage(
            settings = settings,
            cartItems = listOf(item),
            nextStepLabel = if (lang == AppLanguage.HINDI) "उपलब्धता और ऑर्डर की पुष्टि करें" else "Confirm availability & place order",
            lang = lang
        )
    }

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String, onError: (String) -> Unit = {}) {
        val phoneIntl = formatInternationalIndianNumber(rawPhone)
        val encoded = Uri.encode(message)
        val url = "https://wa.me/$phoneIntl?text=$encoded"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            onError("WhatsApp or browser not found on this device. Please call or WhatsApp +$phoneIntl.")
        }
    }
}

object UpiPaymentHelper {
    fun buildVerifiedUpiUri(
        merchantUpiId: String,
        merchantPayeeName: String,
        amountInr: Long,
        orderReference: String
    ): String {
        val cleanVpa = merchantUpiId.trim()
        val cleanName = merchantPayeeName.trim().ifBlank { "Shri Shivam Readymade" }
        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", cleanVpa)
            .appendQueryParameter("pn", cleanName)
            .appendQueryParameter("am", "$amountInr.00")
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", "Order $orderReference")
            .build()
            .toString()
    }

    fun launchUpiApp(context: Context, upiUri: String, onNoApp: () -> Unit) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            onNoApp()
        }
    }
}

object ImageCompressor {
    private val bitmapCache = ConcurrentHashMap<Int, ImageBitmap>()

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            val maxDim = 520
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDim * 2 || (boundsOpts.outHeight / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: error("Could not read selected image")

            val ratio = minOf(
                maxDim.toFloat() / rawBitmap.width.coerceAtLeast(1),
                maxDim.toFloat() / rawBitmap.height.coerceAtLeast(1),
                1f
            )
            val targetW = (rawBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (rawBitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (targetW != rawBitmap.width || targetH != rawBitmap.height) {
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else {
                rawBitmap
            }

            val out = ByteArrayOutputStream()
            var quality = 74
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 190_000 && quality > 38) {
                out.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    }

    fun decodeBase64Cached(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        val key = base64.hashCode()
        bitmapCache[key]?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            if (bmp != null && bitmapCache.size < 60) {
                bitmapCache[key] = bmp
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
