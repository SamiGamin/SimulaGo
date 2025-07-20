package com.martinez.simulago.ui.manage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinez.simulago.data.local.PaymentDao
import com.martinez.simulago.data.local.PaymentEntry
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.local.SimulationDao
import com.martinez.simulago.domain.model.ManageCreditUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ManageCreditViewModel @Inject constructor(
    private val simulationDao: SimulationDao,
    private val paymentDao: PaymentDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val simulationId: StateFlow<Int> = savedStateHandle.getStateFlow("simulationId", 0)

    private val creditFlow: Flow<SavedSimulation?> = simulationId.flatMapLatest { id ->
        if (id > 0) simulationDao.getSimulationById(id) else flowOf(null)
    }

    private val paymentsFlow: Flow<List<PaymentEntry>> = simulationId.flatMapLatest { id ->
        if (id > 0) paymentDao.getPaymentsForSimulation(id) else flowOf(emptyList())
    }
    val uiState: StateFlow<ManageCreditUiState> = simulationId
        .filter { it > 0 } // 1. No hacemos nada hasta tener un ID válido (> 0)
        .flatMapLatest { id -> // 2. Cuando tenemos un ID, empezamos a observar la DB
            combine(
                simulationDao.getSimulationById(id),
                paymentDao.getPaymentsForSimulation(id)
            ) { credit, payments ->
                // 3. Ahora, dentro del combine, estamos seguros de que 'credit' y 'payments'
                // corresponden al ID correcto.
                if (credit == null) {
                    return@combine ManageCreditUiState(isLoading = false, activeCredit = null) // Estado de error/vacío
                }

                // ... tu lógica de cálculo de saldo (que está perfecta) ...
                val monthlyRate = credit.interestRate.toDoubleOrNull()?.div(100) ?: 0.0
                var currentBalance = credit.loanAmountToFinance
                val baseMonthlyPayment = credit.monthlyPayment

                for (payment in payments.sortedBy { it.paymentDate }) {
                    val interestForPeriod = currentBalance * monthlyRate
                    val principalFromBasePayment = baseMonthlyPayment - interestForPeriod
                    currentBalance -= (principalFromBasePayment + payment.extraToCapital)
                }

                ManageCreditUiState(
                    activeCredit = credit,
                    paymentHistory = payments,
                    currentBalance = if (currentBalance < 0) 0.0 else currentBalance,
                    isLoading = false
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ManageCreditUiState(isLoading = true) // El estado inicial es siempre 'cargando'
        )

    fun registerPayment(amount: Double, extraToCapital: Double, insurance: Double, notes: String?) {
        viewModelScope.launch {
            // Verificamos que tengamos un ID de simulación válido antes de intentar guardar.
            val currentId = simulationId.value
            if (currentId > 0) {
                val payment = PaymentEntry(
                    simulationId = currentId,
                    paymentDate = Date(), // Usa la fecha y hora actual para el registro
                    amountPaid = amount,
                    extraToCapital = extraToCapital,
                    insuranceAndOthers = insurance,
                    notes = notes
                )
                // Inserta el nuevo pago en la base de datos.
                // El Flow 'uiState' reaccionará automáticamente a este cambio y se actualizará.
                paymentDao.insertPayment(payment)
            }
        }
    }
}
