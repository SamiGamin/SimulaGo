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
    val uiState: StateFlow<ManageCreditUiState> =
        combine(
            creditFlow,
            paymentsFlow
        ) { credit, payments ->
            // Caso 1: Aún no hemos recibido el crédito de la base de datos.
            // Esto puede pasar al inicio o si el ID no es válido.
            if (credit == null) {
                // Devolvemos un estado de carga o de error, sin más cálculos.
                return@combine ManageCreditUiState(isLoading = true)
            }

            // Si llegamos aquí, 'credit' no es nulo y podemos proceder con los cálculos.
            val monthlyRate = credit.interestRate.toDoubleOrNull()?.div(100) ?: 0.0
            var currentBalance = credit.loanAmountToFinance
            val baseMonthlyPayment = credit.monthlyPayment

            // Iteramos sobre los pagos para calcular el saldo pendiente actualizado.
            // Es importante ordenar por fecha para que el cálculo sea correcto.
            for (payment in payments.sortedBy { it.paymentDate }) {
                // Calculamos el interés generado sobre el saldo ANTES de este pago.
                // (Esta es una aproximación mensual. Es correcta para nuestro modelo).
                val interestForPeriod = currentBalance * monthlyRate

                // Calculamos cuánto de la cuota fija se va a capital.
                val principalFromBasePayment = baseMonthlyPayment - interestForPeriod

                // Reducimos el saldo por el capital pagado de la cuota Y el abono extra.
                currentBalance -= (principalFromBasePayment + payment.extraToCapital)
            }

            // Finalmente, construimos el estado de la UI con todos los datos calculados.
            ManageCreditUiState(
                activeCredit = credit,
                paymentHistory = payments,
                currentBalance = if (currentBalance < 0) 0.0 else currentBalance, // Evitamos saldos negativos
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ManageCreditUiState(isLoading = true)
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
