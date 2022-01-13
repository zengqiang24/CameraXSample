package com.example.cameraxsample.preview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cameraxsample.R
import kotlinx.android.synthetic.main.preview_layout.*

class BasePreviewFragment(private val cameraId: String) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = View.inflate(activity, R.layout.preview_layout,null)
        return  view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera(cameraId)
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
     fun startCamera(cameraId: String) {
        @RequiresApi(Build.VERSION_CODES.Q)
        fun selectExternalOrBestCamera(
            provider: ProcessCameraProvider,
            selectedCameraId: String
        ): CameraSelector {
            Log.d(TAG, "supported cameras size = ${provider.availableCameraInfos.size}")
            val cam2Infos = provider.availableCameraInfos.map {
                val camera2Info = Camera2CameraInfo.from(it)
//                Log.d(TAG, "cameraId: ${camera2Info.cameraId}: ")
                for (value in camera2Info.cameraCharacteristicsMap.values) {
                    for (k in value.keys) {
//                        Log.d(TAG, "key = $k value= ${value.get(k).toString()}")
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
                                thisCamId == selectedCameraId || thisCamId == "8" || thisCamId == "9"|| thisCamId == "11" || thisCamId == "12"
                            }
                        }.build()
                }
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

//            imageCapture = ImageCapture.Builder()
//                .build()

            //Add image reader target
//            val imageAnalyzer = ImageAnalysis.Builder()
////                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, CameraXActivity.LuminosityAnalyzer { luma ->
//                        Log.d( TAG, "Average luminosity: $luma")
//                    })
//                }
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
//            initCamerasSwitchMenu(cameInfoList.map { it.cameraId }, FRONT_CAMERA_ID.toInt())
//            rg_cameras.setOnCheckedChangeListener { _, id ->
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    selectExternalOrBestCamera(cameraProvider, id.toString()),
//                    preview,
//                    imageCapture,
//                    imageAnalyzer
//                )
//            }

            try {
                // Unbind use cases before rebinding

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    selectExternalOrBestCamera(cameraProvider,cameraId),
                    preview,
//                    imageCapture,
//                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    companion object {
        private const val TAG = "BasePreviewFragment"
        fun createFragment(cameraId:  String): BasePreviewFragment{
            return BasePreviewFragment(cameraId)
        }
    }
}