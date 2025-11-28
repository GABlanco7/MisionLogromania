package com.german.misionlogromania.ui.board

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.calendar.DayAdapter
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.DateFormatSymbols
import java.util.Calendar

data class Day(
    val dayNumber: Int,
    val isToday: Boolean,
    val isWeekday: Boolean,
    var isConfirmed: Boolean = false
)

data class Mission(val title: String, val id: String)

class MissionBoardActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var container: LinearLayout
    private lateinit var rvDays: RecyclerView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnBack: ImageButton

    private var childId: String? = null
    private lateinit var adapter: DayAdapter

    private var currentMonth = 0
    private var currentYear = 0
    private val confirmedDays = mutableMapOf<Pair<Int, Int>, List<Int>>()
    private var currentMission: Mission? = null

    // 🆕 Variable para detectar si es modo solo lectura (padre)
    private var isReadOnlyMode = false

    // 🔔 Overlay de notificaciones
    private lateinit var overlayNotificaciones: FrameLayout
    private lateinit var overlayContentNotifications: LinearLayout
    private lateinit var btnAceptar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boards_general)

        // 🔹 Referencias UI
        container = findViewById(R.id.mainBoardContainer)
        rvDays = findViewById(R.id.rvDays)
        tvMonth = findViewById(R.id.tvMonthName)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        btnBack = findViewById(R.id.btnBack)

        overlayNotificaciones = findViewById(R.id.overlayNotificaciones)
        overlayContentNotifications = findViewById(R.id.overlayContentNotifications)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnAceptar.visibility = View.GONE

        // 🔹 Botones
        btnBack.setOnClickListener { finish() }
        btnAceptar.setOnClickListener { hideNotifications() }

        // 🆕 DETECTAR SI ES MODO SOLO LECTURA (viene del padre)
        isReadOnlyMode = intent.getBooleanExtra("readOnlyMode", false)

        // 🔹 Obtener ID del niño
        childId = intent.getStringExtra("childId")
            ?: getSharedPreferences("child_prefs", MODE_PRIVATE).getString("childId", null)

        if (childId == null) {
            Toast.makeText(this, "No se encontró el ID del niño", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val missionTitle = intent.getStringExtra("missionTitle")
        val missionId = intent.getStringExtra("missionId")

        if (!missionTitle.isNullOrBlank() && !missionId.isNullOrBlank()) {
            currentMission = Mission(missionTitle, missionId)
            container.addView(createMissionBoard(currentMission!!))
        } else {
            loadAllMissions()
        }

        val now = Calendar.getInstance()
        currentMonth = now.get(Calendar.MONTH)
        currentYear = now.get(Calendar.YEAR)

        btnPrevMonth.setOnClickListener { changeMonth(-1) }
        btnNextMonth.setOnClickListener { changeMonth(1) }
    }

    private fun loadAllMissions() {
        db.collection("children").document(childId!!).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val raw = doc.get("assignedMissions") as? List<*> ?: emptyList<Any>()
                for (item in raw) {
                    val map = item as? Map<*, *> ?: continue
                    val title = map["title"] as? String ?: continue
                    val missionId = map["id"] as? String ?: continue
                    val mission = Mission(title, missionId)
                    container.addView(createMissionBoard(mission))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar misiones", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createMissionBoard(mission: Mission) =
        layoutInflater.inflate(R.layout.layout_single_board, container, false).apply {
            val titleView = findViewById<TextView>(R.id.tvBoardTitle)
            // ✅ Título normal sin indicador de solo lectura
            titleView.text = mission.title

            currentMission = mission
            loadConfirmedDays(mission) { showMonth(currentYear, currentMonth) }
        }

    private fun loadConfirmedDays(mission: Mission, onFinish: () -> Unit) {
        db.collection("missionConfirmations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("missionId", mission.id)
            .whereEqualTo("confirmedByParent", true)
            .get()
            .addOnSuccessListener { docs: QuerySnapshot ->
                confirmedDays.clear()
                for (doc in docs) {
                    val timestamp = doc.get("timestamp") as? com.google.firebase.Timestamp ?: continue
                    val cal = Calendar.getInstance().apply { time = timestamp.toDate() }
                    val key = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val list = confirmedDays[key] ?: emptyList()
                    confirmedDays[key] = list + day
                }
                onFinish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar confirmaciones", Toast.LENGTH_SHORT).show()
                onFinish()
            }
    }

    private fun showMonth(year: Int, month: Int) {
        val monthName = DateFormatSymbols().months[month].replaceFirstChar { it.uppercase() }
        tvMonth.text = "$monthName $year"

        val days = generateDaysOfMonth(year, month).toMutableList()
        val confirmedList = confirmedDays[Pair(year, month)] ?: emptyList()
        for (d in days) if (d.dayNumber in confirmedList) d.isConfirmed = true

        rvDays.layoutManager = GridLayoutManager(this, 5)

        // 🆕 PASAR EL MODO DE SOLO LECTURA AL ADAPTER
        adapter = DayAdapter(days, isReadOnlyMode) { day ->
            // Solo permite acciones si NO es modo solo lectura
            if (!isReadOnlyMode) {
                if (day.isToday) showConfirmationDialog(day)
                else Toast.makeText(this, "Solo puedes confirmar el día actual", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    this,
                    "👁️ Vista de solo lectura. No puedes editar.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        rvDays.adapter = adapter
    }

    private fun showConfirmationDialog(day: Day) {
        val mission = currentMission ?: return
        val fecha = "${day.dayNumber} de ${DateFormatSymbols().months[currentMonth]} de $currentYear"

        AlertDialog.Builder(this)
            .setTitle("Confirmar tarea")
            .setMessage("¿Estás seguro que realizaste la misión \"${mission.title}\" el $fecha?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                sendMissionConfirmationToParent(day)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.add(Calendar.MONTH, offset)
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
        showMonth(currentYear, currentMonth)
    }

    private fun generateDaysOfMonth(year: Int, month: Int): List<Day> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val todayCal = Calendar.getInstance()
        val today = todayCal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = todayCal.get(Calendar.MONTH)
        val currentYear = todayCal.get(Calendar.YEAR)

        var startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (startDayOfWeek == Calendar.SUNDAY) startDayOfWeek = 8

        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val days = mutableListOf<Day>()
        val emptyDaysBefore = startDayOfWeek - Calendar.MONDAY

        for (i in 1..emptyDaysBefore) days.add(Day(0, false, false))
        for (dayNum in 1..maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, dayNum)
            val dow = calendar.get(Calendar.DAY_OF_WEEK)
            if (dow in Calendar.MONDAY..Calendar.FRIDAY) {
                // 🆕 Si es modo solo lectura, NO marcar como "isToday"
                val isToday = if (isReadOnlyMode) {
                    false // En modo padre, no destacar el día actual
                } else {
                    (dayNum == today && month == currentMonth && year == currentYear)
                }
                days.add(Day(dayNum, isToday, true))
            }
        }
        return days
    }

    /** ✅ Enviar confirmación al padre y guardar notificación */
    private fun sendMissionConfirmationToParent(day: Day) {
        val mission = currentMission ?: return
        val fecha = "${day.dayNumber} de ${DateFormatSymbols().months[currentMonth]} de $currentYear"

        val confirmation = hashMapOf(
            "childId" to childId,
            "missionId" to mission.id,
            "day" to day.dayNumber,
            "confirmedByParent" to false,
            "timestamp" to Calendar.getInstance().time,
            "missionName" to mission.title
        )

        db.collection("missionConfirmations")
            .add(confirmation)
            .addOnSuccessListener {
                val mensaje = "Su hijo completó la misión \"${mission.title}\" el $fecha"
                addNotificationForParent(mission, mensaje)

                Toast.makeText(this, "Confirmación enviada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar confirmación", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔔 Mostrar overlay de notificación */
    private fun showNotifications(message: String) {
        overlayNotificaciones.visibility = View.VISIBLE
        overlayContentNotifications.removeAllViews()

        val tv = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(resources.getColor(R.color.black))
            setPadding(8, 8, 8, 8)
        }
        overlayContentNotifications.addView(tv)
        btnAceptar.visibility = View.VISIBLE
    }

    private fun hideNotifications() {
        overlayNotificaciones.visibility = View.GONE
        btnAceptar.visibility = View.GONE
    }

    /** 🔔 Guardar notificación de misión completada */
    private fun addNotificationForParent(mission: Mission, message: String) {
        val childIdSafe = childId ?: return

        db.collection("children").document(childIdSafe).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val newNotification = mapOf(
                        "missionId" to mission.id,
                        "title" to "Misión completada",
                        "message" to message,
                        "timestamp" to System.currentTimeMillis(),
                        "seen" to false,
                        "type" to "mission_completed"
                    )

                    val currentNotifications = (doc.get("notifications") as? List<*>)?.mapNotNull {
                        it as? Map<String, Any>
                    }?.toMutableList() ?: mutableListOf()

                    val existingIndex = currentNotifications.indexOfFirst { it["missionId"] == mission.id }
                    if (existingIndex >= 0) currentNotifications[existingIndex] = newNotification
                    else currentNotifications.add(newNotification)

                    doc.reference.update("notifications", currentNotifications)
                }
            }
    }
}