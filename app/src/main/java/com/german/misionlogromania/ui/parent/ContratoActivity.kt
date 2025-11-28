package com.german.misionlogromania.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.parent.CreateChildProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ContratoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contrato)

        auth = FirebaseAuth.getInstance()

        val tvContractText = findViewById<TextView>(R.id.tvContractText)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val scrollView = findViewById<ScrollView>(R.id.scrollView)

        // Texto completo del contrato (puedes cargar desde res/raw si quieres)
        val contractText = """
CONTRATO CONDUCTUAL - MISIÓN LOGROMANÍA

1. Introducción

El presente Contrato Conductual establece los acuerdos, responsabilidades 
y compromisos entre padres o tutores y niños participantes dentro del 
programa Misión Logromanía, una aplicación educativa diseñada para 
fomentar hábitos positivos, responsabilidad, autonomía y convivencia 
familiar a través del cumplimiento de misiones y recompensas. 
El objetivo principal es fortalecer el vínculo entre padres e hijos mediante un 
sistema lúdico y motivacional, donde los niños adquieren disciplina y 
compromiso al completar sus misiones diarias o semanales, y los padres 
acompañan activamente su progreso.

2. Propósito del Contrato

El propósito de este contrato es: 
• Promover el cumplimiento responsable de tareas del hogar, escolares y de 
autocuidado. 
• Fortalecer la comunicación familiar y la confianza mutua. 
• Desarrollar habilidades como la organización, empatía, colaboración y 
disciplina. 
• Motivar al niño mediante un sistema de misiones y recompensas visual, 
accesible y divertido. 
• Establecer reglas claras y consecuencias justas dentro del entorno familiar. 

3. Estructura del Programa

3.1. Misiones 
Las misiones son actividades asignadas por el padre o madre al hijo/a y 
se clasifican en tres categorías: 
• Hogar: responsabilidades del entorno familiar (ej. tender la cama, ayudar a 
poner la mesa). 
• Autocuidado: hábitos personales (ej. cepillarse los dientes, bañarse sin 
recordatorio). 
• Escolar: deberes académicos (ej. hacer tareas, leer un libro, practicar 
matemáticas). 
Cada misión tiene una dificultad adaptada a la edad del niño y se completa 
una vez confirmada por el padre. 
3.2. Confirmación y Revisión 
Cuando el niño marca una misión como completada, el padre recibe una 
notificación en su aplicación. 
El padre podrá: 
• Revisar la misión. 
• Confirmarla o rechazarla. 
• Otorgar una estrella de logro al completarse correctamente. 
3.3. Recompensas 
Al acumular estrellas, el niño puede obtener recompensas previamente 
acordadas, como: 
• Tiempo adicional de juego. 
• Actividades familiares. 
• Pequeños premios simbólicos. 
Las recompensas no son materiales necesariamente, sino experiencias que 
fortalezcan los lazos familiares. 

4. Compromisos del Padre o Tutor

Yo, como padre/madre/tutor responsable, me comprometo a: 
1. Acompañar y supervisar el progreso de mi hijo/a dentro de Misión 
Logromanía. 
2. Asignar misiones adecuadas a su edad, intereses y capacidades. 
3. Mantener una comunicación constante, promoviendo el diálogo sobre los 
logros y aprendizajes. 
4. Reconocer los esfuerzos, no solo los resultados, motivando el desarrollo 
personal del niño. 
5. No utilizar la aplicación como castigo, sino como una herramienta educativa 
y positiva. 
6. Confirmar las misiones completadas con objetividad, fomentando la 
honestidad y la responsabilidad. 
7. Cumplir las recompensas acordadas al completarse las metas. 

5. Compromisos del Niño o Niña

Yo, como niño/niña participante, me comprometo a: 
1. Cumplir mis misiones con entusiasmo y responsabilidad. 
2. Ser honesto al marcar mis tareas como completadas. 
3. Pedir ayuda cuando una misión sea difícil. 
4. Mantener una buena actitud frente a los retos y errores. 
5. Respetar las reglas establecidas junto a mis padres. 
6. Entender que las recompensas son fruto del esfuerzo constante.

6. Normas de Convivencia y Uso

1. Respeto mutuo: No se usarán castigos o comparaciones negativas dentro 
del sistema. 
2. Privacidad: Los datos personales y progreso del niño serán usados 
únicamente dentro del entorno seguro de la aplicación. 
3. Constancia: El cumplimiento parcial o total de las misiones será revisado 
diaria o semanalmente por los padres. 
4. Equilibrio: Las misiones deben permitir tiempo libre, descanso y recreación 
saludable. 
5. Reconocimiento: Cada logro, por pequeño que sea, será valorado. 
6. Vigencia del Contrato 
El presente contrato tendrá vigencia durante el uso activo de la aplicación 
Misión Logromanía y podrá ser revisado y actualizado según el crecimiento 
del niño o los acuerdos familiares.

Ambas partes aceptan que este contrato no tiene carácter legal, sino educativo y 
formativo, con el objetivo de fortalecer valores familiares, responsabilidad y 
compromiso personal. 
Este acuerdo representa una alianza entre el juego, el aprendizaje y el amor."""

        tvContractText.text = contractText

        // Habilitar botón solo al llegar al final del ScrollView
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(scrollView.childCount - 1)
            if (view.bottom <= (scrollView.height + scrollView.scrollY)) {
                btnAccept.isEnabled = true
            }
        }

        // Guardar aceptación en Firestore y continuar
        btnAccept.setOnClickListener {
            saveContractAccepted()
        }
    }

    private fun saveContractAccepted() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: usuario no encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val db = Firebase.firestore
        val data = mapOf(
            "contract_accepted" to true,
            "child_commitment_accepted" to true
        )

        db.collection("users").document(userId)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Contrato aceptado correctamente ✅", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CreateChildProfileActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}