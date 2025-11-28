package com.german.misionlogromania.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.parent.SelectMissionsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateChildProfileActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var selectedAvatar = "avatar1"
    private var familyCode: String = ""
    private lateinit var parentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_child_profile)

        auth = FirebaseAuth.getInstance()
        parentId = auth.currentUser?.uid ?: ""

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val imgSelectedAvatar = findViewById<ImageView>(R.id.imgSelectedAvatar)
        val btnExpandAvatars = findViewById<ImageView>(R.id.btnExpandAvatars)
        val avatarOptionsLayout = findViewById<LinearLayout>(R.id.avatarOptionsLayout)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // 🔹 Desactivar el botón mientras se obtiene el código familiar
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // 🔹 Obtener el código familiar del padre
        db.collection("users").document(parentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    familyCode = doc.getString("familyCode") ?: ""
                    if (familyCode.isNotEmpty()) {
                        btnSave.isEnabled = true
                        progressBar.visibility = View.GONE
                    } else {
                        Toast.makeText(this, "El padre no tiene un código familiar válido.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Error: no se encontró el padre en Firestore.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener código familiar: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }

        // Mostrar / ocultar avatares
        btnExpandAvatars.setOnClickListener {
            avatarOptionsLayout.visibility =
                if (avatarOptionsLayout.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Selección de avatar
        val avatarViews = listOf(
            findViewById<ImageView>(R.id.avatar1),
            findViewById<ImageView>(R.id.avatar2),
            findViewById<ImageView>(R.id.avatar3)
        )

        avatarViews.forEach { avatar ->
            avatar.setOnClickListener {
                selectedAvatar = when (it.id) {
                    R.id.avatar1 -> "avatar1"
                    R.id.avatar2 -> "avatar2"
                    R.id.avatar3 -> "avatar3"
                    else -> "avatar1"
                }
                imgSelectedAvatar.setImageResource(
                    resources.getIdentifier(selectedAvatar, "drawable", packageName)
                )
                avatarOptionsLayout.visibility = View.GONE
            }
        }

        // 🔹 Guardar perfil del niño
        btnSave.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val ageStr = etAge.text.toString().trim()
            val username = etUsername.text.toString().trim()

            // Validaciones
            if (firstName.isEmpty() || lastName.isEmpty() || ageStr.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageStr.toIntOrNull()
            if (age == null || age !in 4..11) {
                Toast.makeText(this, "La edad debe estar entre 4 y 11 años", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (familyCode.isEmpty()) {
                Toast.makeText(this, "El código familiar aún no está cargado. Intente de nuevo.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Verificar duplicado de username dentro del mismo familyCode
            db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { query ->
                    if (!query.isEmpty) {
                        Toast.makeText(this, "El nombre de usuario ya existe para esta familia", Toast.LENGTH_SHORT).show()
                    } else {
                        saveChildProfile(firstName, lastName, age, username)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al verificar username: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // ------------------- GUARDAR PERFIL -------------------
    private fun saveChildProfile(
        firstName: String,
        lastName: String,
        age: Int,
        username: String
    ) {
        val childData = hashMapOf(
            "name" to firstName,
            "lastName" to lastName,
            "age" to age,
            "avatar" to selectedAvatar,
            "level" to "facil",
            "parentId" to parentId,
            "username" to username,
            "familyCode" to familyCode
        )

        // Guardar en colección children
        db.collection("children")
            .add(childData)
            .addOnSuccessListener { docRef ->
                val childId = docRef.id

                // Crear usuario del niño en colección users
                val userChildData = hashMapOf(
                    "username" to username,
                    "familyCode" to familyCode,
                    "role" to "child",
                    "childId" to childId,
                    "name" to "$firstName $lastName"
                )

                db.collection("users").document(childId)
                    .set(userChildData)
                    .addOnSuccessListener {
                        // Vincular el hijo con el padre
                        db.collection("users").document(parentId)
                            .update(
                                mapOf(
                                    "childId" to childId,
                                    "hasChild" to true,
                                    "child_profile_created" to true
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(this, "Perfil del hijo creado correctamente", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, SelectMissionsActivity::class.java)
                                intent.putExtra("childId", childId)
                                intent.putExtra("childAge", age)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al vincular padre: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar en users: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}