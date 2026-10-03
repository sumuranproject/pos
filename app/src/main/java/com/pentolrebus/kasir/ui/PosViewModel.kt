package com.pentolrebus.kasir.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pentolrebus.kasir.data.PosRepository
import com.pentolrebus.kasir.domain.CartItem
import com.pentolrebus.kasir.domain.PaymentMethod
import com.pentolrebus.kasir.domain.Product
import com.pentolrebus.kasir.domain.Role
import com.pentolrebus.kasir.domain.Session
import com.pentolrebus.kasir.domain.Shift
import com.pentolrebus.kasir.domain.SyncStatus
import com.pentolrebus.kasir.domain.Transaction
import com.pentolrebus.kasir.domain.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object LoggedOut : AuthState
    data object Loading : AuthState
    data class LoggedIn(val session: Session) : AuthState
    data class Error(val message: String) : AuthState
}

class PosViewModel(private val repo: PosRepository) : ViewModel() {
    private val _auth = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val auth: StateFlow<AuthState> = _auth

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart

    private val _shift = MutableStateFlow<Shift?>(null)
    val shift: StateFlow<Shift?> = _shift

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    fun loginLocal(username: String, pin: String, expectedRole: Role? = null) {
        _auth.value = AuthState.Loading
        viewModelScope.launch {
            repo.localPinLogin(username, pin.toCharArray())
                .onSuccess { session -> acceptSession(session, expectedRole) }
                .onFailure { _auth.value = AuthState.Error(it.message ?: "Login gagal") }
        }
    }

    fun loginEmail(email: String, password: String, expectedRole: Role? = null) {
        _auth.value = AuthState.Loading
        viewModelScope.launch {
            repo.emailLogin(email, password)
                .onSuccess { session -> acceptSession(session, expectedRole) }
                .onFailure { _auth.value = AuthState.Error(it.message ?: "Login gagal") }
        }
    }

    fun register(
        email: String,
        password: String,
        username: String,
        pin: String,
        business: String,
        outlet: String,
        whatsapp: String?
    ) {
        _auth.value = AuthState.Loading
        viewModelScope.launch {
            repo.registerOwner(email, password, username, pin.toCharArray(), business, outlet, whatsapp)
                .onSuccess(::loginSuccess)
                .onFailure { _auth.value = AuthState.Error(it.message ?: "Registrasi gagal") }
        }
    }

    private fun acceptSession(session: Session, expectedRole: Role?) {
        if (expectedRole != null && session.role != expectedRole) {
            _auth.value = AuthState.Error("Akun ini bukan akun ${expectedRoleLabel(expectedRole)}.")
            return
        }
        loginSuccess(session)
    }

    private fun expectedRoleLabel(role: Role): String = if (role == Role.OWNER) "Owner" else "Kasir"

    private fun loginSuccess(session: Session) {
        _auth.value = AuthState.LoggedIn(session)
        viewModelScope.launch {
            _products.value = repo.loadProducts(session.outletId.orEmpty())
            _shift.value = repo.loadActiveShift(session)
            _transactions.value = repo.loadTransactions(session)
        }
    }

    fun add(product: Product) {
        val updated = _cart.value.toMutableList()
        val index = updated.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val current = updated[index]
            updated[index] = current.copy(quantity = current.quantity + 1)
        } else {
            updated.add(CartItem(product, 1))
        }
        _cart.value = updated
    }

    fun remove(product: Product) {
        _cart.value = _cart.value.mapNotNull { item ->
            when {
                item.product.id != product.id -> item
                item.quantity > 1 -> item.copy(quantity = item.quantity - 1)
                else -> null
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun startShift(openingCash: Long) {
        val session = (_auth.value as? AuthState.LoggedIn)?.session ?: return
        viewModelScope.launch {
            repo.startShift(session, openingCash).onSuccess { _shift.value = it }
        }
    }

    fun checkout(method: PaymentMethod, qrisPath: String? = null) {
        val session = (_auth.value as? AuthState.LoggedIn)?.session ?: return
        val currentShift = _shift.value ?: return
        val items = _cart.value.map {
            TransactionItem(
                productId = it.product.id,
                name = it.product.name,
                price = it.product.price,
                quantity = it.quantity,
                subtotal = it.product.price * it.quantity
            )
        }
        if (items.isEmpty()) return

        val total = items.sumOf { it.subtotal }
        val transaction = Transaction(
            ownerUid = session.uid,
            businessId = session.businessId.orEmpty(),
            outletId = session.outletId.orEmpty(),
            shiftId = currentShift.id,
            cashierUid = session.uid,
            items = items,
            subtotal = total,
            total = total,
            paymentMethod = method,
            qrisProofPath = qrisPath,
            syncStatus = SyncStatus.PENDING_SYNC
        )

        viewModelScope.launch {
            repo.saveTransaction(transaction).onSuccess {
                _transactions.value = repo.loadTransactions(session)
                clearCart()
            }
        }
    }

    fun closeShift(closingCash: Long) {
        val currentShift = _shift.value ?: return
        viewModelScope.launch {
            repo.closeShift(currentShift, closingCash).onSuccess { _shift.value = null }
        }
    }

    fun logout() {
        if (_shift.value != null) return
        repo.logout()
        _auth.value = AuthState.LoggedOut
        clearCart()
    }
}
