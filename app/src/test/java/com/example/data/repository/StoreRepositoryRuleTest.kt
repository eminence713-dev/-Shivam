package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCreatesProduct_customerSeesLiveAndPlacesAtomicOrder_crossCustomerDenied() = runBlocking {
        // 1. Verified Shop Owner adds a product on Phone A
        signInWithGoogleEmulatorToken(
            sub = "owner_raju_google_sub",
            email = "rajuguptas123@gmail.com",
            emailVerified = true
        )
        val ownerRepo = StoreRepository(db = firestore, auth = auth)

        val uniqueProdId = "prod_live_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.saveProductWithCost(
                product = Product(
                    id = uniqueProdId,
                    nameEn = "Kalyanpur Festive Banarasi Saree",
                    nameHi = "कल्याणपुर बनारसी साड़ी",
                    category = ProductCategory.SAREES.name,
                    sellingPriceInr = 1199L,
                    originalPriceInr = 1599L,
                    stockCount = 10L,
                    lowStockThreshold = 2L,
                    sizes = listOf("Free Size"),
                    colors = listOf("Maroon Gold"),
                    isPublished = true,
                    isFeatured = true
                ),
                costPriceInr = 800L,
                supplierNote = "Surat Wholesale"
            )
        }
        assertTrue("Owner saveProductWithCost failed: ${saveRes.exceptionOrNull()}", saveRes.isSuccess)

        // 2. Customer A signs in on Phone B and sees the new product in real time!
        val custAUid = signInWithGoogleEmulatorToken(
            sub = "cust_anita_sub_${UUID.randomUUID().toString().take(6)}",
            email = "anita.customer@gmail.com",
            emailVerified = true
        )
        val customerARepo = StoreRepository(db = firestore, auth = auth)

        val liveProducts = withTimeout(FLOW_TIMEOUT_MS) {
            customerARepo.observePublishedProducts().first { list -> list.any { it.id == uniqueProdId } }
        }
        val syncedSaree = liveProducts.first { it.id == uniqueProdId }
        assertEquals(10L, syncedSaree.stockCount)
        assertEquals(25, syncedSaree.discountPercent)

        // 3. Customer A places an online COD order -> atomically deducts stock (10 -> 8)
        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerARepo.placeCustomerOrder(
                customerName = "Anita Devi",
                customerPhone = "9876543210",
                deliveryAddress = "Ward 5, Bada Bazar, Kalyanpur",
                landmark = "Near Main Chowk",
                paymentMethod = PaymentMethodType.COD,
                cartItems = listOf(
                    CartItem(
                        product = syncedSaree,
                        quantity = 2,
                        selectedSize = "Free Size",
                        selectedColor = "Maroon Gold"
                    )
                )
            )
        }
        assertTrue("Customer placeCustomerOrder failed: ${orderRes.exceptionOrNull()}", orderRes.isSuccess)
        val createdOrder = orderRes.getOrThrow()

        // Verify live stock updated to 8
        val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            customerARepo.observePublishedProducts().first { list ->
                list.firstOrNull { it.id == uniqueProdId }?.stockCount == 8L
            }
        }
        assertEquals(8L, updatedProducts.first { it.id == uniqueProdId }.stockCount)

        // Verify Customer A can observe their own order
        val custAOrders = withTimeout(FLOW_TIMEOUT_MS) {
            customerARepo.observeCustomerOrders(custAUid).first { list -> list.any { it.id == createdOrder.id } }
        }
        assertTrue(custAOrders.any { it.id == createdOrder.id })

        // 4. Customer B signs in on Phone C -> CANNOT read Customer A's order PII
        signInWithGoogleEmulatorToken(
            sub = "cust_rahul_sub_${UUID.randomUUID().toString().take(6)}",
            email = "rahul.other@gmail.com",
            emailVerified = true
        )
        val customerBRepo = StoreRepository(db = firestore, auth = auth)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerBRepo.getOrderById(createdOrder.id)
        }
        assertTrue(crossRead.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            crossRead.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // 5. Shop Owner updates order status to CONFIRMED -> syncs back cleanly
        signInWithGoogleEmulatorToken(
            sub = "owner_raju_google_sub",
            email = "eminence713@gmail.com",
            emailVerified = true
        )
        val ownerConfirmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.updateOrderByOwner(
                orderId = createdOrder.id,
                newOrderStatus = OrderStatus.CONFIRMED,
                newPaymentStatus = PaymentStatus.PENDING,
                generateInvoiceIfMissing = true
            )
        }
        assertTrue("Owner confirm order failed: ${ownerConfirmRes.exceptionOrNull()}", ownerConfirmRes.isSuccess)
    }

    @Test
    fun observePublishedProducts_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val unauthRepo = StoreRepository(db = firestore, auth = auth)

        val result = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observePublishedProducts().first()
            }
        }
        assertTrue(result.isFailure)
        val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val FLOW_TIMEOUT_MS = 10_000L
    }
}
