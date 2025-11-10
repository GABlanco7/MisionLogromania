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
    private lateinit var filterSpinner: Spinner

    private var childId = ""
    private var childAge = 0
    private var allMissionsForAge = listOf<Mission>()
    private var currentFilter = "fácil"

    private val selectedHogar = mutableListOf<Mission>()
    private val selectedAutocuidado = mutableListOf<Mission>()
    private val selectedEscolar = mutableListOf<Mission>()

    private val filterOptions = listOf("Fácil", "Medio", "Avanzado")

    // Control de inicialización
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_missions)

        missionsLayout = findViewById(R.id.missionsLayout)
        filterSpinner = findViewById(R.id.filterSpinner)
        val btnAcceptMissions = findViewById<Button>(R.id.btnAcceptMissions)

        // Configurar el filtro
        setupSpinner()

        // Inicializar con los datos del intent actual
        initializeFromIntent(intent)

        // Confirmar selección
        btnAcceptMissions.setOnClickListener {
            if (selectedHogar.size != 1 || selectedAutocuidado.size != 1 || selectedEscolar.size != 1) {
                Toast.makeText(this, "Debes seleccionar una misión de cada categoría", Toast.LENGTH_SHORT).show()
            } else {
                val finalSelected = mutableListOf<Mission>().apply {
                    addAll(selectedHogar)
                    addAll(selectedAutocuidado)
                    addAll(selectedEscolar)
                }

                val missionToConfirm = selectedHogar.firstOrNull()
                    ?: selectedAutocuidado.firstOrNull()
                    ?: selectedEscolar.firstOrNull()

                if (missionToConfirm == null) {
                    Toast.makeText(this, "No se pudo determinar la misión a confirmar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(this, ConfirmMissionsActivity::class.java)
                intent.putExtra("childId", childId)
                intent.putExtra("childAge", childAge) // ✅ Enviamos la edad
                intent.putParcelableArrayListExtra("selectedMissions", ArrayList(finalSelected))
                intent.putExtra("missionId", missionToConfirm.id)
                startActivity(intent)
                finish()
            }
        }
    }

    /** 📩 Cuando la actividad ya existía y recibe un nuevo Intent (por ejemplo, al presionar Cancelar en ConfirmMissionsActivity) */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // actualizar el intent interno
        initializeFromIntent(intent)
    }

    /** 🔹 Procesa el intent recibido y carga las misiones correspondientes */
    private fun initializeFromIntent(intent: Intent) {
        val incomingChildId = intent.getStringExtra("childId") ?: ""
        val incomingChildAge = intent.getIntExtra("childAge", 0)

        Log.d("SelectMissions", "initializeFromIntent: childId=$incomingChildId, childAge=$incomingChildAge")

        // Si los datos son iguales y ya está inicializado, no recargar
        if (initialized && incomingChildId == childId && incomingChildAge == childAge) return

        childId = incomingChildId
        childAge = incomingChildAge

        if (childId.isEmpty()) {
            Toast.makeText(this, "No se encontró ID del niño.", Toast.LENGTH_LONG).show()
            Log.e("SelectMissions", "childId vacío.")
            return
        }

        if (childAge <= 0) {
            Toast.makeText(this, "Edad del niño inválida. No se pueden cargar misiones.", Toast.LENGTH_LONG).show()
            Log.e("SelectMissions", "childAge inválida: $childAge")
            initialized = false
            return
        }

        // Limpiar selecciones previas
        selectedHogar.clear()
        selectedAutocuidado.clear()
        selectedEscolar.clear()

        // Cargar misiones para esa edad
        loadMissions(childAge)

        initialized = true
    }

    /** 🎚️ Configura el Spinner de filtros */
    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                currentFilter = when (position) {
                    0 -> "fácil"
                    1 -> "medio"
                    2 -> "avanzado"
                    else -> "fácil"
                }
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /** 🔍 Aplica el filtro de dificultad actual */
    private fun applyFilter() {
        val filteredMissions = allMissionsForAge.filter { it.difficulty.lowercase() == currentFilter.lowercase() }
        displayMissions(filteredMissions)
    }

    /** 🔹 Carga misiones desde Firestore según la edad */
    private fun loadMissions(age: Int) {
        db.collection("missions")
            .get()
            .addOnSuccessListener { docs ->
                Log.d("SelectMissions", "Documentos encontrados: ${docs.size()}")
                if (docs.isEmpty) {
                    Toast.makeText(this, "No hay misiones disponibles", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                allMissionsForAge = docs.map { doc ->
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
                }.filter { age in it.minAge..it.maxAge }

                if (allMissionsForAge.isEmpty()) {
                    Toast.makeText(this, "No hay misiones para esta edad", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                applyFilter()
            }
            .addOnFailureListener { e ->
                Log.e("SelectMissions", "Error al cargar misiones: ${e.message}")
                Toast.makeText(this, "Error al cargar misiones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🧩 Muestra las misiones agrupadas por categoría */
    private fun displayMissions(missions: List<Mission>) {
        missionsLayout.removeAllViews()

        if (missions.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No hay misiones de dificultad $currentFilter para esta edad"
                textSize = 16f
                setPadding(0, 32, 0, 32)
                gravity = android.view.Gravity.CENTER
            }
            missionsLayout.addView(emptyText)
            return
        }

        val missionsByCategory = missions.groupBy { it.category }

        for ((category, missionsInCategory) in missionsByCategory) {
            val categoryTitle = TextView(this).apply {
                text = when (category.lowercase()) {
                    "hogar" -> "🏠 Misiones del Hogar"
                    "autocuidado" -> "🧴 Misiones de Autocuidado"
                    "escolar" -> "📚 Misiones Escolares"
                    else -> category
                }
                textSize = 18f
                setPadding(0, 24, 0, 16)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            missionsLayout.addView(categoryTitle)

            for (mission in missionsInCategory) {
                val missionCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(R.drawable.mission_card_background)
                    setPadding(16, 16, 16, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 12)
                    }
                }

                val checkBox = CheckBox(this).apply {
                    text = mission.title
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 8)
                }

                val descriptionText = TextView(this).apply {
                    text = mission.description
                    textSize = 14f
                    setTextColor(resources.getColor(android.R.color.darker_gray))
                    setPadding(0, 0, 0, 8)
                }

                val difficultyText = TextView(this).apply {
                    text = "Dificultad: ${mission.difficulty.replaceFirstChar { it.uppercase() }}"
                    textSize = 12f
                    setTextColor(
                        when (mission.difficulty.lowercase()) {
                            "fácil" -> resources.getColor(android.R.color.holo_green_dark)
                            "medio" -> resources.getColor(android.R.color.holo_orange_dark)
                            "avanzado" -> resources.getColor(android.R.color.holo_red_dark)
                            else -> resources.getColor(android.R.color.darker_gray)
                        }
                    )
                }

                missionCard.addView(checkBox)
                missionCard.addView(descriptionText)
                missionCard.addView(difficultyText)

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

                missionsLayout.addView(missionCard)
            }
        }
    }
}