package com.example.wearos_project.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wearos_project.R

class Authen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authen)

        val allowButton: Button = findViewById(R.id.allowButton)
        val denyButton: Button = findViewById(R.id.denyButton)

        allowButton.setOnClickListener {
            Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        denyButton.setOnClickListener {
            Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
