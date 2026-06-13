package com.example.proyectoinnovacionpdm2026_gt2_grupo1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proyectoinnovacionpdm2026_gt2_grupo1.databinding.ActivityCamaraBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CamaraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCamaraBinding
    private var cameraExecutor: ExecutorService? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var imageCapture: ImageCapture? = null
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.btnCapturar.setOnClickListener { takePhoto() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.btnRegresarCamara.setOnClickListener { finish() }

        binding.zoomBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cameraControl?.setLinearZoom(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cameraExecutor = Executors.newSingleThreadExecutor()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                
                binding.viewFinder.setOnTouchListener { v, event ->
                    val factory = binding.viewFinder.meteringPointFactory
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    cameraControl?.startFocusAndMetering(action)
                    v.performClick()
                    true
                }
            } catch (e: Exception) {
                mostrarMensajeAdvertencia("Error al iniciar cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        binding.viewFinder.animate().alpha(0.3f).setDuration(50).withEndAction {
            binding.viewFinder.animate().alpha(1.0f).setDuration(50).start()
        }.start()
        
        binding.progressCargando.visibility = View.VISIBLE
        binding.btnCapturar.isEnabled = false

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                if (bitmap != null) {
                    val croppedBitmap = cropToROI(bitmap)
                    recognizeText(croppedBitmap, bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                binding.progressCargando.visibility = View.GONE
                binding.btnCapturar.isEnabled = true
                mostrarMensajeAdvertencia("No se pudo capturar la imagen")
            }
        })
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun cropToROI(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val roiWidth = (width * 0.6).toInt()
        val roiHeight = (height * 0.2).toInt()
        val left = (width - roiWidth) / 2
        val top = (height - roiHeight) / 2
        return Bitmap.createBitmap(bitmap, left, top, roiWidth, roiHeight)
    }

    private fun recognizeText(bitmap: Bitmap, originalBitmap: Bitmap? = null) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text.trim()
                if (resultText.isNotEmpty()) {
                    binding.progressCargando.visibility = View.GONE
                    val intent = Intent().apply { putExtra("TEXTO_DETECTADO", resultText) }
                    setResult(RESULT_OK, intent)
                    finish()
                } else if (originalBitmap != null) {
                    recognizeText(originalBitmap, null)
                } else {
                    binding.progressCargando.visibility = View.GONE
                    binding.btnCapturar.isEnabled = true
                    mostrarMensajeAdvertencia("No se detectó texto. Intenta de nuevo.")
                }
            }
            .addOnFailureListener { e ->
                binding.progressCargando.visibility = View.GONE
                binding.btnCapturar.isEnabled = true
                mostrarMensajeAdvertencia("Error de lectura: ${e.message}")
            }
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        cameraControl?.enableTorch(isFlashOn)
        binding.btnFlash.setImageResource(if (isFlashOn) android.R.drawable.ic_menu_mylocation else android.R.drawable.ic_menu_camera)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                mostrarMensajeAdvertencia("Permisos de cámara denegados")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
