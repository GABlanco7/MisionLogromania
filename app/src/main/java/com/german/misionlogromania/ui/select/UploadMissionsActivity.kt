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
                // 🔹 Hogar 4-7 años
                Mission("", "Lavar los platos", "Ayuda en la cocina lavando los platos.", "fácil", 4, 7, "hogar"),
                Mission("", "Recoger la mesa", "Recoge la mesa después de comer.", "fácil", 4, 7, "hogar"),
                Mission("", "Guardar juguetes", "Recoge tus juguetes después de jugar.", "fácil", 4, 7, "hogar"),
                Mission("", "Barrer la habitación", "Barre tu habitación con ayuda de un adulto.", "medio", 4, 7, "hogar"),
                Mission("", "Ayudar con la ropa", "Dobla la ropa limpia o ayúdala a guardar.", "medio", 4, 7, "hogar"),
                Mission("", "Regar plantas pequeñas", "Riega las plantas con supervisión.", "medio", 4, 7, "hogar"),
                Mission("", "Sacar la basura", "Lleva la basura al lugar indicado.", "avanzado", 4, 7, "hogar"),
                Mission("", "Organizar la cocina", "Ayuda a ordenar la cocina después de cocinar.", "avanzado", 4, 7, "hogar"),
                Mission("", "Ayudar en la limpieza general", "Participa en limpieza general de la casa.", "avanzado", 4, 7, "hogar"),

                // 🔹 Hogar 8-11 años
                Mission("", "Hacer la cama", "Organiza tu cama todos los días.", "fácil", 8, 11, "hogar"),
                Mission("", "Guardar útiles", "Ordena tus útiles escolares.", "fácil", 8, 11, "hogar"),
                Mission("", "Recoger ropa sucia", "Lleva tu ropa sucia al cesto.", "fácil", 8, 11, "hogar"),
                Mission("", "Ayudar con la ropa", "Dobla la ropa limpia o ayúdala a guardar.", "medio", 8, 11, "hogar"),
                Mission("", "Organizar la biblioteca", "Ordena los libros por tamaño o color.", "medio", 8, 11, "hogar"),
                Mission("", "Preparar la mesa", "Coloca la mesa antes de comer.", "medio", 8, 11, "hogar"),
                Mission("", "Sacar la basura", "Lleva la basura al lugar indicado.", "avanzado", 8, 11, "hogar"),
                Mission("", "Lavar platos complicados", "Lava platos grandes o utensilios.", "avanzado", 8, 11, "hogar"),
                Mission("", "Ayudar en limpieza general", "Participa en limpieza general de la casa.", "avanzado", 8, 11, "hogar"),

                // 🔹 Autocuidado 4-7 años
                Mission("", "Cepillarse los dientes", "Cepíllate los dientes 2 veces al día.", "fácil", 4, 7, "autocuidado"),
                Mission("", "Peinarse", "Peina tu cabello cada mañana.", "fácil", 4, 7, "autocuidado"),
                Mission("", "Cuidar tus juguetes", "Guarda tus juguetes después de usarlos.", "fácil", 4, 7, "autocuidado"),
                Mission("", "Bañarse solo", "Báñate con supervisión de un adulto.", "medio", 4, 7, "autocuidado"),
                Mission("", "Ponerse la ropa", "Vístete solo con ayuda de un adulto.", "medio", 4, 7, "autocuidado"),
                Mission("", "Lavarse las manos correctamente", "Lávate las manos antes de comer.", "medio", 4, 7, "autocuidado"),
                Mission("", "Organizar tu baño", "Guarda tus cosas de baño en su lugar.", "avanzado", 4, 7, "autocuidado"),
                Mission("", "Cuidar tu higiene diaria", "Asegúrate de bañarte y cepillarte dientes diario.", "avanzado", 4, 7, "autocuidado"),
                Mission("", "Preparar tu ropa para el día", "Elige y organiza tu ropa para el día.", "avanzado", 4, 7, "autocuidado"),

                // 🔹 Autocuidado 8-11 años
                Mission("", "Lavarse las manos", "Lávate las manos antes de comer y después de jugar.", "fácil", 8, 11, "autocuidado"),
                Mission("", "Peinarse", "Peina tu cabello cada mañana.", "fácil", 8, 11, "autocuidado"),
                Mission("", "Cuidar tu habitación", "Mantén tu habitación ordenada.", "fácil", 8, 11, "autocuidado"),
                Mission("", "Vestirse solo", "Elige tu ropa y vístete solo.", "medio", 8, 11, "autocuidado"),
                Mission("", "Lavarse la cara", "Lávate la cara cada mañana y noche.", "medio", 8, 11, "autocuidado"),
                Mission("", "Cuidar tu higiene diaria", "Mantén hábitos de higiene correctos.", "medio", 8, 11, "autocuidado"),
                Mission("", "Preparar tu higiene completa", "Bañarte, peinarte y vestirte solo.", "avanzado", 8, 11, "autocuidado"),
                Mission("", "Organizar tu rutina de cuidado", "Planifica tu higiene diaria correctamente.", "avanzado", 8, 11, "autocuidado"),
                Mission("", "Mantener orden y limpieza personal", "Cuida tu aseo y limpieza diariamente.", "avanzado", 8, 11, "autocuidado"),

                // 🔹 Escolar 4-7 años
                Mission("", "Leer un libro", "Lee un cuento o libro corto con ayuda.", "fácil", 4, 7, "escolar"),
                Mission("", "Practicar matemáticas", "Haz ejercicios sencillos de sumas y restas.", "fácil", 4, 7, "escolar"),
                Mission("", "Dibujar algo creativo", "Haz un dibujo libre con colores.", "fácil", 4, 7, "escolar"),
                Mission("", "Aprender palabras nuevas", "Aprende nuevas palabras y repítelas.", "medio", 4, 7, "escolar"),
                Mission("", "Hacer un rompecabezas", "Arma un rompecabezas de 10 piezas.", "medio", 4, 7, "escolar"),
                Mission("", "Ordenar tus libros", "Organiza tus libros o cuadernos.", "medio", 4, 7, "escolar"),
                Mission("", "Resolver ejercicios de lógica", "Resuelve acertijos simples.", "avanzado", 4, 7, "escolar"),
                Mission("", "Escribir un mini cuento", "Crea un pequeño cuento y dibuja ilustraciones.", "avanzado", 4, 7, "escolar"),
                Mission("", "Participar en juego educativo", "Participa activamente en un juego de aprendizaje.", "avanzado", 4, 7, "escolar"),

                // 🔹 Escolar 8-11 años
                Mission("", "Hacer la tarea", "Completa tus tareas escolares todos los días.", "fácil", 8, 11, "escolar"),
                Mission("", "Aprender una canción", "Aprende y canta una canción nueva.", "fácil", 8, 11, "escolar"),
                Mission("", "Leer un capítulo de un libro", "Lee un capítulo de un libro con ayuda.", "fácil", 8, 11, "escolar"),
                Mission("", "Escribir un diario", "Escribe un diario de tus actividades.", "medio", 8, 11, "escolar"),
                Mission("", "Hacer experimentos simples", "Realiza experimentos básicos con supervisión.", "medio", 8, 11, "escolar"),
                Mission("", "Resolver problemas matemáticos", "Resuelve problemas de matemáticas sencillos.", "medio", 8, 11, "escolar"),
                Mission("", "Escribir una historia", "Escribe una historia corta sobre tu día.", "avanzado", 8, 11, "escolar"),
                Mission("", "Realizar un proyecto escolar", "Crea un proyecto creativo de ciencias o arte.", "avanzado", 8, 11, "escolar"),
                Mission("", "Presentar exposición", "Prepara y presenta un tema frente a la familia.", "avanzado", 8, 11, "escolar")
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