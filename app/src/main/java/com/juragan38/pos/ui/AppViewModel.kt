package com.juragan38.pos.ui

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
import com.juragan38.pos.data.local.User
import com.juragan38.pos.data.repository.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
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

    // ---------------------------------------------------------- transaksi

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
