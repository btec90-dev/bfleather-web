package com.example.ui

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MedicalDatabaseScreen(
  pregnancyMeds: List<SyncedPregnancyMed>,
  nutritionFoods: List<SyncedNutritionFood>,
  groceryItems: List<SyncedGroceryItem>,
  healthIssues: List<SyncedHealthIssue>,
  expenses: List<SyncedExpenseRecord>,
  careProviders: List<SyncedCareProvider>,
  searchQuery: String = "",
  onClearSearch: () -> Unit = {},
  activePartnerProfile: String = "Fatou",
  onSavePregnancyMed: (SyncedPregnancyMed) -> Unit,
  onDeletePregnancyMed: (String) -> Unit,
  onSaveNutritionFood: (SyncedNutritionFood) -> Unit,
  onDeleteNutritionFood: (String) -> Unit,
  onSaveGroceryItem: (SyncedGroceryItem) -> Unit,
  onToggleGroceryItem: (String, Boolean) -> Unit,
  onDeleteGroceryItem: (String) -> Unit,
  onSaveHealthIssue: (SyncedHealthIssue) -> Unit,
  onToggleHealthIssue: (String, Boolean) -> Unit,
  onDeleteHealthIssue: (String) -> Unit,
  onSaveExpense: (SyncedExpenseRecord) -> Unit,
  onDeleteExpense: (String) -> Unit,
  onSaveCareProvider: (SyncedCareProvider) -> Unit,
  onDeleteCareProvider: (String) -> Unit,
  theme: ThemeColors,
  language: AppLanguage
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isFr = language == AppLanguage.FR

  var selectedHubTab by remember { mutableIntStateOf(0) } // 0: Meds, 1: Nutrition & Courses, 2: Health & Portal, 3: Expenses

  // Modals state
  var showAddMedDialog by remember { mutableStateOf(false) }
  var showAddFoodDialog by remember { mutableStateOf(false) }
  var showAddGroceryDialog by remember { mutableStateOf(false) }
  var showAddIssueDialog by remember { mutableStateOf(false) }
  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var showAddProviderDialog by remember { mutableStateOf(false) }

  SelectionContainer {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(theme.background)
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // 5 Main Module Sub-Tabs (Category Pills) - Compact single-line row
      val hubTabs = if (isFr) {
        listOf("💊 Médicaments", "🥗 Nutrition & Courses", "🩺 Santé & Portail", "📞 Répertoire", "💰 Dépenses")
      } else {
        listOf("💊 Medications", "🥗 Nutrition & Grocery", "🩺 Health & Portal", "📞 Phone Book", "💰 Expenses")
      }

      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(hubTabs.indices.toList()) { index ->
          val isSel = selectedHubTab == index
          FilterChip(
            selected = isSel,
            onClick = { selectedHubTab = index },
            label = {
              Text(
                text = hubTabs[index],
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = theme.accent,
              selectedLabelColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSel,
              borderColor = theme.border,
              selectedBorderColor = theme.accent
            ),
            modifier = Modifier.height(34.dp)
          )
        }
      }

      // Compact Active Search Query Badge if filtered from primary search
      if (searchQuery.isNotBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(theme.cardSurface)
            .border(BorderStroke(0.8.dp, theme.border), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isFr) "Filtre recherche : \"$searchQuery\"" else "Active search: \"$searchQuery\"",
              style = MaterialTheme.typography.labelSmall,
              color = theme.primaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(
            onClick = onClearSearch,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = if (isFr) "Effacer filtre" else "Clear filter", tint = theme.mutedText, modifier = Modifier.size(14.dp))
          }
        }
      }

      // Module Content Area - Fills all remaining screen space cleanly
      Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when (selectedHubTab) {
          0 -> MedicationsModuleView(
            meds = pregnancyMeds,
            searchQuery = searchQuery,
            theme = theme,
            isFr = isFr,
            onAddMed = { showAddMedDialog = true },
            onDeleteMed = onDeletePregnancyMed,
            clipboardManager = clipboardManager,
            context = context
          )
          1 -> NutritionGroceryModuleView(
            foods = nutritionFoods,
            groceryItems = groceryItems,
            searchQuery = searchQuery,
            theme = theme,
            isFr = isFr,
            onAddFood = { showAddFoodDialog = true },
            onAddGrocery = { showAddGroceryDialog = true },
            onToggleGrocery = onToggleGroceryItem,
            onDeleteFood = onDeleteNutritionFood,
            onDeleteGrocery = onDeleteGroceryItem
          )
          2 -> HealthPortalModuleView(
            issues = healthIssues,
            providers = careProviders,
            searchQuery = searchQuery,
            theme = theme,
            isFr = isFr,
            onAddIssue = { showAddIssueDialog = true },
            onAddProvider = { showAddProviderDialog = true },
            onOpenPhoneBook = { selectedHubTab = 3 },
            onToggleIssue = onToggleHealthIssue,
            onDeleteIssue = onDeleteHealthIssue,
            onDeleteProvider = onDeleteCareProvider,
            context = context
          )
          3 -> PhoneBookScreen(
            providers = careProviders,
            searchQuery = searchQuery,
            theme = theme,
            language = if (isFr) AppLanguage.FR else AppLanguage.EN,
            onSaveProvider = onSaveCareProvider,
            onDeleteProvider = onDeleteCareProvider
          )
          4 -> ExpensesModuleView(
            expenses = expenses,
            searchQuery = searchQuery,
            theme = theme,
            isFr = isFr,
            onAddExpense = { showAddExpenseDialog = true },
            onDeleteExpense = onDeleteExpense
          )
        }
      }
    }
  }

  // Add Medication Dialog
  if (showAddMedDialog) {
    AddPregnancyMedDialog(theme = theme, isFr = isFr, onDismiss = { showAddMedDialog = false }, onSave = { med ->
      onSavePregnancyMed(med.copy(addedBy = activePartnerProfile))
      showAddMedDialog = false
      Toast.makeText(context, if (isFr) "Médicament enregistré !" else "Medication saved!", Toast.LENGTH_SHORT).show()
    })
  }

  // Add Food Dialog
  if (showAddFoodDialog) {
    AddNutritionFoodDialog(theme = theme, isFr = isFr, onDismiss = { showAddFoodDialog = false }, onSave = { food ->
      onSaveNutritionFood(food.copy(addedBy = activePartnerProfile))
      showAddFoodDialog = false
      Toast.makeText(context, if (isFr) "Aliment enregistré !" else "Food saved!", Toast.LENGTH_SHORT).show()
    })
  }

  // Add Grocery Item Dialog
  if (showAddGroceryDialog) {
    AddGroceryItemDialog(theme = theme, isFr = isFr, onDismiss = { showAddGroceryDialog = false }, onSave = { item ->
      onSaveGroceryItem(item.copy(addedBy = activePartnerProfile))
      showAddGroceryDialog = false
      Toast.makeText(context, if (isFr) "Article ajouté !" else "Item added!", Toast.LENGTH_SHORT).show()
    })
  }

  // Add Health Issue Dialog
  if (showAddIssueDialog) {
    AddHealthIssueDialog(theme = theme, isFr = isFr, onDismiss = { showAddIssueDialog = false }, onSave = { issue ->
      onSaveHealthIssue(issue.copy(addedBy = activePartnerProfile))
      showAddIssueDialog = false
      Toast.makeText(context, if (isFr) "Problème enregistré !" else "Issue saved!", Toast.LENGTH_SHORT).show()
    })
  }

  // Add Expense Dialog
  if (showAddExpenseDialog) {
    AddExpenseDialog(theme = theme, isFr = isFr, onDismiss = { showAddExpenseDialog = false }, onSave = { exp ->
      onSaveExpense(exp.copy(addedBy = activePartnerProfile))
      showAddExpenseDialog = false
      Toast.makeText(context, if (isFr) "Dépense enregistrée !" else "Expense saved!", Toast.LENGTH_SHORT).show()
    })
  }

  // Add Care Provider Dialog
  if (showAddProviderDialog) {
    EditCareProviderDialog(
      provider = null,
      theme = theme,
      isFr = isFr,
      onDismiss = { showAddProviderDialog = false },
      onSave = { prov ->
        onSaveCareProvider(prov)
        showAddProviderDialog = false
        Toast.makeText(context, if (isFr) "Contact enregistré !" else "Contact saved!", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

// -------------------------------------------------------------
// MODULE 1: Medications Database View
// -------------------------------------------------------------
@Composable
private fun MedicationsModuleView(
  meds: List<SyncedPregnancyMed>,
  searchQuery: String,
  theme: ThemeColors,
  isFr: Boolean,
  onAddMed: () -> Unit,
  onDeleteMed: (String) -> Unit,
  clipboardManager: androidx.compose.ui.platform.ClipboardManager,
  context: android.content.Context
) {
  var safetyFilter by remember { mutableStateOf("ALL") }

  val filtered = meds.filter { med ->
    val matchesSearch = searchQuery.isBlank() ||
      med.name.contains(searchQuery, true) ||
      med.activeIngredient.contains(searchQuery, true) ||
      med.clinicalNotes.contains(searchQuery, true)
    val matchesSafety = safetyFilter == "ALL" || med.safetyLevel.equals(safetyFilter, true)
    matchesSearch && matchesSafety
  }

  val safetyOptions = listOf(
    "ALL" to (if (isFr) "Tous" else "All"),
    "SAFE" to (if (isFr) "🟢 Sûr" else "🟢 Safe"),
    "CAUTION" to (if (isFr) "🟡 Prudence" else "🟡 Caution"),
    "AVOID" to (if (isFr) "🔴 À Éviter" else "🔴 Avoid")
  )

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Compact Safety Filter Chips + "+ Ajouter" Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(safetyOptions) { (code, label) ->
          FilterChip(
            selected = safetyFilter == code,
            onClick = { safetyFilter = code },
            label = { Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false) },
            modifier = Modifier.height(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onAddMed,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier
          .height(34.dp)
          .testTag("med_add_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isFr) "Ajouter" else "Add",
          color = Color.White,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false
        )
      }
    }

    if (filtered.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Text(if (isFr) "Aucun médicament trouvé" else "No medications found", color = theme.mutedText)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
      ) {
        // Section title scrolls with the content so it never sticks or covers cards
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isFr) "Base Médicaments Grossesse & FIV (${filtered.size})" else "Pregnancy & IVF Medications Database (${filtered.size})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isFr) "Réf. ACOG" else "ACOG Ref.",
              style = MaterialTheme.typography.labelSmall,
              color = theme.accent,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
        items(filtered, key = { it.id }) { med ->
          val (badgeBg, badgeFg, badgeText) = when (med.safetyLevel.uppercase()) {
            "SAFE" -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), if (isFr) "SÛR / RECOMMANDÉ" else "SAFE / APPROVED")
            "CAUTION" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), if (isFr) "PRUDENCE / AVIS MÉDICAL" else "CAUTION / CONSULT")
            else -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), if (isFr) "CONTRE-INDIQUÉ / À ÉVITER" else "AVOID / CONTRAINDICATED")
          }

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(med.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
                  if (med.activeIngredient.isNotBlank()) {
                    Text(
                      text = "${if (isFr) "Principe actif" else "Active"}: ${med.activeIngredient}",
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.mutedText
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = badgeBg
                ) {
                  Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "T1: ${med.trimester1} • T2: ${med.trimester2} • T3: ${med.trimester3}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = theme.accent
              )
              if (med.clinicalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(med.clinicalNotes, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.85f), lineHeight = 17.sp)
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${if (isFr) "Par" else "By"} ${med.addedBy}",
                  style = MaterialTheme.typography.labelSmall,
                  color = theme.mutedText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString("${med.name} (${med.activeIngredient}) - $badgeText: ${med.clinicalNotes}"))
                    Toast.makeText(context, if (isFr) "Copié !" else "Copied!", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = if (isFr) "Copier" else "Copy", tint = theme.mutedText, modifier = Modifier.size(16.dp))
                  }
                  IconButton(onClick = { onDeleteMed(med.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// MODULE 2: Nutrition & Grocery Planner View
// -------------------------------------------------------------
@Composable
private fun NutritionGroceryModuleView(
  foods: List<SyncedNutritionFood>,
  groceryItems: List<SyncedGroceryItem>,
  searchQuery: String,
  theme: ThemeColors,
  isFr: Boolean,
  onAddFood: () -> Unit,
  onAddGrocery: () -> Unit,
  onToggleGrocery: (String, Boolean) -> Unit,
  onDeleteFood: (String) -> Unit,
  onDeleteGrocery: (String) -> Unit
) {
  var subTab by remember { mutableIntStateOf(0) } // 0: Foods, 1: Grocery

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Compact SubTab Switcher + "+ Ajouter" Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = subTab == 0,
          onClick = { subTab = 0 },
          label = { Text(if (isFr) "🥗 Guide Aliments" else "🥗 Food Guide", maxLines = 1, softWrap = false) },
          modifier = Modifier.height(32.dp)
        )
        FilterChip(
          selected = subTab == 1,
          onClick = { subTab = 1 },
          label = {
            Text(
              text = if (isFr) "🛒 Courses (${groceryItems.count { it.isChecked }}/${groceryItems.size})" else "🛒 Grocery (${groceryItems.count { it.isChecked }}/${groceryItems.size})",
              maxLines = 1,
              softWrap = false
            )
          },
          modifier = Modifier.height(32.dp)
        )
      }

      Button(
        onClick = if (subTab == 0) onAddFood else onAddGrocery,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (subTab == 0) (if (isFr) "Aliment" else "Food") else (if (isFr) "Article" else "Item"),
          color = Color.White,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false
        )
      }
    }

    if (subTab == 0) {
      val filteredFoods = foods.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.highlights.contains(searchQuery, true)
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
      ) {
        item {
          Text(
            text = if (isFr) "Aliments à consommer vs à éviter (${filteredFoods.size})" else "Foods to Eat vs Avoid (${filteredFoods.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
        items(filteredFoods, key = { it.id }) { food ->
          val (badgeBg, badgeFg, badgeText) = when (food.safetyStatus.uppercase()) {
            "EAT" -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), if (isFr) "✅ À CONSOMMER" else "✅ EAT")
            "LIMIT" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), if (isFr) "⚠️ À MODÉRER" else "⚠️ LIMIT")
            else -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), if (isFr) "🚫 À ÉVITER" else "🚫 AVOID")
          }

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = theme.cardSurface,
            border = BorderStroke(1.dp, theme.border)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(food.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                  Text(text = badgeText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeFg)
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(food.highlights, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.85f))
              if (food.nutrients.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Nutriments : ${food.nutrients}", style = MaterialTheme.typography.labelSmall, color = theme.accent)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onDeleteFood(food.id) }) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    } else {
      val filteredGrocery = groceryItems.filter {
        searchQuery.isBlank() || it.label.contains(searchQuery, true) || it.category.contains(searchQuery, true)
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
      ) {
        item {
          Text(
            text = if (isFr) "Courses bien-être & grossesse" else "Wellness & Pregnancy Grocery",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = theme.primaryText
          )
        }
        items(filteredGrocery, key = { it.id }) { item ->
          Surface(
            modifier = Modifier.fillMaxWidth().clickable { onToggleGrocery(item.id, item.isChecked) },
            shape = RoundedCornerShape(10.dp),
            color = theme.cardSurface,
            border = BorderStroke(0.8.dp, theme.border)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                  if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (item.isChecked) Color(0xFF16A34A) else theme.mutedText,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(item.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (item.isChecked) theme.mutedText else theme.primaryText)
                  Text("${item.category} • ${item.quantity}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
                }
              }
              IconButton(onClick = { onDeleteGrocery(item.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// MODULE 3: Health Issues & Medical Portal View
// -------------------------------------------------------------
@Composable
private fun HealthPortalModuleView(
  issues: List<SyncedHealthIssue>,
  providers: List<SyncedCareProvider>,
  searchQuery: String,
  theme: ThemeColors,
  isFr: Boolean,
  onAddIssue: () -> Unit,
  onAddProvider: () -> Unit,
  onOpenPhoneBook: () -> Unit = {},
  onToggleIssue: (String, Boolean) -> Unit,
  onDeleteIssue: (String) -> Unit,
  onDeleteProvider: (String) -> Unit,
  context: android.content.Context
) {
  val filteredIssues = issues.filter {
    searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.symptoms.contains(searchQuery, true) || it.doctorNotes.contains(searchQuery, true)
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(if (isFr) "Équipe Médicale & Urgences" else "Care Team & Emergency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(
          onClick = onOpenPhoneBook,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Text(
            text = if (isFr) "Répertoire →" else "Directory →",
            color = theme.accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }
        Button(
          onClick = onAddProvider,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(if (isFr) "Praticien" else "Provider", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        }
      }
    }

    // Care Providers Directory
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(providers, key = { it.id }) { prov ->
        Surface(
          modifier = Modifier.width(260.dp),
          shape = RoundedCornerShape(16.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prov.providerType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = theme.accent)
            Text(prov.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
            Text(prov.affiliation, style = MaterialTheme.typography.bodySmall, color = theme.mutedText)
            Text("📞 ${prov.phone}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = theme.primaryText)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Button(
                onClick = {
                  try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prov.phone}"))
                    context.startActivity(intent)
                  } catch (_: Exception) {
                    Toast.makeText(context, "Appel non disponible", Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                contentPadding = PaddingValues(vertical = 4.dp)
              ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isFr) "Appeler" else "Call",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false
                )
              }
              if (prov.portalLink.isNotBlank() && prov.portalLink.startsWith("http")) {
                OutlinedButton(
                  onClick = {
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(prov.portalLink))
                      context.startActivity(intent)
                    } catch (_: Exception) {}
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                  Text(if (isFr) "Portail" else "Portal", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(if (isFr) "Suivi des Symptômes & Santé" else "Symptoms & Health Issues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
      TextButton(
        onClick = onAddIssue,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Text(if (isFr) "+ Suivi" else "+ Issue", color = theme.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
    ) {
      items(filteredIssues, key = { it.id }) { issue ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, if (issue.isResolved) Color(0xFF16A34A).copy(alpha = 0.4f) else theme.border)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
              Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (issue.isResolved) Color(0xFFDCFCE7) else theme.accent.copy(alpha = 0.15f),
                modifier = Modifier.clickable { onToggleIssue(issue.id, issue.isResolved) }
              ) {
                Text(
                  text = if (issue.isResolved) (if (isFr) "Résolu ✓" else "Resolved ✓") else (if (isFr) "En cours" else "Active"),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (issue.isResolved) Color(0xFF16A34A) else theme.accent,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${if (isFr) "Trimestre" else "Trimester"}: ${issue.trimester} • ${if (isFr) "Sévérité" else "Severity"}: ${issue.severity} • ${issue.dateStr} • ${if (isFr) "Par" else "By"} ${issue.addedBy}",
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText
            )
            if (issue.symptoms.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text("${if (isFr) "Symptômes" else "Symptoms"}: ${issue.symptoms}", style = MaterialTheme.typography.bodySmall, color = theme.primaryText)
            }
            if (issue.doctorNotes.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text("${if (isFr) "Notes médecin" else "Doctor notes"}: ${issue.doctorNotes}", style = MaterialTheme.typography.bodySmall, color = theme.accent)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              IconButton(onClick = { onDeleteIssue(issue.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = if (isFr) "Supprimer" else "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// MODULE 4: Expenses & Financial Ledger View
// -------------------------------------------------------------
@Composable
private fun ExpensesModuleView(
  expenses: List<SyncedExpenseRecord>,
  searchQuery: String,
  theme: ThemeColors,
  isFr: Boolean,
  onAddExpense: () -> Unit,
  onDeleteExpense: (String) -> Unit
) {
  var currencyFilter by remember { mutableStateOf("ALL") }

  val filtered = expenses.filter {
    searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.category.contains(searchQuery, true) || it.notes.contains(searchQuery, true)
  }.filter {
    currencyFilter == "ALL" || it.currency.equals(currencyFilter, true)
  }

  val totalUSD = expenses.filter { it.currency == "USD" }.sumOf { it.amount }
  val totalEUR = expenses.filter { it.currency == "EUR" }.sumOf { it.amount }
  val totalXOF = expenses.filter { it.currency == "XOF" }.sumOf { it.amount }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Totals Summary Card per Currency
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = theme.cardSurface,
      border = BorderStroke(1.dp, theme.border)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("USD ($)", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          Text("$${String.format(Locale.US, "%.2f", totalUSD)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("EUR (€)", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          Text("€${String.format(Locale.US, "%.2f", totalEUR)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("XOF (CFA)", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
          Text("${String.format(Locale.US, "%.0f", totalXOF)} CFA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
        }
      }
    }

    // Currency Filter Chips + "+ Dépense" Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(listOf("ALL" to (if (isFr) "Toutes" else "All"), "USD" to "USD", "EUR" to "EUR", "XOF" to "XOF")) { (code, label) ->
          FilterChip(
            selected = currencyFilter == code,
            onClick = { currencyFilter = code },
            label = { Text(label, maxLines = 1, softWrap = false) },
            modifier = Modifier.height(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onAddExpense,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(if (isFr) "Dépense" else "Expense", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(top = 2.dp, bottom = 84.dp)
    ) {
      item {
        Text(
          text = if (isFr) "Ledger Financier & Dépenses (${filtered.size})" else "Financial Ledger & Expenses (${filtered.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = theme.primaryText
        )
      }
      items(filtered, key = { it.id }) { exp ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = theme.cardSurface,
          border = BorderStroke(1.dp, theme.border)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(exp.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = theme.primaryText)
              Text("${exp.category} • ${exp.phase} • ${exp.dateStr}", style = MaterialTheme.typography.labelSmall, color = theme.mutedText)
              if (exp.notes.isNotBlank()) {
                Text(exp.notes, style = MaterialTheme.typography.bodySmall, color = theme.primaryText.copy(alpha = 0.8f))
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              val symbol = when (exp.currency) {
                "EUR" -> "€"
                "XOF" -> " CFA"
                else -> "$"
              }
              val amtStr = if (exp.currency == "XOF") String.format(Locale.US, "%.0f", exp.amount) else String.format(Locale.US, "%.2f", exp.amount)
              Text(
                text = if (exp.currency == "XOF") "$amtStr$symbol" else "$symbol$amtStr",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.accent
              )
              IconButton(onClick = { onDeleteExpense(exp.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// DIALOGS FOR ADDING NEW ITEMS
// -------------------------------------------------------------
@Composable
private fun AddPregnancyMedDialog(theme: ThemeColors, isFr: Boolean, onDismiss: () -> Unit, onSave: (SyncedPregnancyMed) -> Unit) {
  var name by remember { mutableStateOf("") }
  var activeIngredient by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Médicament") }
  var safetyLevel by remember { mutableStateOf("SAFE") }
  var t1 by remember { mutableStateOf(if (isFr) "Sûr" else "Safe") }
  var t2 by remember { mutableStateOf(if (isFr) "Sûr" else "Safe") }
  var t3 by remember { mutableStateOf(if (isFr) "Sûr" else "Safe") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isFr) "Ajouter Médicament" else "Add Medication", color = theme.primaryText) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isFr) "Nom" else "Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = activeIngredient, onValueChange = { activeIngredient = it }, label = { Text(if (isFr) "Principe actif" else "Active ingredient") }, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("SAFE", "CAUTION", "AVOID")) { lvl ->
            FilterChip(selected = safetyLevel == lvl, onClick = { safetyLevel = lvl }, label = { Text(lvl, maxLines = 1, softWrap = false) })
          }
        }
        OutlinedTextField(value = t1, onValueChange = { t1 = it }, label = { Text(if (isFr) "Trimestre 1" else "Trimester 1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(if (isFr) "Notes cliniques" else "Clinical notes") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(onClick = {
        if (name.isNotBlank()) {
          onSave(SyncedPregnancyMed(
            id = "pm_${System.currentTimeMillis()}",
            name = name,
            activeIngredient = activeIngredient,
            category = category,
            safetyLevel = safetyLevel,
            trimester1 = t1,
            trimester2 = t2,
            trimester3 = t3,
            clinicalNotes = notes
          ))
        }
      }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(if (isFr) "Ajouter" else "Add", color = Color.White) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText) } }
  )
}

@Composable
private fun AddNutritionFoodDialog(theme: ThemeColors, isFr: Boolean, onDismiss: () -> Unit, onSave: (SyncedNutritionFood) -> Unit) {
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(if (isFr) "Aliment" else "Food") }
  var safetyStatus by remember { mutableStateOf("EAT") }
  var highlights by remember { mutableStateOf("") }
  var nutrients by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isFr) "Ajouter Aliment" else "Add Food", color = theme.primaryText) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isFr) "Nom de l'aliment" else "Food name") }, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("EAT", "LIMIT", "AVOID")) { s ->
            FilterChip(selected = safetyStatus == s, onClick = { safetyStatus = s }, label = { Text(s, maxLines = 1, softWrap = false) })
          }
        }
        OutlinedTextField(value = highlights, onValueChange = { highlights = it }, label = { Text(if (isFr) "Points forts / Risques" else "Highlights / Risks") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = nutrients, onValueChange = { nutrients = it }, label = { Text(if (isFr) "Nutriments" else "Nutrients") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(onClick = {
        if (name.isNotBlank()) {
          onSave(SyncedNutritionFood(
            id = "nf_${System.currentTimeMillis()}",
            name = name,
            category = category,
            safetyStatus = safetyStatus,
            highlights = highlights,
            nutrients = nutrients
          ))
        }
      }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(if (isFr) "Ajouter" else "Add", color = Color.White) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText) } }
  )
}

@Composable
private fun AddGroceryItemDialog(theme: ThemeColors, isFr: Boolean, onDismiss: () -> Unit, onSave: (SyncedGroceryItem) -> Unit) {
  var label by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(if (isFr) "Fruits & Légumes" else "Produce") }
  var quantity by remember { mutableStateOf("1") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isFr) "Ajouter Article Courses" else "Add Grocery Item", color = theme.primaryText) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(if (isFr) "Article" else "Item") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text(if (isFr) "Quantité" else "Quantity") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(onClick = {
        if (label.isNotBlank()) {
          onSave(SyncedGroceryItem(id = "gr_${System.currentTimeMillis()}", label = label, category = category, quantity = quantity, isChecked = false))
        }
      }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(if (isFr) "Ajouter" else "Add", color = Color.White) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText) } }
  )
}

@Composable
private fun AddHealthIssueDialog(theme: ThemeColors, isFr: Boolean, onDismiss: () -> Unit, onSave: (SyncedHealthIssue) -> Unit) {
  var title by remember { mutableStateOf("") }
  var symptoms by remember { mutableStateOf("") }
  var doctorNotes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isFr) "Ajouter Suivi Santé" else "Add Health Issue", color = theme.primaryText) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isFr) "Titre / Condition" else "Title / Condition") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text(if (isFr) "Symptômes" else "Symptoms") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = doctorNotes, onValueChange = { doctorNotes = it }, label = { Text(if (isFr) "Notes médecin" else "Doctor notes") }, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(onClick = {
        if (title.isNotBlank()) {
          onSave(SyncedHealthIssue(
            id = "hi_${System.currentTimeMillis()}",
            title = title,
            trimester = "T1",
            severity = if (isFr) "Léger" else "Mild",
            dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()),
            symptoms = symptoms,
            doctorNotes = doctorNotes,
            isResolved = false
          ))
        }
      }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(if (isFr) "Ajouter" else "Add", color = Color.White) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText) } }
  )
}

@Composable
private fun AddExpenseDialog(theme: ThemeColors, isFr: Boolean, onDismiss: () -> Unit, onSave: (SyncedExpenseRecord) -> Unit) {
  var title by remember { mutableStateOf("") }
  var amountStr by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf("USD") }
  var category by remember { mutableStateOf("Prescriptions") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (isFr) "Ajouter Dépense" else "Add Expense", color = theme.primaryText) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isFr) "Titre / Description" else "Title / Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text(if (isFr) "Montant" else "Amount") }, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("USD", "EUR", "XOF")) { cur ->
            FilterChip(selected = currency == cur, onClick = { currency = cur }, label = { Text(cur, maxLines = 1, softWrap = false) })
          }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("Prescriptions", "Baby Stuff", "For the Mom", "Others")) { cat ->
            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, maxLines = 1, softWrap = false) })
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        val amt = amountStr.toDoubleOrNull() ?: 0.0
        if (title.isNotBlank()) {
          onSave(SyncedExpenseRecord(
            id = "exp_${System.currentTimeMillis()}",
            title = title,
            amount = amt,
            currency = currency,
            category = category,
            phase = "IVF / Clinic",
            dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
          ))
        }
      }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) { Text(if (isFr) "Ajouter" else "Add", color = Color.White) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(if (isFr) "Annuler" else "Cancel", color = theme.mutedText) } }
  )
}
