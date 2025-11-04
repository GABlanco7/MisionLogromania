package com.german.misionlogromania.ui.menu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.german.misionlogromania.R
import com.german.misionlogromania.ui.login.ChildLoginActivity
import com.german.misionlogromania.ui.board.MissionBoardActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class KidHomeActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var avatarImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var missionsRecyclerView: RecyclerView
    private lateinit var starsTitleTextView: TextView
    private lateinit var starsCountTextView: TextView

    private var childId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kid_home)

        // 🔹 Referencias UI
        avatarImageView = findViewById(R.id.avatarImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        missionsRecyclerView = findViewById(R.id.missionsRecyclerView)
        starsTitleTextView = findViewById(R.id.tvStarsTitle)
        starsCountTextView = findViewById(R.id.tvStarsCount)

        // 🔹 Verificar sesión del niño
        val prefs = getSharedPreferences("child_prefs", MODE_PRIVATE)
        val isLogged = prefs.getBoolean("isChildLogged", false)

        if (!isLogged) {
            startActivity(Intent(this, ChildLoginActivity::class.java))
            finish()
            return
        }

        childId = prefs.getString("childId", null)
        if (childId != null) {
            loadChildProfile(childId!!)
            listenStars(childId!!)  // 🔹 Actualización en tiempo real
        } else {
            Toast.makeText(this, "Error: ID del niño no encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    /** 🔹 Cargar perfil del niño */
    private fun loadChildProfile(childId: String) {
        db.collection("children").document(childId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val username = doc.getString("username") ?: "Niño"
                    usernameTextView.text = username

                    val avatarName = doc.getString("avatar") ?: "avatar3"
                    val avatarResId = resources.getIdentifier(avatarName, "drawable", packageName)
                    avatarImageView.setImageResource(avatarResId)

                    @Suppress("UNCHECKED_CAST")
                    val missions = doc.get("assignedMissions") as? List<Map<String, Any>> ?: listOf()

                    missionsRecyclerView.layoutManager = LinearLayoutManager(this)
                    missionsRecyclerView.adapter = MissionsAdapter(missions) { mission ->
                        val missionTitle = mission["title"].toString()
                        val missionId = mission["id"].toString()
                        val childNamePref = doc.getString("username") ?: "Niño"

                        val intent = Intent(this, MissionBoardActivity::class.java)
                        intent.putExtra("missionTitle", missionTitle)
                        intent.putExtra("missionId", missionId)
                        intent.putExtra("childId", childId)
                        intent.putExtra("childName", childNamePref)
                        startActivity(intent)
                    }

                    if (missions.isEmpty()) {
                        Toast.makeText(this, "No tienes misiones asignadas todavía", Toast.LENGTH_SHORT).show()
                    }

                    // 🔹 Mostrar estrellas iniciales
                    val stars = doc.getLong("stars") ?: 0L
                    starsCountTextView.text = stars.toString()
                } else {
                    Toast.makeText(this, "Perfil no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔹 Escuchar cambios en las estrellas en tiempo real */
    private fun listenStars(childId: String) {
        db.collection("children").document(childId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val stars = snapshot.getLong("stars") ?: 0L
                    starsCountTextView.text = stars.toString()
                }
            }
    }

    // 🔹 Adaptador de misiones
    class MissionsAdapter(
        private val missions: List<Map<String, Any>>,
        private val onMissionClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<MissionsAdapter.MissionViewHolder>() {

        inner class MissionViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
            fun bind(mission: Map<String, Any>) {
                textView.text = mission["title"].toString()
                textView.setOnClickListener { onMissionClick(mission) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
            return MissionViewHolder(textView)
        }

        override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
            holder.bind(missions[position])
        }

        override fun getItemCount() = missions.size
    }
}