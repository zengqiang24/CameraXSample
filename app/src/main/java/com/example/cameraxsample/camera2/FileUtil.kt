package com.example.cameraxsample.camera2

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.cameraxsample.camerax.CameraXActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FileUtil"
const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    Log.d(
        TAG,
        "saveBitmap() called with: context = $context, bitmap = $bitmap, fileName = $fileName"
    )
    val file = File(
        getOutputDirectory(context, fileName),
        SimpleDateFormat(
            CameraXActivity.FILENAME_FORMAT, Locale.US
        ).format(System.currentTimeMillis()) + ".jpg"
    )
    // Create folder name
    val output = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    output.flush()
    output.close()

    if (bitmap != null && !bitmap.isRecycled)
        bitmap.recycle()
}


/**
 * get image save path
 */
fun getOutputDirectory(context: Context, fileName: String): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, fileName).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else context.filesDir
}