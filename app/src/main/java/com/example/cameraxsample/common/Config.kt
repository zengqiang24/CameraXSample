package com.example.cameraxsample.common

//CAMERA Devices ID depends on specific head unit.
enum class Camera(val ID: String) {
    LEFT_CAMERA("8"), RIGHT_CAMERA("9"), FRONT_CAMERA("10"), BACK_CAMERA("11"), FULL_CAMERA("12");

    override fun toString(): String {
        return name
    }
}

fun getCameraName(id: String): String {
    return when (id) {
        "8" -> Camera.LEFT_CAMERA.toString()
        "9" -> Camera.RIGHT_CAMERA.toString()
        "10" -> Camera.FRONT_CAMERA.toString()
        "11" -> Camera.BACK_CAMERA.toString()
        "12" -> Camera.FULL_CAMERA.toString()
        else -> "unknown"
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

