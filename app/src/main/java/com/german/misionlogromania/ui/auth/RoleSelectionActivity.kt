package com.german.misionlogromania.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.child.ChildLoginActivity
import com.german.misionlogromania.ui.parent.ParentLoginActivity

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vincula el XML de la pantalla de selección de rol
        setContentView(R.layout.activity_rol_selection)

        // Referencias a los botones
        val btnChild: Button = findViewById(R.id.btnHijo)
        val btnParent: Button = findViewById(R.id.btnPadre)

        // Acción al presionar "Child" → abrir login de hijo
        btnChild.setOnClickListener {
            val intent = Intent(this, ChildLoginActivity::class.java)
            startActivity(intent)
        }

        // Acción al presionar "Parent" → abrir login de padre
        btnParent.setOnClickListener {
            val intent = Intent(this, ParentLoginActivity::class.java)
            startActivity(intent)
        }
    }
}