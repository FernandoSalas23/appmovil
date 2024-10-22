package com.aero.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        val signInButton: Button = findViewById(R.id.btnIngresar)
        signInButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.edtCorreo).text.toString()
            val password = findViewById<EditText>(R.id.edtContraseña).text.toString()
            signInUser(email, password)
        }

        val registerButton: TextView = findViewById(R.id.btnRegistrar)
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Login fallido: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
