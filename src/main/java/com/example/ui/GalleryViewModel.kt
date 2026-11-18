package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class GalleryPhotoItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val filePath: String?,
  val timestamp: Long,
  val category: String = "🤰 Ventre & Souvenirs",
  val addedBy: String = "Fatou"
)

class GalleryViewModel : ViewModel() {

  private val _photos = MutableStateFlow<List<GalleryPhotoItem>>(emptyList())
  val photos: StateFlow<List<GalleryPhotoItem>> = _photos.asStateFlow()

  private var isInitialized = false

  fun loadPhotos(context: Context) {
    if (isInitialized) return
    isInitialized = true

    val metaFile = File(context.filesDir, "gallery_metadata.json")
    val loadedItems = mutableListOf<GalleryPhotoItem>()

    if (metaFile.exists()) {
      runCatching {
        val jsonStr = metaFile.readText()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val id = obj.optString("id")
          val title = obj.optString("title")
          val subtitle = obj.optString("subtitle")
          val path = obj.optString("filePath")
          val ts = obj.optLong("timestamp", System.currentTimeMillis())
          val cat = obj.optString("category", "🤰 Ventre & Souvenirs")
          val by = obj.optString("addedBy", "Fatou")

          val validPath = if (path.isNotBlank() && File(path).exists()) path else null
          loadedItems.add(GalleryPhotoItem(id, title, subtitle, validPath, ts, cat, by))
        }
      }
    }

    if (loadedItems.isEmpty()) {
      // Default initial items
      loadedItems.addAll(
        listOf(
          GalleryPhotoItem(
            id = "p_1",
            title = "Échographie T1 — Clarté nucale (1.2 mm)",
            subtitle = "05 Nov 2026 • Dr. Brauer RMA",
            filePath = null,
            timestamp = System.currentTimeMillis() - 86400000L * 2,
            category = "👶 Échographies",
            addedBy = "Fatou"
          ),
          GalleryPhotoItem(
            id = "p_2",
            title = "Ordonnance Estrace 2 mg PO (AM & PM)",
            subtitle = "06 Nov 2026 • RMA New York",
            filePath = null,
            timestamp = System.currentTimeMillis() - 86400000L,
            category = "💊 Ordonnances",
            addedBy = "Partner"
          ),
          GalleryPhotoItem(
            id = "p_3",
            title = "Photo Ventre Semaine 16",
            subtitle = "06 Nov 2026 • Suivi Maman",
            filePath = null,
            timestamp = System.currentTimeMillis(),
            category = "🤰 Ventre & Souvenirs",
            addedBy = "Fatou"
          )
        )
      )
    }

    _photos.value = loadedItems.sortedByDescending { it.timestamp }
    persistMetadata(context)
  }

  private fun persistMetadata(context: Context) {
    runCatching {
      val arr = JSONArray()
      for (item in _photos.value) {
        val obj = JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("subtitle", item.subtitle)
          put("filePath", item.filePath ?: "")
          put("timestamp", item.timestamp)
          put("category", item.category)
          put("addedBy", item.addedBy)
        }
        arr.put(obj)
      }
      val metaFile = File(context.filesDir, "gallery_metadata.json")
      metaFile.writeText(arr.toString())
    }
  }

  fun addPhoto(
    context: Context,
    title: String,
    subtitle: String,
    bitmap: Bitmap?,
    uri: Uri?,
    category: String,
    addedBy: String = "Fatou"
  ) {
    var savedPath: String? = null
    runCatching {
      val filename = "gallery_${System.currentTimeMillis()}.jpg"
      val file = File(context.filesDir, filename)
      FileOutputStream(file).use { fos ->
        val sourceBitmap = if (bitmap != null) {
          bitmap
        } else if (uri != null) {
          val inputStream = context.contentResolver.openInputStream(uri)
          inputStream?.use { BitmapFactory.decodeStream(it) }
        } else {
          null
        }

        if (sourceBitmap != null) {
          // Canvas / Bitmap compression (max 1200px width/height, JPEG 0.82 quality)
          val maxDim = 1200
          var w = sourceBitmap.width
          var h = sourceBitmap.height
          if (w > maxDim || h > maxDim) {
            if (w > h) {
              h = Math.round((h.toFloat() * maxDim) / w)
              w = maxDim
            } else {
              w = Math.round((w.toFloat() * maxDim) / h)
              h = maxDim
            }
          }
          val scaled = Bitmap.createScaledBitmap(sourceBitmap, w, h, true)
          scaled.compress(Bitmap.CompressFormat.JPEG, 82, fos)
          if (scaled != sourceBitmap && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
          }
          savedPath = file.absolutePath
        } else if (uri != null) {
          val inputStream = context.contentResolver.openInputStream(uri)
          inputStream?.use { input ->
            input.copyTo(fos)
          }
          savedPath = file.absolutePath
        }
      }
    }

    val newItem = GalleryPhotoItem(
      id = "p_${System.currentTimeMillis()}",
      title = title,
      subtitle = subtitle,
      filePath = savedPath,
      timestamp = System.currentTimeMillis(),
      category = category,
      addedBy = addedBy
    )

    _photos.value = listOf(newItem) + _photos.value
    persistMetadata(context)
  }

  fun deletePhoto(context: Context, id: String) {
    val target = _photos.value.find { it.id == id }
    if (target?.filePath != null) {
      runCatching { File(target.filePath).delete() }
    }
    _photos.value = _photos.value.filter { it.id != id }
    persistMetadata(context)
  }
}
