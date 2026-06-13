package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.proyectoinnovacionpdm2026_gt2_grupo1.databinding.ActivityReporteBinding
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReporteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReporteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityReporteBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("ReporteActivity", "Error al inflar el layout", e)
            return
        }

        val textoOriginal = intent.getStringExtra("TEXTO_ORIGINAL") ?: "Sin texto"
        val textoTraducido = intent.getStringExtra("TEXTO_TRADUCIDO") ?: "Sin traducción"
        val idioma = intent.getStringExtra("IDIOMA") ?: "Desconocido"

        binding.lblOriginalResumen.text = "Original ($idioma):\n$textoOriginal"
        binding.lblTraduccionResumen.text = "Traducción (Español):\n$textoTraducido"

        // BOTÓN GENERAR Y ENVIAR
        binding.btnGenerarEnviar.setOnClickListener {
            val correo = binding.txtCorreoDestinatario.text.toString().trim()
            
            // 1. Validar correo
            if (correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "⚠️ Por favor ingresa un correo válido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚙️ Generando PDF...", Toast.LENGTH_SHORT).show()
                
                // 2. Generar PDF
                val pdfFile = crearPdfSeguro(textoOriginal, textoTraducido, idioma)
                
                if (pdfFile != null && pdfFile.exists()) {
                    // 3. Enviar
                    compartirPdf(correo, pdfFile)
                } else {
                    Toast.makeText(this, "❌ Error al crear el archivo PDF", Toast.LENGTH_LONG).show()
                }
            }
        }

        // BOTÓN REGRESAR
        binding.btnRegresarReporte.setOnClickListener {
            finish()
        }
    }

    private fun crearPdfSeguro(original: String, traducido: String, idioma: String): File? {
        val documento = Document(PageSize.A4)
        return try {
            // Usamos el directorio de caché para evitar problemas de permisos
            val directory = File(cacheDir, "pdf_reports")
            if (!directory.exists()) directory.mkdirs()
            
            val file = File(directory, "Reporte_Traduccion.pdf")
            if (file.exists()) file.delete()

            PdfWriter.getInstance(documento, FileOutputStream(file))
            documento.open()

            // Logo
            try {
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.logo_ues)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val img = Image.getInstance(stream.toByteArray())
                img.scaleToFit(80f, 80f)
                img.alignment = Element.ALIGN_CENTER
                documento.add(img)
            } catch (e: Exception) {
                Log.e("PDF", "No se pudo añadir el logo", e)
            }

            // Títulos
            val fontTitulo = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor(183, 28, 28))
            val fontCuerpo = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL)
            val fontBold = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor(0, 121, 107))

            documento.add(Paragraph("REPORTE DE TRADUCCIÓN", fontTitulo).apply { alignment = Element.ALIGN_CENTER })
            documento.add(Paragraph("Proyecto Innovación PDM 2026\n\n", fontCuerpo).apply { alignment = Element.ALIGN_CENTER })
            documento.add(Chunk(LineSeparator()))
            
            documento.add(Paragraph("\nIdioma: ${idioma.uppercase()}", fontBold))
            documento.add(Paragraph("\nTexto Original:", fontBold))
            documento.add(Paragraph(original, fontCuerpo))
            documento.add(Paragraph("\nTraducción al Español:", fontBold))
            documento.add(Paragraph(traducido, fontCuerpo))
            
            documento.add(Paragraph("\n"))
            documento.add(Chunk(LineSeparator()))
            
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            documento.add(Paragraph("\nGenerado el: $fecha", fontCuerpo).apply { alignment = Element.ALIGN_RIGHT })

            documento.close()
            file
        } catch (e: Exception) {
            Log.e("PDF", "Error general", e)
            null
        }
    }

    private fun compartirPdf(email: String, file: File) {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(this, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Traducción - Grupo 1")
                putExtra(Intent.EXTRA_TEXT, "Adjunto envío el reporte generado por la App.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Enviar correo con:"))
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró aplicación de correo", Toast.LENGTH_SHORT).show()
        }
    }
}
