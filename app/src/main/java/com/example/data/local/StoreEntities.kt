package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CustomerOrder
import com.example.data.model.Expense
import com.example.data.model.InventoryMovement
import com.example.data.model.OfflineSale
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductCost
import com.example.data.model.StoreSettings

object StoreCodec {
    private const val LIST_SEP = "\u001F"
    private const val FIELD_SEP = "\u001E"

    fun encodeStringList(items: List<String>): String =
        items.map { it.replace(LIST_SEP, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(LIST_SEP)

    fun decodeStringList(encoded: String): List<String> =
        if (encoded.isBlank()) emptyList()
        else encoded.split(LIST_SEP).map { it.trim() }.filter { it.isNotEmpty() }

    fun encodeLineItems(items: List<OrderLineItem>): String =
        items.joinToString(LIST_SEP) { item ->
            listOf(
                item.productId.replace(FIELD_SEP, " ").replace(LIST_SEP, " "),
                item.productName.replace(FIELD_SEP, " ").replace(LIST_SEP, " "),
                item.category.replace(FIELD_SEP, " ").replace(LIST_SEP, " "),
                item.quantity.toString(),
                item.unitPriceInr.toString(),
                item.lineTotalInr.toString(),
                item.selectedSize.replace(FIELD_SEP, " ").replace(LIST_SEP, " "),
                item.selectedColor.replace(FIELD_SEP, " ").replace(LIST_SEP, " ")
            ).joinToString(FIELD_SEP)
        }

    fun decodeLineItems(encoded: String): List<OrderLineItem> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(LIST_SEP).mapNotNull { row ->
            val parts = row.split(FIELD_SEP)
            if (parts.size < 6) null
            else OrderLineItem(
                productId = parts.getOrElse(0) { "" },
                productName = parts.getOrElse(1) { "" },
                category = parts.getOrElse(2) { ProductCategory.SAREES.name },
                quantity = parts.getOrElse(3) { "1" }.toLongOrNull() ?: 1L,
                unitPriceInr = parts.getOrElse(4) { "0" }.toLongOrNull() ?: 0L,
                lineTotalInr = parts.getOrElse(5) { "0" }.toLongOrNull() ?: 0L,
                selectedSize = parts.getOrElse(6) { "" },
                selectedColor = parts.getOrElse(7) { "" }
            )
        }
    }
}

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val id: String = "public_config",
    val storeNameEn: String,
    val storeNameHi: String,
    val ownerName: String,
    val phone: String,
    val whatsappNumber: String,
    val email: String,
    val addressEn: String,
    val addressHi: String,
    val promoBannerEn: String,
    val promoBannerHi: String,
    val deliveryCoverageEn: String,
    val deliveryCoverageHi: String,
    val deliveryChargeInr: Long,
    val freeDeliveryThresholdInr: Long,
    val minOrderAmountInr: Long,
    val expectedDeliveryEn: String,
    val expectedDeliveryHi: String,
    val returnPolicyEn: String,
    val returnPolicyHi: String,
    val policiesConfirmed: Boolean,
    val codEnabled: Boolean,
    val upiEnabled: Boolean,
    val merchantUpiId: String,
    val merchantPayeeName: String,
    val updatedBy: String,
    val updatedAtMillis: Long
) {
    fun toModel(): StoreSettings = StoreSettings(
        id = id,
        storeNameEn = storeNameEn,
        storeNameHi = storeNameHi,
        ownerName = ownerName,
        phone = phone,
        whatsappNumber = whatsappNumber,
        email = email,
        addressEn = addressEn,
        addressHi = addressHi,
        promoBannerEn = promoBannerEn,
        promoBannerHi = promoBannerHi,
        deliveryCoverageEn = deliveryCoverageEn,
        deliveryCoverageHi = deliveryCoverageHi,
        deliveryChargeInr = deliveryChargeInr,
        freeDeliveryThresholdInr = freeDeliveryThresholdInr,
        minOrderAmountInr = minOrderAmountInr,
        expectedDeliveryEn = expectedDeliveryEn,
        expectedDeliveryHi = expectedDeliveryHi,
        returnPolicyEn = returnPolicyEn,
        returnPolicyHi = returnPolicyHi,
        policiesConfirmed = policiesConfirmed,
        codEnabled = codEnabled,
        upiEnabled = upiEnabled,
        merchantUpiId = merchantUpiId,
        merchantPayeeName = merchantPayeeName,
        updatedBy = updatedBy,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromModel(s: StoreSettings): StoreSettingsEntity = StoreSettingsEntity(
            id = "public_config",
            storeNameEn = s.storeNameEn.trim().take(150),
            storeNameHi = s.storeNameHi.trim().ifBlank { s.storeNameEn.trim() }.take(150),
            ownerName = s.ownerName.trim().take(100),
            phone = s.phone.trim().take(20),
            whatsappNumber = s.whatsappNumber.trim().take(20),
            email = s.email.trim().take(120),
            addressEn = s.addressEn.trim().take(250),
            addressHi = s.addressHi.trim().ifBlank { s.addressEn.trim() }.take(250),
            promoBannerEn = s.promoBannerEn.trim().take(300),
            promoBannerHi = s.promoBannerHi.trim().take(300),
            deliveryCoverageEn = s.deliveryCoverageEn.trim().take(300),
            deliveryCoverageHi = s.deliveryCoverageHi.trim().take(300),
            deliveryChargeInr = s.deliveryChargeInr.coerceIn(0L, 10000L),
            freeDeliveryThresholdInr = s.freeDeliveryThresholdInr.coerceIn(0L, 1000000L),
            minOrderAmountInr = s.minOrderAmountInr.coerceIn(0L, 1000000L),
            expectedDeliveryEn = s.expectedDeliveryEn.trim().take(300),
            expectedDeliveryHi = s.expectedDeliveryHi.trim().take(300),
            returnPolicyEn = s.returnPolicyEn.trim().take(600),
            returnPolicyHi = s.returnPolicyHi.trim().take(600),
            policiesConfirmed = s.policiesConfirmed,
            codEnabled = s.codEnabled,
            upiEnabled = s.upiEnabled,
            merchantUpiId = s.merchantUpiId.trim().take(100),
            merchantPayeeName = s.merchantPayeeName.trim().take(120),
            updatedBy = "owner",
            updatedAtMillis = System.currentTimeMillis()
        )
    }
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameHi: String,
    val category: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val sellingPriceInr: Long,
    val originalPriceInr: Long,
    val stockCount: Long,
    val lowStockThreshold: Long,
    val sizesEncoded: String,
    val colorsEncoded: String,
    val variantNotes: String,
    val sampleImageKey: String,
    val imageUrl: String,
    val imageBase64: String,
    val isPublished: Boolean,
    val isFeatured: Boolean,
    val isDemo: Boolean,
    val soldCount: Long,
    val lastOrderId: String,
    val createdBy: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): Product = Product(
        id = id,
        nameEn = nameEn,
        nameHi = nameHi,
        category = category,
        descriptionEn = descriptionEn,
        descriptionHi = descriptionHi,
        sellingPriceInr = sellingPriceInr,
        originalPriceInr = originalPriceInr,
        stockCount = stockCount,
        lowStockThreshold = lowStockThreshold,
        sizes = StoreCodec.decodeStringList(sizesEncoded),
        colors = StoreCodec.decodeStringList(colorsEncoded),
        variantNotes = variantNotes,
        sampleImageKey = sampleImageKey,
        imageUrl = imageUrl,
        imageBase64 = imageBase64,
        isPublished = isPublished,
        isFeatured = isFeatured,
        isDemo = isDemo,
        soldCount = soldCount,
        lastOrderId = lastOrderId,
        createdBy = createdBy,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromModel(p: Product, idOverride: String = p.id, nowMillis: Long = System.currentTimeMillis()): ProductEntity =
            ProductEntity(
                id = idOverride,
                nameEn = p.nameEn.trim().take(140),
                nameHi = p.nameHi.trim().take(140),
                category = p.categoryEnum().name,
                descriptionEn = p.descriptionEn.trim().take(1000),
                descriptionHi = p.descriptionHi.trim().take(1000),
                sellingPriceInr = p.sellingPriceInr.coerceIn(1L, 500000L),
                originalPriceInr = p.originalPriceInr.coerceIn(0L, 500000L),
                stockCount = p.stockCount.coerceIn(0L, 100000L),
                lowStockThreshold = p.lowStockThreshold.coerceIn(0L, 1000L),
                sizesEncoded = StoreCodec.encodeStringList(p.sizes.take(15)),
                colorsEncoded = StoreCodec.encodeStringList(p.colors.take(15)),
                variantNotes = p.variantNotes.trim().take(300),
                sampleImageKey = p.sampleImageKey.trim().take(60),
                imageUrl = p.imageUrl.trim().take(600),
                imageBase64 = p.imageBase64.take(340000),
                isPublished = p.isPublished,
                isFeatured = p.isFeatured,
                isDemo = p.isDemo,
                soldCount = p.soldCount.coerceAtLeast(0L),
                lastOrderId = p.lastOrderId,
                createdBy = p.createdBy.ifBlank { "owner" },
                createdAtMillis = if (p.createdAtMillis > 0L) p.createdAtMillis else nowMillis,
                updatedAtMillis = nowMillis
            )
    }
}

@Entity(tableName = "product_costs")
data class ProductCostEntity(
    @PrimaryKey val productId: String,
    val costPriceInr: Long,
    val supplierNote: String,
    val updatedBy: String,
    val updatedAtMillis: Long
) {
    fun toModel(): ProductCost = ProductCost(
        id = productId,
        productId = productId,
        costPriceInr = costPriceInr,
        supplierNote = supplierNote,
        updatedBy = updatedBy,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "orders")
data class CustomerOrderEntity(
    @PrimaryKey val id: String,
    val orderReference: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val landmark: String,
    val itemsEncoded: String,
    val itemSummary: String,
    val totalQuantity: Long,
    val subtotalInr: Long,
    val deliveryChargeInr: Long,
    val discountInr: Long,
    val totalPayableInr: Long,
    val deliveryFeeConfirmed: Boolean,
    val paymentMethod: String,
    val paymentStatus: String,
    val orderStatus: String,
    val stockDeducted: Boolean,
    val cancellationReason: String,
    val invoiceNumber: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): CustomerOrder = CustomerOrder(
        id = id,
        orderReference = orderReference,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        deliveryAddress = deliveryAddress,
        landmark = landmark,
        lineItems = StoreCodec.decodeLineItems(itemsEncoded),
        itemSummary = itemSummary,
        totalQuantity = totalQuantity,
        subtotalInr = subtotalInr,
        deliveryChargeInr = deliveryChargeInr,
        discountInr = discountInr,
        totalPayableInr = totalPayableInr,
        deliveryFeeConfirmed = deliveryFeeConfirmed,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        orderStatus = orderStatus,
        stockDeducted = stockDeducted,
        cancellationReason = cancellationReason,
        invoiceNumber = invoiceNumber,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "offline_sales")
data class OfflineSaleEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val itemsEncoded: String,
    val itemSummary: String,
    val subtotalInr: Long,
    val discountInr: Long,
    val totalPaidInr: Long,
    val totalCostInr: Long,
    val costCoveredRevenueInr: Long,
    val allCostsRecorded: Boolean,
    val paymentMethod: String,
    val recordedBy: String,
    val saleDateEpochDay: Long,
    val createdAtMillis: Long
) {
    fun toModel(): OfflineSale = OfflineSale(
        id = id,
        invoiceNumber = invoiceNumber,
        customerName = customerName,
        customerPhone = customerPhone,
        lineItems = StoreCodec.decodeLineItems(itemsEncoded),
        itemSummary = itemSummary,
        subtotalInr = subtotalInr,
        discountInr = discountInr,
        totalPaidInr = totalPaidInr,
        totalCostInr = totalCostInr,
        costCoveredRevenueInr = costCoveredRevenueInr,
        allCostsRecorded = allCostsRecorded,
        paymentMethod = paymentMethod,
        recordedBy = recordedBy,
        saleDateEpochDay = saleDateEpochDay,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val amountInr: Long,
    val notes: String,
    val recordedBy: String,
    val expenseDateEpochDay: Long,
    val createdAtMillis: Long
) {
    fun toModel(): Expense = Expense(
        id = id,
        title = title,
        category = category,
        amountInr = amountInr,
        notes = notes,
        recordedBy = recordedBy,
        expenseDateEpochDay = expenseDateEpochDay,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "inventory_movements")
data class InventoryMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val previousStock: Long,
    val newStock: Long,
    val delta: Long,
    val reason: String,
    val referenceId: String,
    val actorId: String,
    val createdAtMillis: Long
) {
    fun toModel(): InventoryMovement = InventoryMovement(
        id = id,
        productId = productId,
        productName = productName,
        previousStock = previousStock,
        newStock = newStock,
        delta = delta,
        reason = reason,
        referenceId = referenceId,
        actorId = actorId,
        createdAtMillis = createdAtMillis
    )
}
