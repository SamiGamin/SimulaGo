package com.martinez.simulago.ui.amortization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.martinez.simulago.databinding.FragmentAmortizationBinding

class AmortizationFragment : Fragment() {
    private var _binding: FragmentAmortizationBinding? = null
    private val args: AmortizationFragmentArgs by navArgs()
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAmortizationBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amortizationList = args.amortizationData.toList()
        val adapter = AmortizationAdapter(amortizationList)
        binding.rvAmortization.layoutManager = LinearLayoutManager(context)
        binding.rvAmortization.adapter = adapter
    }


}