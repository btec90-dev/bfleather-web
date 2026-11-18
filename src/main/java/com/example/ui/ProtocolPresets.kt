package com.example.ui

data class ProtocolStep(
  val id: String,
  var phaseId: Int = 2,
  var date: String,
  var stepName: String,
  var medication: String = "",
  var dosage: String = "",
  var status: String = "En cours",
  var details: String = "",
  var isCompleted: Boolean = false,
  var isCriticalWindow: Boolean = false,
  var targetEpochMillis: Long = 0L
)

typealias ProtocolMilestone = ProtocolStep

data class ProtocolPhaseMeta(
  val phaseId: Int,
  val emoji: String,
  val titleFr: String,
  val titleEn: String,
  val dateRangeFr: String,
  val dateRangeEn: String,
  val statusBadgeFr: String,
  val statusBadgeEn: String,
  val defaultExpanded: Boolean
)

val protocolPhasesMeta = listOf(
  ProtocolPhaseMeta(
    phaseId = 1,
    emoji = "🧬",
    titleFr = "Phase 1 : Stimulation Ovarienne & Ponction",
    titleEn = "Phase 1: Ovarian Stimulation & Retrieval",
    dateRangeFr = "19 sept. – 01 oct.",
    dateRangeEn = "Sep 19 – Oct 01",
    statusBadgeFr = "Terminée",
    statusBadgeEn = "Completed",
    defaultExpanded = false
  ),
  ProtocolPhaseMeta(
    phaseId = 2,
    emoji = "🧫",
    titleFr = "Phase 2 : Développement Embryonnaire & Préparation au TEC",
    titleEn = "Phase 2: Embryo Development & FET Prep",
    dateRangeFr = "02 oct. – 17 nov.",
    dateRangeEn = "Oct 02 – Nov 17",
    statusBadgeFr = "En cours",
    statusBadgeEn = "In Progress",
    defaultExpanded = true
  ),
  ProtocolPhaseMeta(
    phaseId = 3,
    emoji = "👶",
    titleFr = "Phase 3 : Transfert (TEC) & Suivi Précoce",
    titleEn = "Phase 3: Transfer (FET) & Early Care",
    dateRangeFr = "18 nov. – Semaines 8–10",
    dateRangeEn = "Nov 18 – Weeks 8–10",
    statusBadgeFr = "À venir",
    statusBadgeEn = "Upcoming",
    defaultExpanded = true
  ),
  ProtocolPhaseMeta(
    phaseId = 4,
    emoji = "🍼",
    titleFr = "Phase 4 : Suivi Obstétrical & Maternité",
    titleEn = "Phase 4: Obstetrics & Maternity",
    dateRangeFr = "Semaines 11–13 – Semaines 37–41",
    dateRangeEn = "Weeks 11–13 – Weeks 37–41",
    statusBadgeFr = "À venir",
    statusBadgeEn = "Upcoming",
    defaultExpanded = false
  )
)

enum class ProtocolCategory(val labelFr: String, val labelEn: String, val icon: String) {
  MEDICATIONS("Médications", "Medications", "💊"),
  APPOINTMENTS("Rendez-vous", "Appointments", "🩺"),
  MONITORING("Monitorage", "Monitoring", "📊"),
  VITAMINS("Vitamines", "Vitamins", "✨"),
  TRIGGER_SHOT("Déclenchement", "Trigger Shot", "⏰"),
  BLOODWORK("Prise de sang", "Bloodwork", "🩸"),
  ULTRASOUND("Échographie", "Ultrasound", "👶")
}

data class MedicationPreset(
  val nameFr: String,
  val nameEn: String,
  val medication: String,
  val dosage: String,
  val time: String,
  val phaseId: Int,
  val categoryFr: String,
  val categoryEn: String,
  val descriptionFr: String,
  val descriptionEn: String,
  val instructionsFr: String,
  val instructionsEn: String,
  val icon: String
)

val categoryPresetsMap = mapOf(
  ProtocolCategory.MEDICATIONS to listOf(
    MedicationPreset("Estrace 2 mg PO (Matin & Soir)", "Estrace 2 mg PO (AM & PM)", "Estrace (Estradiol)", "2 mg PO", "08:00 & 20:00", 2, "Médications", "Medications", "Begin Estrace 2 mg PO twice daily (AM and PM)", "Begin Estrace 2 mg PO twice daily (AM and PM)", "1 comprimé matin et soir à heures régulières.", "1 tablet AM and PM at regular times.", "💊"),
    MedicationPreset("Progestérone IM (PIO 50 mg)", "Progesterone IM (PIO 50 mg)", "Progestérone IM", "1 mL (50 mg)", "20:00", 2, "Médications", "Medications", "Injections intramusculaires dans la fesse, chaque soir à heure fixe", "Intramuscular gluteal injection every evening at a fixed time", "Chauffer légèrement l'huile dans les mains avant injection.", "Warm oil slightly in hands before injection.", "💉"),
    MedicationPreset("Azithromycine 1 000 mg", "Azithromycin 1,000 mg", "Azithromycine", "1 000 mg", "Avec repas", 2, "Médications", "Medications", "Antibiothérapie prophylactique pré-TEC", "Pre-FET prophylactic antibiotic", "Deux comprimés en une seule prise avec de la nourriture.", "Two tablets as a single dose with food.", "💊"),
    MedicationPreset("Gonal-F 150 UI", "Gonal-F 150 IU", "Gonal-F (FSH)", "150 UI SC", "20:00", 1, "Médications", "Medications", "Stimulation folliculaire multiple", "Multiple follicular stimulation", "Injection sous-cutanée dans la paroi abdominale.", "Subcutaneous injection in abdominal wall.", "💉"),
    MedicationPreset("Cétrotide 0.25 mg", "Cetrotide 0.25 mg", "Cétrorélix acétate", "0.25 mg SC", "07:30", 1, "Médications", "Medications", "Blocage du pic prématuré de LH", "Prevention of premature LH surge", "Injection sous-cutanée le matin dès J6.", "Morning subcutaneous injection from Day 6.", "🛡️"),
    MedicationPreset("Lovenox 40 mg", "Lovenox 40 mg", "Enoxaparine", "40 mg SC", "21:00", 2, "Médications", "Medications", "Vascularisation endométriale optimisée", "Optimized endometrial vascularization", "Injection sous-cutanée abdominale le soir.", "Evening subcutaneous abdominal injection.", "🩸")
  ),
  ProtocolCategory.APPOINTMENTS to listOf(
    MedicationPreset("Consultation Dr. Anate Brauer", "Consultation Dr. Anate Brauer", "Consultation RMA", "Cabinet East Side", "10:00", 2, "Rendez-vous", "Appointments", "Point d'étape protocole TEC et analyse endomètre", "FET protocol milestone and endometrial review", "635 Madison Ave, 10th Floor, New York, NY.", "635 Madison Ave, 10th Floor, New York, NY.", "🩺"),
    MedicationPreset("Transfert d'Embryon Congelé (TEC)", "Frozen Embryo Transfer (FET)", "Procédure Bloc", "13h00–17h00", "14:00", 3, "Rendez-vous", "Appointments", "Transfert du blastocyste euploïde masculin", "Transfer of male euploid blastocyst", "Vessie pleine, strictement sans parfum.", "Full bladder, strictly no perfume.", "⭐"),
    MedicationPreset("Validation Consentement Embryonnaire", "Embryo Consent Validation", "Portail Patient", "En ligne", "14:00", 2, "Rendez-vous", "Appointments", "Validation du formulaire de décongélation", "Thawing and transfer consent sign-off", "Signer électroniquement le formulaire.", "Electronically sign patient portal form.", "🧬")
  ),
  ProtocolCategory.MONITORING to listOf(
    MedicationPreset("Échographie endomètre & Bilan sanguin", "Endometrial Ultrasound & Bloodwork", "Monitorage RMA", "≥ 7–8 mm", "08:00", 2, "Monitorage", "Monitoring", "Vérification épaisseur ≥ 7–8 mm, aspect trilaminaire et suppression progestérone", "Verify ≥ 7–8 mm trilaminar pattern and progesterone suppression", "Rendez-vous matinal au cabinet RMA East Side.", "Morning appointment at RMA East Side.", "📐"),
    MedicationPreset("Folliculométrie J8", "Folliculometry Day 8", "Échographie & Sang", "Monitorage", "07:30", 1, "Monitorage", "Monitoring", "Comptage folliculaire et dosage œstradiol", "Follicle count and estradiol check", "Arrivée matinale pour prise de sang.", "Early morning arrival for lab draw.", "📊")
  ),
  ProtocolCategory.VITAMINS to listOf(
    MedicationPreset("Vitamines Prénatales + DHA", "Prenatal Vitamins + DHA", "Multivitamines", "1 gélule/j", "08:30", 2, "Vitamines", "Vitamins", "Soutien micronutritionnel complet maman & bébé", "Complete micronutrient support for mom & baby", "Prendre avec repas et grand verre d'eau.", "Take with meal and a full glass of water.", "✨"),
    MedicationPreset("Acide Folique 5 mg", "Folic Acid 5 mg", "Vitamine B9", "5 mg/j", "09:00", 2, "Vitamines", "Vitamins", "Prévention anomalies du tube neural", "Neural tube defect prevention", "Prise orale continue tous les matins.", "Continuous morning oral intake.", "🌱")
  ),
  ProtocolCategory.TRIGGER_SHOT to listOf(
    MedicationPreset("Ovitrelle 250 µg (Trigger Shot)", "Ovitrelle 250 mcg (Trigger Shot)", "Choriogonadotropine alfa", "250 µg SC", "23:00", 1, "Déclenchement", "Trigger Shot", "Déclenchement final maturation méiotique à H-36", "Final oocyte meiosis trigger at H-36", "Horaire strict à la minute près.", "Exact time to the minute required.", "⏰")
  ),
  ProtocolCategory.BLOODWORK to listOf(
    MedicationPreset("Bêta-hCG sérique #1 (J9 post-TEC)", "Serum Beta-hCG Test #1 (D9)", "HCG Quantitative", "Prise de sang", "08:00", 3, "Prise de sang", "Bloodwork", "Premier test de grossesse / confirmation biologique de nidation", "First pregnancy test / biological confirmation of implantation", "Prélèvement au laboratoire RMA, résultat l'après-midi.", "RMA lab draw, results same afternoon.", "🩸"),
    MedicationPreset("Prise de sang de contrôle post-TEC (J2)", "Post-FET Bloodwork Check (D2)", "Laboratoire RMA", "J2 post-TEC", "08:00", 3, "Prise de sang", "Bloodwork", "Suivi hormonal précoce après transfert", "Early post-transfer hormone monitoring", "Vérification œstradiol et progestérone.", "Estradiol and progesterone level check.", "📊")
  ),
  ProtocolCategory.ULTRASOUND to listOf(
    MedicationPreset("Échographie précoce 1 (Sac gestationnel)", "Early Ultrasound 1 (Gestational Sac)", "Échographie", "Semaines 5–6", "08:30", 3, "Échographie", "Ultrasound", "Visualisation du sac gestationnel intra-utérin", "Visualization of intrauterine gestational sac", "Réalisée au cabinet RMA.", "Performed at RMA clinic.", "🫧"),
    MedicationPreset("Échographie T1 & DPNI", "T1 Ultrasound & NIPT", "Échographie Obstétricale", "Semaines 11–13", "09:30", 4, "Échographie", "Ultrasound", "Échographie du 1er trimestre (T1) & DPNI", "1st Trimester ultrasound & NIPT", "Dépistage prénatal non invasif.", "Non-invasive prenatal testing.", "👶"),
    MedicationPreset("Échographie morphologique T2", "T2 Detailed Anatomy Scan", "Échographie", "Semaine 22", "11:00", 4, "Échographie", "Ultrasound", "Échographie morphologique du 2e trimestre (T2)", "2nd Trimester anatomy ultrasound", "Examen exhaustif anatomique.", "Comprehensive fetal anatomy review.", "✨")
  )
)
