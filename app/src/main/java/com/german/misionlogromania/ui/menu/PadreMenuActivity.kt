package com.german.misionlogromania.ui.menu

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.board.MissionBoardActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PadreMenuActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private lateinit var contenedorMisiones: LinearLayout
    private lateinit var overlayNotificaciones: FrameLayout
    private lateinit var ivNotification: ImageView
    private lateinit var btnAceptar: Button
    private lateinit var tvFamilyCode: TextView
    private lateinit var overlayContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_padre_menu)
        title = "Panel del Padre"

        contenedorMisiones = findViewById(R.id.contenedorMisiones)
        overlayNotificaciones = findViewById(R.id.overlayNotificaciones)
        ivNotification = findViewById(R.id.ivNotification)
        btnAceptar = findViewById(R.id.btnAceptar)
        tvFamilyCode = findViewById(R.id.tvFamilyCode)
        overlayContainer = findViewById(R.id.overlayContent)

        // 🔹 La campanita siempre visible
        ivNotification.visibility = View.VISIBLE
        ivNotification.setOnClickListener { overlayNotificaciones.visibility = View.VISIBLE }

        btnAceptar.setOnClickListener {
            overlayNotificaciones.visibility = View.GONE
            markNotificationsAsRead()
        }

        overlayNotificaciones.setOnClickListener { overlayNotificaciones.visibility = View.GONE }

        listenChildUpdates()
    }

    /** 🔹 Escuchar cambios en el documento del niño (misiones y notificaciones) */
    private fun listenChildUpdates() {
        val parentId = auth.currentUser?.uid ?: return

        db.collection("children")
            .whereEqualTo("parentId", parentId)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PadreMenu", "Error al escuchar actualizaciones: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val childDoc = snapshots.first()
                    val childName = childDoc.getString("name") ?: "Sin nombre"
                    val familyCode = childDoc.getString("familyCode") ?: "---"
                    tvFamilyCode.text = "Código familiar: $familyCode"

                    // --- Mostrar misiones ---
                    @Suppress("UNCHECKED_CAST")
                    val missions = childDoc.get("assignedMissions") as? List<Map<String, Any>> ?: listOf()
                    if (missions.isNotEmpty()) mostrarMisiones(missions)

                    // --- Mostrar notificaciones filtradas ---
                    @Suppress("UNCHECKED_CAST")
                    val notifications = childDoc.get("notifications") as? List<Map<String, Any>> ?: listOf()
                    overlayContainer.removeAllViews()

                    val unseenNotifications = notifications.filter {
                        !(it["seen"] as? Boolean ?: false) &&
                                ((it["type"] as? String) == "mission_completed" ||
                                        (it["type"] as? String) == "reward_redeemed")
                    }

                    // 🔹 La campanita siempre visible, pero se puede destacar si hay notificaciones
                    ivNotification.alpha = if (unseenNotifications.isNotEmpty()) 1f else 0.6f

                    for (notif in unseenNotifications) {
                        val missionId = notif["missionId"] as? String ?: ""
                        val title = notif["title"] as? String ?: ""
                        val message = notif["message"] as? String ?: ""

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

                        val btnLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            weightSum = 2f
                        }

                        val btnAccept = Button(this).apply {
                            text = "Aceptar"
                            setBackgroundColor(Color.parseColor("#4CAF50"))
                            setTextColor(Color.WHITE)
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
                            setOnClickListener {
                                aceptarNotificacion(childDoc.id, missionId)
                                markNotificationAsRead(childDoc.id, missionId)
                                notifLayout.visibility = View.GONE
                            }
                        }

                        val btnReject = Button(this).apply {
                            text = "Rechazar"
                            setBackgroundColor(Color.parseColor("#F44336"))
                            setTextColor(Color.WHITE)
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8 }
                            setOnClickListener {
                                rechazarNotificacion(childDoc.id, missionId)
                                markNotificationAsRead(childDoc.id, missionId)
                                notifLayout.visibility = View.GONE
                            }
                        }

                        btnLayout.addView(btnAccept)
                        btnLayout.addView(btnReject)
                        notifLayout.addView(tvTitle)
                        notifLayout.addView(tvMessage)
                        notifLayout.addView(btnLayout)

                        overlayContainer.addView(notifLayout)
                    }
                } else {
                    contenedorMisiones.removeAllViews()
                    overlayContainer.removeAllViews()
                    Toast.makeText(this, "No se encontró perfil del niño", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /** ✅ Marcar todas las notificaciones como leídas */
    private fun markNotificationsAsRead() {
        val parentId = auth.currentUser?.uid ?: return
        db.collection("children")
            .whereEqualTo("parentId", parentId)
            .limit(1)
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

    /** 🟢 Aceptar notificación → confirmar misión y sumar estrella */
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

    /** 🔴 Rechazar notificación → marcar misión como rechazada */
    private fun rechazarNotificacion(childId: String, missionId: String) {
        db.collection("missionConfirmations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("missionId", missionId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) doc.reference.update("rejected", true)
            }
    }

    /** 🟡 Marcar notificación interna como leída */
    private fun markNotificationAsRead(childId: String, missionId: String) {
        val childRef = db.collection("children").document(childId)
        childRef.get().addOnSuccessListener { doc ->
            val notifications = doc.get("notifications") as? MutableList<Map<String, Any>> ?: mutableListOf()
            val updated = notifications.map { notif ->
                if (notif["missionId"] == missionId) notif + ("seen" to true) else notif
            }
            childRef.update("notifications", updated)
        }
    }

    /** 🎯 Mostrar misiones agrupadas por categoría */
    private fun mostrarMisiones(missions: List<Map<String, Any>>) {
        contenedorMisiones.removeAllViews()
        val missionsByCategory = missions.groupBy { it["category"] as? String ?: "General" }

        for ((category, missionsInCategory) in missionsByCategory) {
            val tvCategory = TextView(this).apply {
                text = category.uppercase()
                textSize = 20f
                setTextColor(Color.parseColor("#FFD700"))
                setPadding(0, 16, 0, 8)
            }
            contenedorMisiones.addView(tvCategory)

            for (mission in missionsInCategory) {
                val title = mission["title"]?.toString() ?: "Sin título"
                val description = mission["description"]?.toString() ?: ""
                val missionId = mission["id"]?.toString() ?: ""

                val boton = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }

                    text = "🎯 $title\n$description"
                    setBackgroundColor(Color.parseColor("#1F1F1F"))
                    setTextColor(Color.parseColor("#FFD700"))
                    textSize = 16f
                    setPadding(16, 16, 16, 16)

                    setOnClickListener {
                        val intent = Intent(this@PadreMenuActivity, MissionBoardActivity::class.java)
                        intent.putExtra("missionId", missionId)
                        intent.putExtra("missionTitle", title)
                        startActivity(intent)
                    }
                }

                contenedorMisiones.addView(boton)
            }
        }
    }
}
