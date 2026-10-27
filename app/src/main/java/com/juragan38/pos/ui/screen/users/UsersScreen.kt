package com.juragan38.pos.ui.screen.users

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juragan38.pos.data.local.User
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.theme.Amber500
import com.juragan38.pos.ui.theme.Blue700
import com.juragan38.pos.ui.theme.Green600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val users by viewModel.users.collectAsState()
    val session by viewModel.session.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<User?>(null) }
    var deleting by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Akun") },
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
                Icon(Icons.Default.Add, contentDescription = "Tambah akun")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Total ${users.size} akun aktif. Akun baru otomatis bisa login di HP lain.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(users, key = { it.id }) { user ->
                val isMe = user.username == session?.username
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(roleColor(user.role).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = roleColor(user.role)
                            )
                        }
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    user.fullName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                if (isMe) {
                                    Text(
                                        "  (Anda)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                "@${user.username}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                user.role,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = roleColor(user.role)
                            )
                        }
                        IconButton(
                            onClick = {
                                editing = user
                                showForm = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah akun")
                        }
                        if (!isMe) {
                            IconButton(onClick = { deleting = user }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Nonaktifkan akun",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showForm) {
        UserFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { user, password, onResult ->
                viewModel.saveUser(user, password) { error ->
                    onResult(error)
                    if (error == null) showForm = false
                }
            }
        )
    }

    deleting?.let { user ->
        var error by remember(user.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Nonaktifkan Akun") },
            text = {
                Column {
                    Text("Akun ${user.fullName} (@${user.username}) tidak akan bisa login lagi.")
                    Text(
                        "Data transaksi lamanya tetap tersimpan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUser(user) { err ->
                            if (err == null) deleting = null else error = err
                        }
                    }
                ) {
                    Text("Nonaktifkan", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun UserFormDialog(
    initial: User?,
    onDismiss: () -> Unit,
    onSave: (User, String?, (String?) -> Unit) -> Unit
) {
    var fullName by remember { mutableStateOf(initial?.fullName.orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initial?.role ?: "KASIR") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isNew = initial == null
    val valid = fullName.isNotBlank() &&
        username.isNotBlank() &&
        (!isNew || password.length >= 5)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (isNew) "Tambah Akun" else "Ubah Akun") },
        text = {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (isNew) username = it.filter { c -> !c.isWhitespace() } },
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = isNew,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isNew) {
                    Text(
                        "Username tidak bisa diubah (terikat akun login).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isNew) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min. 5 karakter)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Password tidak dapat diubah dari sini. Buat akun baru bila karyawan lupa password.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Role", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("KASIR", "MANAJER", "ADMIN").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saving = true
                    error = null
                    onSave(
                        User(
                            id = initial?.id.orEmpty(),
                            username = username.trim(),
                            fullName = fullName.trim(),
                            role = role,
                            active = true
                        ),
                        if (isNew) password.trim() else null
                    ) { err ->
                        saving = false
                        error = err
                    }
                },
                enabled = valid && !saving
            ) {
                Text(if (saving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Batal") }
        }
    )
}

private fun roleColor(role: String): Color = when (role) {
    "ADMIN" -> Blue700
    "MANAJER" -> Amber500
    else -> Green600
}
