package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.CustomerOrder
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.OfflineSale
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ReportDateRange
import com.example.ui.viewmodel.SalesAnalyticsSummary
import com.example.util.InvoicePrintHelper
import com.example.util.formatInr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPosCounterSection(
    products: List<Product>,
    offlineSales: List<OfflineSale>,
    lastSavedSale: OfflineSale?,
    settings: StoreSettings,
    lang: AppLanguage,
    isBusy: Boolean,
    onRecordSale: (String, String, List<OrderLineItem>, Long, String) -> Unit,
    onDismissLastSale: () -> Unit
) {
    val context = LocalContext.current
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var billItems by remember { mutableStateOf<List<OrderLineItem>>(emptyList()) }
    var discountInput by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var customItemName by remember { mutableStateOf("") }
    var customItemPrice by remember { mutableStateOf("") }
    var customItemQty by remember { mutableStateOf("1") }
    var prodSearch by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (lang == AppLanguage.HINDI) "दुकान काउंटर बिलिंग (Offline POS)" else "Walk-in Shop Counter Sale & Bill",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Recorded separately from online orders to prevent duplicate revenue counting while deducting linked catalog stock atomically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (lastSavedSale != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.5.dp, VerifiedGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerifiedGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Saved Bill ${lastSavedSale.invoiceNumber} • ${formatInr(lastSavedSale.totalPaidInr)}", fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                InvoicePrintHelper.printInvoice(
                                    context,
                                    settings,
                                    InvoicePrintHelper.fromOfflineSale(lastSavedSale)
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print / Save PDF Bill")
                        }
                        OutlinedButton(onClick = onDismissLastSale) {
                            Text("New Bill")
                        }
                    }
                }
            }
        }

        // Quick Add from Store Catalogue
        OutlinedTextField(
            value = prodSearch,
            onValueChange = { prodSearch = it },
            label = { Text("Tap Catalogue Item to Add to Counter Bill") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val matchingProds = products.filter {
            it.stockCount > 0 && (prodSearch.isBlank() || it.nameEn.contains(prodSearch.trim(), ignoreCase = true))
        }.take(12)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matchingProds, key = { it.id }) { prod ->
                OutlinedButton(
                    onClick = {
                        val existing = billItems.firstOrNull { it.productId == prod.id }
                        val currentQty = existing?.quantity ?: 0L
                        if (currentQty + 1L <= prod.stockCount) {
                            billItems = if (existing != null) {
                                billItems.map {
                                    if (it.productId == prod.id) {
                                        val nextQ = it.quantity + 1L
                                        it.copy(quantity = nextQ, lineTotalInr = nextQ * it.unitPriceInr)
                                    } else it
                                }
                            } else {
                                billItems + OrderLineItem(
                                    productId = prod.id,
                                    productName = prod.nameEn,
                                    category = prod.category,
                                    quantity = 1L,
                                    unitPriceInr = prod.sellingPriceInr,
                                    lineTotalInr = prod.sellingPriceInr
                                )
                            }
                        }
                    }
                ) {
                    Text("${prod.nameEn.take(20)} (${formatInr(prod.sellingPriceInr)})")
                }
            }
        }

        // Custom Walk-in Item Row
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Or Add Unlisted Counter Item:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = customItemQty,
                        onValueChange = { customItemQty = it.filter { c -> c.isDigit() } },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = customItemPrice,
                        onValueChange = { customItemPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Rate ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        val rate = customItemPrice.toLongOrNull() ?: 0L
                        val qty = (customItemQty.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                        if (customItemName.isNotBlank() && rate > 0L) {
                            billItems = billItems + OrderLineItem(
                                productId = "",
                                productName = customItemName.trim(),
                                category = ProductCategory.SHIRTS.name,
                                quantity = qty,
                                unitPriceInr = rate,
                                lineTotalInr = rate * qty
                            )
                            customItemName = ""
                            customItemPrice = ""
                            customItemQty = "1"
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Custom Line")
                }
            }
        }

        // Current Counter Bill Items
        if (billItems.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, AntiqueGold)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current Bill Items", fontWeight = FontWeight.Bold)
                    billItems.forEachIndexed { idx, line ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${line.productName} × ${line.quantity}", modifier = Modifier.weight(1f))
                            Text(formatInr(line.lineTotalInr), fontWeight = FontWeight.SemiBold)
                            IconButton(
                                onClick = { billItems = billItems.filterIndexed { i, _ -> i != idx } }
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    val sub = billItems.sumOf { it.lineTotalInr }
                    val disc = (discountInput.toLongOrNull() ?: 0L).coerceIn(0L, sub)
                    val netPayable = (sub - disc).coerceAtLeast(0L)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name (Opt)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it.filter { c -> c.isDigit() }.take(10) },
                            label = { Text("Mobile (Opt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Counter Discount ₹ (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASH", "UPI", "CARD").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }

                    Text(
                        text = "Net Counter Total: ${formatInr(netPayable)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            onRecordSale(customerName, customerPhone, billItems, disc, paymentMode)
                            billItems = emptyList()
                            discountInput = ""
                            customerName = ""
                            customerPhone = ""
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_counter_sale_button")
                    ) {
                        Text("Save Counter Sale & Generate Bill")
                    }
                }
            }
        }

        // Recent Counter Sales History
        if (offlineSales.isNotEmpty()) {
            HorizontalDivider()
            Text("Recent Shop Counter Bills (${offlineSales.size})", style = MaterialTheme.typography.titleMedium)
            offlineSales.take(15).forEach { sale ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sale.invoiceNumber} • ${formatInr(sale.totalPaidInr)} (${sale.paymentMethod})", fontWeight = FontWeight.Bold)
                            Text(sale.itemSummary, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                InvoicePrintHelper.printInvoice(context, settings, InvoicePrintHelper.fromOfflineSale(sale))
                            }
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = "Print counter invoice", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSalesAndProfitSection(
    selectedRange: ReportDateRange,
    onSelectRange: (ReportDateRange) -> Unit,
    summary: SalesAnalyticsSummary,
    products: List<Product>,
    orders: List<CustomerOrder>,
    offlineSales: List<OfflineSale>,
    lang: AppLanguage
) {
    val context = LocalContext.current
    val lowStockItems = products.filter { it.stockCount <= it.lowStockThreshold }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (lang == AppLanguage.HINDI) "बिक्री और वास्तविक मुनाफा" else "Sales & Honest Profit Report",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = { InvoicePrintHelper.exportCsvReport(context, products, orders, offlineSales) },
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export CSV")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReportDateRange.entries) { r ->
                FilterChip(
                    selected = selectedRange == r,
                    onClick = { onSelectRange(r) },
                    label = { Text(if (lang == AppLanguage.HINDI) r.labelHi else r.labelEn) }
                )
            }
        }

        // Revenue vs Profit Transparency Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AntiqueGold)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportRow("Online Orders Gross Subtotal", formatInr(summary.onlineGrossRevenueInr))
                ReportRow("Shop Counter Gross Subtotal", formatInr(summary.offlineGrossSubtotalInr))
                ReportRow("Total Discounts Given", "- ${formatInr(summary.totalDiscountsInr)}")
                HorizontalDivider()
                ReportRow("Net Active Sales Revenue", formatInr(summary.netCollectedAndActiveRevenueInr), bold = true)
                ReportRow("Cancelled Orders (${summary.cancelledOrdersCount})", formatInr(summary.cancelledOrdersValueInr))
                ReportRow("Refunded Orders Value", formatInr(summary.refundedValueInr))
                ReportRow("Recorded Shop Expenses", formatInr(summary.totalExpensesInr))
                HorizontalDivider()
                Text(
                    text = "Gross Profit (Calculated ONLY on Cost-Recorded Items)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                ReportRow("Revenue of Cost-Recorded Items", formatInr(summary.costCoveredNetRevenueInr))
                ReportRow("Recorded Purchase Cost (COGS)", "- ${formatInr(summary.recordedCostOfGoodsSoldInr)}")
                ReportRow("Verified Gross Margin", formatInr(summary.trueGrossProfitOnCostedItemsInr), bold = true)
                ReportRow(
                    "Net Margin After Shop Expenses",
                    formatInr(summary.trueGrossProfitOnCostedItemsInr - summary.totalExpensesInr),
                    bold = true
                )

                if (summary.uncostedSoldUnitsCount > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${summary.uncostedSoldUnitsCount} sold unit(s) did not have a purchase cost entered and are excluded from Gross Margin so revenue is never falsely counted as profit.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Low-Stock Report
        if (lowStockItems.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Low / Out-of-Stock Alert (${lowStockItems.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    lowStockItems.take(10).forEach { p ->
                        Text("• ${p.nameEn} — Stock: ${p.stockCount} pcs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Category-wise Sales
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category-wise Gross Sales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                summary.categoryWiseSales.forEach { (cat, amt) ->
                    ReportRow(cat.label(lang), formatInr(amt))
                }
            }
        }

        // Product-wise Sales
        if (summary.productWiseSales.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Top Selling Products (Real Store Data)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    summary.productWiseSales.take(10).forEach { (name, qty, rev) ->
                        ReportRow("$name ($qty pcs)", formatInr(rev))
                    }
                }
            }
        }

        // Date-wise Sales
        if (summary.dateWiseTotals.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date-wise Net Sales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    summary.dateWiseTotals.forEach { (day, amt) ->
                        ReportRow(day, formatInr(amt))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminExpensesSection(
    expenses: List<Expense>,
    lang: AppLanguage,
    isBusy: Boolean,
    onAddExpense: (String, ExpenseCategory, Long, String, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.STOCK_PURCHASE) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (lang == AppLanguage.HINDI) "नया दुकान खर्च जोड़ें" else "Record Shop Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; err = null },
                    label = { Text("Expense Title (e.g. Surat Saree Lot / Electricity) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label(lang)) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() }; err = null },
                        label = { Text("Amount ₹ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Opt)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }
                if (err != null) {
                    Text(err.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val amt = amount.toLongOrNull() ?: 0L
                        if (title.trim().length < 2 || amt <= 0L) {
                            err = "Enter expense title and valid INR amount."
                        } else {
                            onAddExpense(title, category, amt, notes) {
                                title = ""
                                amount = ""
                                notes = ""
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("save_expense_button")
                ) {
                    Text("Save Expense")
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(expenses, key = { it.id }) { exp ->
                val dt = SimpleDateFormat("dd MMM yyyy", Locale.US).format(exp.createdAt?.toDate() ?: Date())
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.title, fontWeight = FontWeight.SemiBold)
                            Text("${exp.categoryEnum().label(lang)} • $dt", style = MaterialTheme.typography.bodySmall)
                            if (exp.notes.isNotBlank()) {
                                Text(exp.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatInr(exp.amountInr), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            IconButton(onClick = { onDeleteExpense(exp.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete expense")
                            }
                        }
                    }
                }
            }
        }
    }
}
