package com.german.misionlogromania.ui.select

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.model.Mission
import com.german.misionlogromania.ui.confirm.ConfirmMissionsActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SelectMissionsActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var missionsLayout: LinearLayout

    private var childId = ""
    private var childAge = 0

    // Listas separadas por categoría
    private val selectedHogar = mutableListOf<Mission>()
    private val selectedAutocuidado = mutableListOf<Mission>()
    private val selectedEscolar = mutableListOf<Mission>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_missions)

        missionsLayout = findViewById(R.id.missionsLayout)
        val btnAcceptMissions = findViewById<Button>(R.id.btnAcceptMissions)

        // 🔹 Recibir datos del niño
        childId = intent.getStringExtra("childId") ?: ""
        childAge = intent.getIntExtra("childAge", 0)

        if (childAge <= 0) {
            Toast.makeText(this, "Edad del niño inválida. No se pueden cargar misiones.", Toast.LENGTH_LONG).show()
            Log.e("SelectMissions", "childAge inválida: $childAge")
            return
        }

        // 🔹 Cargar misiones desde Firebase según edad
        loadMissions(childAge)

        // 🔹 Botón para confirmar selección
        btnAcceptMissions.setOnClickListener {
            if (selectedHogar.size != 1 || selectedAutocuidado.size != 1 || selectedEscolar.size != 1) {
                Toast.makeText(this, "Debes seleccionar una misión de cada categoría", Toast.LENGTH_SHORT).show()
            } else {
                val finalSelected = mutableListOf<Mission>()
                finalSelected.addAll(selectedHogar)
                finalSelected.addAll(selectedAutocuidado)
                finalSelected.addAll(selectedEscolar)

                // 🔹 Elegir la misión que se confirmará primero (ejemplo: Hogar)
                val missionToConfirm = selectedHogar.firstOrNull()
                    ?: selectedAutocuidado.firstOrNull()
                    ?: selectedEscolar.firstOrNull()

                if (missionToConfirm == null) {
                    Toast.makeText(this, "No se pudo determinar la misión a confirmar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(this, ConfirmMissionsActivity::class.java)
                intent.putExtra("childId", childId)
                intent.putParcelableArrayListExtra("selectedMissions", ArrayList(finalSelected))
                intent.putExtra("missionId", missionToConfirm.id) // ✅ misión específica
                startActivity(intent)
                finish()
            }
        }
    }

    private fun loadMissions(age: Int) {
        db.collection("missions")
            .get()
            .addOnSuccessListener { docs ->
                Log.d("SelectMissions", "Documentos encontrados: ${docs.size()}")
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay misiones disponibles", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val allMissions = docs.map { doc ->
                    val minAge = (doc.getLong("minAge") ?: 0L).toInt()
                    val maxAge = (doc.getLong("maxAge") ?: 0L).toInt()
                    Mission(
                        id = doc.id,
                        title = doc.getString("title") ?: "Misión sin título",
                        description = doc.getString("description") ?: "",
                        difficulty = doc.getString("difficulty") ?: "fácil",
                        minAge = minAge,
                        maxAge = maxAge,
                        category = doc.getString("category") ?: "General"
                    )
                }

                val missionsForAge = allMissions.filter { age in it.minAge..it.maxAge }

                if (missionsForAge.isEmpty()) {
                    Toast.makeText(this, "No hay misiones para esta edad", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val missionsByCategory = missionsForAge.groupBy { it.category }

                missionsLayout.removeAllViews()

                for ((category, missions) in missionsByCategory) {
                    val categoryTitle = TextView(this).apply {
                        text = category
                        textSize = 20f
                        setPadding(0, 16, 0, 8)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    missionsLayout.addView(categoryTitle)

                    for (mission in missions) {
                        val checkBox = CheckBox(this).apply {
                            text = "${mission.title}\n${mission.description}"
                            textSize = 16f
                            setPadding(12, 12, 12, 12)
                        }

                        checkBox.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                when (mission.category.lowercase()) {
                                    "hogar" -> {
                                        if (selectedHogar.isEmpty()) selectedHogar.add(mission)
                                        else {
                                            checkBox.isChecked = false
                                            Toast.makeText(this, "Ya seleccionaste una misión de Hogar", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "autocuidado" -> {
                                        if (selectedAutocuidado.isEmpty()) selectedAutocuidado.add(mission)
                                        else {
                                            checkBox.isChecked = false
                                            Toast.makeText(this, "Ya seleccionaste una misión de Autocuidado", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "escolar" -> {
                                        if (selectedEscolar.isEmpty()) selectedEscolar.add(mission)
                                        else {
                                            checkBox.isChecked = false
                                            Toast.makeText(this, "Ya seleccionaste una misión Escolar", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                when (mission.category.lowercase()) {
                                    "hogar" -> selectedHogar.remove(mission)
                                    "autocuidado" -> selectedAutocuidado.remove(mission)
                                    "escolar" -> selectedEscolar.remove(mission)
                                }
                            }
                        }

                        missionsLayout.addView(checkBox)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SelectMissions", "Error al cargar misiones: ${e.message}")
                Toast.makeText(this, "Error al cargar misiones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}