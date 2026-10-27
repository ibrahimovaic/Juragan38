package com.juragan38.pos.ui.screen.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.theme.Green600
import com.juragan38.pos.ui.theme.Red600
import com.juragan38.pos.utils.toDateTime
import com.juragan38.pos.utils.toRupiah
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()

    val startOfDay = startOfDayMillis()
    val today = transactions.filter { it.timestamp >= startOfDay }
    val totalSales = today.sumOf { it.total }
    val totalCost = today.sumOf { it.costTotal }
    val totalProfit = today.sumOf { it.profit }
    val count = today.size
    val average = if (count == 0) 0.0 else totalSales / count

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Ringkasan Hari Ini", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
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

            Spacer(modifier = Modifier.height(18.dp))
            Text("Rincian Transaksi Hari Ini", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (today.isEmpty()) {
                Text(
                    "Belum ada transaksi hari ini",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                today.forEach { trx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
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
                                }
                                Column {
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
                                }
                            }
                        }
                    }
                }
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

private fun startOfDayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
