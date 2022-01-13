package com.example.cameraxsample.common

import android.content.Context
import java.io.File

//CAMERA Devices ID depends on specific head unit.
enum class Camera(val ID: String){
    LEFT_CAMERA("8"), RIGHT_CAMERA("9"), FRONT_CAMERA("10"), BACK_CAMERA_("11");

    override fun toString(): String {
        return name
    }

}

fun main() {
    println(Camera.LEFT_CAMERA.ID)
}
//Camera@ff429d5[id=3]                         UNKNOWN
//Camera@ed43245[id=9]                         UNKNOWN
//Camera@b022ae1[id=2]                         CLOSED
//Camera@c8dcf3e[id=10]                        UNKNOWN
//Camera@dbc68bb[id=11]                        UNKNOWN
//Camera@2df2fb7[id=8]                         UNKNOWN
//Camera@79f2384[id=12]                        UNKNOWN

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