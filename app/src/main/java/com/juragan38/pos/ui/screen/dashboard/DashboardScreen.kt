package com.juragan38.pos.ui.screen.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.ThemeMode
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.UpdateState
import com.juragan38.pos.ui.components.CenteredContent
import com.juragan38.pos.ui.components.isMediumScreen
import com.juragan38.pos.utils.InstalledVersion
import com.juragan38.pos.utils.UpdateChecker
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah
import java.util.Calendar

private data class MenuEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onOpenPos: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeShift: () -> Unit,
    onLogout: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val context = LocalContext.current
    val version = remember(context) { UpdateChecker.installedVersion(context) }

    val startOfDay = rememberStartOfDay()
    val todayTransactions = transactions.filter { it.timestamp >= startOfDay }
    val todaySales = todayTransactions.sumOf { it.total }
    val todayProfit = todayTransactions.sumOf { it.profit }

    val role = session?.role ?: "KASIR"
    val twoColumns = isMediumScreen()

    val isDark = themeMode == ThemeMode.DARK ||
        (themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())

    val menus = buildList {
        add(MenuEntry("Kasir / POS", "Buat transaksi penjualan", Icons.Default.ShoppingCart, onOpenPos))
        if (role == "ADMIN" || role == "MANAJER") {
            add(MenuEntry("Manajemen Produk", "Tambah, ubah, hapus produk", Icons.Default.Add, onOpenProducts))
            add(MenuEntry("Laporan Penjualan", "Laba rugi dan rekap penjualan", Icons.Default.Assessment, onOpenReports))
        }
        add(MenuEntry("Riwayat Transaksi", "Semua transaksi tersimpan", Icons.Default.List, onOpenTransactions))
        if (role == "ADMIN") {
            add(MenuEntry("Manajemen Akun", "Tambah akun kasir & manajer", Icons.Default.Person, onOpenUsers))
        }
        add(MenuEntry("Pengaturan", "Tema, outlet, shift, watermark", Icons.Default.Settings, onOpenSettings))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settings.storeName.ifBlank { "JURAGAN 38" }) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.setTheme(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                        }
                    ) {
                        Icon(
                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti mode gelap/terang"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Keluar")
                    }
                }
            )
        }
    ) { padding ->
        CenteredContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxWidth = 1100.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Halo, ${session?.fullName ?: "Pengguna"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role: $role",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onChangeShift,
                        label = {
                            Text(
                                session?.outletName?.ifBlank { "Outlet" } ?: "Outlet",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    AssistChip(
                        onClick = onChangeShift,
                        label = {
                            Text(
                                settings.shiftLabel(session?.shiftKey.orEmpty()),
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                TextButton(
                    onClick = onChangeShift,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("Ganti outlet / shift", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Penjualan Hari Ini",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            todaySales.toRupiah(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SummaryColumn("Laba", todayProfit.toRupiah())
                            SummaryColumn("Transaksi", "${todayTransactions.size}")
                            SummaryColumn("Produk", "${products.size}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Menu", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                menus.chunked(if (twoColumns) 2 else 1).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { entry ->
                            MenuTile(
                                entry = entry,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1 && twoColumns) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Stok menipis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val lowStock = products.filter { it.stock <= 5 }
                if (lowStock.isEmpty()) {
                    Text(
                        "Semua stok aman",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    lowStock.forEach { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        product.category,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Sisa ${product.stock}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                AppInfoCard(
                    version = version,
                    state = updateState,
                    onCheck = { viewModel.checkForUpdate(manual = true) },
                    onInstall = { viewModel.installDownloadedUpdate() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    UpdateDialog(
        state = updateState,
        version = version,
        onDownload = { viewModel.downloadUpdate() },
        onInstall = { viewModel.installDownloadedUpdate() },
        onPermission = { viewModel.openInstallPermission() },
        onDismiss = { viewModel.dismissUpdate() }
    )
}

/** Info versi aplikasi + tombol cek pembaruan, tampil di dasar menu utama. */
@Composable
private fun AppInfoCard(
    version: InstalledVersion,
    state: UpdateState,
    onCheck: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        "JURAGAN 38 POS",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Versi ${version.versionName} (build ${version.versionCode})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Terpasang: ${version.installedAt.toDateTime()}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.lastCheckedAt > 0) {
                Text(
                    "Terakhir cek pembaruan: ${state.lastCheckedAt.toDateTime()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCheck,
                    enabled = !state.checking && state.progress == null
                ) {
                    Text(
                        if (state.checking) "Memeriksa..." else "Cek Pembaruan",
                        fontSize = 12.sp
                    )
                }
                if (state.downloaded) {
                    Button(onClick = onInstall) {
                        Text("Pasang Pembaruan", fontSize = 12.sp)
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            state.error?.let { error ->
                Text(
                    error,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.available != null && !state.downloaded && state.progress == null) {
                Text(
                    "Pembaruan versi ${state.available.versionName} tersedia.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Data tersimpan di cloud Firebase dan tersinkron otomatis saat online.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Dialog pembaruan: info versi baru, progress unduh, dan tombol pasang. */
@Composable
private fun UpdateDialog(
    state: UpdateState,
    version: InstalledVersion,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = state.available ?: return
    val progress = state.progress

    when {
        progress != null -> AlertDialog(
            onDismissRequest = { },
            title = { Text("Mengunduh pembaruan") },
            text = {
                Column {
                    Text(
                        "Versi ${info.versionName} sedang diunduh. Jangan tutup aplikasi.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (progress <= 0) "Menyiapkan unduhan..." else "$progress%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = { }
        )

        state.downloaded -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pembaruan siap dipasang") },
            text = {
                Column {
                    Text(
                        "Versi ${info.versionName} sudah diunduh. Tekan Pasang untuk " +
                            "membuka layar instalasi Android.",
                        fontSize = 12.sp
                    )
                    if (!UpdateChecker.canInstall(LocalContext.current)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Jika diminta, izinkan pemasangan dari aplikasi ini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onInstall) { Text("Pasang") }
            },
            dismissButton = {
                if (!UpdateChecker.canInstall(LocalContext.current)) {
                    TextButton(onClick = onPermission) { Text("Izin") }
                } else {
                    TextButton(onClick = onDismiss) { Text("Nanti") }
                }
            }
        )

        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pembaruan tersedia") },
            text = {
                Column {
                    Text(
                        "Terpasang: v${version.versionName} (build ${version.versionCode})",
                        fontSize = 12.sp
                    )
                    Text(
                        "Terbaru: v${info.versionName} (build ${info.versionCode})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (info.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(info.notes, fontSize = 12.sp)
                    }
                    if (info.force) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pembaruan ini sebaiknya segera dipasang.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.error,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDownload) { Text("Unduh & Pasang") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Nanti") }
            }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SummaryColumn(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MenuTile(entry: MenuEntry, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable { entry.onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    entry.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun rememberStartOfDay(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
