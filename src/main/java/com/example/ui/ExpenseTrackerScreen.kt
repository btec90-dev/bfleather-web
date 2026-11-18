package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AiAssistantService
import com.example.data.ExportCategory
import com.example.data.ExportCenterUtils
import com.example.data.SyncedExpenseRecord
import com.example.data.SyncedUserProfile
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import com.example.ui.theme.getCategoryBadgeColors
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

data class ExpensePrefill(
  val title: String = "",
  val category: String = "Prescriptions",
  val notes: String = "",
  val attachmentTitle: String = "",
  val receiptBase64: String = ""
)

@Composable
fun ExpenseTrackerScreen(
  expenses: List<SyncedExpenseRecord>,
  profile: SyncedUserProfile = SyncedUserProfile(),
  activePartnerProfile: String = "Fatou",
  prefilledExpense: ExpensePrefill? = null,
  onClearPrefill: () -> Unit = {},
  onSaveExpense: (SyncedExpenseRecord) -> Unit,
  onDeleteExpense: (String) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR
  val scope = rememberCoroutineScope()
  var isScanningReceipt by remember { mutableStateOf(false) }

  var selectedCategory by remember { mutableStateOf("ALL") }
  var selectedCurrencyFilter by remember { mutableStateOf("ALL") }
  var searchQuery by remember { mutableStateOf("") }

  var showAddEditDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var editingExpense by remember { mutableStateOf<SyncedExpenseRecord?>(null) }
  var previewReceiptExpense by remember { mutableStateOf<SyncedExpenseRecord?>(null) }

  var titleInput by remember { mutableStateOf("") }
  var amountInput by remember { mutableStateOf("") }
  var insurancePaidInput by remember { mutableStateOf("") }
  var totalBillInput by remember { mutableStateOf("") }
  var currencyInput by remember { mutableStateOf("USD") }
  var categoryInput by remember { mutableStateOf("Medical") }
  var phaseInput by remember { mutableStateOf("IVF / Clinic") }
  var paymentStatusInput by remember { mutableStateOf("Out-of-Pocket") }
  var dateInput by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())) }
  var notesInput by remember { mutableStateOf("") }
  var attachmentTitleInput by remember { mutableStateOf("") }
  var receiptBase64Input by remember { mutableStateOf("") }

  val categoryOptions = listOf(
    "Medical" to (if (isFr) "Médical & Clinique" else "Medical"),
    "Baby" to (if (isFr) "Bébé" else "Baby"),
    "Prescriptions" to (if (isFr) "Ordonnances" else "Prescriptions"),
    "Baby Stuff" to (if (isFr) "Affaires Bébé" else "Baby Stuff"),
    "For the Mom" to (if (isFr) "Pour Maman" else "For the Mom"),
    "Others" to (if (isFr) "Autres" else "Others")
  )

  val phaseOptions = listOf(
    "IVF / Clinic" to (if (isFr) "FIV / Clinique" else "IVF / Clinic"),
    "1st Trimester" to (if (isFr) "1er Trimestre" else "1st Trimester"),
    "2nd Trimester" to (if (isFr) "2e Trimestre" else "2nd Trimester"),
    "3rd Trimester" to (if (isFr) "3e Trimestre" else "3rd Trimester"),
    "Baby Gear" to (if (isFr) "Équipement Bébé" else "Baby Gear")
  )

  val statusOptions = listOf(
    "Out-of-Pocket" to (if (isFr) "Reste à charge" else "Out-of-Pocket"),
    "Insurance Copay" to (if (isFr) "Ticket modérateur" else "Insurance Copay"),
    "Reimbursed" to (if (isFr) "Remboursé" else "Reimbursed")
  )

  // Open prefilled modal automatically when arriving from Digital Wallet or Ordonnances shortcut
  LaunchedEffect(prefilledExpense) {
    if (prefilledExpense != null) {
      editingExpense = null
      titleInput = prefilledExpense.title
      amountInput = ""
      currencyInput = "USD"
      categoryInput = prefilledExpense.category
      phaseInput = "IVF / Clinic"
      paymentStatusInput = "Out-of-Pocket"
      dateInput = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
      notesInput = prefilledExpense.notes
      attachmentTitleInput = prefilledExpense.attachmentTitle
      receiptBase64Input = prefilledExpense.receiptBase64
      showAddEditDialog = true
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (bytes.size <= 20_000_000) {
              // 3. Keep the file attached immediately upon selection
              receiptBase64Input = b64
              attachmentTitleInput = if (isFr) "Reçu photo" else "Photo receipt"
              showAddEditDialog = true

              isScanningReceipt = true
              try {
                val extracted = AiAssistantService.parseReceiptWithGemini(bytes, mimeType, isFr)
                titleInput = extracted.title
                amountInput = if (extracted.amount > 0.0) extracted.amount.toString() else ""
                insurancePaidInput = if (extracted.insurancePaid > 0.0) extracted.insurancePaid.toString() else ""
                totalBillInput = if (extracted.totalBill > 0.0) extracted.totalBill.toString() else ""
                dateInput = extracted.dateStr
                categoryInput = extracted.category
                if (extracted.paymentMethod.isNotBlank()) {
                  notesInput = if (isFr) "Mode de paiement: ${extracted.paymentMethod}" else "Payment method: ${extracted.paymentMethod}"
                }
                Toast.makeText(context, if (isFr) "Photo analysée par Gemini !" else "Photo scanned by Gemini!", Toast.LENGTH_SHORT).show()
              } catch (e: Exception) {
                Log.e("ExpenseTracker", "Photo scan extraction error", e)
                Toast.makeText(context, if (isFr) "Photo jointe (saisie manuelle disponible)" else "Photo attached (manual entry available)", Toast.LENGTH_SHORT).show()
              } finally {
                isScanningReceipt = false
              }
            } else {
              Toast.makeText(context, if (isFr) "Image trop grande (>20MB)" else "Image too large (>20MB)", Toast.LENGTH_SHORT).show()
            }
          }
        }.onFailure { e ->
          Log.e("ExpenseTracker", "Photo open error", e)
          Toast.makeText(context, if (isFr) "Erreur de lecture de l'image" else "Photo read error", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (bytes.size <= 20_000_000) {
              // 3. Keep the PDF attached immediately upon selection
              receiptBase64Input = b64
              attachmentTitleInput = if (mimeType.contains("pdf", ignoreCase = true)) {
                if (isFr) "Facture / Reçu PDF" else "Medical Bill PDF"
              } else {
                if (isFr) "Reçu joint" else "Attached Receipt"
              }
              showAddEditDialog = true

              isScanningReceipt = true
              try {
                val extracted = AiAssistantService.parseReceiptWithGemini(bytes, mimeType, isFr)
                titleInput = extracted.title
                amountInput = if (extracted.amount > 0.0) extracted.amount.toString() else ""
                insurancePaidInput = if (extracted.insurancePaid > 0.0) extracted.insurancePaid.toString() else ""
                totalBillInput = if (extracted.totalBill > 0.0) extracted.totalBill.toString() else ""
                dateInput = extracted.dateStr
                categoryInput = extracted.category
                if (extracted.paymentMethod.isNotBlank()) {
                  notesInput = if (extracted.paymentMethod.isNotBlank()) {
                    if (isFr) "Mode de paiement: ${extracted.paymentMethod}" else "Payment method: ${extracted.paymentMethod}"
                  } else {
                    notesInput
                  }
                }
                Toast.makeText(context, if (isFr) "PDF analysé par Gemini avec succès !" else "PDF scanned by Gemini successfully!", Toast.LENGTH_SHORT).show()
              } catch (e: Exception) {
                Log.e("ExpenseTracker", "PDF scan extraction error", e)
                Toast.makeText(context, if (isFr) "PDF joint avec succès (saisie manuelle disponible)" else "PDF attached successfully (manual entry available)", Toast.LENGTH_SHORT).show()
              } finally {
                isScanningReceipt = false
              }
            } else {
              Toast.makeText(context, if (isFr) "Fichier PDF trop volumineux (>20MB)" else "PDF file too large (>20MB)", Toast.LENGTH_SHORT).show()
            }
          }
        }.onFailure { e ->
          Log.e("ExpenseTracker", "PDF open error", e)
          Toast.makeText(context, if (isFr) "Erreur de lecture du fichier PDF" else "PDF file read error", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val medicalExpenses = remember(expenses) {
    expenses.filter {
      it.category.equals("Medical", true) ||
      it.category.equals("Prescriptions", true) ||
      it.category.equals("Medical & Hospital", true) ||
      it.category.equals("Tests & Ultrasounds", true)
    }
  }

  val babyExpenses = remember(expenses) {
    expenses.filter {
      it.category.equals("Baby", true) ||
      it.category.equals("Baby Stuff", true) ||
      it.category.equals("Baby Gear", true) ||
      it.category.contains("Baby", true)
    }
  }

  val totalMedicalOutOfPocket = medicalExpenses.sumOf { convertToUsd(it.amount, it.currency) }
  val totalMedicalInsurancePaid = medicalExpenses.sumOf { convertToUsd(it.insurancePaid, it.currency) }
  val totalMedicalBilled = medicalExpenses.sumOf { convertToUsd(it.totalBill, it.currency) }

  val totalBabySpending = babyExpenses.sumOf { convertToUsd(it.amount, it.currency) }
  val combinedOutOfPocketTotal = totalMedicalOutOfPocket + totalBabySpending

  val filteredExpenses = remember(expenses, selectedCategory, selectedCurrencyFilter, searchQuery) {
    expenses.filter { exp ->
      val catMatch = when (selectedCategory) {
        "ALL" -> true
        "Medical" -> exp.category.equals("Medical", true) || exp.category.equals("Prescriptions", true) || exp.category.equals("Medical & Hospital", true) || exp.category.equals("Tests & Ultrasounds", true)
        "Baby" -> exp.category.equals("Baby", true) || exp.category.equals("Baby Stuff", true) || exp.category.equals("Baby Gear", true) || exp.category.contains("Baby", true)
        else -> exp.category.equals(selectedCategory, ignoreCase = true)
      }
      val curMatch = selectedCurrencyFilter == "ALL" || exp.currency.equals(selectedCurrencyFilter, ignoreCase = true)
      val qMatch = searchQuery.isBlank() ||
        exp.title.contains(searchQuery, ignoreCase = true) ||
        exp.notes.contains(searchQuery, ignoreCase = true) ||
        exp.category.contains(searchQuery, ignoreCase = true) ||
        exp.addedBy.contains(searchQuery, ignoreCase = true)
      catMatch && curMatch && qMatch
    }
  }



  val activeCur = if (selectedCurrencyFilter == "ALL") "USD" else selectedCurrencyFilter
  val currencySubset = remember(expenses, selectedCurrencyFilter) {
    if (selectedCurrencyFilter == "ALL") expenses else expenses.filter { it.currency.equals(selectedCurrencyFilter, ignoreCase = true) }
  }
  val grossTotal = if (selectedCurrencyFilter == "ALL") {
    currencySubset.sumOf { convertToUsd(it.amount, it.currency) }
  } else {
    currencySubset.sumOf { it.amount }
  }
  val reimbursedTotal = if (selectedCurrencyFilter == "ALL") {
    currencySubset.filter { it.paymentStatus == "Reimbursed" }.sumOf { convertToUsd(it.amount, it.currency) }
  } else {
    currencySubset.filter { it.paymentStatus == "Reimbursed" }.sumOf { it.amount }
  }
  val netOutOfPocket = (grossTotal - reimbursedTotal).coerceAtLeast(0.0)



  SelectionContainer {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(theme.background),
      contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 1. Header & Import / Add Buttons Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isFr) "Dépenses Grossesse & FIV" else "Pregnancy & IVF Expenses",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Text(
              text = if (isFr) "Suivi multi-devises & partagé" else "Multi-currency shared ledger",
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText,
              maxLines = 1
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = { documentPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
              modifier = Modifier.heightIn(min = 36.dp),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
              border = BorderStroke(1.dp, theme.accent)
            ) {
              Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = theme.accent)
              Spacer(modifier = Modifier.width(3.dp))
              Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.accent)
            }

            Button(
              onClick = {
                editingExpense = null
                titleInput = ""
                amountInput = ""
                currencyInput = if (selectedCurrencyFilter == "ALL") "USD" else selectedCurrencyFilter
                categoryInput = if (selectedCategory == "ALL") "Prescriptions" else selectedCategory
                phaseInput = "IVF / Clinic"
                paymentStatusInput = "Out-of-Pocket"
                dateInput = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
                notesInput = ""
                attachmentTitleInput = ""
                receiptBase64Input = ""
                showAddEditDialog = true
              },
              modifier = Modifier.testTag("add_expense_btn"),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isFr) "Ajouter" else "Add",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          }
        }
      }

      // 1.5. Medical & Baby Summary Dashboard Cards
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Medical Summary Card
            val isMedicalSelected = selectedCategory.equals("Medical", true) || selectedCategory.equals("Prescriptions", true)
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedCategory = if (isMedicalSelected) "ALL" else "Medical"
                },
              shape = RoundedCornerShape(16.dp),
              color = if (isMedicalSelected) theme.accent.copy(alpha = 0.15f) else theme.cardSurface,
              border = BorderStroke(1.dp, if (isMedicalSelected) theme.accent else theme.border),
              shadowElevation = 1.dp
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.LocalHospital, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                  Text(
                    text = if (isFr) "Médical" else "Medical",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                }
                Text(
                  text = "${if (isFr) "Payé" else "Out-of-Pocket"}: ${formatMoney(totalMedicalOutOfPocket, activeCur)}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
                Text(
                  text = "${if (isFr) "Assurance" else "Insurance"}: ${formatMoney(totalMedicalInsurancePaid, activeCur)}",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 11.sp,
                  color = Color(0xFF15803D)
                )
                Text(
                  text = "${if (isFr) "Facturé" else "Billed"}: ${formatMoney(totalMedicalBilled, activeCur)}",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 11.sp,
                  color = theme.mutedText
                )
              }
            }

            // Baby Summary Card
            val isBabySelected = selectedCategory.equals("Baby", true) || selectedCategory.equals("Baby Stuff", true)
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedCategory = if (isBabySelected) "ALL" else "Baby"
                },
              shape = RoundedCornerShape(16.dp),
              color = if (isBabySelected) theme.accent.copy(alpha = 0.15f) else theme.cardSurface,
              border = BorderStroke(1.dp, if (isBabySelected) theme.accent else theme.border),
              shadowElevation = 1.dp
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.ChildFriendly, contentDescription = null, tint = theme.accent, modifier = Modifier.size(15.dp))
                  Text(
                    text = if (isFr) "Bébé" else "Baby",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                  )
                }
                Text(
                  text = if (isFr) "Dépenses Bébé :" else "Baby Spending:",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = formatMoney(totalBabySpending, activeCur),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
              }
            }
          }

          // Combined Out-of-Pocket Grand Total & Filter Reset
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (isFr) "Total Combiné Out-of-Pocket" else "Combined Out-of-Pocket Total",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = formatMoney(combinedOutOfPocketTotal, activeCur),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
              }
              if (selectedCategory != "ALL") {
                OutlinedButton(
                  onClick = { selectedCategory = "ALL" },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.heightIn(min = 30.dp)
                ) {
                  Text(if (isFr) "Toutes les dépenses" else "All Expenses", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                }
              }
            }
          }
        }
      }

      // 2. Compact Analytics Summary Card
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border),
          shadowElevation = 1.dp
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (isFr) "Reste à Charge Net ($activeCur)" else "Net Out-of-Pocket ($activeCur)",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = formatMoney(netOutOfPocket, activeCur),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.accent
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                  onClick = {
                    val file = ExportCenterUtils.generateCsvReport(
                      context = context,
                      categories = setOf(ExportCategory.EXPENSES),
                      protocolSteps = emptyList(),
                      expenses = filteredExpenses,
                      appointments = emptyList(),
                      tasks = emptyList(),
                      notes = emptyList(),
                      medReminders = emptyList()
                    )
                    ExportCenterUtils.shareFile(context = context, file = file, mimeType = "text/csv", subject = "CSV Export")
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.heightIn(min = 32.dp)
                ) {
                  Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(13.dp), tint = theme.accent)
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primaryText)
                }
                OutlinedButton(
                  onClick = {
                    val file = ExportCenterUtils.generatePdfReport(
                      context = context,
                      categories = setOf(ExportCategory.EXPENSES),
                      isFrench = isFr,
                      profile = profile,
                      protocolSteps = emptyList(),
                      medReminders = emptyList(),
                      pregnancyMeds = emptyList(),
                      careProviders = emptyList(),
                      expenses = filteredExpenses,
                      appointments = emptyList(),
                      tasks = emptyList(),
                      notes = emptyList(),
                      voiceMemos = emptyList(),
                      walletCards = emptyList()
                    )
                    ExportCenterUtils.shareFile(context = context, file = file, mimeType = "application/pdf", subject = "PDF Export")
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.heightIn(min = 32.dp)
                ) {
                  Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp), tint = theme.accent)
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primaryText)
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${if (isFr) "Total Brut" else "Gross"}: ${formatMoney(grossTotal, activeCur)}",
                style = MaterialTheme.typography.labelSmall,
                color = theme.primaryText
              )
              Text(
                text = "${if (isFr) "Remboursé" else "Reimbursed"}: ${formatMoney(reimbursedTotal, activeCur)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF15803D),
                fontWeight = FontWeight.SemiBold
              )
            }

            val usdSum = expenses.filter { it.currency == "USD" }.sumOf { it.amount }
            val eurSum = expenses.filter { it.currency == "EUR" }.sumOf { it.amount }
            val xofSum = expenses.filter { it.currency == "XOF" }.sumOf { it.amount }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(theme.background.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("USD: ${formatMoney(usdSum, "USD")}", fontSize = 10.sp, color = theme.mutedText)
              Text("EUR: ${formatMoney(eurSum, "EUR")}", fontSize = 10.sp, color = theme.mutedText)
              Text("XOF: ${formatMoney(xofSum, "XOF")}", fontSize = 10.sp, color = theme.mutedText)
            }
          }
        }
      }

      // 3. Search Bar + Filter Button Row
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
              Text(
                text = if (isFr) "Filtrer dépenses, reçus..." else "Filter expenses, receipts...",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
              )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).heightIn(min = 40.dp)
          )

          val activeFilterCount = (if (selectedCategory != "ALL") 1 else 0) + (if (selectedCurrencyFilter != "ALL") 1 else 0)
          OutlinedButton(
            onClick = { showFilterDialog = true },
            modifier = Modifier.heightIn(min = 40.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, if (activeFilterCount > 0) theme.accent else theme.border)
          ) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = theme.accent)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isFr) "Filtrer" else "Filter",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            if (activeFilterCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Surface(
                shape = CircleShape,
                color = theme.accent,
                modifier = Modifier.size(18.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text("$activeFilterCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        }
      }

      // 4. Expenses List or Empty State
      if (filteredExpenses.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = theme.mutedText.copy(alpha = 0.5f)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (isFr) "Aucune dépense enregistrée" else "No expenses recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.mutedText
              )
            }
          }
        }
      } else {
        items(filteredExpenses, key = { it.id }) { exp ->
            val (badgeBg, badgeFg) = getCategoryBadgeColors(exp.category)
            val catDisplay = categoryOptions.firstOrNull { it.first.equals(exp.category, true) }?.second ?: exp.category
            val statusDisplay = statusOptions.firstOrNull { it.first.equals(exp.paymentStatus, true) }?.second ?: exp.paymentStatus
            val statusColor = when (exp.paymentStatus) {
              "Reimbursed" -> Color(0xFF15803D)
              "Insurance Copay" -> Color(0xFF1D4ED8)
              else -> Color(0xFFB45309)
            }

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  editingExpense = exp
                  titleInput = exp.title
                  amountInput = exp.amount.toString()
                  insurancePaidInput = if (exp.insurancePaid > 0.0) exp.insurancePaid.toString() else ""
                  totalBillInput = if (exp.totalBill > 0.0) exp.totalBill.toString() else ""
                  currencyInput = exp.currency
                  categoryInput = exp.category
                  phaseInput = exp.phase
                  paymentStatusInput = exp.paymentStatus
                  dateInput = exp.dateStr
                  notesInput = exp.notes
                  attachmentTitleInput = exp.attachmentTitle
                  receiptBase64Input = exp.receiptBase64
                  showAddEditDialog = true
                },
              shape = RoundedCornerShape(16.dp),
              color = theme.cardSurface,
              border = BorderStroke(1.dp, theme.border)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                  Surface(
                    shape = CircleShape,
                    color = badgeBg,
                    modifier = Modifier.size(40.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = when (exp.category) {
                          "Prescriptions" -> Icons.Default.Medication
                          "Baby Stuff" -> Icons.Default.ChildFriendly
                          "For the Mom" -> Icons.Default.Favorite
                          else -> Icons.AutoMirrored.Filled.ReceiptLong
                        },
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                      text = exp.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = theme.primaryText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      item {
                        Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                          Text(
                            text = catDisplay,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg,
                            maxLines = 1,
                            softWrap = false
                          )
                        }
                      }
                      item {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = statusColor.copy(alpha = 0.12f)
                        ) {
                          Text(
                            text = statusDisplay,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false
                          )
                        }
                      }
                    }
                    Text(
                      text = "${exp.phase} • ${exp.dateStr} • ${if (isFr) "Par" else "By"} ${exp.addedBy}",
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.mutedText
                    )
                    if (exp.notes.isNotBlank()) {
                      Text(
                        text = exp.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primaryText.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                    if (exp.receiptBase64.isNotBlank() || exp.attachmentTitle.isNotBlank()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                          .clickable { previewReceiptExpense = exp }
                          .padding(top = 2.dp)
                      ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = exp.attachmentTitle.ifBlank { if (isFr) "Voir le reçu" else "View Receipt" },
                          style = MaterialTheme.typography.labelSmall,
                          color = theme.accent,
                          fontWeight = FontWeight.SemiBold
                        )
                      }
                    }
                  }
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = formatMoney(exp.amount, exp.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.accent
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  IconButton(
                    onClick = {
                      onDeleteExpense(exp.id)
                      Toast.makeText(
                        context,
                        if (isFr) "Dépense supprimée" else "Expense deleted",
                        Toast.LENGTH_SHORT
                      ).show()
                    },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      Icons.Default.DeleteOutline,
                      contentDescription = if (isFr) "Supprimer" else "Delete",
                      tint = Color(0xFFDC2626),
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

  // Add / Edit Expense Dialog
  if (showAddEditDialog) {
    AlertDialog(
      onDismissRequest = {
        showAddEditDialog = false
        onClearPrefill()
      },
      title = {
        Text(
          text = if (editingExpense != null) {
            if (isFr) "Modifier la dépense" else "Edit Expense"
          } else {
            if (isFr) "Nouvelle dépense" else "New Expense"
          },
          color = theme.primaryText,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text(if (isFr) "Article / Description" else "Item / Description") },
            placeholder = { Text(if (isFr) "ex. Ordonnance Estrace / Poussette" else "e.g. Estrace Prescription / Stroller") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = amountInput,
              onValueChange = { amountInput = it },
              label = { Text(if (isFr) "Montant payé" else "Amount I Paid") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = insurancePaidInput,
              onValueChange = { insurancePaidInput = it },
              label = { Text(if (isFr) "Assurance" else "Insurance Paid") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f)
            )
          }
          OutlinedTextField(
            value = totalBillInput,
            onValueChange = { totalBillInput = it },
            label = { Text(if (isFr) "Total Facturé / Charges" else "Total Bill / Charges") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = dateInput,
            onValueChange = { dateInput = it },
            label = { Text(if (isFr) "Date" else "Date") },
            modifier = Modifier.fillMaxWidth()
          )

          Text(if (isFr) "Devise" else "Currency", style = MaterialTheme.typography.labelMedium, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("USD", "EUR", "XOF")) { cur ->
              FilterChip(
                selected = currencyInput == cur,
                onClick = { currencyInput = cur },
                label = { Text(cur, maxLines = 1, softWrap = false) }
              )
            }
          }

          Text(if (isFr) "Catégorie" else "Category", style = MaterialTheme.typography.labelMedium, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categoryOptions) { (key, label) ->
              FilterChip(
                selected = categoryInput == key,
                onClick = { categoryInput = key },
                label = { Text(label, maxLines = 1, softWrap = false) }
              )
            }
          }

          Text(if (isFr) "Phase" else "Phase", style = MaterialTheme.typography.labelMedium, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(phaseOptions) { (key, label) ->
              FilterChip(
                selected = phaseInput == key,
                onClick = { phaseInput = key },
                label = { Text(label, maxLines = 1, softWrap = false) }
              )
            }
          }

          Text(if (isFr) "Statut de paiement" else "Payment Status", style = MaterialTheme.typography.labelMedium, color = theme.mutedText)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(statusOptions) { (key, label) ->
              FilterChip(
                selected = paymentStatusInput == key,
                onClick = { paymentStatusInput = key },
                label = { Text(label, maxLines = 1, softWrap = false) }
              )
            }
          }

          OutlinedTextField(
            value = notesInput,
            onValueChange = { notesInput = it },
            label = { Text(if (isFr) "Notes (assurance, pharmacie...)" else "Notes (insurance, pharmacy...)") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = {
                  photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 36.dp)
              ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isFr) "Photo" else "Photo",
                  fontSize = 12.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
              OutlinedButton(
                onClick = {
                  documentPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 36.dp)
              ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isFr) "PDF / Reçu" else "PDF / Bill",
                  fontSize = 12.sp,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            if (receiptBase64Input.isNotBlank()) {
              TextButton(onClick = { receiptBase64Input = ""; attachmentTitleInput = "" }) {
                Text(if (isFr) "Retirer" else "Remove", color = Color(0xFFDC2626), fontSize = 12.sp)
              }
            }
          }

          if (receiptBase64Input.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = theme.accent.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp), tint = theme.accent)
                Text(
                  text = attachmentTitleInput.ifBlank { if (isFr) "Document PDF / Reçu joint ✓" else "PDF / Receipt Attached ✓" },
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = theme.accent,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amount = amountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
            val insurancePaid = insurancePaidInput.replace(",", ".").toDoubleOrNull() ?: 0.0
            val totalBill = totalBillInput.replace(",", ".").toDoubleOrNull() ?: 0.0
            if (titleInput.isNotBlank() && (amount > 0.0 || insurancePaid > 0.0 || totalBill > 0.0)) {
              val record = SyncedExpenseRecord(
                id = editingExpense?.id ?: "exp_${System.currentTimeMillis()}",
                title = titleInput.trim(),
                amount = amount,
                insurancePaid = insurancePaid,
                totalBill = totalBill,
                currency = currencyInput,
                category = categoryInput,
                phase = phaseInput,
                paymentStatus = paymentStatusInput,
                dateStr = if (dateInput.isBlank()) SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) else dateInput,
                notes = notesInput.trim(),
                receiptBase64 = receiptBase64Input,
                attachmentTitle = attachmentTitleInput,
                addedBy = editingExpense?.addedBy ?: activePartnerProfile
              )
              onSaveExpense(record)
              showAddEditDialog = false
              onClearPrefill()
              Toast.makeText(
                context,
                if (isFr) "Dépense synchronisée !" else "Expense saved & synced!",
                Toast.LENGTH_SHORT
              ).show()
            } else {
              Toast.makeText(
                context,
                if (isFr) "Veuillez saisir un titre et un montant valide" else "Please enter a title and valid amount",
                Toast.LENGTH_SHORT
              ).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Enregistrer" else "Save", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showAddEditDialog = false
          onClearPrefill()
        }) {
          Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText)
        }
      }
    )
  }

  // Receipt Photo / PDF Viewer Dialog
  previewReceiptExpense?.let { exp ->
    val bitmap = remember(exp.receiptBase64) {
      if (exp.receiptBase64.isBlank()) null
      else runCatching {
        val cleanBase64 = if (exp.receiptBase64.contains(",")) exp.receiptBase64.substringAfter(",") else exp.receiptBase64
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) {
          bmp
        } else {
          // It's a PDF document! Render first page with PdfRenderer
          val tempFile = java.io.File.createTempFile("preview", ".pdf")
          tempFile.writeBytes(bytes)
          val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
          val renderer = PdfRenderer(pfd)
          val page = renderer.openPage(0)
          val rendered = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(rendered)
          canvas.drawColor(android.graphics.Color.WHITE)
          page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          renderer.close()
          pfd.close()
          tempFile.delete()
          rendered
        }
      }.getOrNull()
    }
    Dialog(onDismissRequest = { previewReceiptExpense = null }) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primaryText)
          if (exp.attachmentTitle.isNotBlank()) {
            Text(exp.attachmentTitle, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
          }
          if (bitmap != null) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = exp.title,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .clip(RoundedCornerShape(12.dp))
            )
          } else {
            Text(
              text = if (isFr) "Document joint ✓ (visualisation complète sauvegardée)" else "Attached Document ✓ (full file preserved)",
              style = MaterialTheme.typography.bodySmall,
              color = theme.mutedText
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { previewReceiptExpense = null }) {
              Text(if (isFr) "Fermer" else "Close", color = theme.accent)
            }
          }
        }
      }
    }
  }

  // Filter Bottom Sheet / Dialog
  if (showFilterDialog) {
    Dialog(onDismissRequest = { showFilterDialog = false }) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isFr) "Filtres de Dépenses" else "Expense Filters",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            IconButton(onClick = { showFilterDialog = false }) {
              Icon(Icons.Default.Close, contentDescription = null, tint = theme.mutedText)
            }
          }

          HorizontalDivider(color = theme.border)

          Text(
            text = if (isFr) "Catégorie" else "Category",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = theme.accent
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterChip(
                selected = selectedCategory == "ALL",
                onClick = { selectedCategory = "ALL" },
                label = { Text(if (isFr) "Tout" else "All") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.accent, selectedLabelColor = Color.White)
              )
            }
            items(categoryOptions) { (key, label) ->
              FilterChip(
                selected = selectedCategory == key,
                onClick = { selectedCategory = key },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.accent, selectedLabelColor = Color.White)
              )
            }
          }

          Text(
            text = if (isFr) "Devise" else "Currency",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = theme.accent
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("ALL" to (if (isFr) "Toutes" else "All"), "USD" to "USD ($)", "EUR" to "EUR (€)", "XOF" to "XOF (CFA)")) { (code, label) ->
              FilterChip(
                selected = selectedCurrencyFilter == code,
                onClick = { selectedCurrencyFilter = code },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.accent, selectedLabelColor = Color.White)
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                selectedCategory = "ALL"
                selectedCurrencyFilter = "ALL"
              },
              modifier = Modifier.weight(1f).heightIn(min = 40.dp),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(if (isFr) "Réinitialiser" else "Reset", color = theme.primaryText)
            }
            Button(
              onClick = { showFilterDialog = false },
              modifier = Modifier.weight(1f).heightIn(min = 40.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
              Text(if (isFr) "Appliquer" else "Apply", color = Color.White)
            }
          }
        }
      }
    }
  }

  if (isScanningReceipt) {
    Dialog(onDismissRequest = {}) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.cardSurface,
        border = BorderStroke(1.dp, theme.border)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          CircularProgressIndicator(color = theme.accent)
          Text(
            text = if (isFr) "Gemini lit et extrait le reçu..." else "Gemini is reading and scanning receipt...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
      }
    }
  }
}

private fun convertToUsd(amount: Double, currency: String): Double {
  return when (currency.uppercase()) {
    "EUR" -> amount * 1.08
    "XOF" -> amount / 600.0
    else -> amount
  }
}

private fun formatMoney(amount: Double, currency: String): String {
  return when (currency.uppercase()) {
    "EUR" -> "€${String.format(Locale.US, "%.2f", amount)}"
    "XOF" -> "${String.format(Locale.US, "%.0f", amount)} CFA"
    else -> "$${String.format(Locale.US, "%.2f", amount)}"
  }
}
