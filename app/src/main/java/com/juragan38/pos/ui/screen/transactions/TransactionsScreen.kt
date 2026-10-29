package com.juragan38.pos.ui.screen.transactions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.components.CenteredContent
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()
    val session by viewModel.session.collectAsState()
    val context = LocalContext.current

    val isAdmin = session?.role == "ADMIN"
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { confirmDeleteAll = true },
                            enabled = transactions.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus semua riwayat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada transaksi", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Transaksi akan muncul setelah pembayaran di menu Kasir",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            CenteredContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                maxWidth = 1000.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { trx ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trx.invoiceNo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        trx.timestamp.toDateTime(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${trx.cashier} - ${trx.paymentMethod}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (trx.outlet.isNotBlank() || trx.shift.isNotBlank()) {
                                        Text(
                                            listOf(trx.outlet, trx.shift)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • "),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        trx.total.toRupiah(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Laba ${trx.profit.toRupiah()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isAdmin) {
                                    IconButton(onClick = { deleting = trx }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus transaksi",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    deleting?.let { trx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus Transaksi") },
            text = {
                Text(
                    "Hapus transaksi ${trx.invoiceNo} sebesar ${trx.total.toRupiah()}? " +
                        "Data di server juga akan dihapus."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        viewModel.deleteTransaction(trx) { err ->
                            Toast.makeText(
                                context,
                                err ?: "Transaksi dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Batal") }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = {
                Text(
                    "Seluruh ${transactions.size} transaksi akan dihapus dari HP ini dan dari server " +
                        "(semua perangkat). Stok tidak dikembalikan dan tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAll = false
                        viewModel.deleteAllTransactions { err ->
                            Toast.makeText(
                                context,
                                err ?: "Riwayat penjualan dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Hapus Semua", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Batal") }
            }
        )
    }
}
