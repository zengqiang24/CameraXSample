package com.example.cameraxsample.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.getOutputDirectory
import com.example.cameraxsample.jni.GLJNI
import kotlinx.android.synthetic.main.camerax_activity_main.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

class CameraXActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.cameraxsample.R.layout.camerax_activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        rg_cameras.setOnCheckedChangeListener { group, checkId ->
            Log.d(TAG, "user tap camera id = $checkId")

        }
        Log.d(TAG, "onCreate() called with: rg_cameras = ${rg_cameras.childCount}")
        outputDirectory = getOutputDirectory(
            this.applicationContext,
            resources.getString(com.example.cameraxsample.R.string.app_name)
        )

        //use for image Analyzer
        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.d("qiang", "${GLJNI.helloJNI()}")
    }

    private fun initCamerasSwitchMenu(cameraIdList: List<String>, selectedCameraId: String) {
        for (id in cameraIdList) {
            val rb = RadioButton(this)
            rb.text = "$id"
            rb.setTextColor(resources.getColor(com.example.cameraxsample.R.color.design_default_color_primary))
            rb.id = id.toInt()
            rg_cameras.addView(rb)
        }
        rg_cameras.check((selectedCameraId.toInt()))
    }

    //switch camera than updateCameraResolutionList
    private fun configCamerasResolution(selectedCameraId: Int) {

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun startCamera() {
        @RequiresApi(Build.VERSION_CODES.Q)
        fun selectExternalOrBestCamera(
            provider: ProcessCameraProvider,
            selectedCameraId: String
        ): CameraSelector {
            Log.d(TAG, "supported cameras size = ${provider.availableCameraInfos.size}")
            val cam2Infos = provider.availableCameraInfos.map {
                val camera2Info = Camera2CameraInfo.from(it)
                for (value in camera2Info.cameraCharacteristicsMap.values) {
                    for (k in value.keys) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Log.d(
                                TAG,
                                "key = $k value= ${
                                    value.get(k).toString()
                                }                          value.physicalCameraIds= ${
                                    value.physicalCameraIds
                                }"
                            )
                        }
                    }
                }
            }
//                .sortedByDescending {
//                // HARDWARE_LEVEL is Int type, with the order of:
//                // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
//                it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
//            }

            return when {
                cam2Infos.isNotEmpty() -> {
                    CameraSelector.Builder()
                        .addCameraFilter {
                            it.filter { camInfo ->
                                // cam2Infos[0] is either EXTERNAL or best built-in camera
                                val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
                                thisCamId == selectedCameraId
                            }
                        }.build()
                }
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(2560, 1440))
                .build()

            //Add image reader target
            val imageAnalyzer = ImageAnalysis.Builder()
//                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetResolution(Size(2560, 1440))
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
            // Select back camera as a default
            //    Camera@160b454[id=11]                        UNKNOWN
            //    Camera@3e7d742[id=10]                        UNKNOWN
            //    Camera@aa70e3e[id=12]                        UNKNOWN
            //    Camera@25ee592[id=8]                         OPENING
            //    Camera@22ebe30[id=2]                         UNKNOWN
            //    Camera@47b6865[id=3]                         UNKNOWN
            //    Camera@7e6ee78[id=9]
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//            val cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK).addCameraFilter {
//                    it.filter { cameraInfo ->
//                        val c = cameraInfo as Camera2CameraInfoImpl
//                        (c.cameraId == "12")
//                    }
//                }.build()

            val cameInfoList = cameraProvider.availableCameraInfos.map { cameraInfo ->
                Camera2CameraInfo.from(cameraInfo)
            }
            initCamerasSwitchMenu(cameInfoList.map { it.cameraId }, Camera.FRONT_CAMERA.ID)
            rg_cameras.setOnCheckedChangeListener { _, id ->
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    selectExternalOrBestCamera(cameraProvider, id.toString()),
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            }

            val cameraSelector =
                selectExternalOrBestCamera(cameraProvider, Camera.FRONT_CAMERA.ID)
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

//    private fun getOutputDirectory(): File {
//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(com.example.cameraxsample.R.string.app_name)).apply { mkdirs() }
//        }
//        return if (mediaDir != null && mediaDir.exists())
//            mediaDir else filesDir
//    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        //process in thread pool
        override fun analyze(image: ImageProxy) {
            Log.d(TAG, "analyze() called with: image = ${image.width} rect = ${image.cropRect}")
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
}