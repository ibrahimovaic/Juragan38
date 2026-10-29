package com.juragan38.pos.ui.screen.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.local.TransactionItem
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.components.CenteredContent
import com.juragan38.pos.ui.components.TransactionDetailDialog
import com.juragan38.pos.ui.theme.Green600
import com.juragan38.pos.ui.theme.Red600
import com.juragan38.pos.utils.toDateOnly
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah
import java.util.Calendar

private const val ALL = "Semua"

private enum class ReportPeriod(val label: String) {
    TODAY("Hari Ini"),
    WEEK("7 Hari"),
    MONTH("Bulan Ini"),
    DATE("Tanggal")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    val context = LocalContext.current

    var period by remember { mutableStateOf(ReportPeriod.TODAY) }
    var customDate by remember { mutableStateOf(startOfDay(System.currentTimeMillis())) }
    var outletFilter by remember { mutableStateOf(ALL) }
    var cashierFilter by remember { mutableStateOf(ALL) }
    var shiftFilter by remember { mutableStateOf(ALL) }

    var detail by remember { mutableStateOf<Transaction?>(null) }
    var detailItems by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }

    val outletOptions = remember(transactions) {
        listOf(ALL) + transactions.map { it.outlet }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val cashierOptions = remember(transactions) {
        listOf(ALL) + transactions.map { it.cashier }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val shiftOptions = remember(transactions) {
        listOf(ALL) + transactions.map { it.shift }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    LaunchedEffect(outletOptions) {
        if (outletFilter != ALL && outletFilter !in outletOptions) outletFilter = ALL
    }
    LaunchedEffect(cashierOptions) {
        if (cashierFilter != ALL && cashierFilter !in cashierOptions) cashierFilter = ALL
    }
    LaunchedEffect(shiftOptions) {
        if (shiftFilter != ALL && shiftFilter !in shiftOptions) shiftFilter = ALL
    }

    val (rangeStart, rangeEnd) = remember(period, customDate) {
        reportRange(period, customDate)
    }

    val filtered = remember(
        transactions, rangeStart, rangeEnd, outletFilter, cashierFilter, shiftFilter
    ) {
        transactions.filter { trx ->
            trx.timestamp >= rangeStart && trx.timestamp < rangeEnd &&
                (outletFilter == ALL || trx.outlet == outletFilter) &&
                (cashierFilter == ALL || trx.cashier == cashierFilter) &&
                (shiftFilter == ALL || trx.shift == shiftFilter)
        }
    }

    val totalSales = filtered.sumOf { it.total }
    val totalCost = filtered.sumOf { it.costTotal }
    val totalProfit = filtered.sumOf { it.profit }
    val count = filtered.size
    val average = if (count == 0) 0.0 else totalSales / count

    val openDetail: (Transaction) -> Unit = { trx ->
        detail = trx
        detailItems = emptyList()
        detailLoading = true
        viewModel.loadItems(trx.id) { items ->
            detailItems = items
            detailLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Penjualan") },
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
            maxWidth = 1000.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ------------------------------------------------ filter
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Periode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportPeriod.entries.forEach { option ->
                                    FilterChip(
                                        selected = period == option,
                                        onClick = { period = option },
                                        label = { Text(option.label, fontSize = 12.sp) }
                                    )
                                }
                            }

                            if (period == ReportPeriod.DATE) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.timeInMillis = customDate
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val picked = Calendar.getInstance()
                                                picked.set(year, month, day, 0, 0, 0)
                                                picked.set(Calendar.MILLISECOND, 0)
                                                customDate = picked.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Pilih Tanggal: ${customDate.toDateOnly()}", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Rentang: ${rangeStart.toDateOnly()} - ${(rangeEnd - 1).toDateOnly()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Filter Laporan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterDropdown(
                                label = "Outlet",
                                value = outletFilter,
                                options = outletOptions,
                                onSelect = { outletFilter = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterDropdown(
                                label = "Kasir",
                                value = cashierFilter,
                                options = cashierOptions,
                                onSelect = { cashierFilter = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterDropdown(
                                label = "Shift",
                                value = shiftFilter,
                                options = shiftOptions,
                                onSelect = { shiftFilter = it }
                            )

                            val hasFilter = outletFilter != ALL ||
                                cashierFilter != ALL || shiftFilter != ALL
                            if (hasFilter) {
                                TextButton(
                                    onClick = {
                                        outletFilter = ALL
                                        cashierFilter = ALL
                                        shiftFilter = ALL
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset filter", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------- ringkasan
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Total Penjualan",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                totalSales.toRupiah(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ReportMini("HPP", totalCost.toRupiah(), Modifier.weight(1f))
                                ReportMini("Laba", totalProfit.toRupiah(), Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ReportMini("Transaksi", "$count", Modifier.weight(1f))
                                ReportMini("Rata-rata", average.toRupiah(), Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Rincian Transaksi ($count)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Tidak ada transaksi pada filter ini.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { trx ->
                        ReportRow(trx = trx, onClick = { openDetail(trx) })
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    detail?.let { trx ->
        TransactionDetailDialog(
            transaction = trx,
            items = detailItems,
            loading = detailLoading,
            outletAddress = outlets.firstOrNull { it.name == trx.outlet }?.address.orEmpty(),
            onDismiss = { detail = null }
        )
    }
}

@Composable
private fun ReportRow(trx: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trx.invoiceNo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "${trx.timestamp.toDateTime()} - ${trx.cashier}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trx.paymentMethod,
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
                        fontSize = 14.sp
                    )
                    Text(
                        "Laba ${trx.profit.toRupiah()}",
                        fontSize = 11.sp,
                        color = if (trx.profit >= 0) Green600 else Red600
                    )
                    Text(
                        "Ketuk untuk detail",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 360.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
        Text(
            value,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun startOfDay(time: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun plusDays(time: Long, days: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = time
    cal.add(Calendar.DAY_OF_YEAR, days)
    return cal.timeInMillis
}

/** Rentang [mulai, selesai) sesuai periode yang dipilih. */
private fun reportRange(period: ReportPeriod, customDate: Long): Pair<Long, Long> {
    val todayStart = startOfDay(System.currentTimeMillis())
    val end = plusDays(todayStart, 1)
    return when (period) {
        ReportPeriod.TODAY -> todayStart to end
        ReportPeriod.WEEK -> plusDays(todayStart, -6) to end
        ReportPeriod.MONTH -> {
            val cal = Calendar.getInstance()
            cal.timeInMillis = todayStart
            cal.set(Calendar.DAY_OF_MONTH, 1)
            startOfDay(cal.timeInMillis) to end
        }
        ReportPeriod.DATE -> {
            val start = startOfDay(customDate)
            start to plusDays(start, 1)
        }
    }
}
