package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.model.CustomerOrder
import com.example.data.model.OfflineSale
import com.example.data.model.OrderLineItem
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrintableInvoiceData(
    val invoiceNumber: String,
    val orderReference: String,
    val dateFormatted: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val paymentLabel: String,
    val items: List<OrderLineItem>,
    val subtotalInr: Long,
    val deliveryInr: Long,
    val discountInr: Long,
    val totalPayableInr: Long
)

object InvoicePrintHelper {

    fun fromCustomerOrder(order: CustomerOrder): PrintableInvoiceData {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            .format(order.createdAtDate())
        return PrintableInvoiceData(
            invoiceNumber = order.invoiceNumber.ifBlank { "BILL-${order.orderReference.takeLast(8)}" },
            orderReference = order.orderReference,
            dateFormatted = dateStr,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            customerAddress = buildString {
                append(order.deliveryAddress)
                if (order.landmark.isNotBlank()) append(" (${order.landmark})")
            },
            paymentLabel = "${order.paymentMethod} (${order.paymentStatus})",
            items = order.parsedItems(),
            subtotalInr = order.subtotalInr,
            deliveryInr = order.deliveryChargeInr,
            discountInr = order.discountInr,
            totalPayableInr = order.totalPayableInr
        )
    }

    fun fromOfflineSale(sale: OfflineSale): PrintableInvoiceData {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
            .format(sale.createdAtDate())
        return PrintableInvoiceData(
            invoiceNumber = sale.invoiceNumber,
            orderReference = "Shop Counter Sale",
            dateFormatted = dateStr,
            customerName = sale.customerName.ifBlank { "Walk-in Customer" },
            customerPhone = sale.customerPhone.ifBlank { "-" },
            customerAddress = "Bada Bazar, Kalyanpur Store Counter",
            paymentLabel = "${sale.paymentMethod} (PAID AT COUNTER)",
            items = sale.parsedItems(),
            subtotalInr = sale.subtotalInr,
            deliveryInr = 0L,
            discountInr = sale.discountInr,
            totalPayableInr = sale.totalPaidInr
        )
    }

    fun printInvoice(context: Context, settings: StoreSettings, data: PrintableInvoiceData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "ShivamRiyaMart_${data.invoiceNumber}"

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var pdfDocument: PrintedPdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    pdfDocument = PrintedPdfDocument(context, newAttributes)
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("${data.invoiceNumber}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    val doc = pdfDocument ?: run {
                        callback.onWriteFailed("PDF document not initialized")
                        return
                    }
                    val page = doc.startPage(0)
                    drawInvoiceCanvas(page.canvas, settings, data)
                    doc.finishPage(page)

                    try {
                        doc.writeTo(FileOutputStream(destination.fileDescriptor))
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage)
                    } finally {
                        doc.close()
                        pdfDocument = null
                    }
                }
            },
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    private fun drawInvoiceCanvas(canvas: Canvas, settings: StoreSettings, data: PrintableInvoiceData) {
        val maroonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6E1423")
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 16f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#23191B")
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 10.5f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#23191B")
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#B6862C")
            strokeWidth = 1.2f
        }

        val left = 36f
        val right = (canvas.width - 36).toFloat()
        var y = 44f

        canvas.drawText(settings.storeNameEn, left, y, maroonPaint)
        y += 16f
        canvas.drawText("${settings.addressEn}  |  Ph: ${settings.phone}", left, y, bodyPaint)
        y += 14f
        canvas.drawText("Proprietor: ${settings.ownerName}  |  Email: ${settings.email}", left, y, bodyPaint)
        y += 12f
        canvas.drawLine(left, y, right, y, linePaint)

        y += 20f
        canvas.drawText("CUSTOMER BILL / INVOICE: ${data.invoiceNumber}", left, y, boldPaint)
        canvas.drawText("Date: ${data.dateFormatted}", right - 185f, y, bodyPaint)
        y += 15f
        canvas.drawText("Ref: ${data.orderReference}", left, y, bodyPaint)
        canvas.drawText("Payment: ${data.paymentLabel}", right - 185f, y, bodyPaint)

        y += 18f
        canvas.drawText("Customer: ${data.customerName} (${data.customerPhone})", left, y, boldPaint)
        y += 14f
        canvas.drawText("Address: ${data.customerAddress.take(90)}", left, y, bodyPaint)

        y += 16f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 16f
        canvas.drawText("Item", left, y, boldPaint)
        canvas.drawText("Qty", right - 140f, y, boldPaint)
        canvas.drawText("Rate", right - 95f, y, boldPaint)
        canvas.drawText("Amount", right - 45f, y, boldPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, linePaint)

        data.items.take(22).forEach { item ->
            y += 16f
            val variant = listOf(item.selectedSize, item.selectedColor).filter { it.isNotBlank() }.joinToString("/")
            val label = if (variant.isBlank()) item.productName else "${item.productName} ($variant)"
            canvas.drawText(label.take(48), left, y, bodyPaint)
            canvas.drawText("${item.quantity}", right - 138f, y, bodyPaint)
            canvas.drawText("Rs.${item.unitPriceInr}", right - 98f, y, bodyPaint)
            canvas.drawText("Rs.${item.lineTotalInr}", right - 48f, y, bodyPaint)
        }

        y += 14f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f
        canvas.drawText("Items Subtotal: Rs.${data.subtotalInr}", right - 175f, y, bodyPaint)
        if (data.deliveryInr > 0) {
            y += 14f
            canvas.drawText("Delivery Charge: Rs.${data.deliveryInr}", right - 175f, y, bodyPaint)
        }
        if (data.discountInr > 0) {
            y += 14f
            canvas.drawText("Discount: -Rs.${data.discountInr}", right - 175f, y, bodyPaint)
        }
        y += 18f
        canvas.drawText("TOTAL PAYABLE: Rs.${data.totalPayableInr}", right - 175f, y, maroonPaint)

        y += 28f
        canvas.drawText("Policy: ${settings.returnPolicyEn.take(105)}", left, y, bodyPaint)
        y += 16f
        canvas.drawText("Thank you for shopping at ${settings.storeNameEn}, Kalyanpur!", left, y, boldPaint)
    }

    fun exportCsvReport(
        context: Context,
        products: List<Product>,
        orders: List<CustomerOrder>,
        offlineSales: List<OfflineSale>
    ) {
        val csv = buildString {
            appendLine("=== PRODUCTS INVENTORY ===")
            appendLine("ProductID,Name,Category,SellingPriceINR,Stock,SoldCount,Published,IsDemo")
            products.forEach { p ->
                appendLine("\"${p.id}\",\"${p.nameEn.replace("\"", "'")}\",${p.category},${p.sellingPriceInr},${p.stockCount},${p.soldCount},${p.isPublished},${p.isDemo}")
            }
            appendLine()
            appendLine("=== ONLINE CUSTOMER ORDERS ===")
            appendLine("OrderRef,CustomerName,Phone,Items,SubtotalINR,DeliveryINR,TotalINR,PaymentMethod,PaymentStatus,OrderStatus")
            orders.forEach { o ->
                appendLine("${o.orderReference},\"${o.customerName}\",${o.customerPhone},\"${o.itemSummary.replace("\"", "'")}\",${o.subtotalInr},${o.deliveryChargeInr},${o.totalPayableInr},${o.paymentMethod},${o.paymentStatus},${o.orderStatus}")
            }
            appendLine()
            appendLine("=== OFFLINE SHOP COUNTER SALES ===")
            appendLine("InvoiceNo,Customer,Items,SubtotalINR,DiscountINR,TotalPaidINR,PaymentMethod")
            offlineSales.forEach { s ->
                appendLine("${s.invoiceNumber},\"${s.customerName}\",\"${s.itemSummary.replace("\"", "'")}\",${s.subtotalInr},${s.discountInr},${s.totalPaidInr},${s.paymentMethod}")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Shri Shivam Readymade — Store Inventory & Sales Export")
            putExtra(Intent.EXTRA_TEXT, csv)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Store CSV Data"))
    }
}
