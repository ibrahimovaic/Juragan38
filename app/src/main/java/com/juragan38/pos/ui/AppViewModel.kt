package com.juragan38.pos.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juragan38.pos.data.Session
import com.juragan38.pos.data.SessionManager
import com.juragan38.pos.data.ThemeMode
import com.juragan38.pos.data.local.AppSettings
import com.juragan38.pos.data.local.CartItem
import com.juragan38.pos.data.local.Outlet
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.local.TransactionItem
import com.juragan38.pos.data.local.User
import com.juragan38.pos.data.repository.PosRepository
import com.juragan38.pos.utils.UpdateCheckResult
import com.juragan38.pos.utils.UpdateChecker
import com.juragan38.pos.utils.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Status pengecekan / unduh / pasang pembaruan APK. */
data class UpdateState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    /** 0..100 saat mengunduh, null kalau tidak sedang mengunduh. */
    val progress: Int? = null,
    val downloaded: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val lastCheckedAt: Long = 0L
)

private const val AUTO_CHECK_INTERVAL = 6 * 60 * 60 * 1000L
private const val KEY_LAST_CHECK = "last_update_check"

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PosRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val products: StateFlow<List<Product>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val users: StateFlow<List<User>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val outlets: StateFlow<List<Outlet>> = repository.outlets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val session: StateFlow<Session?> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val localPrefs by lazy {
        context.getSharedPreferences("juragan38_local", Context.MODE_PRIVATE)
    }
    private var downloadedApk: File? = null

    init {
        viewModelScope.launch {
            if (!repository.hasSession()) {
                sessionManager.clear()
            } else {
                val saved = sessionManager.session.first()
                if (saved != null) {
                    repository.startRealtimeSync(viewModelScope, saved.role)
                }
            }
        }
        viewModelScope.launch {
            sessionManager.session.collect { s ->
                _themeMode.value =
                    if (s == null) ThemeMode.SYSTEM else sessionManager.themeOf(s.username)
            }
        }
        // Cek pembaruan otomatis (maksimal sekali per 6 jam).
        checkForUpdate(manual = false)
    }

    // --------------------------------------------------------------- auth

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "Username dan password wajib diisi"
                return@launch
            }
            _syncing.value = true
            try {
                val user = repository.login(username.trim(), password)
                sessionManager.save(
                    Session(
                        fullName = user.fullName,
                        username = user.username,
                        role = user.role
                    )
                )
                repository.startRealtimeSync(viewModelScope, user.role)
                if (user.role == "ADMIN") {
                    repository.prepareFirstAdminData()
                }
                onSuccess()
            } catch (e: Exception) {
                _loginError.value = e.message ?: "Gagal masuk"
            } finally {
                _syncing.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            sessionManager.clear()
            _cart.value = emptyList()
        }
    }

    // -------------------------------------------------------------- shift

    fun startShift(outlet: Outlet, shiftKey: String, onDone: () -> Unit) {
        viewModelScope.launch {
            sessionManager.saveShift(outlet.id, outlet.name, shiftKey)
            _cart.value = emptyList()
            onDone()
        }
    }

    fun endShift(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearShift()
            _cart.value = emptyList()
            onDone()
        }
    }

    // --------------------------------------------------------------- tema

    fun setTheme(mode: String) {
        _themeMode.value = mode
        val username = session.value?.username ?: return
        viewModelScope.launch { sessionManager.setTheme(username, mode) }
    }

    // --------------------------------------------------------------- cart

    fun addToCart(product: Product) {
        if (product.stock <= 0) return
        val current = _cart.value
        val existing = current.firstOrNull { it.productId == product.id }
        _cart.value = if (existing == null) {
            current + CartItem(
                productId = product.id,
                name = product.name,
                price = product.price,
                cost = product.cost,
                qty = 1,
                image = product.image
            )
        } else {
            if (existing.qty >= product.stock) return
            current.map {
                if (it.productId == product.id) it.copy(qty = it.qty + 1) else it
            }
        }
    }

    fun decrease(item: CartItem) {
        _cart.value = _cart.value.mapNotNull {
            if (it.productId == item.productId) {
                if (it.qty <= 1) null else it.copy(qty = it.qty - 1)
            } else it
        }
    }

    fun removeFromCart(item: CartItem) {
        _cart.value = _cart.value.filterNot { it.productId == item.productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun cartTotal(): Double = _cart.value.sumOf { it.subtotal }

    fun checkout(paymentMethod: String, paidAmount: Double, onDone: (Transaction?) -> Unit) {
        viewModelScope.launch {
            val items = _cart.value
            if (items.isEmpty()) {
                onDone(null)
                return@launch
            }
            val s = session.value
            val result = runCatching {
                repository.checkout(
                    items = items,
                    cashier = s?.fullName ?: "Kasir",
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    outlet = s?.outletName.orEmpty(),
                    shift = settings.value.shiftLabel(s?.shiftKey.orEmpty())
                )
            }.getOrNull()
            if (result != null) _cart.value = emptyList()
            onDone(result)
        }
    }

    // ------------------------------------------------------------ products

    fun saveProduct(product: Product) {
        viewModelScope.launch { runCatching { repository.saveProduct(product) } }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { runCatching { repository.deleteProduct(product) } }
    }

    fun encodeImage(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repository.encodeImage(uri)) }
    }

    // ------------------------------------------------------------- outlets

    fun saveOutlet(outlet: Outlet, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.saveOutlet(outlet))
        }
    }

    fun deleteOutlet(outlet: Outlet, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteOutlet(outlet))
        }
    }

    // ------------------------------------------------------------ settings

    fun saveSettings(settings: AppSettings, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.saveSettings(settings))
        }
    }

    // ----------------------------------------------------------- pembaruan

    /**
     * Cek versi terbaru. Otomatis dipanggil saat aplikasi dibuka (maks. tiap
     * 6 jam); [manual] = true saat pengguna menekan tombol "Cek Pembaruan"
     * sehingga pesan "sudah versi terbaru"/error ikut ditampilkan.
     */
    fun checkForUpdate(manual: Boolean = false) {
        val state = _updateState.value
        if (state.checking || state.progress != null) return

        val now = System.currentTimeMillis()
        val lastCheck = localPrefs.getLong(KEY_LAST_CHECK, 0L)
        if (!manual && now - lastCheck < AUTO_CHECK_INTERVAL) {
            _updateState.update { it.copy(lastCheckedAt = lastCheck) }
            return
        }

        viewModelScope.launch {
            _updateState.update {
                it.copy(checking = true, error = null, message = null)
            }
            val currentCode = UpdateChecker.installedVersion(context).versionCode
            val result = UpdateChecker.check(currentCode)
            val checkedAt = System.currentTimeMillis()
            localPrefs.edit().putLong(KEY_LAST_CHECK, checkedAt).apply()

            when (result) {
                is UpdateCheckResult.Available -> {
                    downloadedApk = null
                    _updateState.update {
                        it.copy(
                            checking = false,
                            available = result.info,
                            downloaded = false,
                            progress = null,
                            message = null,
                            error = null,
                            lastCheckedAt = checkedAt
                        )
                    }
                }

                UpdateCheckResult.UpToDate -> _updateState.update {
                    it.copy(
                        checking = false,
                        available = null,
                        downloaded = false,
                        lastCheckedAt = checkedAt,
                        message = if (manual) "Aplikasi sudah versi terbaru." else null
                    )
                }

                is UpdateCheckResult.Failed -> _updateState.update {
                    it.copy(
                        checking = false,
                        lastCheckedAt = checkedAt,
                        error = if (manual) result.message else null
                    )
                }
            }
        }
    }

    /** Unduh APK pembaruan, lalu buka layar instalasi Android. */
    fun downloadUpdate() {
        val info = _updateState.value.available ?: return
        if (_updateState.value.progress != null) return

        viewModelScope.launch {
            _updateState.update {
                it.copy(progress = 0, error = null, message = null)
            }
            val result = runCatching {
                UpdateChecker.download(context, info) { percent ->
                    _updateState.update { it.copy(progress = percent) }
                }
            }
            val file = result.getOrNull()
            if (file == null) {
                _updateState.update {
                    it.copy(
                        progress = null,
                        error = result.exceptionOrNull()?.message
                            ?: "Gagal mengunduh pembaruan. Coba lagi."
                    )
                }
                return@launch
            }

            downloadedApk = file
            _updateState.update { it.copy(progress = null, downloaded = true) }
            if (UpdateChecker.canInstall(context)) {
                UpdateChecker.install(context, file)
            } else {
                UpdateChecker.openInstallPermission(context)
                _updateState.update {
                    it.copy(
                        message = "Izinkan \"Instal aplikasi tidak dikenal\" untuk JURAGAN 38, " +
                            "lalu tekan Pasang."
                    )
                }
            }
        }
    }

    /** Pasang APK yang sudah selesai diunduh. */
    fun installDownloadedUpdate() {
        val file = downloadedApk
        if (file == null || !file.exists()) {
            _updateState.update {
                it.copy(downloaded = false, message = "File pembaruan tidak ditemukan, unduh ulang.")
            }
            return
        }
        if (UpdateChecker.canInstall(context)) {
            UpdateChecker.install(context, file)
        } else {
            UpdateChecker.openInstallPermission(context)
            _updateState.update {
                it.copy(message = "Izinkan pemasangan aplikasi lalu tekan Pasang lagi.")
            }
        }
    }

    fun openInstallPermission() {
        UpdateChecker.openInstallPermission(context)
    }

    /** Tutup dialog pembaruan (unduhan yang sedang berjalan tidak dibatalkan). */
    fun dismissUpdate() {
        if (_updateState.value.progress != null) return
        _updateState.update { it.copy(available = null) }
    }

    fun clearUpdateMessage() {
        _updateState.update { it.copy(message = null, error = null) }
    }

    // --------------------------------------------------------------- users

    fun saveUser(user: User, password: String?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = runCatching { repository.saveUser(user, password) }
                .getOrElse { it.message ?: "Gagal menyimpan akun" }
            onResult(error)
        }
    }

    fun deleteUser(user: User, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = runCatching { repository.deleteUser(user) }
                .getOrElse { it.message ?: "Gagal menghapus akun" }
            onResult(error)
        }
    }

    /** Ubah password akun (butuh password lama sebagai verifikasi). */
    fun changePassword(
        user: User,
        currentPassword: String,
        newPassword: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val error = runCatching {
                repository.changePassword(user, currentPassword, newPassword)
            }.getOrElse { it.message ?: "Gagal mengubah password" }
            onResult(error)
        }
    }

    // ---------------------------------------------------------- transaksi

    /** Muat rincian item transaksi (dari cache, atau server bila belum ada). */
    fun loadItems(transactionId: String, onLoaded: (List<TransactionItem>) -> Unit) {
        viewModelScope.launch {
            val items = runCatching { repository.itemsOf(transactionId) }
                .getOrDefault(emptyList())
            onLoaded(items)
        }
    }

    fun deleteTransaction(transaction: Transaction, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteTransaction(transaction))
        }
    }

    fun deleteAllTransactions(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.deleteAllTransactions())
        }
    }
}
