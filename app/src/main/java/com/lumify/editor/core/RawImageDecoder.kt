package com.lumify.editor.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

data class ImageMetadata(
    val format: String,
    val width: Int,
    val height: Int,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val focalLength: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val dateTime: String? = null,
    val isRaw: Boolean = false
)

object RawImageDecoder {

    private val RAW_EXTENSIONS = setOf(
        "dng", "cr2", "cr3", "nef", "arw", "orf", "rw2", "pef", "raf", "raw"
    )

    fun isRawFile(uri: Uri, context: Context): Boolean {
        val extension = getExtension(uri, context).lowercase(Locale.ROOT)
        return RAW_EXTENSIONS.contains(extension)
    }

    private fun getExtension(uri: Uri, context: Context): String {
        val path = uri.path ?: return ""
        val lastDot = path.lastIndexOf('.')
        if (lastDot != -1) {
            return path.substring(lastDot + 1)
        }
        val type = context.contentResolver.getType(uri) ?: return ""
        return type.substringAfter('/')
    }

    /**
     * Decodes any supported image (RAW, JPG, PNG, WEBP, etc.) with proper EXIF orientation
     * and downsampling to maintain silky smooth UI performance.
     */
    suspend fun decodeImage(
        context: Context,
        uri: Uri,
        maxTargetDimension: Int = 2560
    ): Pair<Bitmap, ImageMetadata> = withContext(Dispatchers.IO) {
        val extension = getExtension(uri, context).lowercase(Locale.ROOT)
        val isRaw = RAW_EXTENSIONS.contains(extension)

        // 1. Extract EXIF and determine orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        var metadata = ImageMetadata(
            format = extension.uppercase(Locale.ROOT).ifEmpty { "UNKNOWN" },
            width = 0,
            height = 0,
            isRaw = isRaw
        )

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                metadata = metadata.copy(
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL),
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "$it mm" },
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" },
                    shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "$it s" },
                    dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                )
            }
        } catch (e: Exception) {
            // Non-fatal if EXIF is missing or malformed
        }

        // 2. Decode bounds to determine sample size
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight

        var sampleSize = 1
        if (rawWidth > 0 && rawHeight > 0) {
            var w = rawWidth
            var h = rawHeight
            while (w > maxTargetDimension || h > maxTargetDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }
        }

        // 3. Full decode with sample size and ARGB_8888 for full 32-bit color fidelity
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        var decodedBitmap: Bitmap? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                decodedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: OutOfMemoryError) {
            // Fallback with larger sample size
            decodeOptions.inSampleSize = sampleSize * 2
            context.contentResolver.openInputStream(uri)?.use { stream ->
                decodedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }

        // If direct decode failed on proprietary RAW, attempt embedded preview/thumbnail extraction
        if (decodedBitmap == null && isRaw) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val thumbBytes = exif.thumbnailBytes
                if (thumbBytes != null) {
                    decodedBitmap = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size)
                }
            }
        }

        val baseBitmap = decodedBitmap
            ?: throw IllegalArgumentException("Unsupported or unreadable image file format ($extension).")

        // 4. Correct EXIF Orientation
        val correctedBitmap = applyExifOrientation(baseBitmap, orientation)

        val finalMetadata = metadata.copy(
            width = correctedBitmap.width,
            height = correctedBitmap.height
        )

        Pair(correctedBitmap, finalMetadata)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        val transformed = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (transformed != bitmap) {
            bitmap.recycle()
        }
        return transformed
    }
}
