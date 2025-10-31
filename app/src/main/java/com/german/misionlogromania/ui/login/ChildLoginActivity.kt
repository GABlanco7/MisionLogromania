package com.german.misionlogromania.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.menu.KidHomeActivity
import com.german.misionlogromania.ui.roles.RoleSelectionActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.MessageDigest

class ChildLoginActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val useHash = false // Cambiar a true si usas hash de contraseña

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_login)

        val prefs = getSharedPreferences("child_prefs", MODE_PRIVATE)

        // 🔹 Referencias UI
        val etFamilyCode = findViewById<EditText>(R.id.etFamilyCode)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnChildLogin)
        val cbRemember = findViewById<CheckBox>(R.id.cbRememberSession)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // 🔹 Flecha para volver al selector de roles
        btnBack.setOnClickListener {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }

        // 🔹 Botón iniciar sesión
        btnLogin.setOnClickListener {
            val familyCode = etFamilyCode.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (familyCode.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordToCheck = if (useHash) hashPassword(password) else password

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .whereEqualTo("username", username)
                .whereEqualTo(if (useHash) "passwordHash" else "password", passwordToCheck)
                .whereEqualTo("role", "child")
                .get()
                .addOnSuccessListener { query ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    if (!query.isEmpty) {
                        val childDoc = query.documents[0]
                        val childId = childDoc.id
                        val childName = childDoc.getString("name") ?: username

                        Log.d("ChildLogin", "Inicio exitoso: $childName ($childId)")

                        // 🔹 Guardar sesión según la opción del usuario
                        if (cbRemember.isChecked) {
                            prefs.edit()
                                .putString("childId", childId)
                                .putString("childName", childName)
                                .putString("username", username)
                                .putString("familyCode", familyCode)
                                .putBoolean("isChildLogged", true)
                                .putBoolean("rememberSession", true)
                                .putString("role", "child")
                                .apply()
                            Log.d("ChildLogin", "Sesión guardada (recordar activado)")
                        } else {
                            // ✅ Sesión temporal (solo mientras la app está abierta)
                            prefs.edit()
                                .putString("childId", childId)
                                .putString("childName", childName)
                                .putBoolean("isChildLogged", true)
                                .putBoolean("rememberSession", false)
                                .putString("role", "child")
                                .apply()
                            Log.d("ChildLogin", "Inicio temporal (no se recordará)")
                        }

                        Toast.makeText(this, "¡Bienvenido, $childName!", Toast.LENGTH_SHORT).show()
                        goToKidHome(childId, childName)

                    } else {
                        Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Log.e("ChildLogin", "Error Firestore: ${e.message}")
                    Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 🔹 Función para ir a la pantalla principal del niño
    private fun goToKidHome(childId: String, childName: String) {
        val intent = Intent(this, KidHomeActivity::class.java).apply {
            putExtra("childId", childId)
            putExtra("childName", childName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ✅ Limpia sesión temporal al cerrar la app
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            val prefs = getSharedPreferences("child_prefs", MODE_PRIVATE)
            val rememberSession = prefs.getBoolean("rememberSession", false)

            if (!rememberSession) {
                prefs.edit().clear().apply()
                Log.d("ChildLogin", "Sesión temporal borrada")
            }
        }
    }
}