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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCreatesProduct_zeroLoginGuestCustomerSeesLiveAndPlacesOrder_ownerConfirmsAndDeductsStock() = runBlocking {
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

        // 2. Zero-login Customer on Phone B (signed out!) sees the new product in real time!
        auth.signOut()
        val guestCustomerRepo = StoreRepository(db = firestore, auth = auth)

        val liveProducts = withTimeout(FLOW_TIMEOUT_MS) {
            guestCustomerRepo.observePublishedProducts().first { list -> list.any { it.id == uniqueProdId } }
        }
        val syncedSaree = liveProducts.first { it.id == uniqueProdId }
        assertEquals(10L, syncedSaree.stockCount)
        assertEquals(25, syncedSaree.discountPercent)

        // 3. Zero-login Customer places a COD order without signing into Google!
        val guestOrderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            guestCustomerRepo.placeCustomerOrder(
                customerName = "Sunil Kumar",
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
        assertTrue("Guest placeCustomerOrder failed: ${guestOrderRes.exceptionOrNull()}", guestOrderRes.isSuccess)
        val guestOrder = guestOrderRes.getOrThrow()
        assertEquals("guest", guestOrder.customerId)
        assertFalse(guestOrder.stockDeducted)

        // 4. Another signed-in non-owner Customer CANNOT read the guest order PII
        signInWithGoogleEmulatorToken(
            sub = "cust_rahul_sub_${UUID.randomUUID().toString().take(6)}",
            email = "rahul.other@gmail.com",
            emailVerified = true
        )
        val otherCustomerRepo = StoreRepository(db = firestore, auth = auth)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            otherCustomerRepo.getOrderById(guestOrder.id)
        }
        assertTrue(crossRead.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            crossRead.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // 5. Verified Shop Owner sees the guest order and confirms it -> atomically deducts stock (10 -> 8)!
        signInWithGoogleEmulatorToken(
            sub = "owner_raju_google_sub",
            email = "eminence713@gmail.com",
            emailVerified = true
        )
        val ownerOrders = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeAllOrdersForOwner().first { list -> list.any { it.id == guestOrder.id } }
        }
        assertTrue(ownerOrders.any { it.id == guestOrder.id })

        val ownerConfirmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.updateOrderByOwner(
                orderId = guestOrder.id,
                newOrderStatus = OrderStatus.CONFIRMED,
                newPaymentStatus = PaymentStatus.PENDING,
                generateInvoiceIfMissing = true
            )
        }
        assertTrue("Owner confirm guest order failed: ${ownerConfirmRes.exceptionOrNull()}", ownerConfirmRes.isSuccess)

        // Verify live stock updated to 8 on unauthenticated customer catalog too!
        auth.signOut()
        val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            guestCustomerRepo.observePublishedProducts().first { list ->
                list.firstOrNull { it.id == uniqueProdId }?.stockCount == 8L
            }
        }
        assertEquals(8L, updatedProducts.first { it.id == uniqueProdId }.stockCount)
    }

    @Test
    fun observeAllProductsForOwner_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val unauthRepo = StoreRepository(db = firestore, auth = auth)

        // Unfiltered list (including drafts) must fail for unauthenticated users
        val result = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observeAllProductsForOwner().first()
            }
        }
        assertTrue(result.isFailure)
        val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val FLOW_TIMEOUT_MS = 10_000L
    }
}
