package com.german.misionlogromania.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.contract.ContratoActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val chkStayLoggedIn = findViewById<CheckBox>(R.id.chkStayLoggedIn)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnBack.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Crear usuario en Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        Log.d("RegisterActivity", "Usuario creado con UID: $userId")
                        if (userId != null) {
                            saveUserToFirestore(userId, firstName, lastName, email, chkStayLoggedIn.isChecked)
                        } else {
                            Toast.makeText(this, "Error al obtener UID del usuario", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("RegisterActivity", "Error al crear usuario", task.exception)
                        Toast.makeText(this, "Error al registrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserToFirestore(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        stayLoggedIn: Boolean
    ) {
        // 🔹 Generar un código familiar único
        val familyCode = generateFamilyCode()

        val user = hashMapOf(
            "name" to "$firstName $lastName",
            "email" to email,
            "role" to "padre",
            "familyCode" to familyCode,
            "contract_accepted" to false,
            "child_profile_created" to false
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Usuario guardado en Firestore con familyCode: $familyCode")

                // Guardar el estado local de sesión
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("stay_logged_in", stayLoggedIn)
                    .putString("family_code", familyCode)
                    .putString("user_id", userId)
                    .apply()

                Toast.makeText(this, "✅ Cuenta creada correctamente", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "Tu código familiar es: $familyCode", Toast.LENGTH_LONG).show()

                // Ir al contrato directamente
                startActivity(Intent(this, ContratoActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Error al guardar en Firestore", e)
                Toast.makeText(this, "Error al guardar en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Generador de código familiar
    private fun generateFamilyCode(): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val part1 = (1..4).map { allowed.random() }.joinToString("")
        val part2 = (1..6).map { allowed.random() }.joinToString("")
        return "$part1-$part2"
    }
}