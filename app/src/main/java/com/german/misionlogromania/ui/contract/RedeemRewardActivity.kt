package com.german.misionlogromania.ui.rewards

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RedeemRewardActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private var childId: String? = null
    private var childStars: Long = 0
    private val TAG = "RedeemRewardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redeem_reward)

        // 🔙 Botón de regreso
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        childId = intent.getStringExtra("childId")
        Log.d(TAG, "Child ID recibido: $childId")

        if (childId == null) {
            Toast.makeText(this, "No se encontró el ID del niño", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchChildStars()
    }

    // === OBTENER ESTRELLAS DEL NIÑO ===
    private fun fetchChildStars() {
        db.collection("children").document(childId!!)
            .get()
            .addOnSuccessListener { doc ->
                childStars = doc.getLong("stars") ?: 0
                Log.d(TAG, "Estrellas actuales del niño: $childStars")
                loadRewards()
            }
            .addOnFailureListener {
                childStars = 0
                Toast.makeText(this, "Error al obtener estrellas", Toast.LENGTH_SHORT).show()
                loadRewards()
            }
    }

    // === CARGAR RECOMPENSAS ===
    private fun loadRewards() {
        db.collection("rewards")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "No hay recompensas disponibles", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val rewards = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title")
                    val points = doc.getLong("points")
                    val level = doc.getString("level")

                    if (title != null && points != null && level != null) {
                        val normalizedLevel = when (level.trim().lowercase()) {
                            "facil", "fácil" -> "Fácil"
                            "medio" -> "Medio"
                            "avanzado", "dificil", "difícil" -> "Avanzado"
                            else -> level
                        }
                        Reward(doc.id, title, points.toInt(), normalizedLevel)
                    } else null
                }

                val grouped = rewards.groupBy { it.level }
                displayGroupedRewards(grouped)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar recompensas", Toast.LENGTH_SHORT).show()
            }
    }

    // === MOSTRAR RECOMPENSAS AGRUPADAS ===
    private fun displayGroupedRewards(groupedRewards: Map<String, List<Reward>>) {
        val container = findViewById<LinearLayout>(R.id.rewardsContainer)
        container.removeAllViews()

        val preferredOrder = listOf("Fácil", "Facil", "Medio", "Avanzado")
        val sortedLevels = groupedRewards.keys.sortedBy {
            val index = preferredOrder.indexOfFirst { level -> level.equals(it, true) }
            if (index >= 0) index else 999
        }

        sortedLevels.forEach { level ->
            val levelRewards = groupedRewards[level] ?: return@forEach

            val title = TextView(this).apply {
                text = "🌟 $level"
                textSize = 22f
                setTextColor(resources.getColor(android.R.color.white, null))
                setPadding(0, 32, 0, 16)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            container.addView(title)

            levelRewards.forEach { reward ->
                val itemView = layoutInflater.inflate(R.layout.item_reward, container, false)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvRewardTitle)
                val tvStars = itemView.findViewById<TextView>(R.id.tvRewardStars)
                val btnRedeem = itemView.findViewById<Button>(R.id.btnRedeem)

                tvTitle.text = reward.title
                tvStars.text = "${reward.requiredStars} ⭐"
                val canRedeem = childStars >= reward.requiredStars
                btnRedeem.isEnabled = canRedeem
                btnRedeem.alpha = if (canRedeem) 1f else 0.5f

                btnRedeem.setOnClickListener {
                    showConfirmDialog(reward)
                }

                container.addView(itemView)
            }
        }
    }

    // === CONFIRMACIÓN DE CANJE ===
    private fun showConfirmDialog(reward: Reward) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar canje")
            .setMessage("¿Estás seguro de que deseas canjear la recompensa \"${reward.title}\" por ${reward.requiredStars} estrellas?")
            .setPositiveButton("Aceptar") { _, _ ->
                redeemReward(reward)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // === CANJEAR RECOMPENSA ===
    private fun redeemReward(reward: Reward) {
        if (childStars < reward.requiredStars) {
            Toast.makeText(this, "No tienes suficientes estrellas", Toast.LENGTH_SHORT).show()
            return
        }

        val newStars = childStars - reward.requiredStars

        // 🔸 1. Registrar canje en "rewardRedemptions"
        val redemption = hashMapOf(
            "childId" to childId,
            "rewardId" to reward.id,
            "rewardTitle" to reward.title,
            "pointsSpent" to reward.requiredStars,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )

        db.collection("rewardRedemptions")
            .add(redemption)
            .addOnSuccessListener {
                // 🔸 2. Actualizar estrellas del niño
                db.collection("children").document(childId!!)
                    .update("stars", newStars)
                    .addOnSuccessListener {
                        childStars = newStars
                        Toast.makeText(this, "Canje exitoso 🎉\nTe quedan $newStars estrellas", Toast.LENGTH_LONG).show()

                        // 🔸 3. Enviar notificación al padre
                        sendParentNotification(reward)

                        // 🔸 4. Recargar pantalla
                        loadRewards()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar las estrellas", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar el canje", Toast.LENGTH_SHORT).show()
            }
    }

    // === ENVIAR NOTIFICACIÓN AL PADRE ===
    private fun sendParentNotification(reward: Reward) {
        val notification = hashMapOf(
            "type" to "reward_redeemed",
            "message" to "Tu hijo ha canjeado la recompensa \"${reward.title}\"",
            "timestamp" to System.currentTimeMillis(),
            "childId" to childId
        )

        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d(TAG, "Notificación enviada correctamente al padre.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al enviar notificación: ${e.message}", e)
            }
    }
}

// === MODELO ===
data class Reward(
    val id: String,
    val title: String,
    val requiredStars: Int,
    val level: String
)