package com.german.misionlogromania.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.german.misionlogromania.R
import com.german.misionlogromania.databinding.ActivityMainBinding
import com.german.misionlogromania.ui.menu.KidHomeActivity
import com.german.misionlogromania.ui.menu.PadreMenuActivity
import com.german.misionlogromania.ui.roles.RoleSelectionActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannelIfNeeded()

        requestNotificationPermission()

        binding.btnStart.setOnClickListener {
            checkSavedSession()
        }
    }

    /** 🔹 Verifica si hay sesión guardada al presionar "Comenzar" */
    private fun checkSavedSession() {
        val authUser = Firebase.auth.currentUser

        val childPrefs = getSharedPreferences("child_prefs", MODE_PRIVATE)
        val parentPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // --- Niño ---
        val isChildLogged = childPrefs.getBoolean("isChildLogged", false)
        val rememberChild = childPrefs.getBoolean("rememberSession", false)
        val childId = childPrefs.getString("childId", null)
        val childName = childPrefs.getString("childName", "Niño")

        // --- Padre ---
        val isParentLogged = parentPrefs.getBoolean("stay_logged_in", false)

        when {
            // 🧒 Niño con sesión guardada Y marcada como "recordar sesión"
            isChildLogged && rememberChild && childId != null -> {
                val intent = Intent(this, KidHomeActivity::class.java)
                intent.putExtra("childId", childId)
                intent.putExtra("childName", childName)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            // 👨‍👧 Padre con sesión guardada Y FirebaseAuth activo
            isParentLogged && authUser != null -> {
                val intent = Intent(this, PadreMenuActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            // 🚪 Ninguna sesión guardada → ir a selección de rol
            else -> {
                val intent = Intent(this, RoleSelectionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    /** ✅ Crear canal de notificación */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "MISSION_CHANNEL_V2"
            val name = "Misiones Completadas"
            val descriptionText = "Notificaciones de logros o misiones completadas"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                lightColor = ContextCompat.getColor(this@MainActivity, R.color.purple_500)
            }

            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** ✅ Pedir permiso de notificación (Android 13+) */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
    }
}