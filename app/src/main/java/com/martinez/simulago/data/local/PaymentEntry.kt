package com.martinez.simulago.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SavedSimulation::class,
            parentColumns = ["id"],
            childColumns = ["simulationId"],
            onDelete = ForeignKey.CASCADE // Si se borra la simulación, se borran sus pagos
        )
    ]
)
data class PaymentEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val simulationId: Int, // La clave foránea al crédito activo
    val paymentDate: Date,
    val amountPaid: Double, // El monto real pagado (cuota + extras)
    val extraToCapital: Double = 0.0, // Abono a capital específico de este pago
    val insuranceAndOthers: Double = 0.0, // Seguros y otros cargos de este pago
    val notes: String? = null // Para que el usuario añada notas
)