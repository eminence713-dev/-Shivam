package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShivamMartDatabase
import com.example.data.model.CartItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentStatus
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: ShivamMartDatabase
    private lateinit var repository: StoreRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = ShivamMartDatabase.createInMemory(context)
        repository = StoreRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches Shivam and Riya Mart`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Shivam & Riya Mart", appName)
    }

    @Test
    fun `owner password verifies default and supports updating`() = runBlocking {
        val langPrefs = LanguagePreferencesRepository(context)
        assertTrue(langPrefs.verifyOwnerPassword(LanguagePreferencesRepository.DEFAULT_OWNER_PASSWORD))
        assertFalse(langPrefs.verifyOwnerPassword("wrong_pass"))

        val changeResult = langPrefs.changeOwnerPassword(
            currentPassword = LanguagePreferencesRepository.DEFAULT_OWNER_PASSWORD,
            newPassword = "raju2026"
        )
        assertTrue(changeResult.isSuccess)
        assertTrue(langPrefs.verifyOwnerPassword("raju2026"))
        assertFalse(langPrefs.verifyOwnerPassword(LanguagePreferencesRepository.DEFAULT_OWNER_PASSWORD))

        // Restore default for clean test isolation
        langPrefs.changeOwnerPassword("raju2026", LanguagePreferencesRepository.DEFAULT_OWNER_PASSWORD)
        Unit
    }

    @Test
    fun `room store repository seeds catalog places order and restocks on cancel`() = runBlocking {
        val seededCount = repository.seedDemoProducts().getOrThrow()
        assertTrue(seededCount > 0)

        val published = repository.observePublishedProducts().first()
        assertTrue(published.isNotEmpty())
        val firstProduct = published.first()
        val initialStock = firstProduct.stockCount

        val orderResult = repository.placeCustomerOrder(
            customerName = "Ramesh Kumar",
            customerPhone = "9876543210",
            deliveryAddress = "Bada Bazar, Kalyanpur",
            landmark = "Near Main Chowk",
            paymentMethod = PaymentMethodType.COD,
            cartItems = listOf(
                CartItem(
                    product = firstProduct,
                    quantity = 2,
                    selectedSize = firstProduct.sizes.firstOrNull().orEmpty(),
                    selectedColor = firstProduct.colors.firstOrNull().orEmpty()
                )
            )
        )
        assertTrue(orderResult.isSuccess)
        val savedOrder = orderResult.getOrThrow()

        val afterOrderProducts = repository.observePublishedProducts().first()
        val updatedProduct = afterOrderProducts.first { it.id == firstProduct.id }
        assertEquals(initialStock - 2, updatedProduct.stockCount)

        // Cancel order and verify stock is restored
        val cancelResult = repository.updateOrderByOwner(
            orderId = savedOrder.id,
            newOrderStatus = OrderStatus.CANCELLED,
            newPaymentStatus = PaymentStatus.PENDING,
            cancellationReason = "Customer changed size",
            generateInvoiceIfMissing = false
        )
        assertTrue(cancelResult.isSuccess)

        val afterCancelProducts = repository.observePublishedProducts().first()
        val restockedProduct = afterCancelProducts.first { it.id == firstProduct.id }
        assertEquals(initialStock, restockedProduct.stockCount)
    }
}
