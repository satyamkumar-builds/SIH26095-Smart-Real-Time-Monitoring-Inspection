package com.example.myapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CaptureFragment : Fragment() {

    private lateinit var viewFinder: PreviewView
    private lateinit var imgCapturedPreview: ImageView
    private lateinit var btnCloseCapture: ImageView
    private lateinit var btnDoneCapture: TextView

    private lateinit var tvOverlayInspectionId: TextView
    private lateinit var tvOverlayTime: TextView
    private lateinit var tvOverlayLocation: TextView

    private lateinit var btnLeftControl: ImageView
    private lateinit var btnShutter: ImageView
    private lateinit var btnRightControl: ImageView

    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var capturedBitmap: Bitmap? = null
    private var isPhotoCaptured: Boolean = false

    private var currentInspectionId: String = "INS-2026-1042"
    private var currentLocationText: String = "Lat: --, Long: --"
    private var isLocationEnabled: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            tvOverlayTime.text = getCurrentIstTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_capture, container, false)

        viewFinder = view.findViewById(R.id.view_finder)
        imgCapturedPreview = view.findViewById(R.id.img_captured_preview)
        btnCloseCapture = view.findViewById(R.id.btn_close_capture)
        btnDoneCapture = view.findViewById(R.id.btn_done_capture)

        tvOverlayInspectionId = view.findViewById(R.id.tv_overlay_inspection_id)
        tvOverlayTime = view.findViewById(R.id.tv_overlay_time)
        tvOverlayLocation = view.findViewById(R.id.tv_overlay_location)

        btnLeftControl = view.findViewById(R.id.btn_left_control)
        btnShutter = view.findViewById(R.id.btn_shutter)
        btnRightControl = view.findViewById(R.id.btn_right_control)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 1. Resolve Active Inspection ID from JSON based on time
        currentInspectionId = resolveActiveInspectionId()
        tvOverlayInspectionId.text = "ID: $currentInspectionId"

        // 2. Start Live Time Overlay Update
        handler.post(timeRunnable)

        // 3. Request Camera & Location permissions
        checkPermissionsAndStartCamera()

        // 4. Setup Click Handlers
        btnCloseCapture.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_tasks
        }

        btnDoneCapture.setOnClickListener {
            if (isPhotoCaptured && capturedBitmap != null) {
                saveWatermarkedImageToCache(capturedBitmap!!)
                Toast.makeText(requireContext(), "Evidence Saved Successfully!", Toast.LENGTH_SHORT).show()
                activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_tasks
            }
        }

        btnShutter.setOnClickListener {
            takePhoto()
        }

        btnLeftControl.setOnClickListener {
            if (isPhotoCaptured) {
                // Discard photo and return to live camera
                resetToCameraState()
            } else {
                // Toggle Location Lat/Long
                toggleLocation()
            }
        }

        btnRightControl.setOnClickListener {
            if (isPhotoCaptured) {
                // Save watermarked photo to phone gallery
                capturedBitmap?.let { bmp ->
                    saveWatermarkedImageToGallery(bmp)
                }
            } else {
                // Flip camera (Rear <-> Front)
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                startCamera()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable)
    }

    private fun checkPermissionsAndStartCamera() {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startCamera()
        } else {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File.createTempFile("captured_", ".jpg", requireContext().cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val rawBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    capturedBitmap = applyWatermarks(rawBitmap)

                    // Switch UI to Captured / Preview State
                    switchToCapturedState()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun switchToCapturedState() {
        isPhotoCaptured = true
        viewFinder.visibility = View.GONE
        imgCapturedPreview.visibility = View.VISIBLE
        imgCapturedPreview.setImageBitmap(capturedBitmap)

        // Done Button -> Active Blue
        btnDoneCapture.setTextColor(Color.parseColor("#007AFF"))
        btnDoneCapture.isClickable = true

        // Left Control -> Cross Discard Icon
        btnLeftControl.setImageResource(R.drawable.ic_discard)

        // Right Control -> Save to Gallery Icon
        btnRightControl.setImageResource(R.drawable.ic_save_gallery)
    }

    private fun resetToCameraState() {
        isPhotoCaptured = false
        capturedBitmap = null
        imgCapturedPreview.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE

        // Done Button -> Greyed out
        btnDoneCapture.setTextColor(Color.parseColor("#9E9E9E"))
        btnDoneCapture.isClickable = false

        // Left Control -> Location Icon
        btnLeftControl.setImageResource(R.drawable.ic_location)

        // Right Control -> Flip Camera Icon
        btnRightControl.setImageResource(R.drawable.ic_flip_camera)
    }

    private fun toggleLocation() {
        if (!isLocationEnabled) {
            fetchLocation()
        } else {
            isLocationEnabled = false
            tvOverlayLocation.visibility = View.GONE
        }
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocationText = String.format(
                        Locale.US,
                        "Lat: %.4f, Long: %.4f",
                        location.latitude,
                        location.longitude
                    )
                } else {
                    currentLocationText = "Lat: 28.6139, Long: 77.2090" // Fallback mock coordinates
                }
                tvOverlayLocation.text = currentLocationText
                tvOverlayLocation.visibility = View.VISIBLE
                isLocationEnabled = true
            }.addOnFailureListener {
                currentLocationText = "Lat: 28.6139, Long: 77.2090"
                tvOverlayLocation.text = currentLocationText
                tvOverlayLocation.visibility = View.VISIBLE
                isLocationEnabled = true
            }
        } else {
            currentLocationText = "Lat: 28.6139, Long: 77.2090"
            tvOverlayLocation.text = currentLocationText
            tvOverlayLocation.visibility = View.VISIBLE
            isLocationEnabled = true
        }
    }

    private fun getCurrentIstTime(): String {
        val sdf = SimpleDateFormat("hh:mm:ss a 'IST'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    private fun resolveActiveInspectionId(): String {
        return try {
            val jsonString = requireContext().assets.open("inspections.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                jsonArray.getJSONObject(0).getString("id")
            } else {
                "INS-2026-1042"
            }
        } catch (e: Exception) {
            "INS-2026-1042"
        }
    }

    private fun applyWatermarks(original: Bitmap): Bitmap {
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = (mutableBitmap.height * 0.025f).coerceAtLeast(36f)
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val timeStamp = getCurrentIstTime()
        val textId = "ID: $currentInspectionId"
        val textTime = "TIME: $timeStamp"
        val textLoc = if (isLocationEnabled) "LOC: $currentLocationText" else ""

        val margin = (mutableBitmap.height * 0.03f).coerceAtLeast(30f)
        val lineSpacing = paint.textSize * 1.3f

        // Top Left: Inspection ID
        canvas.drawText(textId, margin, margin + paint.textSize, paint)

        // Top Right: Time
        val timeBounds = Rect()
        paint.getTextBounds(textTime, 0, textTime.length, timeBounds)
        canvas.drawText(textTime, mutableBitmap.width - timeBounds.width() - margin, margin + paint.textSize, paint)

        // Bottom Left: Location (if enabled)
        if (textLoc.isNotEmpty()) {
            canvas.drawText(textLoc, margin, mutableBitmap.height - margin, paint)
        }

        return mutableBitmap
    }

    private fun saveWatermarkedImageToCache(bitmap: Bitmap): File {
        val file = File(requireContext().cacheDir, "evidence_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    private fun saveWatermarkedImageToGallery(bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/InspectionEvidence")
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = File(requireContext().getExternalFilesDir(null), "InspectionEvidence")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val imageFile = File(imagesDir, filename)
                fos = FileOutputStream(imageFile)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                Toast.makeText(requireContext(), "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save to Gallery", Toast.LENGTH_SHORT).show()
        }
    }
}
