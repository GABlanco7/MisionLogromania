package com.german.misionlogromania.ui.upload

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.model.Mission
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UploadMissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_missions)

        val db = Firebase.firestore
        val btnUpload = findViewById<Button>(R.id.btnUploadMissions)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnUpload.setOnClickListener {
            tvStatus.text = "⏳ Subiendo misiones..."
            btnUpload.isEnabled = false

            val missions = listOf(
                // 🔹 Hogar (4 a 7 años)
                Mission("", "Lavar los platos", "Ayuda en la cocina lavando los platos.", "fácil", 4, 7, "hogar"),
                Mission("", "Guardar juguetes", "Recoge tus juguetes después de jugar.", "medio", 4, 7, "hogar"),
                Mission("", "Regar las plantas", "Riega las plantas con ayuda de un adulto.", "fácil", 4, 7, "hogar"),

                // 🔹 Hogar (8 a 11 años)
                Mission("", "Hacer la cama", "Organiza tu cama todos los días.", "fácil", 8, 11, "hogar"),
                Mission("", "Ayudar con la ropa", "Dobla la ropa limpia o ayúdala a guardar.", "medio", 8, 11, "hogar"),
                Mission("", "Sacar la basura", "Lleva la basura al lugar indicado.", "avanzado", 8, 11, "hogar"),
                Mission("", "Organizar la biblioteca", "Ordena los libros por tamaño o color.", "medio", 8, 11, "hogar"),

                // 🔹 Autocuidado (4 a 7 años)
                Mission("", "Cepillarse los dientes", "Cepíllate los dientes 2 veces al día.", "fácil", 4, 7, "autocuidado"),
                Mission("", "Bañarse solo", "Báñate con supervisión de un adulto.", "medio", 4, 7, "autocuidado"),
                Mission("", "Cuidar tus juguetes", "Guarda tus juguetes después de usarlos.", "fácil", 4, 7, "autocuidado"),

                // 🔹 Autocuidado (8 a 11 años)
                Mission("", "Lavarse las manos", "Lávate las manos antes de comer y después de jugar.", "fácil", 8, 11, "autocuidado"),
                Mission("", "Peinarse", "Peina tu cabello cada mañana.", "medio", 8, 11, "autocuidado"),
                Mission("", "Vestirse solo", "Elige tu ropa y vístete solo.", "avanzado", 8, 11, "autocuidado"),
                Mission("", "Lavarse la cara", "Lávate la cara cada mañana y noche.", "medio", 8, 11, "autocuidado"),

                // 🔹 Escolar (4 a 7 años)
                Mission("", "Leer un libro", "Lee un cuento o libro corto con ayuda.", "fácil", 4, 7, "escolar"),
                Mission("", "Practicar matemáticas", "Haz ejercicios sencillos de sumas y restas.", "medio", 4, 7, "escolar"),
                Mission("", "Dibujar algo creativo", "Haz un dibujo libre con colores.", "fácil", 4, 7, "escolar"),

                // 🔹 Escolar (8 a 11 años)
                Mission("", "Hacer la tarea", "Completa tus tareas escolares todos los días.", "medio", 8, 11, "escolar"),
                Mission("", "Escribir una historia", "Escribe una historia corta sobre tu día.", "avanzado", 8, 11, "escolar"),
                Mission("", "Aprender una canción", "Aprende y canta una canción nueva.", "fácil", 8, 11, "escolar")
            )

            // Subir a Firestore con ID incluido
            missions.forEachIndexed { index, mission ->
                val docRef = db.collection("missions").document()
                val missionWithId = mission.copy(id = docRef.id)

                docRef.set(missionWithId)
                    .addOnSuccessListener {
                        Log.d("Upload", "✅ Misión '${mission.title}' subida con ID: ${docRef.id}")
                        if (index == missions.lastIndex) {
                            tvStatus.text = "✅ Misiones subidas correctamente."
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Upload", "❌ Error al subir ${mission.title}", e)
                        tvStatus.text = "⚠️ Error al subir alguna misión. Ver consola."
                    }
            }
        }
    }
}