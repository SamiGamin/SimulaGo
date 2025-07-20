package com.martinez.simulago.ui.manage

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.martinez.simulago.R
import com.martinez.simulago.databinding.DialogAddPaymentBinding
import com.martinez.simulago.databinding.FragmentManageCreditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
@Suppress("DEPRECATION")
class ManageCreditFragment : Fragment() {

    private var _binding: FragmentManageCreditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManageCreditViewModel by viewModels()
    private lateinit var paymentHistoryAdapter: PaymentHistoryAdapter

    // SAMINGAMIN: Tag para logs
    private val TAG = "ManageCreditFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Fragmento creado")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Inflando vista")
        _binding = FragmentManageCreditBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Vista creada")

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Configurando RecyclerView")
        paymentHistoryAdapter = PaymentHistoryAdapter()
        binding.rvPaymentHistory.adapter = paymentHistoryAdapter
        binding.rvPaymentHistory.setHasFixedSize(true)
        binding.rvPaymentHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners: Configurando listeners")
        binding.fabAddPayment.setOnClickListener {
            Log.d(TAG, "fabAddPayment onClick: Botón presionado")
            showAddPaymentDialog()
        }
    }

    private fun observeUiState() {
        Log.d(TAG, "observeUiState: Observando UI State")
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "observeUiState: Nuevo estado recibido: $state")
                    binding.tvCreditName.text = state.activeCredit?.simulationName
                    binding.tvCurrentBalanceValue.text = formatCurrency(state.currentBalance)
                    paymentHistoryAdapter.submitList(state.paymentHistory)
                    binding.tvEmptyHistory.isVisible = state.paymentHistory.isEmpty()
                }
            }
        }
    }
    private fun formatCurrency(value: Double): String {
        val colombianLocale = Locale("es", "CO")
        return NumberFormat.getCurrencyInstance(colombianLocale).apply {
            maximumFractionDigits = 0
        }.format(value)
    }

    private fun showAddPaymentDialog() {
        Log.d(TAG, "showAddPaymentDialog: Mostrando diálogo para agregar pago")
        val dialogBinding = DialogAddPaymentBinding.inflate(LayoutInflater.from(context))
        val baseMonthlyPayment = viewModel.uiState.value.activeCredit?.monthlyPayment ?: 0.0
        Log.d(TAG, "showAddPaymentDialog: Cuota base mensual: $baseMonthlyPayment")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar Nuevo Pago")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "showAddPaymentDialog: Diálogo cancelado")
            }
            .setPositiveButton("Guardar Pago") { _, _ ->
                val extraToCapital = dialogBinding.tietExtraToCapital.text.toString().toDoubleOrNull() ?: 0.0
                val insurance = dialogBinding.tietInsurance.text.toString().toDoubleOrNull() ?: 0.0
                val notes = dialogBinding.tietNotes.text.toString().takeIf { it.isNotBlank() }
                val totalAmountPaid = baseMonthlyPayment + extraToCapital + insurance
                Log.d(TAG, "showAddPaymentDialog: Guardando pago - ExtraCapital: $extraToCapital, Seguro: $insurance, Notas: $notes, TotalPagado: $totalAmountPaid")

                viewModel.registerPayment(
                    amount = totalAmountPaid,
                    extraToCapital = extraToCapital,
                    insurance = insurance,
                    notes = notes
                )
                Toast.makeText(context, "Pago registrado con éxito", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "showAddPaymentDialog: Pago registrado")
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Vista destruida, limpiando binding")
        _binding = null // Importante para evitar fugas de memoria con ViewBinding
    }
}
