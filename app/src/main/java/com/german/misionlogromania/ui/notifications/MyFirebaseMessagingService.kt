package com.german.misionlogromania.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.parent.ParentMenuActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.ktx.Firebase

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val db = Firebase.firestore

    /**
     * Se ejecuta cuando Firebase genera un nuevo token para este dispositivo
     * (por ejemplo, al reinstalar la app o limpiar datos)
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Guardar token en Firestore (colección "parents")
            db.collection("parents").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    println("✅ Token FCM actualizado correctamente")
                }
                .addOnFailureListener {
                    println("⚠️ Error al guardar token FCM: ${it.message}")
                }
        }
    }

    /**
     * Se ejecuta cuando llega una notificación desde Firebase Cloud Messaging (FCM)
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Si viene con notificación visual (title/body)
        message.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    /**
     * Muestra una notificación local en el dispositivo
     */
    private fun sendNotification(title: String?, body: String?) {
        val intent = Intent(this, ParentMenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "missions_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: "Nueva notificación")
            .setContentText(body ?: "Tienes una nueva misión para revisar")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es Android 8 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Misiones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de confirmación de misiones"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}