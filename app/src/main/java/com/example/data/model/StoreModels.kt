package com.example.data.model

import java.util.Date

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

data class StoreSettings(
    val id: String = "public_config",
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
    val deliveryCoverageEn: String = "Local areas around Bada Bazar, Kalyanpur & nearby villages",
    val deliveryCoverageHi: String = "बड़ा बाज़ार, कल्याणपुर और आसपास के क्षेत्र",
    val deliveryChargeInr: Long = 0L,
    val freeDeliveryThresholdInr: Long = 0L,
    val minOrderAmountInr: Long = 0L,
    val expectedDeliveryEn: String = "Contact shop on WhatsApp (9162685162) for delivery timing.",
    val expectedDeliveryHi: String = "डिलीवरी जानकारी के लिए व्हाट्सऐप (9162685162) पर संपर्क करें।",
    val returnPolicyEn: String = "Easy size & colour check at Bada Bazar, Kalyanpur store. Contact Raju Kumar Gupta for support.",
    val returnPolicyHi: String = "साइज़ या रंग बदलने हेतु बड़ा बाज़ार, कल्याणपुर दुकान पर संपर्क करें।",
    val policiesConfirmed: Boolean = false,
    val codEnabled: Boolean = true,
    val upiEnabled: Boolean = false,
    val merchantUpiId: String = "",
    val merchantPayeeName: String = "Shri Shivam Readymade & Riya Fancy Shoes Mart",
    val updatedBy: String = "owner",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val isUpiConfiguredAndActive: Boolean
        get() = upiEnabled && merchantUpiId.contains("@") && merchantUpiId.length >= 5
}

data class Product(
    val id: String = "",
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
    val createdBy: String = "owner",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
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
}

data class ProductCost(
    val id: String = "",
    val productId: String = "",
    val costPriceInr: Long = 0L,
    val supplierNote: String = "",
    val updatedBy: String = "owner",
    val updatedAtMillis: Long = System.currentTimeMillis()
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
)

data class CustomerOrder(
    val id: String = "",
    val orderReference: String = "",
    val customerId: String = "local_customer",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val landmark: String = "",
    val lineItems: List<OrderLineItem> = emptyList(),
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
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun parsedItems(): List<OrderLineItem> = lineItems
    fun statusEnum(): OrderStatus = OrderStatus.fromKey(orderStatus)
    fun paymentStatusEnum(): PaymentStatus =
        PaymentStatus.entries.firstOrNull { it.name == paymentStatus } ?: PaymentStatus.PENDING
    fun paymentMethodEnum(): PaymentMethodType =
        PaymentMethodType.entries.firstOrNull { it.name == paymentMethod } ?: PaymentMethodType.COD
    fun createdAtDate(): Date = Date(if (createdAtMillis > 0L) createdAtMillis else System.currentTimeMillis())
}

data class OfflineSale(
    val id: String = "",
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val lineItems: List<OrderLineItem> = emptyList(),
    val itemSummary: String = "",
    val subtotalInr: Long = 0L,
    val discountInr: Long = 0L,
    val totalPaidInr: Long = 0L,
    val totalCostInr: Long = 0L,
    val costCoveredRevenueInr: Long = 0L,
    val allCostsRecorded: Boolean = false,
    val paymentMethod: String = "CASH",
    val recordedBy: String = "owner",
    val saleDateEpochDay: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun parsedItems(): List<OrderLineItem> = lineItems
    fun createdAtDate(): Date = Date(if (createdAtMillis > 0L) createdAtMillis else System.currentTimeMillis())
}

data class Expense(
    val id: String = "",
    val title: String = "",
    val category: String = ExpenseCategory.OTHER.name,
    val amountInr: Long = 0L,
    val notes: String = "",
    val recordedBy: String = "owner",
    val expenseDateEpochDay: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun categoryEnum(): ExpenseCategory =
        ExpenseCategory.entries.firstOrNull { it.name == category } ?: ExpenseCategory.OTHER
    fun createdAtDate(): Date = Date(if (createdAtMillis > 0L) createdAtMillis else System.currentTimeMillis())
}

data class InventoryMovement(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val previousStock: Long = 0L,
    val newStock: Long = 0L,
    val delta: Long = 0L,
    val reason: String = "MANUAL_UPDATE",
    val referenceId: String = "",
    val actorId: String = "owner",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun createdAtDate(): Date = Date(if (createdAtMillis > 0L) createdAtMillis else System.currentTimeMillis())
}
