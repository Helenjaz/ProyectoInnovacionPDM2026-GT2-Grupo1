package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectoinnovacionpdm2026_gt2_grupo1.databinding.ActivityTraductorBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.text.Normalizer
import java.util.*

class TraductorActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityTraductorBinding
    private var idiomaSeleccionado: String? = null
    private val REQ_CODE_SPEECH_INPUT = 101
    private var tts: TextToSpeech? = null
    
    // LIBRERÍA: Google ML Kit
    private var translator: Translator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTraductorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        // Idioma desde el Intent
        val rawIdioma = intent.getStringExtra("IDIOMA_SELECCIONADO") ?: "Desconocido"
        idiomaSeleccionado = normalizar(rawIdioma)
        binding.lblTituloTraduciendo.text = "MODO: ${rawIdioma.uppercase()}"

        // Configuración inicial del motor de Google
        if (idiomaSeleccionado == "ingles" || idiomaSeleccionado == "frances") {
            configurarMotorGoogle()
        }

        // Lógica de escritura
        binding.txtInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runnable?.let { handler.removeCallbacks(it) }
                binding.btnClear.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                
                val texto = s.toString().trim()
                if (texto.isEmpty()) {
                    binding.lblResultado.text = "Resultado"
                    binding.progressTraduccion.visibility = View.GONE
                } else {
                    if (!buscarEnBaseDatosLocal(texto)) {
                        if (idiomaSeleccionado == "nahuat") {
                            binding.lblResultado.text = "Palabra no encontrada"
                        } else {
                            binding.lblResultado.text = "Traduciendo..."
                            binding.progressTraduccion.visibility = View.VISIBLE
                            runnable = Runnable { traducirConIA(texto) }
                            handler.postDelayed(runnable!!, 600)
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- BOTONES DE ACCIÓN ---

        binding.btnMicrofono.setOnClickListener { iniciarDictadoVoz() }
        
        binding.btnCamara.setOnClickListener { 
            startActivityForResult(Intent(this, CamaraActivity::class.java), 100) 
        }

        binding.btnEscuchar.setOnClickListener {
            val t = binding.lblResultado.text.toString()
            if (t != "Resultado" && !t.contains("...")) tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        binding.btnRegresar.setOnClickListener { finish() }

        binding.btnClear.setOnClickListener {
            binding.txtInput.setText("")
            binding.lblResultado.text = "Resultado"
            binding.progressTraduccion.visibility = View.GONE
        }

        // BOTÓN REPORTE: Con advertencia si no hay nada que reportar
        binding.btnGenerarPdf.setOnClickListener {
            val original = binding.txtInput.text.toString().trim()
            val traducido = binding.lblResultado.text.toString().trim()

            if (original.isEmpty() || traducido == "Resultado" || traducido == "Palabra no encontrada") {
                // ADVERTENCIA
                Toast.makeText(this, "⚠️ No hay nada para reportar. Escribe una palabra primero.", Toast.LENGTH_LONG).show()
            } else if (traducido.contains("...") || traducido.contains("Descargando")) {
                Toast.makeText(this, "⏳ Espera a que termine la traducción...", Toast.LENGTH_SHORT).show()
            } else {
                // TODO CORRECTO: Ir al reporte
                val intent = Intent(this, ReporteActivity::class.java).apply {
                    putExtra("TEXTO_ORIGINAL", original)
                    putExtra("TEXTO_TRADUCIDO", traducido)
                    putExtra("IDIOMA", rawIdioma)
                }
                startActivity(intent)
            }
        }
    }

    private fun configurarMotorGoogle() {
        val sourceLang = when (idiomaSeleccionado) {
            "ingles" -> TranslateLanguage.ENGLISH
            "frances" -> TranslateLanguage.FRENCH
            else -> return
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        translator = Translation.getClient(options)
        translator?.downloadModelIfNeeded(DownloadConditions.Builder().build())
    }

    private fun buscarEnBaseDatosLocal(texto: String): Boolean {
        return try {
            val db = DatabaseHelper(this).readableDatabase
            val cursor = db.rawQuery(
                "SELECT palabra_espanol FROM traducciones WHERE idioma_origen = ? AND palabra_busqueda = ?",
                arrayOf(idiomaSeleccionado, normalizar(texto))
            )
            val encontrado = if (cursor.moveToFirst()) {
                binding.lblResultado.text = cursor.getString(0)
                binding.progressTraduccion.visibility = View.GONE
                true
            } else false
            cursor.close()
            db.close()
            encontrado
        } catch (e: Exception) { false }
    }

    private fun traducirConIA(texto: String) {
        val t = translator ?: return
        t.downloadModelIfNeeded().addOnSuccessListener {
            t.translate(texto).addOnSuccessListener { res ->
                if (binding.txtInput.text.toString().trim() == texto) {
                    binding.lblResultado.text = res
                    binding.progressTraduccion.visibility = View.GONE
                }
            }
        }.addOnFailureListener {
            binding.lblResultado.text = "Descargando motor..."
        }
    }

    private fun normalizar(texto: String?): String {
        if (texto == null) return ""
        val temp = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return temp.replace("[\\u0300-\\u036f]".toRegex(), "").replace("[^a-zA-Z0-9\\s]".toRegex(), "").lowercase().trim()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale("es", "ES")
    }

    private fun iniciarDictadoVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        val locale = when (idiomaSeleccionado) {
            "frances" -> Locale.FRANCE
            "ingles" -> Locale.US
            else -> Locale("es", "ES")
        }
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
        try { startActivityForResult(intent, REQ_CODE_SPEECH_INPUT) } catch (e: Exception) {}
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); translator?.close()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) binding.txtInput.setText(data?.getStringExtra("TEXTO_DETECTADO") ?: "")
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            val res = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!res.isNullOrEmpty()) binding.txtInput.setText(res[0])
        }
    }
}
