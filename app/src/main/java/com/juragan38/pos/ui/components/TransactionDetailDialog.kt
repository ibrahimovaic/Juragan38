package com.juragan38.pos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.local.TransactionItem
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah

/**
 * Popup rincian transaksi. Dipakai di Laporan Penjualan dan Riwayat Transaksi,
 * lengkap dengan tombol cetak ulang struk.
 */
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    items: List<TransactionItem>,
    loading: Boolean = false,
    outletAddress: String = "",
    onDismiss: () -> Unit,
    onPrint: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detail Transaksi") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(transaction.invoiceNo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(transaction.timestamp.toDateTime(), fontSize = 12.sp)
                Text("Kasir: ${transaction.cashier}", fontSize = 12.sp)
                if (transaction.outlet.isNotBlank()) {
                    Text("Outlet: ${transaction.outlet}", fontSize = 12.sp)
                    if (outletAddress.isNotBlank()) {
                        Text(
                            outletAddress,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (transaction.shift.isNotBlank()) {
                    Text("Shift: ${transaction.shift}", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))

                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(22.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally),
                        strokeWidth = 2.dp
                    )

                    items.isEmpty() -> Text(
                        "Rincian item tidak tersedia (perlu koneksi internet sekali saja).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> {
                        items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "${item.productName} x${item.qty}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(item.subtotal.toRupiah(), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Total",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        transaction.total.toRupiah(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "Bayar (${transaction.paymentMethod}): ${transaction.paidAmount.toRupiah()}",
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Kembali: ${transaction.changeAmount.toRupiah()}",
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Laba: ${transaction.profit.toRupiah()} (HPP ${transaction.costTotal.toRupiah()})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (onPrint != null) {
                TextButton(onClick = onPrint, enabled = !loading) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Ulang")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
