package com.example.cameraxsample.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.common.ExecutorsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean


class CameraHandler(
    private val context: Context,
    private val cameraId: String,
    private val vCameraRepository: VCameraRepository
) {
    private val isRecording: AtomicBoolean = AtomicBoolean(true)
    private var captureSession: CameraCaptureSession? = null
    private var previewConfig: AVMPreviewConfig? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var cameraSystemManager =
        context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    val imageReader: ImageReader by lazy {
        newImageReader()
    }

    private var imageDataThread: CameraStateManager.BackgroundThread? = null
    private var imageDataHandler: Handler? = null
    private var backgroundThread: CameraStateManager.BackgroundThread? = null
    private var backgroundHandler: Handler? = null
    private fun newImageReader(): ImageReader {
        return ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 1)
    }

    init {
        imageDataThread = CameraStateManager.BackgroundThread("preview-thread$cameraId")
        imageDataThread?.let {
            it.start()
            imageDataHandler = Handler(it.looper)
        }

        imageReader.setOnImageAvailableListener(
            {
                Log.d(TAG, "imageReader= $cameraId")
                if (isRecording.get()) {
                    Log.d(
                        TAG,
                        " imageReader id = $cameraId in imageReader, "
                    )
                    val image = it.acquireNextImage()
                    var bitmap = encodeFrame(image)
                     bitmap?.let {
                         addToCache(cameraId, bitmap)
                         CameraStateManager.lockCountDown()
                         image.close()
                     }
                } else {
                    it.close()
                }
            }, imageDataHandler
        )
    }

    /**
     * save  frame will block cpu
     */
    private fun encodeFrame(image: Image): Bitmap? {
        val timestamp: Long = image.timestamp
        Log.d(
            TAG,
            "[qiang111111111111111111111] called with: cameraId = $cameraId timestamp= $timestamp thread name = ${Thread.currentThread().name}"
        )

        val rez: ByteArray

        val out = ByteArrayOutputStream()
        val byteBuf: ByteBuffer = imageToByteBuffer(image)
        rez = byteBuf.array()

        val rect = image.cropRect
        val yuvImage = YuvImage(rez, ImageFormat.NV21, rect.width(), rect.height(), null)

        try {
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val imageBytes = out.toByteArray()
        // Display for the end user
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap == null) {
            Log.d(TAG, "saveFrame() called with: bitmap = $bitmap")
            image.close()
        }
        return bitmap
    }

    private fun addToCache(cameraId: String, bitmap: Bitmap) {
        Log.d(TAG, "addBitmap() called with: cameraId = $cameraId, bitmap = $bitmap")
        val sequenceId = CameraStateManager.getCurrentSequenceId()
        Log.d(TAG, "sequenceId = $sequenceId")
        var frame = CameraStateManager.frameQueue[sequenceId]
        var data: HashMap<String, Bitmap>?
        if (frame == null) {
            data = hashMapOf()
            frame = Frame(sequenceId, data)
            CameraStateManager.frameQueue[sequenceId] = frame
            CameraStateManager.sequenceIdToList.offer(sequenceId)
        } else {
            data = frame.bitmaps
        }
        data[cameraId] = bitmap
    }

    private fun imageToByteBuffer(image: Image): ByteBuffer {
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

    fun preview(previewConfig: AVMPreviewConfig) {
        previewConfig.surfaces.add(imageReader.surface)
        this.previewConfig = previewConfig
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraSystemManager.openCamera(
                cameraId,
                ExecutorsHelper.SINGLE_THREAD_POOL,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val sessionConfiguration = SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR,
                            previewConfig.surfaces.map {
                                OutputConfiguration(it)
                            },
                            ExecutorsHelper.SINGLE_THREAD_POOL,
                            object :
                                CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    val cameraCharacteristics =
                                        cameraSystemManager.getCameraCharacteristics(cameraId);
                                    val streamConfigurationMap =
                                        cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                                    var sizes =
                                        streamConfigurationMap?.getOutputSizes(ImageFormat.YUV_420_888)
                                    for (size in sizes!!) {
                                        Log.d(
                                            TAG,
                                            "cameraid = $cameraId cameraCharacteristics  called with: size width= ${size.width} height = ${size.height}"
                                        )
                                    }
                                    captureSession = session
                                    val request =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    request.addTarget(previewConfig.surfaces[0])

                                    val bui = request.build()
                                    session.setRepeatingRequest(
                                        bui,
                                        null,
                                        CameraStateManager.getBackgroundHandler()
                                    )
                                    Log.d(TAG, "onConfigured() called with: session = $session")
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.d(
                                        TAG,
                                        "onConfigureFailed() called with: session = $session"
                                    )
                                }

                            }
                        )
                        camera.createCaptureSession(sessionConfiguration)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.d(TAG, "onDisconnected() called with: camera = $camera")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.d(TAG, "onError() called with: camera = $camera, error = $error")
                    }

                })
        }

    }

    fun capture(onSuccess: (filePath: String) -> Unit, onFail: (e: Exception) -> Unit) {
        captureSession?.let { sesstion ->
            var request: CaptureRequest =
                sesstion.device?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).also {
                    it.addTarget(imageReader.surface)
                }.build()

            sesstion.capture(request, object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Log.d(
                        TAG,
                        "onCaptureCompleted() called with: session = $session, request = $request, result = $result Thread = ${Thread.currentThread().name}"
                    )

                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    onFail(java.lang.Exception(failure.toString()))
                }
            }, CameraStateManager.getBackgroundHandler())
        }
    }

    fun getCameraIds(): List<String> {
        return vCameraRepository.getCameraIds()
    }

    fun getSolutionSizes(id: String): Array<Size>? {
        return vCameraRepository.getCaptureResolutionList(id)
    }

    fun release() {
        if (captureSession?.device != null) {
            captureSession?.device?.close()
            captureSession?.device == null
        }

        if (captureSession != null) {
            captureSession?.close()
            captureSession == null
        }

        if (imageReader != null) {
            imageReader?.close()
            imageReader == null
        }

        Log.d(TAG, "camera release() called")
    }

    fun stopRecord() {
        isRecording.compareAndSet(true, false)
        Log.d(TAG, "stopRecord() called isRecording = ${isRecording.get()}")
    }

    fun startRecord() {
        isRecording.compareAndSet(false, true)
        Log.d(TAG, "startRecord() called isRecording = ${isRecording.get()}")
    }

    suspend fun deleteImages() {
        vCameraRepository.deleteAllImages(context)
    }

    fun onResume() {
        CameraStateManager.onResume()
        if (imageDataThread == null) {
            imageDataThread = CameraStateManager.BackgroundThread("preview-thread")
            imageDataThread?.let {
                it.start()
                imageDataHandler = Handler(it.looper)
            }

        }

//        if(backgroundThread == null) {
//            backgroundThread =
//                CameraStateManager.BackgroundThread("background-thread")
//            backgroundThread?.let {
//                it.start()
//                backgroundHandler = Handler(it.looper)
//            }
//        }
    }

    fun onPause() {
        imageDataThread?.quitSafely()
        imageDataThread = null
        imageDataHandler = null


        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    companion object {
        private const val TAG = "qiang VCameraManager"
    }

}