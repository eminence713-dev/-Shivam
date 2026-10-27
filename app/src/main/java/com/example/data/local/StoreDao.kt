package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    @Query("SELECT * FROM store_settings WHERE id = 'public_config' LIMIT 1")
    fun observeSettings(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 'public_config' LIMIT 1")
    suspend fun getSettingsOnce(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(entity: StoreSettingsEntity)

    @Query("SELECT * FROM products WHERE isPublished = 1 ORDER BY createdAtMillis DESC")
    fun observePublishedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY updatedAtMillis DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("SELECT * FROM product_costs")
    fun observeProductCosts(): Flow<List<ProductCostEntity>>

    @Query("SELECT * FROM product_costs WHERE productId = :productId LIMIT 1")
    suspend fun getProductCostById(productId: String): ProductCostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductCost(cost: ProductCostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductCosts(costs: List<ProductCostEntity>)

    @Query("DELETE FROM product_costs WHERE productId = :productId")
    suspend fun deleteProductCostById(productId: String)

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun observeAllOrders(): Flow<List<CustomerOrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): CustomerOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: CustomerOrderEntity)

    @Query("SELECT * FROM offline_sales ORDER BY createdAtMillis DESC")
    fun observeOfflineSales(): Flow<List<OfflineSaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineSale(sale: OfflineSaleEntity)

    @Query("SELECT * FROM expenses ORDER BY createdAtMillis DESC")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("SELECT * FROM inventory_movements ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeInventoryMovements(): Flow<List<InventoryMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryMovement(movement: InventoryMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryMovements(movements: List<InventoryMovementEntity>)
}
