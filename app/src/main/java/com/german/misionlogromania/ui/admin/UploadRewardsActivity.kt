package com.german.misionlogromania.ui.upload

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UploadRewardsActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_rewards)

        val btnUpload = findViewById<Button>(R.id.btnUploadRewards)
        btnUpload.setOnClickListener {
            uploadRewards()
        }
    }

    private fun uploadRewards() {
        // 🔹 Recompensas predefinidas: 3 por cada nivel
        val rewards = listOf(
            // Nivel fácil (5 puntos)
            mapOf("title" to "Pegatina sonriente", "description" to "Una pegatina divertida", "level" to "facil"),
            mapOf("title" to "Icono colorido", "description" to "Un icono colorido para tu perfil", "level" to "facil"),
            mapOf("title" to "Mini sticker", "description" to "Un pequeño sticker para coleccionar", "level" to "facil"),

            // Nivel medio (10 puntos)
            mapOf("title" to "Juego extra", "description" to "Acceso a un mini juego", "level" to "medio"),
            mapOf("title" to "Pegatina especial", "description" to "Una pegatina especial para coleccionistas", "level" to "medio"),
            mapOf("title" to "Avatar adicional", "description" to "Desbloquea un avatar adicional", "level" to "medio"),

            // Nivel avanzado (15 puntos)
            mapOf("title" to "Premio exclusivo", "description" to "Un premio único y exclusivo", "level" to "avanzado"),
            mapOf("title" to "Acceso VIP", "description" to "Acceso a contenido VIP", "level" to "avanzado"),
            mapOf("title" to "Super avatar", "description" to "Desbloquea un super avatar especial", "level" to "avanzado")
        )

        for (reward in rewards) {
            val points = when (reward["level"]) {
                "facil" -> 5
                "medio" -> 10
                "avanzado" -> 15
                else -> 0
            }

            val rewardData = hashMapOf(
                "title" to reward["title"],
                "description" to reward["description"],
                "level" to reward["level"],
                "points" to points
            )

            // Subir a Firestore (colección "rewards")
            db.collection("rewards")
                .add(rewardData)
                .addOnSuccessListener { docRef ->
                    Log.d("UploadRewards", "Recompensa subida: ${docRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("UploadRewards", "Error al subir recompensa: ${e.message}")
                }
        }

        Toast.makeText(this, "Recompensas subidas a Firestore", Toast.LENGTH_SHORT).show()
    }
}