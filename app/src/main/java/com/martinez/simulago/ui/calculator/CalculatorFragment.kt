package com.martinez.simulago.ui.calculator

import android.R.attr.text
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.martinez.simulago.R
import com.martinez.simulago.databinding.FragmentCalculatorBinding
import com.martinez.simulago.domain.model.CalculatorUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
@Suppress("DEPRECATION")
@AndroidEntryPoint
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
        binding.btnViewPlan.setOnClickListener {
            viewModel.uiState.value.amortizationTable?.let { table ->
                if (table.isNotEmpty()) {
                    // Navegar al Fragment de Amortización con los datos
                    val action = CalculatorFragmentDirections.actionCalculatorToAmortization(table.toTypedArray())
                    findNavController().navigate(action)
                } else {
                    Toast.makeText(context, getString(R.string.error_no_amortization_data), Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        binding.btnViewPlan.setOnClickListener {
            Log.d("NAV_DEBUG", "Botón 'Ver plan' pulsado!")
            viewModel.uiState.value.amortizationTable?.let { table ->
                Log.d("NAV_DEBUG", "Tabla de amortización encontrada con ${table.size} filas.")
                if (table.isNotEmpty()) {
                    val action = CalculatorFragmentDirections.actionCalculatorToAmortization(table.toTypedArray())
                    findNavController().navigate(action)
                    Log.d("NAV_DEBUG", "Navegación iniciada.")
                }else{
                    Log.d("NAV_DEBUG", "La tabla está vacía. No se navega.")
                }
            }?: run {
                Log.d("NAV_DEBUG", "Tabla de amortización es nula. No se navega.")
                Toast.makeText(context, getString(R.string.error_no_amortization_data), Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSaveSimulation.setOnClickListener {
            showNameInputDialog()
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
// En CalculatorFragment.kt

    private fun showNameInputDialog() {
        val context = requireContext()

        // 1. Crear el contenedor (un FrameLayout)
        val container = FrameLayout(context)

        // 2. Crear el EditText
        val input = EditText(context).apply {
            setSingleLine()
            hint = "Ej. Coche Rojo" // Un hint es mejor que un mensaje largo
        }

        // 3. Definir los márgenes para el EditText dentro del contenedor
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // Aquí está la magia: define los márgenes laterales
            marginStart = resources.getDimensionPixelSize(R.dimen.dialog_edittext_margin_start_end)
            marginEnd = resources.getDimensionPixelSize(R.dimen.dialog_edittext_margin_start_end)
        }
        input.layoutParams = params

        // 4. Añadir el EditText al contenedor
        container.addView(input)

        // 5. Construir y mostrar el diálogo
        MaterialAlertDialogBuilder(context)
            .setTitle("Guardar Simulación")
            .setMessage("Escribe un nombre para identificar esta simulación.")
            // 6. ¡Añadir el CONTENEDOR al diálogo, no el EditText directamente!
            .setView(container)
            .setPositiveButton("Guardar") { dialog, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.onSaveSimulationClicked(name)
                } else {
                    Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
