package com.martinez.simulago.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.martinez.simulago.R
import com.martinez.simulago.databinding.FragmentCalculatorBinding
import java.text.NumberFormat
import java.util.Locale

class CalculatorFragment : Fragment() {
    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupListeners() {
        // Listener para el slider de Monto
        binding.sliderLoanAmount.addOnChangeListener { slider, value, fromUser ->
            // Formatear el valor como moneda
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0 // Sin decimales
            binding.tvLoanAmountValue.text = format.format(value)
        }
        // Listener para el slider de Plazo
        binding.sliderLoanTerm.addOnChangeListener { slider, value, fromUser ->
            binding.tvLoanTermValue.text = "${value.toInt()} months"
        }

        // Listener para el botón de simulación
        binding.btnSimulate.setOnClickListener {
            // --- AQUÍ IRÁ LA LÓGICA DE CÁLCULO MÁS ADELANTE ---
            // Por ahora, solo mostramos la tarjeta de resultado
            calculateAndShowResult()
        }
    }

    private fun calculateAndShowResult() {
        // TODO: Reemplazar con el cálculo real desde el ViewModel

        // Hacemos visible la tarjeta de resultado
        binding.cardResult.isVisible = true
        // Puedes poner un valor de ejemplo por ahora
        binding.tvResultPayment.text = "$322.67"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiamos el binding para evitar memory leaks
        _binding = null
    }


}


