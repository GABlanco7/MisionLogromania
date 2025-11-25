package com.german.misionlogromania.ui.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R

class DayAdapter(
    private val days: List<Day>,
    private val isReadOnlyMode: Boolean = false, // 🆕 Parámetro para modo solo lectura
    private val onClick: (Day) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val ivStar: ImageView = view.findViewById(R.id.ivStar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        // Ocultar las celdas vacías (espacios antes del inicio del mes)
        if (day.dayNumber == 0) {
            holder.itemView.visibility = View.INVISIBLE
            return
        } else {
            holder.itemView.visibility = View.VISIBLE
        }

        // Mostrar el número del día
        holder.tvDay.text = day.dayNumber.toString()

        // Asignar el fondo según el estado
        holder.tvDay.setBackgroundResource(
            when {
                day.isConfirmed -> R.drawable.bg_day_confirmed   // Día confirmado (verde)
                day.isToday -> R.drawable.bg_day_today           // Día actual (azul)
                else -> R.drawable.bg_day_normal                 // Día normal (gris o blanco)
            }
        )

        // Cambiar el color del texto según el fondo
        holder.tvDay.setTextColor(
            when {
                day.isConfirmed -> holder.itemView.context.getColor(android.R.color.white)
                day.isToday -> holder.itemView.context.getColor(android.R.color.white)
                else -> holder.itemView.context.getColor(android.R.color.black) // <- visible en fondo claro
            }
        )

        // Mostrar estrellita solo si está confirmado
        holder.ivStar.visibility = if (day.isConfirmed) View.VISIBLE else View.GONE

        // 🆕 LÓGICA DE CLICK MODIFICADA
        holder.itemView.setOnClickListener {
            if (isReadOnlyMode) {
                // Si es modo solo lectura, mostrar mensaje informativo
                Toast.makeText(
                    holder.itemView.context,
                    "👁️ Vista de solo lectura. No puedes editar desde aquí.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Si es modo editable (hijo), permitir las acciones normales
                when {
                    day.isConfirmed -> {
                        Toast.makeText(holder.itemView.context, "Este día ya fue confirmado", Toast.LENGTH_SHORT).show()
                    }
                    day.isToday -> onClick(day)
                    else -> {
                        Toast.makeText(holder.itemView.context, "Solo puedes marcar el día actual", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = days.size
}