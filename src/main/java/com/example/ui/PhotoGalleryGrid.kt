package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotoGalleryGrid(
  viewModel: GalleryViewModel,
  theme: ThemeColors,
  language: AppLanguage,
  modifier: Modifier = Modifier,
  activePartnerProfile: String = "Fatou",
  onLogExpenseFromPrescription: (String, String) -> Unit = { _, _ -> }
) {
  val context = LocalContext.current
  val isFrench = language == AppLanguage.FR

  LaunchedEffect(Unit) {
    viewModel.loadPhotos(context)
  }

  val photos by viewModel.photos.collectAsState()
  var selectedCategory by remember { mutableStateOf("ALL") }
  var selectedPhoto by remember { mutableStateOf<GalleryPhotoItem?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  var titleInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("🤰 Ventre & Souvenirs") }

  val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
    if (bitmap != null) {
      val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
      viewModel.addPhoto(
        context = context,
        title = titleInput.ifBlank { if (isFrench) "Photo Caméra" else "Camera Photo" },
        subtitle = subtitle,
        bitmap = bitmap,
        uri = null,
        category = categoryInput,
        addedBy = activePartnerProfile
      )
      showAddDialog = false
      Toast.makeText(context, if (isFrench) "📷 Photo enregistrée !" else "📷 Photo saved!", Toast.LENGTH_SHORT).show()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      cameraLauncher.launch(null)
    } else {
      Toast.makeText(context, if (isFrench) "Permission caméra refusée" else "Camera permission denied", Toast.LENGTH_SHORT).show()
    }
  }

  val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) {
      val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
      viewModel.addPhoto(
        context = context,
        title = titleInput.ifBlank { if (isFrench) "Pièce jointe Galerie" else "Gallery Attachment" },
        subtitle = subtitle,
        bitmap = null,
        uri = uri,
        category = categoryInput,
        addedBy = activePartnerProfile
      )
      showAddDialog = false
      Toast.makeText(context, if (isFrench) "📁 Pièce jointe enregistrée !" else "📁 Attachment saved!", Toast.LENGTH_SHORT).show()
    }
  }

  val filterOptions = listOf(
    "ALL" to (if (isFrench) "Tous" else "All"),
    "👶 Échographies" to (if (isFrench) "👶 Échographies" else "👶 Ultrasounds"),
    "💊 Ordonnances" to (if (isFrench) "💊 Ordonnances" else "💊 Prescriptions"),
    "🤰 Ventre & Souvenirs" to (if (isFrench) "🤰 Souvenirs" else "🤰 Bump & Memories")
  )

  val filteredPhotos = if (selectedCategory == "ALL") {
    photos
  } else {
    photos.filter { it.category == selectedCategory }
  }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (isFrench) "Galerie & Album Maman & Bébé" else "Mom & Baby Photo Gallery",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (isFrench) "Chronologie visuelle et documents sécurisés" else "Visual timeline & secure documents",
          style = MaterialTheme.typography.bodySmall,
          color = theme.mutedText
        )
      }

      Button(
        onClick = { showAddDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (isFrench) "Ajouter" else "Add", color = Color.White, fontSize = 13.sp, maxLines = 1, softWrap = false)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Single-line horizontal scrollable category pills (no wrapping/truncation)
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(filterOptions) { (key, label) ->
        val isSelected = selectedCategory == key
        Surface(
          onClick = { selectedCategory = key },
          shape = RoundedCornerShape(20.dp),
          color = if (isSelected) theme.accent else theme.cardSurface,
          border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
          modifier = Modifier.height(36.dp)
        ) {
          Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) Color.White else theme.primaryText,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredPhotos.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(54.dp), tint = theme.mutedText.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (isFrench) "Aucune photo dans cette catégorie" else "No photos in this category",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.mutedText
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filteredPhotos, key = { it.id }) { photo ->
          PhotoGridCard(
            photo = photo,
            theme = theme,
            isFrench = isFrench,
            onClick = { selectedPhoto = photo },
            onDelete = { viewModel.deletePhoto(context, photo.id) },
            onLogExpense = { onLogExpenseFromPrescription(photo.title, "Prescriptions") }
          )
        }
      }
    }
  }

  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text(text = if (isFrench) "Ajouter un souvenir ou ordonnance" else "Add Memory, Scan or Prescription", color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text(if (isFrench) "Titre / Description" else "Title / Description") },
            placeholder = { Text("ex: Échographie T1 / Ordonnance Estrace") },
            modifier = Modifier.fillMaxWidth()
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("👶 Échographies", "💊 Ordonnances", "🤰 Ventre & Souvenirs")) { presetCat ->
              val sel = categoryInput == presetCat
              Surface(
                onClick = { categoryInput = presetCat },
                shape = RoundedCornerShape(16.dp),
                color = if (sel) theme.accent else theme.background,
                border = BorderStroke(1.dp, if (sel) theme.accent else theme.border)
              ) {
                Text(
                  text = presetCat,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (sel) Color.White else theme.primaryText,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                  cameraLauncher.launch(null)
                } else {
                  cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isFrench) "Caméra" else "Camera", fontSize = 12.sp, color = Color.White, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
              onClick = {
                galleryPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              border = BorderStroke(1.dp, theme.accent)
            ) {
              Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isFrench) "Galerie" else "Gallery", fontSize = 12.sp, color = theme.primaryText, maxLines = 1, softWrap = false)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (titleInput.isNotBlank()) {
              val subtitle = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date())
              viewModel.addPhoto(
                context = context,
                title = titleInput,
                subtitle = subtitle,
                bitmap = null,
                uri = null,
                category = categoryInput,
                addedBy = activePartnerProfile
              )
              titleInput = ""
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(text = if (isFrench) "Enregistrer" else "Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text(text = if (isFrench) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  selectedPhoto?.let { photo ->
    val isPrescription = photo.category.contains("Ordonn") || photo.category.contains("Prescrip")
    AlertDialog(
      onDismissRequest = { selectedPhoto = null },
      title = { Text(text = photo.title, color = theme.primaryText) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(theme.background),
            contentAlignment = Alignment.Center
          ) {
            if (photo.filePath != null) {
              AsyncImage(
                model = File(photo.filePath),
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = theme.mutedText)
            }
          }
          Text(text = photo.subtitle, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
          Text(
            text = "${photo.category} • ${if (isFrench) "Ajouté par" else "Added by"} ${photo.addedBy}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = theme.accent
          )
          if (isPrescription) {
            Button(
              onClick = {
                selectedPhoto = null
                onLogExpenseFromPrescription(photo.title, "Prescriptions")
              },
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(if (isFrench) "Noter cette dépense" else "Log Expense", color = Color.White)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedPhoto = null }) {
          Text(text = if (isFrench) "Fermer" else "Close")
        }
      }
    )
  }
}

@Composable
private fun PhotoGridCard(
  photo: GalleryPhotoItem,
  theme: ThemeColors,
  isFrench: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  onLogExpense: () -> Unit
) {
  val isPrescription = photo.category.contains("Ordonn") || photo.category.contains("Prescrip")
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(if (isPrescription) 224.dp else 198.dp)
      .clickable { onClick() },
    shape = RoundedCornerShape(18.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border),
    shadowElevation = 2.dp
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(theme.background),
          contentAlignment = Alignment.Center
        ) {
          if (photo.filePath != null) {
            AsyncImage(
              model = File(photo.filePath),
              contentDescription = photo.title,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp), tint = theme.mutedText.copy(alpha = 0.6f))
          }
        }

        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = photo.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = theme.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${if (isFrench) "Ajouté par" else "Added by"} ${photo.addedBy}",
            style = MaterialTheme.typography.labelSmall,
            color = theme.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (isPrescription) {
            Spacer(modifier = Modifier.height(2.dp))
            OutlinedButton(
              onClick = onLogExpense,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .testTag("gallery_log_expense_${photo.id}")
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null, tint = theme.accent, modifier = Modifier.size(13.dp))
              Spacer(Modifier.width(4.dp))
              Text(
                text = if (isFrench) "Dépense" else "Log Expense",
                style = MaterialTheme.typography.labelSmall,
                color = theme.accent,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(32.dp)
          .background(Color.Black.copy(alpha = 0.42f), CircleShape)
      ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
      }
    }
  }
}
