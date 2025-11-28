package com.german.misionlogromania.ui.parent

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.model.Mission
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfirmMissionsActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var missionsLayout: LinearLayout
    private lateinit var rewardsLayout: LinearLayout
    private lateinit var btnConfirmSelection: Button
    private lateinit var btnCancel: Button

    private var childId = ""
    private var childAge = 0 // ✅ Edad persistente del niño
    private val TAG = "ConfirmMissionsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_missions)

        // Referencias de UI
        missionsLayout = findViewById(R.id.layoutMissions)
        rewardsLayout = findViewById(R.id.layoutRewards)
        btnConfirmSelection = findViewById(R.id.btnConfirmSelection)
        btnCancel = findViewById(R.id.btnCancel)

        // 📦 Recuperar datos del intent
        childId = intent.getStringExtra("childId") ?: ""
        childAge = intent.getIntExtra("childAge", 0)

        Log.d(TAG, "onCreate: childId=$childId, childAge=$childAge")

        // Recuperar misiones seleccionadas
        val selectedMissions: ArrayList<Mission>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("selectedMissions", Mission::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("selectedMissions")
        }

        // Mostrar o advertir
        if (selectedMissions.isNullOrEmpty()) {
            Toast.makeText(this, "No se encontraron misiones seleccionadas", Toast.LENGTH_SHORT).show()
        } else {
            displayMissions(selectedMissions)
            loadRewards()
        }

        // ✅ Confirmar selección
        btnConfirmSelection.setOnClickListener {
            if (selectedMissions.isNullOrEmpty()) {
                Toast.makeText(this, "No hay misiones para confirmar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMissionsToFirestore(selectedMissions)
        }

        // 🔙 Cancelar: volver a SelectMissionsActivity
        btnCancel.setOnClickListener {
            Log.d(TAG, "Cancel pressed -> back to SelectMissionsActivity")
            val intent = Intent(this, SelectMissionsActivity::class.java)
            intent.putExtra("childId", childId)
            intent.putExtra("childAge", childAge) // ✅ Reenviar edad
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    /** 🎯 Mostrar lista de misiones seleccionadas */
    private fun displayMissions(missions: List<Mission>) {
        missionsLayout.removeAllViews()
        missions.forEach { mission ->
            val tv = TextView(this).apply {
                text = "🎯 ${mission.title}\n${mission.description}"
                textSize = 16f
                setPadding(16, 8, 16, 8)
                setTextColor(Color.parseColor("#000000"))   // 👈 AGREGADO
            }
            missionsLayout.addView(tv)
        }
    }


    /** 🏆 Cargar recompensas desde Firestore agrupadas por nivel */
    private fun loadRewards() {
        Log.d(TAG, "Cargando recompensas desde Firestore...")
        rewardsLayout.removeAllViews()

        db.collection("rewards")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    val tv = TextView(this).apply {
                        text = "No hay recompensas disponibles"
                        textSize = 16f
                        setPadding(16, 8, 16, 8)
                    }
                    rewardsLayout.addView(tv)
                    return@addOnSuccessListener
                }

                val rewardsByLevel = mutableMapOf<String, MutableList<RewardData>>()

                docs.forEach { doc ->
                    val title = doc.getString("title") ?: "Recompensa sin nombre"
                    val description = doc.getString("description") ?: ""
                    val points = doc.getLong("points") ?: 0L
                    val level = doc.getString("level") ?: "medio"

                    val normalizedLevel = when (level.trim().lowercase()) {
                        "facil", "fácil" -> "Fácil"
                        "medio" -> "Medio"
                        "avanzado", "dificil", "difícil" -> "Avanzado"
                        else -> level.trim().replaceFirstChar { it.uppercase() }
                    }

                    rewardsByLevel.getOrPut(normalizedLevel) { mutableListOf() }
                        .add(RewardData(title, description, points, normalizedLevel))
                }

                // Orden visual
                val levelOrder = listOf("Fácil", "Medio", "Avanzado")
                val sortedLevels = rewardsByLevel.keys.sortedBy { level ->
                    levelOrder.indexOf(level).takeIf { it >= 0 } ?: 999
                }

                // Mostrar recompensas por nivel
                sortedLevels.forEach { level ->
                    val rewards = rewardsByLevel[level] ?: return@forEach

                    val levelTitle = TextView(this).apply {
                        text = "🌟 $level"
                        textSize = 20f
                        setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                        setPadding(16, 24, 16, 8)
                        setTypeface(null, Typeface.BOLD)
                    }
                    rewardsLayout.addView(levelTitle)

                    rewards.forEach { reward ->
                        val tv = TextView(this).apply {
                            text = "🏆 ${reward.title}\n" +
                                    (if (reward.description.isNotEmpty()) "${reward.description}\n" else "") +
                                    "Puntos: ${reward.points} ⭐"
                            textSize = 16f
                            setPadding(32, 8, 16, 8)
                            setLineSpacing(4f, 1f)
                            setTextColor(Color.parseColor("#000000"))  // 👈 AGREGADO
                        }

                        rewardsLayout.addView(tv)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar recompensas: ${e.message}", e)
                Toast.makeText(this, "Error al cargar recompensas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 💾 Guardar misiones asignadas y crear notificación para el padre */
    private fun saveMissionsToFirestore(missions: List<Mission>) {
        val childRef = db.collection("children").document(childId)

        childRef.get()
            .addOnSuccessListener { doc ->
                val currentMissions = (doc.get("assignedMissions") as? MutableList<Map<String, Any>> ?: mutableListOf())
                val currentNotifications = (doc.get("notifications") as? MutableList<Map<String, Any>> ?: mutableListOf())

                val newMissions = missions.map { mission ->
                    mapOf(
                        "id" to mission.id,
                        "title" to mission.title,
                        "description" to mission.description,
                        "difficulty" to mission.difficulty,
                        "category" to mission.category,
                        "points" to 1
                    )
                }

                val finalMissions = currentMissions.toMutableList().apply {
                    newMissions.forEach { new ->
                        if (none { it["id"] == new["id"] }) add(new)
                    }
                }

                val newNotifications = missions.map { mission ->
                    mapOf(
                        "missionId" to mission.id,
                        "title" to "Nueva misión asignada",
                        "message" to "Tu hijo tiene una nueva misión: ${mission.title}",
                        "seen" to false
                    )
                }

                val finalNotifications = currentNotifications.toMutableList().apply {
                    addAll(newNotifications)
                }

                childRef.update(
                    mapOf(
                        "assignedMissions" to finalMissions,
                        "notifications" to finalNotifications
                    )
                ).addOnSuccessListener {
                    Toast.makeText(this, "Misiones y notificaciones guardadas", Toast.LENGTH_SHORT).show()
                    goToParentMenu()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener niño: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔙 Regresar al menú del padre (solo al confirmar) */
    private fun goToParentMenu() {
        val intent = Intent(this, ParentMenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    /** 📦 Estructura para recompensas */
    private data class RewardData(
        val title: String,
        val description: String,
        val points: Long,
        val level: String
    )
}