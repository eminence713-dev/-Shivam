package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.repository.LanguagePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `app name matches Shivam and Riya Mart`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Shivam & Riya Mart", appName)
    }

    @Test
    fun `owner password verifies default, wishlist toggles, and local guest order persists`() = runBlocking {
        val langPrefs = LanguagePreferencesRepository(context)
        assertTrue(langPrefs.verifyOwnerPassword(LanguagePreferencesRepository.DEFAULT_OWNER_PASSWORD))
        assertFalse(langPrefs.verifyOwnerPassword("wrong_pass"))

        val added = langPrefs.toggleWishlist("demo_saree_banarasi")
        assertTrue(added)
        assertTrue("demo_saree_banarasi" in langPrefs.wishlistIdsFlow.first())

        val addedSecondToggle = langPrefs.toggleWishlist("demo_saree_banarasi")
        assertFalse(addedSecondToggle)
        assertFalse("demo_saree_banarasi" in langPrefs.wishlistIdsFlow.first())

        val sampleOrder = CustomerOrder(
            id = "ord_local_test_1",
            orderReference = "SSR-20261027-TEST1",
            customerId = "guest",
            customerName = "Ramesh Prasad",
            customerPhone = "9876543210",
            deliveryAddress = "Bada Bazar, Kalyanpur",
            lineItems = listOf(
                OrderLineItem(
                    productId = "demo_saree_banarasi",
                    productName = "Banarasi Art Silk Festive Saree",
                    quantity = 1L,
                    unitPriceInr = 1150L,
                    lineTotalInr = 1150L
                )
            ),
            itemSummary = "Banarasi Art Silk Festive Saree x1",
            totalQuantity = 1L,
            subtotalInr = 1150L,
            totalPayableInr = 1150L
        )
        langPrefs.saveLocalCustomerOrder(sampleOrder)
        val savedOrders = langPrefs.localCustomerOrdersFlow.first()
        assertTrue(savedOrders.any { it.id == "ord_local_test_1" && it.orderReference == "SSR-20261027-TEST1" })
    }

    @Test
    fun `product discount percent and cart line savings calculate accurately`() {
        val product = Product(
            id = "p1",
            nameEn = "Banarasi Saree",
            sellingPriceInr = 1199L,
            originalPriceInr = 1599L,
            stockCount = 5L
        )
        assertEquals(25, product.discountPercent)
        assertEquals(400L, product.savingsInr)

        val cartItem = CartItem(product = product, quantity = 2)
        assertEquals(2398L, cartItem.lineSubtotalInr)
        assertEquals(800L, cartItem.lineSavingsInr)
    }
}
