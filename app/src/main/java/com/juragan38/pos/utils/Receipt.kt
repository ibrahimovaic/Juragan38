package com.juragan38.pos.utils

import com.juragan38.pos.data.local.Transaction

/** Satu baris item di struk, dipakai bersama oleh layar Kasir dan Riwayat. */
data class ReceiptItem(
    val name: String,
    val price: Double,
    val qty: Int,
    val subtotal: Double
)

/**
 * Susun teks struk printer Bluetooth 58mm (30 karakter per baris).
 * Header = nama outlet + alamat outlet, footer = pengaturan admin.
 */
fun buildReceiptText(
    transaction: Transaction,
    cashier: String,
    items: List<ReceiptItem>,
    storeName: String,
    outletAddress: String,
    footer: String
): String {
    val sb = StringBuilder()
    val header = transaction.outlet.ifBlank { storeName }
    sb.append(center(header, 30)).append("\n")
    if (outletAddress.isNotBlank()) {
        outletAddress.lines().forEach { line ->
            if (line.isNotBlank()) sb.append(center(line.trim(), 30)).append("\n")
        }
    }
    sb.append("------------------------------\n")
    sb.append("No   : ${transaction.invoiceNo}\n")
    sb.append("Tgl  : ${transaction.timestamp.toDateTime()}\n")
    sb.append("Kasir: $cashier\n")
    if (transaction.shift.isNotBlank()) {
        sb.append("Shift : ${transaction.shift}\n")
    }
    sb.append("------------------------------\n")
    items.forEach { item ->
        sb.append("${item.name}\n")
        sb.append("  ${item.qty} x ${item.price.toRupiah()} = ${item.subtotal.toRupiah()}\n")
    }
    sb.append("------------------------------\n")
    sb.append("TOTAL   : ${transaction.total.toRupiah()}\n")
    sb.append("BAYAR   : ${transaction.paidAmount.toRupiah()}\n")
    sb.append("KEMBALI : ${transaction.changeAmount.toRupiah()}\n")
    sb.append("METODE  : ${transaction.paymentMethod}\n")
    sb.append("------------------------------\n")
    val footerText = footer.ifBlank { "Terima kasih sudah berbelanja" }
    footerText.lines().forEach { line ->
        sb.append(center(line, 30)).append("\n")
    }
    sb.append("\n\n")
    return sb.toString()
}

private fun center(text: String, width: Int): String =
    if (text.length >= width) text else " ".repeat((width - text.length) / 2) + text
