package com.german.misionlogromania.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ParentLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        // Referencias a vistas
        val etEmail = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)
        val cbStayLogged = findViewById<CheckBox>(R.id.cbStayLogged)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // ✅ Flecha de volver → simplemente cierra esta actividad
        btnBack.setOnClickListener {
            finish() // Vuelve automáticamente a RoleSelectionActivity
        }

        // 🔹 Mantener sesión abierta si ya estaba logueado
        val stayLogged = prefs.getBoolean("stay_logged_in", false)
        if (stayLogged && auth.currentUser != null) {
            checkFirstTimeAndRedirect()
        }

        // 🔹 Botón login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Guardar preferencia de sesión
                        prefs.edit().putBoolean("stay_logged_in", cbStayLogged.isChecked).apply()
                        Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                        checkFirstTimeAndRedirect()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // 🔹 Crear cuenta
        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, ParentRegisterActivity::class.java))
            // ❌ NO pongas finish() aquí
        }

        // ✅ Recuperar contraseña (ya está bien)
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
            // ❌ NO pongas finish() aquí
        }
    }

    // 🔹 Verifica primera vez y redirige correctamente según Firestore y misiones del niño
    private fun checkFirstTimeAndRedirect() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    navigateTo(ParentMenuActivity::class.java)
                    return@addOnSuccessListener
                }

                val role = doc.getString("role") ?: ""
                val contractAccepted = doc.getBoolean("contract_accepted") ?: false
                val childProfileCreated = doc.getBoolean("child_profile_created") ?: false

                when {
                    role != "padre" -> {
                        navigateTo(ParentMenuActivity::class.java)
                    }
                    !contractAccepted -> {
                        navigateTo(ContratoActivity::class.java)
                    }
                    !childProfileCreated -> {
                        navigateTo(CreateChildProfileActivity::class.java)
                    }
                    else -> {
                        // 🔹 Perfil creado, ahora verificamos si el niño tiene misiones
                        db.collection("children")
                            .whereEqualTo("parentId", userId)
                            .get()
                            .addOnSuccessListener { childrenDocs ->
                                if (childrenDocs.isEmpty) {
                                    navigateTo(CreateChildProfileActivity::class.java)
                                    return@addOnSuccessListener
                                }

                                val childDoc = childrenDocs.first()
                                val childId = childDoc.id
                                val assignedMissions = childDoc.get("assignedMissions") as? List<*>

                                if (assignedMissions.isNullOrEmpty()) {
                                    // Niño creado pero sin misiones → ir a seleccionar misiones
                                    val intent = Intent(this, SelectMissionsActivity::class.java)
                                    intent.putExtra("childId", childId)
                                    intent.putExtra("childAge", childDoc.getLong("age")?.toInt() ?: 0)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Niño creado y con misiones → ir al menú del padre
                                    navigateTo(ParentMenuActivity::class.java)
                                }
                            }
                            .addOnFailureListener {
                                navigateTo(ParentMenuActivity::class.java)
                            }
                    }
                }
            }
            .addOnFailureListener {
                navigateTo(ParentMenuActivity::class.java)
            }
    }

    // ✅ Helper para navegar y limpiar el stack
    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}