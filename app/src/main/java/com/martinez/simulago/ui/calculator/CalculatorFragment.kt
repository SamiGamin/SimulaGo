package com.martinez.simulago.ui.calculator

import android.R.attr.text
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import com.martinez.simulago.R
import com.martinez.simulago.databinding.FragmentCalculatorBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CalculatorFragment : Fragment() {
    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalculatorViewModel by viewModels()
    private val colombianLocale = Locale("es", "CO")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeUiState()
    }
    // Estos listeners no causan problemas porque son eventos únicos
    private fun setupListeners() {
        // Volvemos a los addOnChangeListener para el tiempo real
        binding.sliderVehiclePrice.addOnChangeListener { _, value, fromUser ->
            // LA GUARDIA CLAVE: Solo notificar al ViewModel si el cambio fue hecho por el USUARIO
            if (fromUser) {
                viewModel.onVehiclePriceChange(value)
            }
        }

        binding.sliderDownPayment.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.onDownPaymentChange(value)
            }
        }

        binding.sliderLoanTerm.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.onTermChange(value)
            }
        }

        binding.etInterestRate.doAfterTextChanged { text ->
            viewModel.onRateChange(text.toString())
        }

        binding.btnSimulate.setOnClickListener {
            viewModel.onSimulateClicked()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    // Una función dedicada para actualizar toda la UI de una vez
    private fun updateUi(state: CalculatorUiState) {
        val currencyFormat = NumberFormat.getCurrencyInstance(colombianLocale).apply { maximumFractionDigits = 0 }

        binding.sliderVehiclePrice.value = state.vehiclePrice
        binding.tvVehiclePriceValue.text = currencyFormat.format(state.vehiclePrice)

        binding.sliderDownPayment.value = state.downPayment
        binding.tvDownPaymentValue.text = currencyFormat.format(state.downPayment)

        binding.sliderLoanTerm.value = state.loanTermInMonths.toFloat()
        binding.tvLoanTermValue.text = "${state.loanTermInMonths} meses"

        if (binding.etInterestRate.text.toString() != state.interestRate) {
            binding.etInterestRate.setText(state.interestRate)
            binding.etInterestRate.setSelection(state.interestRate.length)
        }

        // Resultados
        // La visibilidad ahora depende del estado en el ViewModel
        val hasValidResult = state.monthlyPayment != null
        binding.cardResult.isVisible = hasValidResult || state.showResults

        if (hasValidResult) {
            val fullCurrencyFormat = NumberFormat.getCurrencyInstance(colombianLocale).apply { maximumFractionDigits = 0 }
            binding.tvResultPayment.text = fullCurrencyFormat.format(state.monthlyPayment)
            binding.tvTotalInterest.text = fullCurrencyFormat.format(state.totalInterestPaid)
            binding.tvTotalLoanCost.text = fullCurrencyFormat.format(state.totalLoanCost)
        }

        // Errores
        state.error?.let { errorMsg ->
            // Aquí puedes mapear el error a tus strings.xml
            val userFriendlyError = getString(R.string.error_invalid_interest_rate) // Ejemplo
            Toast.makeText(context, userFriendlyError, Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
