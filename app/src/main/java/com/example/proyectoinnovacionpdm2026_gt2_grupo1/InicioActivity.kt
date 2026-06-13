package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.proyectoinnovacionpdm2026_gt2_grupo1.databinding.ActivityInicioBinding
import kotlin.concurrent.thread

class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // LIBRERÍA 1: GLIDE - Carga optimizada de imágenes
        Glide.with(this)
            .load(R.drawable.logo_ues)
            .into(binding.imgLogo)

        // Forzar la creación de la base de datos en un hilo secundario
        thread {
            val dbHelper = DatabaseHelper(this)
            dbHelper.writableDatabase // Esto dispara onCreate() si no existe
            dbHelper.close()
        }

        binding.btnIniciar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finalizamos inicio para que no regrese aquí con el botón atrás
        }
    }
}
