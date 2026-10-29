package com.juragan38.pos.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.ThemeMode
import com.juragan38.pos.data.local.AppSettings
import com.juragan38.pos.data.local.Outlet
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.components.CenteredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val session by viewModel.session.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isAdmin = session?.role == "ADMIN"
    val isManager = isAdmin || session?.role == "MANAJER"

    var form by remember { mutableStateOf(settings) }
    LaunchedEffect(settings) { form = settings }

    var message by remember { mutableStateOf<String?>(null) }
    var showOutletForm by remember { mutableStateOf(false) }
    var editingOutlet by remember { mutableStateOf<Outlet?>(null) }
    var confirmDeleteHistory by remember { mutableStateOf(false) }
    var savingSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
        }
    ) { padding ->
        CenteredContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxWidth = 900.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ------------------------------------------------ tampilan
                item {
                    SettingsCard("Tampilan", Icons.Default.LightMode) {
                        Text(
                            "Mode gelap / terang tersimpan per akun, jadi tiap user bisa beda.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeChip("Terang", Icons.Default.LightMode, themeMode == ThemeMode.LIGHT) {
                                viewModel.setTheme(ThemeMode.LIGHT)
                            }
                            ThemeChip("Gelap", Icons.Default.DarkMode, themeMode == ThemeMode.DARK) {
                                viewModel.setTheme(ThemeMode.DARK)
                            }
                            ThemeChip(
                                "Otomatis",
                                Icons.Default.SettingsBrightness,
                                themeMode == ThemeMode.SYSTEM
                            ) {
                                viewModel.setTheme(ThemeMode.SYSTEM)
                            }
                        }
                    }
                }

                // ------------------------------------------------- outlet
                if (isAdmin) {
                    item {
                        SettingsCard("Outlet / Cabang", Icons.Default.Add) {
                            Text(
                                "Tambah, ubah, atau hapus outlet. Outlet dipilih saat mulai shift.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (outlets.isEmpty()) {
                                Text("Belum ada outlet.", fontSize = 12.sp)
                            } else {
                                outlets.forEach { outlet ->
                                    OutletRow(
                                        outlet = outlet,
                                        onEdit = {
                                            editingOutlet = outlet
                                            showOutletForm = true
                                        },
                                        onDelete = {
                                            viewModel.deleteOutlet(outlet) { err ->
                                                message = err ?: "Outlet ${outlet.name} dihapus"
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    editingOutlet = null
                                    showOutletForm = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Outlet")
                            }
                        }
                    }
                }

                // ------------------------------------------- shift & jam
                if (isAdmin) {
                    item {
                        SettingsCard("Shift & Jam Kerja", Icons.Default.SettingsBrightness) {
                            Text(
                                "Atur nama dan jam kerja tiap shift. Perubahan berlaku untuk semua HP.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            ShiftEditor(
                                title = "Shift 1",
                                name = form.shift1Name,
                                start = form.shift1Start,
                                end = form.shift1End,
                                onName = { form = form.copy(shift1Name = it) },
                                onStart = { form = form.copy(shift1Start = it) },
                                onEnd = { form = form.copy(shift1End = it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ShiftEditor(
                                title = "Shift 2",
                                name = form.shift2Name,
                                start = form.shift2Start,
                                end = form.shift2End,
                                onName = { form = form.copy(shift2Name = it) },
                                onStart = { form = form.copy(shift2Start = it) },
                                onEnd = { form = form.copy(shift2End = it) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = form.storeName,
                                onValueChange = { form = form.copy(storeName = it) },
                                label = { Text("Nama Toko") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // --------------------------------------------- watermark
                if (isAdmin) {
                    item {
                        SettingsCard("Watermark", Icons.Default.SettingsBrightness) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tampilkan watermark", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Teks transparan di seluruh layar aplikasi",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = form.watermarkEnabled,
                                    onCheckedChange = { form = form.copy(watermarkEnabled = it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = form.watermarkText,
                                onValueChange = { form = form.copy(watermarkText = it) },
                                label = { Text("Teks Watermark") },
                                singleLine = true,
                                enabled = form.watermarkEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Ketebalan: ${"%.0f".format(form.watermarkOpacity * 100)}%",
                                fontSize = 12.sp
                            )
                            Slider(
                                value = form.watermarkOpacity,
                                onValueChange = { form = form.copy(watermarkOpacity = it) },
                                valueRange = 0.02f..0.20f,
                                enabled = form.watermarkEnabled
                            )
                        }
                    }
                }

                // ---------------------------------------------- struk print
                if (isAdmin) {
                    item {
                        SettingsCard("Struk Print", Icons.Default.Print) {
                            Text(
                                "Header struk otomatis memakai nama outlet tempat transaksi. " +
                                    "Footer bisa diubah sesuka hati (boleh beberapa baris).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = form.receiptFooter,
                                onValueChange = { form = form.copy(receiptFooter = it) },
                                label = { Text("Footer Struk") },
                                minLines = 2,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ------------------------------------------------- simpan
                if (isAdmin) {
                    item {
                        androidx.compose.material3.Button(
                            onClick = {
                                if (savingSettings) return@Button
                                savingSettings = true
                                viewModel.saveSettings(form) { err ->
                                    savingSettings = false
                                    message = err ?: "Pengaturan tersimpan"
                                }
                            },
                            enabled = !savingSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                if (savingSettings) "Menyimpan..." else "Simpan Pengaturan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ------------------------------------------------- hapus riwayat
                if (isAdmin) {
                    item {
                        SettingsCard("Hapus Riwayat Penjualan", Icons.Default.Delete) {
                            Text(
                                "Ada ${transactions.size} transaksi tersimpan. Menghapus riwayat " +
                                    "tidak mengembalikan stok dan tidak bisa dibatalkan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { confirmDeleteHistory = true },
                                enabled = transactions.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hapus Semua Riwayat", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (!isManager) {
                    item {
                        Text(
                            "Pengaturan outlet, shift, dan watermark hanya bisa diubah oleh admin.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                message?.let { text ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(text, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showOutletForm) {
        val edited = editingOutlet
        OutletFormDialog(
            outlet = edited,
            onDismiss = {
                showOutletForm = false
                editingOutlet = null
            },
            onSave = { name, address ->
                // Tutup dialog langsung supaya tidak menggantung saat server lambat/offline.
                showOutletForm = false
                editingOutlet = null
                val target = edited?.copy(name = name, address = address)
                    ?: Outlet(name = name, address = address)
                viewModel.saveOutlet(target) { err ->
                    message = err ?: if (edited == null) {
                        "Outlet $name ditambahkan"
                    } else {
                        "Outlet $name diperbarui"
                    }
                }
            }
        )
    }

    if (confirmDeleteHistory) {
        AlertDialog(
            onDismissRequest = { confirmDeleteHistory = false },
            title = { Text("Hapus Semua Riwayat?") },
            text = {
                Text(
                    "Seluruh ${transactions.size} transaksi akan dihapus dari HP ini dan dari server " +
                        "(semua perangkat). Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteHistory = false
                        viewModel.deleteAllTransactions { err ->
                            message = err ?: "Riwayat penjualan dihapus"
                        }
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteHistory = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun ShiftEditor(
    title: String,
    name: String,
    start: String,
    end: String,
    onName: (String) -> Unit,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit
) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text("Nama") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row {
        OutlinedTextField(
            value = start,
            onValueChange = { onStart(it.filter { c -> c.isDigit() || c == ':' }) },
            label = { Text("Mulai") },
            placeholder = { Text("08:00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = end,
            onValueChange = { onEnd(it.filter { c -> c.isDigit() || c == ':' }) },
            label = { Text("Selesai") },
            placeholder = { Text("16:00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OutletRow(outlet: Outlet, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(outlet.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (outlet.address.isNotBlank()) {
                Text(
                    outlet.address,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Ubah outlet",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Hapus outlet",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OutletFormDialog(
    outlet: Outlet?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(outlet) { mutableStateOf(outlet?.name.orEmpty()) }
    var address by remember(outlet) { mutableStateOf(outlet?.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (outlet == null) "Tambah Outlet" else "Edit Outlet") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Outlet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Outlet (tampil di struk)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), address.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
