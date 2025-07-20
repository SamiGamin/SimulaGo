package com.martinez.simulago.ui.manage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.martinez.simulago.data.local.PaymentEntry
import com.martinez.simulago.databinding.ItemPaymentHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class PaymentHistoryAdapter : ListAdapter<PaymentEntry, PaymentHistoryAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PaymentViewHolder(private val binding: ItemPaymentHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        // Formateadores para moneda y fecha
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        private val dateFormat = SimpleDateFormat("dd MMM. yyyy", Locale.getDefault())

        fun bind(payment: PaymentEntry) {
            binding.tvPaymentDate.text = dateFormat.format(payment.paymentDate)
            binding.tvTotalPaidValue.text = currencyFormat.format(payment.amountPaid)
            binding.tvExtraToCapitalValue.text = currencyFormat.format(payment.extraToCapital)
            binding.tvInsuranceValue.text = currencyFormat.format(payment.insuranceAndOthers)
        }
    }
}

class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentEntry>() {
    override fun areItemsTheSame(oldItem: PaymentEntry, newItem: PaymentEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PaymentEntry, newItem: PaymentEntry): Boolean {
        return oldItem == newItem
    }
}