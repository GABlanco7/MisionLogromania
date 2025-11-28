package com.german.misionlogromania.ui.parent

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.MainActivity
import com.german.misionlogromania.ui.board.MissionBoardActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.iterator

class ParentMenuActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private lateinit var contenedorMisiones: LinearLayout
    private lateinit var overlayNotificaciones: FrameLayout
    private lateinit var ivNotification: ImageView
    private lateinit var btnAceptar: Button
    private lateinit var tvFamilyCode: TextView
    private lateinit var overlayContainer: LinearLayout
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_padre_menu)
        title = "Panel del Padre"

        // UI references
        contenedorMisiones = findViewById(R.id.contenedorMisiones)
        overlayNotificaciones = findViewById(R.id.overlayNotificaciones)
        ivNotification = findViewById(R.id.ivNotification)
        btnAceptar = findViewById(R.id.btnAceptar)
        tvFamilyCode = findViewById(R.id.tvFamilyCode)
        overlayContainer = findViewById(R.id.overlayContentNotifications)
        btnLogout = findViewById(R.id.btnLogout)

        // Campanita visible siempre
        // Campanita visible siempre
        ivNotification.visibility = View.VISIBLE
        ivNotification.setColorFilter(Color.YELLOW) // 🔹 Agregado color amarillo brillante
        ivNotification.setOnClickListener { overlayNotificaciones.visibility = View.VISIBLE }

        btnAceptar.setOnClickListener {
            overlayNotificaciones.visibility = View.GONE
            markNotificationsAsRead()
        }

        overlayNotificaciones.setOnClickListener { overlayNotificaciones.visibility = View.GONE }

        btnLogout.setOnClickListener { logoutParent() }

        // Escuchar cambios del hijo
        listenChildUpdates()
    }

    private fun logoutParent() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                auth.signOut()
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                prefs.clear()
                prefs.apply()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun listenChildUpdates() {
        val parentId = auth.currentUser?.uid ?: return
        db.collection("children")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PadreMenu", "Error al escuchar actualizaciones: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null && snapshots.documents.isNotEmpty()) {
                    val childDoc = snapshots.documents.first()
                    handleChildDocument(childDoc)
                }
            }
    }

    private fun handleChildDocument(childDoc: DocumentSnapshot) {
        tvFamilyCode.text = "Código familiar: ${childDoc.getString("familyCode") ?: "---"}"

        // Misiones
        val missions = childDoc.get("assignedMissions") as? List<Map<String, Any>> ?: listOf()
        if (missions.isNotEmpty()) mostrarMisiones(missions, childDoc.id) // 🆕 Pasamos childId

        // Notificaciones
        val notifications = childDoc.get("notifications") as? List<Map<String, Any>> ?: listOf()
        overlayContainer.removeAllViews()

        val unseenNotifications = notifications.filter {
            !(it["seen"] as? Boolean ?: false) &&
                    ((it["type"] as? String) == "mission_completed" ||
                            (it["type"] as? String) == "reward_redeemed")
        }

        ivNotification.alpha = if (unseenNotifications.isNotEmpty()) 1f else 0.6f

        for (notif in unseenNotifications) displayNotificationCard(childDoc.id, notif)
    }

    private fun displayNotificationCard(childId: String, notif: Map<String, Any>) {
        val missionId = notif["missionId"] as? String ?: ""
        val title = notif["title"] as? String ?: ""
        val message = notif["message"] as? String ?: ""
        val type = notif["type"] as? String ?: ""

        val cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f
            setColor(Color.parseColor("#2E2E2E"))
            setStroke(2, Color.parseColor("#FFD700"))
        }

        val notifLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 30 }
        }

        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 20f
            setPadding(0, 0, 0, 10)
        }

        val tvMessage = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        notifLayout.addView(tvTitle)
        notifLayout.addView(tvMessage)

        if (type == "mission_completed") {
            val btnLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 2f }
            val btnAccept = Button(this).apply {
                text = "Aceptar"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
                setOnClickListener {
                    aceptarNotificacion(childId, missionId)
                    markNotificationAsRead(childId, missionId)
                    notifLayout.visibility = View.GONE
                }
            }
            val btnReject = Button(this).apply {
                text = "Rechazar"
                setBackgroundColor(Color.parseColor("#F44336"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8 }
                setOnClickListener {
                    rechazarNotificacion(childId, missionId)
                    markNotificationAsRead(childId, missionId)
                    notifLayout.visibility = View.GONE
                }
            }
            btnLayout.addView(btnAccept)
            btnLayout.addView(btnReject)
            notifLayout.addView(btnLayout)
        } else if (type == "reward_redeemed") {
            val btnAccept = Button(this).apply {
                text = "Aceptar"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    markNotificationAsRead(childId, missionId)
                    notifLayout.visibility = View.GONE
                }
            }
            notifLayout.addView(btnAccept)
        }

        overlayContainer.addView(notifLayout)
    }

    private fun markNotificationsAsRead() {
        val parentId = auth.currentUser?.uid ?: return
        db.collection("children")
            .whereEqualTo("parentId", parentId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val childDoc = docs.first()
                    val notifications = childDoc.get("notifications") as? List<Map<String, Any>> ?: listOf()
                    val updated = notifications.map { it + ("seen" to true) }
                    childDoc.reference.update("notifications", updated)
                }
            }
    }

    private fun aceptarNotificacion(childId: String, missionId: String) {
        val childRef = db.collection("children").document(childId)
        db.collection("missionConfirmations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("missionId", missionId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) doc.reference.update("confirmedByParent", true)
                childRef.update("stars", FieldValue.increment(1))
            }
    }

    private fun rechazarNotificacion(childId: String, missionId: String) {
        db.collection("missionConfirmations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("missionId", missionId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) doc.reference.update("rejected", true)
            }
    }

    private fun markNotificationAsRead(childId: String, missionId: String) {
        val childRef = db.collection("children").document(childId)
        childRef.get().addOnSuccessListener { doc ->
            val notifications = doc.get("notifications") as? MutableList<Map<String, Any>> ?: mutableListOf()
            val updated = notifications.map { notif -> if (notif["missionId"] == missionId) notif + ("seen" to true) else notif }
            childRef.update("notifications", updated)
        }
    }

    fun addRewardNotificationForParent(rewardTitle: String, message: String) {
        val childIdSafe = auth.currentUser?.uid ?: return
        db.collection("children").document(childIdSafe).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val parentId = doc.getString("parentId") ?: return@addOnSuccessListener

                val newNotification = mapOf(
                    "childId" to childIdSafe,
                    "parentId" to parentId,
                    "title" to "Recompensa canjeada",
                    "message" to message,
                    "rewardTitle" to rewardTitle,
                    "timestamp" to System.currentTimeMillis(),
                    "seen" to false,
                    "type" to "reward_redeemed",
                    "missionId" to "reward_${System.currentTimeMillis()}"
                )

                val currentNotifications = (doc.get("notifications") as? MutableList<Map<String, Any>> ?: mutableListOf()).toMutableList()
                currentNotifications.add(newNotification)
                doc.reference.update("notifications", currentNotifications)
            }
    }

    // 🆕 FUNCIÓN MODIFICADA: Ahora recibe childId y abre el calendario en modo solo lectura
    private fun mostrarMisiones(missions: List<Map<String, Any>>, childId: String) {
        contenedorMisiones.removeAllViews()
        val missionsByCategory = missions.groupBy { it["category"] as? String ?: "General" }

        for ((category, missionsInCategory) in missionsByCategory) {
            // --- Título de categoría ---
            val tvCategory = TextView(this).apply {
                text = category.uppercase()
                textSize = 20f
                setTextColor(Color.parseColor("#A3CEF1")) // azul celeste
                setShadowLayer(3f, 1f, 1f, Color.parseColor("#55000000")) // sombra ligera
                setPadding(0, 16, 0, 8)
                typeface = resources.getFont(R.font.poppins)
            }
            contenedorMisiones.addView(tvCategory)

            for (mission in missionsInCategory) {
                val title = mission["title"]?.toString() ?: "Sin título"
                val description = mission["description"]?.toString() ?: ""
                val missionId = mission["id"]?.toString() ?: ""

                // --- Tarjeta de misión ---
                val cardBackground = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    // Gradiente azul suave
                    colors = intArrayOf(Color.parseColor("#4A90E2"), Color.parseColor("#1E3A8A"))
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                }

                val boton = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = cardBackground
                    setPadding(24, 24, 24, 24)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 16 }

                    // --- Título de misión ---
                    val tvTitle = TextView(context).apply {
                        text = title
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        setTypeface(typeface, Typeface.BOLD)
                    }

                    // --- Descripción ---
                    val tvDescription = TextView(context).apply {
                        text = description
                        textSize = 14f
                        setTextColor(Color.parseColor("#DCE6F1")) // azul muy claro para jerarquía
                        setPadding(0, 8, 0, 0)
                    }

                    addView(tvTitle)
                    addView(tvDescription)

                    setOnClickListener {
                        // Abrir el calendario en modo solo lectura
                        openMissionBoardReadOnly(childId, missionId, title)
                    }
                }

                contenedorMisiones.addView(boton)
            }
        }
    }

    // 🆕 NUEVA FUNCIÓN: Abre MissionBoardActivity en modo solo lectura
    private fun openMissionBoardReadOnly(childId: String, missionId: String, missionTitle: String) {
        val intent = Intent(this, MissionBoardActivity::class.java).apply {
            putExtra("childId", childId)
            putExtra("missionId", missionId)
            putExtra("missionTitle", missionTitle)
            putExtra("readOnlyMode", true) // 🔒 Activar modo solo lectura
        }
        startActivity(intent)
    }
}