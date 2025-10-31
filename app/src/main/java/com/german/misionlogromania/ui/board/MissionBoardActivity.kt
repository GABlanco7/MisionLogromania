package com.german.misionlogromania.ui.board

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R
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
    private lateinit var btnBack: ImageButton
    private lateinit var rvDays: RecyclerView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private var childId: String? = null
    private lateinit var adapter: DayAdapter

    // Calendario y confirmaciones independientes para cada misión
    private var currentMonth = 0
    private var currentYear = 0
    private val confirmedDays = mutableMapOf<Pair<Int, Int>, List<Int>>()
    private var currentMission: Mission? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boards_general)

        container = findViewById(R.id.mainBoardContainer)
        btnBack = findViewById(R.id.btnBack)
        rvDays = findViewById(R.id.rvDays)
        tvMonth = findViewById(R.id.tvMonthName)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        btnBack.setOnClickListener { finish() }

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
            // ✅ Modo misión única (desde KidHome)
            currentMission = Mission(missionTitle, missionId)
            container.addView(createMissionBoard(currentMission!!))
        } else {
            // 🔄 Modo general (si se abre sin parámetros)
            loadAllMissions()
        }

        val now = Calendar.getInstance()
        currentMonth = now.get(Calendar.MONTH)
        currentYear = now.get(Calendar.YEAR)

        btnPrevMonth.setOnClickListener { changeMonth(-1) }
        btnNextMonth.setOnClickListener { changeMonth(1) }
    }

    /** 🔹 Cargar todas las misiones asignadas al niño */
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

    /** 🔹 Crear tablero para una misión específica */
    private fun createMissionBoard(mission: Mission) =
        layoutInflater.inflate(R.layout.layout_single_board, container, false).apply {
            findViewById<TextView>(R.id.tvBoardTitle).text = mission.title

            // Calendario independiente por misión
            val now = Calendar.getInstance()
            currentMonth = now.get(Calendar.MONTH)
            currentYear = now.get(Calendar.YEAR)
            currentMission = mission

            loadConfirmedDays(mission) { showMonth(currentYear, currentMonth) }
        }

    /** 🔹 Cargar días confirmados de Firestore */
    private fun loadConfirmedDays(mission: Mission, onFinish: () -> Unit) {
        db.collection("missionConfirmations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("missionId", mission.id)
            .whereEqualTo("confirmedByParent", true)
            .get()
            .addOnSuccessListener { docs ->
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

    /** 🔹 Mostrar calendario del mes */
    private fun showMonth(year: Int, month: Int) {
        val monthName = DateFormatSymbols().months[month].replaceFirstChar { it.uppercase() }
        tvMonth.text = "$monthName $year"

        val days = generateDaysOfMonth(year, month).toMutableList()
        val confirmedList = confirmedDays[Pair(year, month)] ?: emptyList()
        for (d in days) if (d.dayNumber in confirmedList) d.isConfirmed = true

        rvDays.layoutManager = GridLayoutManager(this, 5)
        adapter = DayAdapter(days) { day ->
            if (day.isToday) sendMissionConfirmationToParent(day)
            else Toast.makeText(this, "Solo puedes confirmar el día actual", Toast.LENGTH_SHORT).show()
        }
        rvDays.adapter = adapter
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
                val isToday = (dayNum == today && month == currentMonth && year == currentYear)
                days.add(Day(dayNum, isToday, true))
            }
        }
        return days
    }

    /** ✅ Enviar confirmación */
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
                Toast.makeText(this, "Confirmación enviada al padre", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar confirmación", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🟡 Guardar notificación para el padre */
    private fun addNotificationForParent(mission: Mission, message: String) {
        db.collection("children").document(childId!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val newNotification = mapOf(
                        "missionId" to mission.id,
                        "title" to "Misión completada",
                        "message" to message,
                        "timestamp" to System.currentTimeMillis(),
                        "seen" to false
                    )

                    val currentNotifications =
                        (doc.get("notifications") as? MutableList<Map<String, Any>>
                            ?: mutableListOf()).toMutableList()

                    val existingIndex = currentNotifications.indexOfFirst { it["missionId"] == mission.id }
                    if (existingIndex >= 0) currentNotifications[existingIndex] = newNotification
                    else currentNotifications.add(newNotification)

                    doc.reference.update("notifications", currentNotifications)
                }
            }
    }
}