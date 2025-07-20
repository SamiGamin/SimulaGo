package com.martinez.simulago.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.martinez.simulago.R
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.databinding.ItemSimulationBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class SimulationAdapter(
    private val onItemClicked: (SavedSimulation) -> Unit,
    private val onDeleteClicked: (SavedSimulation) -> Unit
) : ListAdapter<SavedSimulation, SimulationAdapter.SimulationViewHolder>(SimulationDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimulationViewHolder {
        val binding =
            ItemSimulationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SimulationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimulationViewHolder, position: Int) {
        val simulation = getItem(position)
        holder.bind(simulation)
    }

    inner class SimulationViewHolder(private val binding: ItemSimulationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(simulation: SavedSimulation) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                maximumFractionDigits = 0
            }
            val dateFormat = SimpleDateFormat("dd MMM. yyyy", Locale.getDefault())

            binding.tvSimulationName.text = simulation.simulationName
            binding.tvMonthlyPaymentValue.text = currencyFormat.format(simulation.monthlyPayment)
            binding.tvSimulationDate.text =
                "Guardado el: ${dateFormat.format(simulation.createdAt)}"
            if (simulation.isActiveCredit) {
                binding.root.strokeWidth = 4
                binding.root.strokeColor = itemView.context.getColor(R.color.md_theme_primary)
                binding.ivActiveIcon.isVisible = true
                binding.ivActiveIcon.text = "Crédito activo"
            } else {
                binding.root.strokeWidth = 0
                binding.ivActiveIcon.isVisible = false
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClicked(simulation)
            }
            binding.root.setOnClickListener {
                onItemClicked(simulation)
            }

        }
    }
}

class SimulationDiffCallback : DiffUtil.ItemCallback<SavedSimulation>() {
    override fun areItemsTheSame(oldItem: SavedSimulation, newItem: SavedSimulation): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SavedSimulation, newItem: SavedSimulation): Boolean {
        return oldItem == newItem
    }
}