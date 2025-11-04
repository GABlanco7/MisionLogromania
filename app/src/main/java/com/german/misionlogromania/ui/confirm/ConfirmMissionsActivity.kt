package com.german.misionlogromania.ui.confirm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.model.Mission
import com.german.misionlogromania.ui.menu.PadreMenuActivity
import com.german.misionlogromania.ui.select.SelectMissionsActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConfirmMissionsActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var missionsLayout: LinearLayout
    private lateinit var rewardsLayout: LinearLayout
    private lateinit var btnConfirmSelection: Button
    private lateinit var btnCancel: Button
    private var childId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_missions)

        // Referencias a vistas
        missionsLayout = findViewById(R.id.layoutMissions)
        rewardsLayout = findViewById(R.id.layoutRewards)
        btnConfirmSelection = findViewById(R.id.btnConfirmSelection)
        btnCancel = findViewById(R.id.btnCancel)

        childId = intent.getStringExtra("childId") ?: ""

        val selectedMissions: ArrayList<Mission>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("selectedMissions", Mission::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Mission>("selectedMissions")
        }

        if (selectedMissions.isNullOrEmpty()) {
            Toast.makeText(this, "No se encontraron misiones seleccionadas", Toast.LENGTH_SHORT).show()
        } else {
            displayMissions(selectedMissions)
            loadRewards()
        }

        // Acción botón confirmar
        btnConfirmSelection.setOnClickListener {
            if (selectedMissions.isNullOrEmpty()) {
                Toast.makeText(this, "No hay misiones para confirmar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveMissionsToFirestore(selectedMissions)
        }

        // Acción botón cancelar → volver a selección de misiones
        btnCancel.setOnClickListener {
            val intent = Intent(this, SelectMissionsActivity::class.java)
            intent.putExtra("childId", childId)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    /** 🎯 Mostrar misiones seleccionadas */
    private fun displayMissions(missions: List<Mission>) {
        missionsLayout.removeAllViews()
        missions.forEach { mission ->
            val tv = TextView(this).apply {
                text = "🎯 ${mission.title}\n${mission.description}"
                textSize = 16f
                setPadding(16, 8, 16, 8)
            }
            missionsLayout.addView(tv)
        }
    }

    /** 🏆 Cargar recompensas */
    private fun loadRewards() {
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

                docs.forEach { doc ->
                    val title = doc.getString("title") ?: "Recompensa sin nombre"
                    val description = doc.getString("description") ?: ""
                    val points = doc.getLong("points") ?: 0L

                    val tv = TextView(this).apply {
                        text = "🏆 $title\n$description\nPuntos: $points"
                        textSize = 16f
                        setPadding(16, 8, 16, 8)
                    }
                    rewardsLayout.addView(tv)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar recompensas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 💾 Guardar misiones y crear notificación al padre */
    private fun saveMissionsToFirestore(missions: List<Mission>) {
        val childRef = db.collection("children").document(childId)

        childRef.get().addOnSuccessListener { doc ->
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

            // 🔹 Evitar duplicados
            val finalMissions = currentMissions.toMutableList()
            for (m in newMissions) {
                val exists = finalMissions.any { it["id"] == m["id"] }
                if (!exists) finalMissions.add(m)
            }

            // 🔹 Crear notificaciones nuevas
            val newNotifications = missions.map { mission ->
                mapOf(
                    "missionId" to mission.id,
                    "title" to "Nueva misión asignada",
                    "message" to "Tu hijo tiene una nueva misión: ${mission.title}",
                    "seen" to false
                )
            }

            val finalNotifications = currentNotifications.toMutableList()
            finalNotifications.addAll(newNotifications)

            // 🔹 Guardar en Firestore
            childRef.update(
                mapOf(
                    "assignedMissions" to finalMissions,
                    "notifications" to finalNotifications
                )
            )
                .addOnSuccessListener {
                    Toast.makeText(this, "Misiones y notificaciones guardadas", Toast.LENGTH_SHORT).show()
                    goToParentMenu()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al obtener niño: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 🔙 Regresar al menú del padre (solo se usa al confirmar) */
    private fun goToParentMenu() {
        startActivity(Intent(this, PadreMenuActivity::class.java))
        finish()
    }
}
