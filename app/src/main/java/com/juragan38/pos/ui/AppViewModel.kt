package com.juragan38.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juragan38.pos.data.Session
import com.juragan38.pos.data.SessionManager
import com.juragan38.pos.data.local.CartItem
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.repository.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val session: StateFlow<Session?> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.seedIfNeeded() }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            if (username.isBlank() || password.isBlank()) {
                _loginError.value = "Username dan password wajib diisi"
                return@launch
            }
            runCatching { repository.seedIfNeeded() }
            val user = runCatching { repository.login(username.trim(), password) }.getOrNull()
            if (user == null) {
                _loginError.value = "Username atau password salah"
            } else {
                sessionManager.save(Session(user.fullName, user.username, user.role))
                onSuccess()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clear()
            _cart.value = emptyList()
        }
    }

    fun addToCart(product: Product) {
        if (product.stock <= 0) return
        val current = _cart.value
        val existing = current.firstOrNull { it.productId == product.id }
        _cart.value = if (existing == null) {
            current + CartItem(product.id, product.name, product.price, product.cost, 1)
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
            val result = runCatching {
                repository.checkout(
                    items = items,
                    cashier = session.value?.fullName ?: "Kasir",
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount
                )
            }.getOrNull()
            if (result != null) _cart.value = emptyList()
            onDone(result)
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch { runCatching { repository.saveProduct(product) } }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { runCatching { repository.deleteProduct(product) } }
    }
}
