package com.juragan38.pos.ui.screen.products

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.Outlet
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.components.CenteredContent
import com.juragan38.pos.ui.components.ProductImage
import com.juragan38.pos.utils.toRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val outlets by viewModel.outlets.collectAsState()

    var editing by remember { mutableStateOf<Product?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    var outletFilter by remember { mutableStateOf("") }

    // "" = semua outlet. Produk tanpa outlet tetap tampil di filter mana pun.
    val filtered = remember(products, outletFilter) {
        if (outletFilter.isBlank()) {
            products
        } else {
            products.filter { it.outletId.isBlank() || it.outletId == outletFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Produk") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showForm = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah produk")
            }
        }
    ) { padding ->
        CenteredContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxWidth = 1100.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (outlets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = outletFilter.isBlank(),
                            onClick = { outletFilter = "" },
                            label = { Text("Semua outlet", fontSize = 12.sp) }
                        )
                        outlets.forEach { outlet ->
                            FilterChip(
                                selected = outletFilter == outlet.id,
                                onClick = { outletFilter = outlet.id },
                                label = { Text(outlet.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (products.isEmpty()) "Belum ada produk" else "Tidak ada produk di outlet ini",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (products.isEmpty()) {
                                "Tekan tombol + untuk menambah produk"
                            } else {
                                "Pilih outlet lain atau tambah produk baru"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 330.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProductImage(
                                        image = product.image,
                                        contentDescription = product.name,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text(
                                            "${product.category} - stok ${product.stock}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Outlet: " + (
                                                outlets.firstOrNull { it.id == product.outletId }?.name
                                                    ?: "Semua outlet"
                                                ),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Jual ${product.price.toRupiah()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "HPP ${product.cost.toRupiah()} - Laba ${(product.price - product.cost).toRupiah()}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editing = product
                                            showForm = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deleting = product }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showForm) {
        ProductFormDialog(
            initial = editing,
            outlets = outlets,
            viewModel = viewModel,
            onDismiss = { showForm = false },
            onSave = { product ->
                viewModel.saveProduct(product)
                showForm = false
            }
        )
    }

    deleting?.let { product ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus Produk") },
            text = { Text("Yakin hapus ${product.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(product)
                        deleting = null
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
}

@Composable
private fun ProductFormDialog(
    initial: Product?,
    outlets: List<Outlet>,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "Umum") }
    var price by remember { mutableStateOf(initial?.price?.toInt()?.toString().orEmpty()) }
    var cost by remember { mutableStateOf(initial?.cost?.toInt()?.toString().orEmpty()) }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "0") }
    var image by remember { mutableStateOf(initial?.image.orEmpty()) }
    var outletId by remember { mutableStateOf(initial?.outletId.orEmpty()) }
    var outletMenu by remember { mutableStateOf(false) }
    var encoding by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            encoding = true
            viewModel.encodeImage(uri) { encoded ->
                encoding = false
                if (encoded != null) {
                    image = encoded
                } else {
                    Toast.makeText(context, "Gambar terlalu besar atau gagal dibaca", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val valid = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk" else "Ubah Produk") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(
                        image = image,
                        contentDescription = "Gambar produk",
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { picker.launch("image/*") },
                            enabled = !encoding
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (encoding) "Memproses..." else "Pilih Gambar")
                        }
                        if (image.isNotBlank()) {
                            TextButton(onClick = { image = "" }) {
                                Text("Hapus gambar", fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Gambar dari galeri otomatis diperkecil dan ikut tersinkron ke HP lain.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { outletMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Outlet",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                outlets.firstOrNull { it.id == outletId }?.name
                                    ?: "Semua outlet",
                                fontSize = 13.sp
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = outletMenu,
                        onDismissRequest = { outletMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua outlet", fontSize = 13.sp) },
                            onClick = {
                                outletId = ""
                                outletMenu = false
                            }
                        )
                        outlets.forEach { outlet ->
                            DropdownMenuItem(
                                text = { Text(outlet.name, fontSize = 13.sp) },
                                onClick = {
                                    outletId = outlet.id
                                    outletMenu = false
                                }
                            )
                        }
                    }
                }
                Text(
                    "Produk hanya muncul di kasir outlet yang dipilih. \"Semua outlet\" " +
                        "berarti produk tampil di semua outlet.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Jual") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it.filter { c -> c.isDigit() } },
                        label = { Text("HPP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (image.startsWith("http")) image else "",
                    onValueChange = { image = it.trim() },
                    label = { Text("Atau tautkan URL gambar (opsional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        Product(
                            id = initial?.id.orEmpty(),
                            name = name.trim(),
                            category = category.ifBlank { "Umum" }.trim(),
                            price = price.toDoubleOrNull() ?: 0.0,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0,
                            image = image.trim(),
                            outletId = outletId
                        )
                    )
                },
                enabled = valid
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
