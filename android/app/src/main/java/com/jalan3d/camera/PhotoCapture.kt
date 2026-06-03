package com.jalan3d.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper for capturing photos via the system camera.
 * Creates a temp file URI via FileProvider for use with TakePicture contract.
 */
object PhotoCapture {

    private const val PHOTO_DIR = "photos"

    /**
     * Create a temp URI for saving a photo from the camera.
     */
    fun createPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, PHOTO_DIR)
        dir.mkdirs()
        val file = File(dir, "jalan3d_${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
