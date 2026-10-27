package com.juragan38.pos.ui.screen.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.juragan38.pos.data.local.CartItem
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.utils.BluetoothPrinter
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val session by viewModel.session.collectAsState()
    val context = LocalContext.current

    var showCart by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf<Transaction?>(null) }
    var receiptItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }

    val total = cart.sumOf { it.subtotal }

    val doPrint = {
        val printers = BluetoothPrinter.pairedPrinters()
        if (printers.isEmpty()) {
            Toast.makeText(context, "Tidak ada printer Bluetooth terpasang", Toast.LENGTH_SHORT).show()
        } else {
            val trx = receipt
            if (trx != null) {
                val ok = BluetoothPrinter.print(
                    printers.first(),
                    buildReceiptText(trx, session?.fullName.orEmpty(), receiptItems)
                )
                Toast.makeText(
                    context,
                    if (ok) "Struk terkirim ke ${printers.first().name}" else "Gagal mencetak struk",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val bluetoothPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doPrint()
        else Toast.makeText(context, "Izin Bluetooth ditolak", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            total.toRupiah(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { showCart = true },
                        enabled = cart.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keranjang (${cart.size})")
                    }
                }
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada produk. Tambahkan produk dulu di menu Manajemen Produk.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductTile(product = product, onAdd = { viewModel.addToCart(product) })
                }
            }
        }
    }

    if (showCart) {
        CartDialog(
            cart = cart,
            total = total,
            onIncrease = { item ->
                val product = products.firstOrNull { it.id == item.productId }
                if (product != null) viewModel.addToCart(product)
            },
            onDecrease = { item -> viewModel.decrease(item) },
            onRemove = { item -> viewModel.removeFromCart(item) },
            onDismiss = { showCart = false },
            onCheckout = {
                showCart = false
                showPayment = true
            }
        )
    }

    if (showPayment) {
        PaymentDialog(
            total = total,
            onDismiss = { showPayment = false },
            onConfirm = { method, paid ->
                val snapshot = cart
                viewModel.checkout(method, paid) { transaction ->
                    showPayment = false
                    receiptItems = snapshot
                    receipt = transaction
                }
            }
        )
    }

    receipt?.let { trx ->
        ReceiptDialog(
            transaction = trx,
            cashier = session?.fullName.orEmpty(),
            items = receiptItems,
            onDismiss = { receipt = null },
            onPrint = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    doPrint()
                }
            }
        )
    }
}

@Composable
private fun ProductTile(product: Product, onAdd: () -> Unit) {
    val habis = product.stock <= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !habis) { onAdd() },
        colors = CardDefaults.cardColors(
            containerColor = if (habis) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2
            )
            Text(
                product.category,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                product.price.toRupiah(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (habis) "Stok habis" else "Stok ${product.stock}",
                    fontSize = 11.sp,
                    color = if (habis) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (!habis) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartDialog(
    cart: List<CartItem>,
    total: Double,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keranjang") },
        text = {
            if (cart.isEmpty()) {
                Text("Keranjang kosong")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    listItems(cart, key = { it.productId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "${item.price.toRupiah()} x ${item.qty}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDecrease(item) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                            }
                            Text("${item.qty}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { onIncrease(item) }) {
                                Icon(Icons.Default.Add, contentDescription = "Tambah")
                            }
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCheckout, enabled = cart.isNotEmpty()) {
                Text("Bayar ${total.toRupiah()}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun PaymentDialog(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var method by remember { mutableStateOf("TUNAI") }
    var paidText by remember { mutableStateOf("") }
    val paid = paidText.toDoubleOrNull() ?: 0.0
    val valid = method != "TUNAI" || paid >= total

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column {
                Text(
                    "Total: ${total.toRupiah()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Metode Pembayaran", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TUNAI", "QRIS", "TRANSFER").forEach { m ->
                        val selected = method == m
                        OutlinedButton(
                            onClick = { method = m },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(m, fontSize = 11.sp)
                        }
                    }
                }
                if (method == "TUNAI") {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it.filter { c -> c.isDigit() } },
                        label = { Text("Uang Diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (paid > 0) {
                        Text(
                            "Kembalian: ${(if (paid > total) paid - total else 0.0).toRupiah()}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(method, if (method == "TUNAI") paid else total) },
                enabled = valid
            ) {
                Text("Selesaikan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ReceiptDialog(
    transaction: Transaction,
    cashier: String,
    items: List<CartItem>,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaksi Berhasil") },
        text = {
            Column {
                Text("No: ${transaction.invoiceNo}", fontSize = 12.sp)
                Text(transaction.timestamp.toDateTime(), fontSize = 12.sp)
                Text("Kasir: $cashier", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${item.name} x${item.qty}",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(item.subtotal.toRupiah(), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(transaction.total.toRupiah(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            }
        },
        confirmButton = {
            TextButton(onClick = onPrint) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cetak Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}

private fun buildReceiptText(transaction: Transaction, cashier: String, items: List<CartItem>): String {
    val sb = StringBuilder()
    sb.append("      JURAGAN 38\n")
    sb.append("   Aplikasi Kasir POS\n")
    sb.append("------------------------------\n")
    sb.append("No   : ${transaction.invoiceNo}\n")
    sb.append("Tgl  : ${transaction.timestamp.toDateTime()}\n")
    sb.append("Kasir: $cashier\n")
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
    sb.append("   Terima kasih sudah belanja\n\n\n")
    return sb.toString()
}
