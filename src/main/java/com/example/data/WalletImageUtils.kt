package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Handles client-side image compression (max 1200px width, WebP/JPEG at 80% quality),
 * EXIF orientation correction, Base64 encoding for cross-device Firestore sync,
 * and local disk file caching for instant offline-first rendering.
 */
object WalletImageUtils {

  private const val TAG = "WalletImageUtils"
  private const val MAX_WIDTH_PX = 1200
  private const val MAX_HEIGHT_PX = 1200
  private const val TARGET_QUALITY = 80
  private const val MAX_RAW_BYTES = 240_000

  fun createTempCameraCaptureUri(context: Context): Uri {
    val captureDir = File(context.cacheDir, "wallet_captures").apply { mkdirs() }
    val photoFile = File(captureDir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      photoFile
    )
  }

  suspend fun compressAndEncodeUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }
      val origW = boundsOptions.outWidth
      val origH = boundsOptions.outHeight
      if (origW <= 0 || origH <= 0) return@withContext null

      var sampleSize = 1
      while ((origW / sampleSize) > MAX_WIDTH_PX * 2 || (origH / sampleSize) > MAX_HEIGHT_PX * 2) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return@withContext null

      val rotatedBitmap = applyExifRotationIfNeeded(context, uri, sampledBitmap)
      val scaledBitmap = scaleToMaxBounds(rotatedBitmap, MAX_WIDTH_PX, MAX_HEIGHT_PX)
      if (rotatedBitmap !== sampledBitmap) {
        sampledBitmap.recycle()
      }
      if (scaledBitmap !== rotatedBitmap) {
        rotatedBitmap.recycle()
      }

      val compressedBytes = compressWithinBudget(scaledBitmap)
      scaledBitmap.recycle()
      if (compressedBytes.isEmpty()) null
      else Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to compress card image from Uri", e)
      null
    }
  }

  private fun applyExifRotationIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
      val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
      } ?: ExifInterface.ORIENTATION_NORMAL

      val matrix = Matrix()
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return bitmap
      }
      Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (_: Exception) {
      bitmap
    }
  }

  private fun scaleToMaxBounds(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxWidth && h <= maxHeight) return bitmap

    val scale = minOf(maxWidth.toFloat() / w.toFloat(), maxHeight.toFloat() / h.toFloat())
    val targetW = (w * scale).toInt().coerceAtLeast(1)
    val targetH = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }

  private fun compressWithinBudget(bitmap: Bitmap): ByteArray {
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      Bitmap.CompressFormat.JPEG
    }

    var currentBitmap = bitmap
    var didCreateIntermediate = false
    val qualities = listOf(TARGET_QUALITY, 70, 58, 45)
    var bestBytes = ByteArray(0)

    for (scaleStep in 0..2) {
      for (quality in qualities) {
        val out = ByteArrayOutputStream()
        currentBitmap.compress(format, quality, out)
        bestBytes = out.toByteArray()
        if (bestBytes.size <= MAX_RAW_BYTES) {
          if (didCreateIntermediate && currentBitmap !== bitmap) {
            currentBitmap.recycle()
          }
          return bestBytes
        }
      }
      val nextW = (currentBitmap.width * 0.78f).toInt().coerceAtLeast(320)
      val nextH = (currentBitmap.height * 0.78f).toInt().coerceAtLeast(240)
      val resized = Bitmap.createScaledBitmap(currentBitmap, nextW, nextH, true)
      if (didCreateIntermediate && currentBitmap !== bitmap) {
        currentBitmap.recycle()
      }
      currentBitmap = resized
      didCreateIntermediate = true
    }

    if (didCreateIntermediate && currentBitmap !== bitmap) {
      currentBitmap.recycle()
    }
    return bestBytes
  }

  private fun cacheFileFor(context: Context, userId: String, cardId: String, side: String): File {
    val safeUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default" }
    val safeCardId = cardId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "card" }
    val dir = File(context.filesDir, "wallet_cache/$safeUid").apply { mkdirs() }
    return File(dir, "${safeCardId}_${side}.img")
  }

  suspend fun decodeAndCacheCardBitmap(
    context: Context,
    userId: String,
    cardId: String,
    side: String,
    base64: String
  ): Bitmap? = withContext(Dispatchers.IO) {
    val cacheFile = cacheFileFor(context, userId, cardId, side)
    if (base64.isBlank()) {
      if (cacheFile.exists()) cacheFile.delete()
      return@withContext null
    }

    return@withContext try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      if (!cacheFile.exists() || cacheFile.length().toInt() != bytes.size) {
        FileOutputStream(cacheFile).use { it.write(bytes) }
      }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
      Log.w(TAG, "Falling back to local disk cache for $cardId/$side", e)
      if (cacheFile.exists()) {
        runCatching { BitmapFactory.decodeFile(cacheFile.absolutePath) }.getOrNull()
      } else {
        null
      }
    }
  }

  suspend fun decodePreviewBitmap(base64: String): Bitmap? = withContext(Dispatchers.IO) {
    if (base64.isBlank()) return@withContext null
    return@withContext runCatching {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
  }
}
