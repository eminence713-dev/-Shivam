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
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCreatesProduct_customerQueriesPublished_andPlacesAtomicOrder_andCrossUserOrderDenied() = runBlocking {
        // 1. Sign in owner and seed trusted admin role in emulator
        val ownerUid = signInTestUser("owner_raju@test.com")
        seedAdminRoleInEmulator(ownerUid, "rajuguptas123@gmail.com")
        val ownerRepo = StoreRepository(firestore)

        val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val createdProductResult = withTimeout(TIMEOUT_MS) {
            ownerRepo.saveProductWithCost(
                product = Product(
                    nameEn = "Kalyanpur Silk Saree $uniqueSuffix",
                    nameHi = "कल्याणपुर सिल्क साड़ी",
                    category = ProductCategory.SAREES.name,
                    descriptionEn = "Pure festive saree",
                    sellingPriceInr = 1250L,
                    originalPriceInr = 1500L,
                    stockCount = 5L,
                    lowStockThreshold = 2L,
                    sizes = listOf("Free Size"),
                    colors = listOf("Maroon"),
                    isPublished = true,
                    isFeatured = true,
                    isDemo = false
                ),
                costPriceInr = 850L
            )
        }
        assertTrue("Owner product creation failed: ${createdProductResult.exceptionOrNull()}", createdProductResult.isSuccess)
        val productId = createdProductResult.getOrThrow()

        // 2. Customer A signs in, observes published products, and places an atomic COD order
        val custAUid = signInTestUser("anita_$uniqueSuffix@test.com")
        val custARepo = StoreRepository(firestore)

        val published = withTimeout(FLOW_TIMEOUT_MS) {
            custARepo.observePublishedProducts().first { list -> list.any { it.id == productId } }
        }
        val targetProduct = published.first { it.id == productId }
        assertEquals(5L, targetProduct.stockCount)

        // Customer A cannot access private owner expenses
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                custARepo.observeExpensesForOwner().first()
            }
            fail("Expected PERMISSION_DENIED when customer reads expenses")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }

        val orderResult = withTimeout(TIMEOUT_MS) {
            custARepo.placeCustomerOrder(
                customerName = "Anita Devi",
                customerPhone = "9876543210",
                deliveryAddress = "Bada Bazar Road, Kalyanpur, Motihari",
                landmark = "Near Post Office",
                paymentMethod = PaymentMethodType.COD,
                cartItems = listOf(
                    CartItem(
                        product = targetProduct,
                        quantity = 2,
                        selectedSize = "Free Size",
                        selectedColor = "Maroon"
                    )
                )
            )
        }
        assertTrue("Customer atomic order failed: ${orderResult.exceptionOrNull()}", orderResult.isSuccess)
        val savedOrder = orderResult.getOrThrow()
        assertEquals(2500L, savedOrder.totalPayableInr)

        // Verify stock decreased atomically to 3
        val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            custARepo.observePublishedProducts().first { list ->
                list.firstOrNull { it.id == productId }?.stockCount == 3L
            }
        }
        assertEquals(3L, updatedProducts.first { it.id == productId }.stockCount)

        // Customer A can observe their own order
        val custAOrders = withTimeout(FLOW_TIMEOUT_MS) {
            custARepo.observeCustomerOrders(custAUid).first { list -> list.any { it.id == savedOrder.id } }
        }
        assertTrue(custAOrders.any { it.id == savedOrder.id })

        // 3. Customer B signs in and is denied reading Customer A's order
        signInTestUser("rahul_$uniqueSuffix@test.com")
        val custBRepo = StoreRepository(firestore)
        val crossReadResult = withTimeout(TIMEOUT_MS) {
            custBRepo.getOrderById(savedOrder.id)
        }
        assertTrue(crossReadResult.isFailure)
        val crossEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossEx?.code)

        // 4. Owner cancels order and verifies stock is atomically restored to 5
        signInTestUser("owner_raju@test.com")
        val cancelResult = withTimeout(TIMEOUT_MS) {
            ownerRepo.updateOrderByOwner(
                orderId = savedOrder.id,
                newOrderStatus = OrderStatus.CANCELLED,
                newPaymentStatus = PaymentStatus.PENDING,
                cancellationReason = "Customer requested size change"
            )
        }
        assertTrue("Owner order cancellation failed: ${cancelResult.exceptionOrNull()}", cancelResult.isSuccess)

        val restockedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeAllProductsForOwner().first { list ->
                list.firstOrNull { it.id == productId }?.stockCount == 5L
            }
        }
        assertEquals(5L, restockedProducts.first { it.id == productId }.stockCount)
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StoreRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublishedProducts().first()
            }
            fail("Expected unauthenticated caller to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
