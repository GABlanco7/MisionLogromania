package com.german.misionlogromania.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.google.firebase.auth.FirebaseAuth

class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recover_password)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias al layout
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnRecover = findViewById<Button>(R.id.btnRecover)
        val title = findViewById<TextView>(R.id.title)

        // Cambiar texto del título
        title.text = "Recuperar contraseña"

        // ✅ Funcionalidad de la flecha: simplemente volver atrás
        btnBack.setOnClickListener {
            finish() // Esto cierra la actividad actual y vuelve a la anterior
        }

        // Enviar correo de recuperación
        btnRecover.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Ingrese su correo", Toast.LENGTH_SHORT).show()
            } else {
                // ✅ Deshabilitar botón para evitar múltiples clics
                btnRecover.isEnabled = false

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        btnRecover.isEnabled = true // Rehabilitar botón

                        if (task.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Se ha enviado un enlace de recuperación a $email",
                                Toast.LENGTH_LONG
                            ).show()
                            // ✅ Volver al login después de enviar
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }
}