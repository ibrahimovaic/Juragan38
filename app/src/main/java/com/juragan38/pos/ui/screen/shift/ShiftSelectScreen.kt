package com.juragan38.pos.ui.screen.shift

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.AppSettings
import com.juragan38.pos.data.local.Outlet
import com.juragan38.pos.ui.AppViewModel
import java.util.Calendar

/**
 * Muncul setelah login (dan saat ganti shift):
 * user memilih outlet lalu shift 1 / shift 2.
 */
@Composable
fun ShiftSelectScreen(
    viewModel: AppViewModel,
    onStarted: () -> Unit,
    onLogout: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val outlets by viewModel.outlets.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val fallbackOutlet = remember { Outlet(id = "utama", name = "Outlet Utama") }
    val outletList = remember(outlets) { outlets.ifEmpty { listOf(fallbackOutlet) } }
    val shifts = remember(settings) { settings.shifts() }

    var outlet by remember(outlets) {
        mutableStateOf(
            outletList.firstOrNull { it.id == session?.outletId } ?: outletList.first()
        )
    }
    var shiftKey by remember(settings) {
        mutableStateOf(
            session?.shiftKey
                ?.takeIf { key -> shifts.any { it.id == key } }
                ?: currentShiftKey(settings)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 520.dp)) {
            Text(
                text = "Mulai Shift",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Halo, ${session?.fullName ?: "Pengguna"}. Pilih outlet dan shift kerja kamu.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SelectorLabel("Outlet")
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownField(
                        icon = Icons.Default.Home,
                        value = outlet.name,
                        options = outletList.map { it.name },
                        onSelect = { index -> outlet = outletList[index] }
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    SelectorLabel("Shift")
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownField(
                        icon = Icons.Default.DateRange,
                        value = settings.shiftLabel(shiftKey),
                        options = shifts.map { it.label() },
                        onSelect = { index ->
                            shifts.getOrNull(index)?.let { shiftKey = it.id }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Jam kerja saat ini: " + nowText(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (outlets.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        "Belum ada outlet di server. Sementara memakai outlet default. " +
                            "Admin bisa menambah outlet di menu Pengaturan.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.startShift(outlet, shiftKey) { onStarted() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(50.dp)
            ) {
                Text("MULAI KERJA", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keluar akun", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SelectorLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DropdownField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 14.sp
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

@Composable
private fun nowText(): String {
    val cal = Calendar.getInstance()
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return "%02d:%02d".format(h, m)
}

/** Tebak shift yang sedang berjalan berdasarkan jam sekarang. */
private fun currentShiftKey(settings: AppSettings): String {
    val shifts = settings.shifts()
    val cal = Calendar.getInstance()
    val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val match = shifts.firstOrNull { shift ->
        val start = toMinutes(shift.start) ?: return@firstOrNull false
        val end = endMinutes(shift.end)
        now in start until end
    }
    return match?.id ?: shifts.firstOrNull()?.id.orEmpty()
}

private fun toMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** "00:00" pada jam selesai dianggap 24:00 (shift malam). */
private fun endMinutes(text: String): Int {
    val value = toMinutes(text) ?: return 24 * 60
    return if (value == 0) 24 * 60 else value
}
