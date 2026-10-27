package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage {
    ENGLISH,
    HINDI
}

enum class ProductCategory(
    val labelEn: String,
    val labelHi: String,
    val sampleKey: String
) {
    SAREES("Sarees", "साड़ियाँ", "saree"),
    KURTIS("Kurtis", "कुर्तियाँ", "kurti"),
    LADIES_FROCKS("Ladies Frocks", "लेडीज़ फ्रॉक", "frock"),
    SHIRTS("Shirts", "शर्ट्स", "shirt"),
    TSHIRTS("T-shirts", "टी-शर्ट्स", "tshirt"),
    JEANS("Jeans", "जींस", "jeans"),
    TROUSERS("Trousers", "ट्राउज़र्स", "trousers"),
    SHOES("Shoes", "जूते (Shoes)", "shoes"),
    SANDALS("Sandals", "सैंडल", "sandals"),
    SLIPPERS("Slippers", "चप्पल / स्लिपर्स", "slippers");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn

    companion object {
        fun fromKey(raw: String): ProductCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SAREES
    }
}

enum class OrderStatus(val labelEn: String, val labelHi: String) {
    PENDING("Pending", "प्रतीक्षित (Pending)"),
    CONFIRMED("Confirmed", "कन्फर्म (Confirmed)"),
    PACKED("Packed", "पैक किया गया (Packed)"),
    OUT_FOR_DELIVERY("Out for Delivery", "डिलीवरी के लिए निकला"),
    DELIVERED("Delivered", "डिलीवर हो गया"),
    CANCELLED("Cancelled", "रद्द (Cancelled)");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn

    companion object {
        fun fromKey(raw: String): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class PaymentMethodType(val labelEn: String, val labelHi: String) {
    COD("Cash on Delivery", "कैश ऑन डिलीवरी (COD)"),
    UPI_QR("UPI QR Payment", "यूपीआई क्यूआर (UPI QR)");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn
}

enum class PaymentStatus(val labelEn: String, val labelHi: String) {
    PENDING("Pending Verification", "भुगतान सत्यापन बाकी"),
    VERIFIED_PAID("Verified Paid", "भुगतान प्राप्त (सत्यापित)"),
    REFUNDED("Refunded", "रिफंड किया गया");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn
}

enum class ExpenseCategory(val labelEn: String, val labelHi: String) {
    STOCK_PURCHASE("Stock / Wholesale Purchase", "माल खरीद (थोक)"),
    SHOP_RENT("Shop Rent", "दुकान किराया"),
    TRANSPORT("Transport & Freight", "भाड़ा / ट्रांसपोर्ट"),
    ELECTRICITY_UTILITIES("Electricity & Bills", "बिजली और बिल"),
    PACKAGING("Bags & Packaging", "पैकिंग / कैरी बैग"),
    STAFF_HELPER("Helper / Wages", "मजदूरी / सहायता"),
    OTHER("Other Shop Expense", "अन्य खर्च");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI) labelHi else labelEn
}

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "public_config",
    val storeNameEn: String = "Shri Shivam Readymade & Riya Fancy Shoes Mart",
    val storeNameHi: String = "श्री शिवम रेडीमेड एंड रिया फैंसी शूज़ मार्ट",
    val ownerName: String = "Raju Kumar Gupta",
    val phone: String = "9162685162",
    val whatsappNumber: String = "9162685162",
    val email: String = "rajuguptas123@gmail.com",
    val addressEn: String = "Bada Bazar, Kalyanpur, Motihari, Bihar, India",
    val addressHi: String = "बड़ा बाज़ार, कल्याणपुर, मोतिहारी, बिहार, भारत",
    val promoBannerEn: String = "Welcome to our official Kalyanpur family clothing & footwear store! Browse festive & daily wear online or order on WhatsApp.",
    val promoBannerHi: String = "कल्याणपुर के आपके भरोसेमंद पारिवारिक कपड़े और फुटवियर स्टोर में स्वागत है! ऑनलाइन देखें या व्हाट्सऐप पर ऑर्डर करें।",
    val deliveryCoverageEn: String = "Local areas around Bada Bazar, Kalyanpur & nearby villages (Awaiting final owner confirmation)",
    val deliveryCoverageHi: String = "बड़ा बाज़ार, कल्याणपुर और आसपास के क्षेत्र (मालिक की अंतिम पुष्टि की प्रतीक्षा)",
    val deliveryChargeInr: Long = 0L,
    val freeDeliveryThresholdInr: Long = 0L,
    val minOrderAmountInr: Long = 0L,
    val expectedDeliveryEn: String = "Awaiting owner confirmation — contact shop on WhatsApp (9162685162) for delivery timing.",
    val expectedDeliveryHi: String = "मालिक द्वारा समय की पुष्टि बाकी है — डिलीवरी जानकारी के लिए व्हाट्सऐप (9162685162) पर संपर्क करें।",
    val returnPolicyEn: String = "Return & exchange policy awaiting owner confirmation. Please check size/colour at delivery or contact Raju Kumar Gupta at Bada Bazar, Kalyanpur.",
    val returnPolicyHi: String = "वापसी और एक्सचेंज नीति की पुष्टि बाकी है। कृपया बड़ा बाज़ार, कल्याणपुर दुकान पर संपर्क करें।",
    val policiesConfirmed: Boolean = false,
    val codEnabled: Boolean = true,
    val upiEnabled: Boolean = false,
    val merchantUpiId: String = "",
    val merchantPayeeName: String = "Shri Shivam Readymade & Riya Fancy Shoes Mart",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isUpiConfiguredAndActive: Boolean
        get() = upiEnabled && merchantUpiId.contains("@") && merchantUpiId.length >= 5

    fun toFirestoreMap(actorUid: String): Map<String, Any> = mapOf(
        "storeNameEn" to storeNameEn.trim().take(150),
        "storeNameHi" to storeNameHi.trim().ifBlank { storeNameEn.trim() }.take(150),
        "ownerName" to ownerName.trim().take(100),
        "phone" to phone.trim().take(20),
        "whatsappNumber" to whatsappNumber.trim().take(20),
        "email" to email.trim().take(120),
        "addressEn" to addressEn.trim().take(250),
        "addressHi" to addressHi.trim().ifBlank { addressEn.trim() }.take(250),
        "promoBannerEn" to promoBannerEn.trim().take(300),
        "promoBannerHi" to promoBannerHi.trim().take(300),
        "deliveryCoverageEn" to deliveryCoverageEn.trim().take(300),
        "deliveryCoverageHi" to deliveryCoverageHi.trim().take(300),
        "deliveryChargeInr" to deliveryChargeInr.coerceIn(0L, 10000L),
        "freeDeliveryThresholdInr" to freeDeliveryThresholdInr.coerceIn(0L, 1000000L),
        "minOrderAmountInr" to minOrderAmountInr.coerceIn(0L, 1000000L),
        "expectedDeliveryEn" to expectedDeliveryEn.trim().take(300),
        "expectedDeliveryHi" to expectedDeliveryHi.trim().take(300),
        "returnPolicyEn" to returnPolicyEn.trim().take(600),
        "returnPolicyHi" to returnPolicyHi.trim().take(600),
        "policiesConfirmed" to policiesConfirmed,
        "codEnabled" to codEnabled,
        "upiEnabled" to upiEnabled,
        "merchantUpiId" to merchantUpiId.trim().take(100),
        "merchantPayeeName" to merchantPayeeName.trim().take(120),
        "updatedBy" to actorUid,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameHi: String = "",
    val category: String = ProductCategory.SAREES.name,
    val descriptionEn: String = "",
    val descriptionHi: String = "",
    val sellingPriceInr: Long = 0L,
    val originalPriceInr: Long = 0L,
    val stockCount: Long = 0L,
    val lowStockThreshold: Long = 3L,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val variantNotes: String = "",
    val sampleImageKey: String = "",
    val imageUrl: String = "",
    val imageBase64: String = "",
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val isDemo: Boolean = false,
    val soldCount: Long = 0L,
    val lastOrderId: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayName(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI && nameHi.isNotBlank()) nameHi else nameEn

    fun displayDescription(lang: AppLanguage): String =
        if (lang == AppLanguage.HINDI && descriptionHi.isNotBlank()) descriptionHi else descriptionEn

    fun categoryEnum(): ProductCategory = ProductCategory.fromKey(category)

    val isLowStock: Boolean
        get() = stockCount in 1..lowStockThreshold

    val isOutOfStock: Boolean
        get() = stockCount <= 0L

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "nameEn" to nameEn.trim().take(140),
        "nameHi" to nameHi.trim().take(140),
        "category" to categoryEnum().name,
        "descriptionEn" to descriptionEn.trim().take(1000),
        "descriptionHi" to descriptionHi.trim().take(1000),
        "sellingPriceInr" to sellingPriceInr.coerceIn(1L, 500000L),
        "originalPriceInr" to originalPriceInr.coerceIn(0L, 500000L),
        "stockCount" to stockCount.coerceIn(0L, 100000L),
        "lowStockThreshold" to lowStockThreshold.coerceIn(0L, 1000L),
        "sizes" to sizes.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15),
        "colors" to colors.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15),
        "variantNotes" to variantNotes.trim().take(300),
        "sampleImageKey" to sampleImageKey.trim().take(60),
        "imageUrl" to imageUrl.trim().take(600),
        "imageBase64" to imageBase64.take(340000),
        "isPublished" to isPublished,
        "isFeatured" to isFeatured,
        "isDemo" to isDemo,
        "soldCount" to soldCount.coerceAtLeast(0L),
        "createdBy" to ownerUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toOwnerUpdateMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn.trim().take(140),
        "nameHi" to nameHi.trim().take(140),
        "category" to categoryEnum().name,
        "descriptionEn" to descriptionEn.trim().take(1000),
        "descriptionHi" to descriptionHi.trim().take(1000),
        "sellingPriceInr" to sellingPriceInr.coerceIn(1L, 500000L),
        "originalPriceInr" to originalPriceInr.coerceIn(0L, 500000L),
        "stockCount" to stockCount.coerceIn(0L, 100000L),
        "lowStockThreshold" to lowStockThreshold.coerceIn(0L, 1000L),
        "sizes" to sizes.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15),
        "colors" to colors.map { it.trim().take(60) }.filter { it.isNotEmpty() }.take(15),
        "variantNotes" to variantNotes.trim().take(300),
        "sampleImageKey" to sampleImageKey.trim().take(60),
        "imageUrl" to imageUrl.trim().take(600),
        "imageBase64" to imageBase64.take(340000),
        "isPublished" to isPublished,
        "isFeatured" to isFeatured,
        "isDemo" to isDemo,
        "soldCount" to soldCount.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ProductCost(
    @DocumentId val id: String = "",
    val productId: String = "",
    val costPriceInr: Long = 0L,
    val supplierNote: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val selectedSize: String = "",
    val selectedColor: String = ""
) {
    val key: String
        get() = "${product.id}_${selectedSize}_${selectedColor}"

    val lineSubtotalInr: Long
        get() = product.sellingPriceInr * quantity.toLong()
}

data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val category: String = ProductCategory.SAREES.name,
    val quantity: Long = 1L,
    val unitPriceInr: Long = 0L,
    val lineTotalInr: Long = 0L,
    val selectedSize: String = "",
    val selectedColor: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "category" to category,
        "quantity" to quantity,
        "unitPriceInr" to unitPriceInr,
        "lineTotalInr" to lineTotalInr,
        "selectedSize" to selectedSize,
        "selectedColor" to selectedColor
    )

    companion object {
        fun fromRawMap(raw: Map<*, *>): OrderLineItem = OrderLineItem(
            productId = raw["productId"] as? String ?: "",
            productName = raw["productName"] as? String ?: "",
            category = raw["category"] as? String ?: ProductCategory.SAREES.name,
            quantity = (raw["quantity"] as? Number)?.toLong() ?: 1L,
            unitPriceInr = (raw["unitPriceInr"] as? Number)?.toLong() ?: 0L,
            lineTotalInr = (raw["lineTotalInr"] as? Number)?.toLong() ?: 0L,
            selectedSize = raw["selectedSize"] as? String ?: "",
            selectedColor = raw["selectedColor"] as? String ?: ""
        )
    }
}

@IgnoreExtraProperties
data class CustomerOrder(
    @DocumentId val id: String = "",
    val orderReference: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val landmark: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val itemSummary: String = "",
    val totalQuantity: Long = 1L,
    val subtotalInr: Long = 0L,
    val deliveryChargeInr: Long = 0L,
    val discountInr: Long = 0L,
    val totalPayableInr: Long = 0L,
    val deliveryFeeConfirmed: Boolean = false,
    val paymentMethod: String = PaymentMethodType.COD.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val orderStatus: String = OrderStatus.PENDING.name,
    val stockDeducted: Boolean = true,
    val cancellationReason: String = "",
    val invoiceNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<OrderLineItem> = items.map { OrderLineItem.fromRawMap(it) }
    fun statusEnum(): OrderStatus = OrderStatus.fromKey(orderStatus)
    fun paymentStatusEnum(): PaymentStatus =
        PaymentStatus.entries.firstOrNull { it.name == paymentStatus } ?: PaymentStatus.PENDING
    fun paymentMethodEnum(): PaymentMethodType =
        PaymentMethodType.entries.firstOrNull { it.name == paymentMethod } ?: PaymentMethodType.COD
}

@IgnoreExtraProperties
data class OfflineSale(
    @DocumentId val id: String = "",
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val itemSummary: String = "",
    val subtotalInr: Long = 0L,
    val discountInr: Long = 0L,
    val totalPaidInr: Long = 0L,
    val totalCostInr: Long = 0L,
    val costCoveredRevenueInr: Long = 0L,
    val allCostsRecorded: Boolean = false,
    val paymentMethod: String = "CASH",
    val recordedBy: String = "",
    val saleDateEpochDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun parsedItems(): List<OrderLineItem> = items.map { OrderLineItem.fromRawMap(it) }
}

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = ExpenseCategory.OTHER.name,
    val amountInr: Long = 0L,
    val notes: String = "",
    val recordedBy: String = "",
    val expenseDateEpochDay: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun categoryEnum(): ExpenseCategory =
        ExpenseCategory.entries.firstOrNull { it.name == category } ?: ExpenseCategory.OTHER
}

@IgnoreExtraProperties
data class InventoryMovement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val previousStock: Long = 0L,
    val newStock: Long = 0L,
    val delta: Long = 0L,
    val reason: String = "MANUAL_UPDATE",
    val referenceId: String = "",
    val actorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
