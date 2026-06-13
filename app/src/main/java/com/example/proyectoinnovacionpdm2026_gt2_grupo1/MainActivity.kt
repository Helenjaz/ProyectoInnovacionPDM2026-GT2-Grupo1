package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoinnovacionpdm2026_gt2_grupo1.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Usamos etiquetas internas sin tildes para evitar errores en la base de datos
        binding.btnNahuat.setOnClickListener { abrirTraductor("NAHUAT") }
        binding.btnIngles.setOnClickListener { abrirTraductor("INGLES") }
        binding.btnFrances.setOnClickListener { abrirTraductor("FRANCES") }
    }

    private fun abrirTraductor(idioma: String) {
        val intent = Intent(this, TraductorActivity::class.java).apply {
            putExtra("IDIOMA_SELECCIONADO", idioma)
        }
        startActivity(intent)
    }

    private fun mostrarMensajeAdvertencia(mensaje: String) {
        val snackbar = Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor("#FF9800"))
        snackbar.setTextColor(Color.WHITE)
        val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_alert, 0, 0, 0)
        textView.compoundDrawablePadding = 24
        snackbar.show()
    }
}
