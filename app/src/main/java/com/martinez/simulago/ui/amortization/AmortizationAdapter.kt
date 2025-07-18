package com.martinez.simulago.ui.amortization
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.martinez.simulago.databinding.ItemAmortizationRowBinding
import com.martinez.simulago.domain.model.AmortizationEntry
import java.text.NumberFormat
import java.util.Locale

class AmortizationAdapter(private val entries: List<AmortizationEntry>) : RecyclerView.Adapter<AmortizationAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAmortizationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount() = entries.size

    inner class ViewHolder(private val binding: ItemAmortizationRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: AmortizationEntry) {
            binding.tvMonthNumber.text = entry.monthNumber.toString()
            binding.tvInterest.text = currencyFormat.format(entry.interestPaid)
            binding.tvPrincipal.text = currencyFormat.format(entry.principalPaid)
            binding.tvRemainingBalance.text = currencyFormat.format(entry.remainingBalance)
        }
    }
}