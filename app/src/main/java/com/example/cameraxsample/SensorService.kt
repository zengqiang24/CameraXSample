package com.example.cameraxsample

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cameraxsample.camera2.Camera2Activity
import com.example.cameraxsample.common.Camera
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 *  传感器后台服务
 *  传感器包括： 相机传感器，定位等等。
 */
class SensorService : Service() {
    private var lastImageBitmap: Bitmap? = null
    private var cameraManager: CameraManager? = null
    private var workThread: Camera2Activity.CaptureHandlerThread? = null
    private var workHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var session: CameraCaptureSession? = null
    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
        //Configure camera context;
        workThread = Camera2Activity.CaptureHandlerThread("capture")
        workThread?.start()
        workThread?.let {
            workHandler = Handler(it.looper)
        }
        imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 1)
        imageReader?.setOnImageAvailableListener({ reader ->

            val image = reader.acquireNextImage()
            val format = image.format
            val width = image.width
            val height = image.height
            val timestamp = image.timestamp
            val out = ByteArrayOutputStream()
            // Create YUV image file
            //rez = getDataFromImage(image, COLOR_FormatI420);
            val byteBuf = imageToByteBuffer(image)
            val rez = byteBuf!!.array()
            val rect = image.cropRect
            val yuvImage = YuvImage(rez, ImageFormat.NV21, rect.width(), rect.height(), null)

            try {
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val imageBytes: ByteArray = out.toByteArray()
            // Display for the end user
            // Display for the end user
            lastImageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            Log.d(TAG, "lastImageBitmap: $lastImageBitmap")
            // Make sure we close the image
            image.close()
        }, workHandler)

        openCamera()
    }

    private fun openCamera() {
        cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        cameraManager?.openCamera(
            Camera.LEFT_CAMERA.ID,
            ContextCompat.getMainExecutor(this),
            CameraStateCallback()
        )
    }

    private inner class CameraStateCallback : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "onOpened() called with: camera = ${camera.id} ")
            val targets = listOf(imageReader?.surface)
            camera?.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        Log.d(
                            TAG,
                            "onConfigured() called with: cameraCaptureSession = $cameraCaptureSession"
                        )
                        // Do something with `session`
                        session = cameraCaptureSession

                        //preview
                        session?.let {
                            var req =
                                it.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            Log.d(
                                TAG,
                                "onConfigured() called imageReader?.surface = ${imageReader?.surface}"
                            )

                            if (imageReader?.surface != null) req.addTarget(imageReader?.surface!!)
                            session?.setRepeatingRequest(
                                req.build(),
                                object : CameraCaptureSession.CaptureCallback() {
                                    override fun onCaptureCompleted(
                                        session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult
                                    ) {
                                        super.onCaptureCompleted(session, request, result)
                                    }
                                },
                                workHandler
                            )
                        }
                    }

                    // Omitting for brevity...
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.d(
                            TAG,
                            "onConfigureFailed() called with: session = $session"
                        )
                    }
                }, null // set thread context for its callback handled
            )
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "onDisconnected() called with: camera = $camera")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "onError() called with: camera = $camera, error = $error")
        }

    }

    fun getLatestImage(): Bitmap? {
        return lastImageBitmap
    }

    inner class MyBinder : Binder() {
        val service: SensorService
            get() = this@SensorService
    }

    companion object {
        private const val TAG = "SensorService"
    }

    private fun imageToByteBuffer(image: Image): ByteBuffer? {
        val crop = image.cropRect
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val rowData = ByteArray(planes[0].rowStride)
        val bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
        val output = ByteBuffer.allocateDirect(bufferSize)
        var channelOffset = 0
        var outputStride = 0
        for (planeIndex in 0..2) {
            if (planeIndex == 0) {
                channelOffset = 0
                outputStride = 1
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1
                outputStride = 2
            } else if (planeIndex == 2) {
                channelOffset = width * height
                outputStride = 2
            }
            val buffer = planes[planeIndex].buffer
            val rowStride = planes[planeIndex].rowStride
            val pixelStride = planes[planeIndex].pixelStride
            val shift = if (planeIndex == 0) 0 else 1
            val widthShifted = width shr shift
            val heightShifted = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until heightShifted) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted
                    buffer[output.array(), channelOffset, length]
                    channelOffset += length
                } else {
                    length = (widthShifted - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until widthShifted) {
                        output.array()[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return output
    }
}