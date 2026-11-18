package com.example.data

data class TaskItem(
  val id: String = "",
  val title: String = "",
  val date: String = "",
  val category: String = "",
  val details: String = "",
  var isCompleted: Boolean = false,
  val hasAlarm: Boolean = false,
  val alarmTimeMinutesBefore: Int = 15, // e.g. 0 (at time), 15, 60 (1 hr), 1440 (1 day)
  val addedBy: String = "Fatou"
)

data class PhaseSection(
  val phaseNumber: Int,
  val title: String,
  val subtitle: String,
  val dateRange: String,
  val status: String,
  val yieldBadge: String? = null,
  val description: String,
  val items: List<TaskItem>
)

data class ClinicalInstruction(
  val topic: String,
  val subtitle: String,
  val instructions: List<String>,
  val alertType: String // e.g., "Critical", "Medication", "Recovery"
)

data class ScannedDocument(
  val id: String,
  val title: String,
  val category: String, // Ultrasound, Lab, Insurance ID
  val date: String,
  val note: String
)

object PmaRepository {
  val patientName = "Fatou"
  val dossierId = "RMA307254"
  val clinicName = "RMA of New York — Cabinet East Side"
  val clinicAddress = "635 Madison Ave, 10th Floor, New York, NY"
  val doctorName = "Dr. Anate Brauer"
  val embryologistInfo = "Alex (Embryologiste)"
  val surgicalCenter = "Gramercy Surgery Center (380 2nd Avenue, Suite 1000)"
  val objective = "Transfert d'Embryon Congelé (TEC) — Blastocyste euploïde masculin"

  val theoreticalDueDate = "Mercredi 6 août 2027"
  val theoreticalDueDateNote = "DPA calculée à 40 SA (basée sur un transfert de blastocyste J5 le 18 novembre 2026)"

  val immediateTasks = listOf(
    TaskItem(
      id = "t1",
      title = "📝 Formulaire de décongélation",
      date = "13 oct.",
      category = "Portail Patient",
      details = "Validé sur le portail patient (sélection d'un embryon euploïde de sexe masculin, score morphologique optimal).",
      isCompleted = true,
      hasAlarm = false
    ),
    TaskItem(
      id = "t2",
      title = "🩺 Sonohystérographie",
      date = "15 oct.",
      category = "Imagerie RMA",
      details = "Réalisée à 8h00 au cabinet RMA East Side.",
      isCompleted = true,
      hasAlarm = false,
      alarmTimeMinutesBefore = 60
    ),
    TaskItem(
      id = "t3",
      title = "🏥 Hystéroscopie opératoire",
      date = "Fin oct.",
      category = "Gramercy Surgery",
      details = "Complétée au Gramercy Surgery Center (380 2nd Avenue, Suite 1000) — résection et révision de la cavité endométriale terminées.",
      isCompleted = true,
      hasAlarm = false
    ),
    TaskItem(
      id = "t4",
      title = "🔬 Monitorages & Bilans",
      date = "05 nov. & 12 nov.",
      category = "Cabinet East Side",
      details = "Réalisés au cabinet East Side.",
      isCompleted = true,
      hasAlarm = false,
      alarmTimeMinutesBefore = 15
    ),
    TaskItem(
      id = "t5",
      title = "🩺 Monitorage pré-transfert",
      date = "17 nov.",
      category = "Cabinet East Side",
      details = "Réalisé à 9h30 AM.",
      isCompleted = true,
      hasAlarm = false,
      alarmTimeMinutesBefore = 15
    ),
    TaskItem(
      id = "t6",
      title = "📅 Confirmation du TEC",
      date = "18 nov. (Aujourd'hui)",
      category = "Transfert TEC",
      details = "Aujourd'hui à 14h00 (02:00 PM EST) 🥳🎉.",
      isCompleted = false,
      hasAlarm = true,
      alarmTimeMinutesBefore = 120
    )
  )

  val phases = listOf(
    PhaseSection(
      phaseNumber = 1,
      title = "Stimulation Ovarienne & Ponction Ovocytaire",
      subtitle = "Protocole de stimulation et prélèvement ovocytaire",
      dateRange = "19 sept. – 01 oct.",
      status = "Complétée",
      yieldBadge = "Bilan de récolte : 42 ovocytes prélevés",
      description = "Préparation ovarienne, monitorages hormonaux & folliculaires séquentiels, déclenchement à heure exacte et ponction sous anesthésie légère.",
      items = listOf(
        TaskItem("p1_1", "Blocage ovarien initial", "19 sept.", "Médication", "Prise quotidienne de Nortrel.", true),
        TaskItem("p1_2", "Début de la stimulation", "21 sept.", "Injections", "Injections quotidiennes : Gonal-F 150 UI + Menopur 150 UI.", true),
        TaskItem("p1_3", "Monitorage 1", "23 sept.", "Échographie & Sang", "Échographie pelvienne et bilan hormonal sanguin à 7h00.", true),
        TaskItem("p1_4", "Blocage du pic ovulatoire (LH)", "25 sept.", "Antagoniste", "Introduction quotidienne de Cétrotide 0,25 mg.", true),
        TaskItem("p1_5", "Monitorage 2", "27 sept.", "Échographie & Sang", "Échographie de folliculométrie et dosages sériques à 7h30.", true),
        TaskItem("p1_6", "Déclenchement (Trigger Shot)", "29 sept.", "Injection Critique", "Injection précisant à 23h00.", true),
        TaskItem("p1_7", "Contrôle pré-opératoire", "30 sept.", "Bilan Sécurité", "Bilan biologique de sécurité et consultation à 8h45.", true),
        TaskItem("p1_8", "Ponction ovocytaire (OPU)", "01 oct.", "Intervention", "Intervention sous anesthésie légère : 42 ovocytes prélevés.", true)
      )
    ),
    PhaseSection(
      phaseNumber = 2,
      title = "Développement Embryonnaire & Préparation au TEC",
      subtitle = "Fécondation ICSI, PGT-A et préparation endométriale",
      dateRange = "02 oct. – Début nov.",
      status = "En cours",
      yieldBadge = "Rendement : 42 ovocytes -> 34 matures -> 11 fécondés -> 8 blastocystes vitrifiés",
      description = "Culture prolongée J5/J6, biopsie trophectoderme PGT-A, hystéroscopie opératoire et protocole d'imprégnation hormonale synchronisée.",
      items = listOf(
        TaskItem("p2_1", "Fécondation ICSI", "02 oct.", "Laboratoire", "11 ovocytes fécondés avec succès.", true),
        TaskItem("p2_2", "Culture prolongée & PGT-A", "06–08 oct.", "Génétique", "Stade blastocyste J5/J6 et biopsie trophectoderme.", true),
        TaskItem("p2_3", "Cryoconservation", "09 oct.", "Laboratoire", "Vitrification de 8 blastocystes.", true),
        TaskItem("p2_4", "Échographie basale pré-TEC", "11 oct.", "Imagerie", "Contrôle morphologique à 9h15 et reprise transitoire de Nortrel.", true),
        TaskItem("p2_5", "Sélection de l'embryon", "13 oct.", "Clinique", "Transfert d'un blastocyste euploïde masculin.", true),
        TaskItem("p2_6", "Bilan de cavité utérine", "15 oct.", "Imagerie", "Sonohystérographie de contraste à 8h00.", true),
        TaskItem("p2_7", "Préparation de l'endomètre", "Mi-oct.", "Hormonologie", "Imprégnation œstrogénique.", true),
        TaskItem("p2_8", "Hystéroscopie opératoire", "Fin oct.", "Chirurgie", "Gramercy Surgery Center.", true),
        TaskItem("p2_9", "Contrôle de phase", "05 nov.", "Monitorage", "Échographie endométriale + bilan hormonal.", true),
        TaskItem("p2_10", "Transfert d'Embryon Congelé (TEC)", "Début nov.", "Procédure", "Dépôt intra-utérin sous guidage échographique (Vessie pleine, sans parfum).", false)
      )
    ),
    PhaseSection(
      phaseNumber = 3,
      title = "Suivi Précoce en Centre PMA — RMA of New York",
      subtitle = "Confirmation biologique et échographies de vitalité précoce",
      dateRange = "08 nov. – Semaines 10",
      status = "À venir",
      description = "Dosages bêta-hCG sériques sériés, vérification de la cinétique de doublement, et échographies précoces avant graduation obstétricale.",
      items = listOf(
        TaskItem("p3_1", "Bêta-hCG sérique sériée #1", "08–10 nov. (J9–J11 post-TEC)", "Laboratoire", "Prise de sang : première confirmation biologique d'implantation.", false),
        TaskItem("p3_2", "Bêta-hCG sérique sériée #2 & #3", "10–12 nov. (J11–J14 post-TEC)", "Laboratoire", "Prises de sang à 48h d'intervalle pour vérifier le doublement et le taux de progestérone.", false),
        TaskItem("p3_3", "Échographie précoce #1", "Semaines 5–6", "Imagerie RMA", "Sac gestationnel intra-utérin & vésicule vitelline.", false),
        TaskItem("p3_4", "Échographie précoce #2", "Semaines 7–8", "Imagerie RMA", "Activité cardiaque fœtale & longueur cranio-caudale (LCC).", false),
        TaskItem("p3_5", "Relais & Sortie PMA (Graduation)", "Semaines 8–10", "Consultation", "Confirmation relève placentaire & transfert obstétrical officiel.", false)
      )
    ),
    PhaseSection(
      phaseNumber = 4,
      title = "Suivi Obstétrical & Maternité",
      subtitle = "Parcours de grossesse et examens prénataux complets",
      dateRange = "Semaines 11 – 41",
      status = "À venir",
      description = "Suivi clinique, dépistage génétique, échographies morphologiques et préparation à l'accouchement à terme.",
      items = listOf(
        TaskItem("p4_1", "Dépistage T1 & DPNI", "Semaines 11–13", "Imagerie & Génétique", "Clarté nucale & ADN fœtal libre circulant.", false),
        TaskItem("p4_2", "Prise en charge obstétricale", "Semaine 12", "Consultation", "Première consultation prénatale officielle.", false),
        TaskItem("p4_3", "Perception active des mouvements", "Semaines 18–20", "Clinique", "Perception sensorielle des premiers mouvements fœtaux (Quickening).", false),
        TaskItem("p4_4", "Échographie morphologique (T2)", "Semaine 22", "Imagerie Spécialisée", "Analyse anatomique complète & insertion placentaire.", false),
        TaskItem("p4_5", "Dépistage du glucose (HGPO)", "Semaines 24–28", "Laboratoire", "Tolérance au glucose pour dépistage du diabète gestationnel.", false),
        TaskItem("p4_6", "Échographie de croissance (T3)", "Semaine 32", "Biométrie", "Biométrie, estimation du poids fœtal, présentation, Doppler fœto-maternel.", false),
        TaskItem("p4_7", "Bilan pré-accouchement", "Semaines 35–37", "Maternité", "Dépistage Streptocoque B, consultation anesthésique & projet de naissance.", false),
        TaskItem("p4_8", "Terme & Accouchement", "Semaines 37–41", "Maternité", "Maturation fœtale complète & surveillance du travail.", false)
      )
    )
  )

  val clinicalInstructions = listOf(
    ClinicalInstruction(
      topic = "Mémo Posologies : Estrace 2 mg PO (TID)",
      subtitle = "Estrace 2 mg PO 3x/jour (Matin, Midi, Soir)",
      instructions = listOf(
        "Posologie : 1 comprimé d'Estrace 2 mg par voie orale 3 fois par jour (Matin 08h00, Midi 14h00, Soir 20h00 en continu).",
        "Historique : Débuté à 2 mg PO 2x/j du 05 au 08 nov., augmenté à 3x/j (TID) depuis le 09 nov.",
        "Continuité : Poursuivre à heures régulières sans interruption."
      ),
      alertType = "Medication"
    ),
    ClinicalInstruction(
      topic = "Mémo Posologies : Progestérone IM (PIO)",
      subtitle = "Injections intramusculaires quotidiennes le soir",
      instructions = listOf(
        "17 & 18 nov. : 1.0 mL IM une fois par jour le soir (à 20h00).",
        "À partir du 19 nov. : Augmentation à 1.5 mL IM une fois par jour le soir.",
        "20 nov. : Progesterone Check @ 9:30 AM (dosage susceptible d'être réajusté selon résultats sériques)."
      ),
      alertType = "Medication"
    ),
    ClinicalInstruction(
      topic = "Jour du Transfert (TEC — 18 nov. à 14h00)",
      subtitle = "Aujourd'hui — Transfer Day 🥳🎉",
      instructions = listOf(
        "Vessie pleine : Boire 750ml à 1L d'eau 45 à 60 minutes avant le geste à 14h00 (02:00 PM EST).",
        "Strictement sans parfum : Ni parfum, déodorant parfumé ni crème pour préserver l'embryon euploïde.",
        "Traitements : Poursuivre rigoureusement Estrace 2 mg TID + PIO 1 mL IM."
      ),
      alertType = "Critical"
    ),
    ClinicalInstruction(
      topic = "Poursuite du Traitement Hormonal",
      subtitle = "Soutien de la phase lutéale",
      instructions = listOf(
        "Ne jamais interrompre ou modifier les prises d'œstrogènes ou de progestérone sans un accord médical formel et écrit de l'équipe du Dr. Brauer."
      ),
      alertType = "Medication"
    ),
    ClinicalInstruction(
      topic = "Période d'Attente post-TEC (Two-Week Wait)",
      subtitle = "Recommandations de repos relatif",
      instructions = listOf(
        "Éviter impérativement les bains très chauds, saunas et séances de hammam.",
        "Suspendre tout effort cardiovasculaire à fort impact jusqu'au premier contrôle sanguin du dosage hCG."
      ),
      alertType = "Recovery"
    )
  )

  val goldenRule = "Règle d'or des 120 Heures (Fenêtre d'Implantation) : Première dose de progestérone (voie intramusculaire ou vaginale) administrée exactement 120 heures avant l'horaire précis prévu pour le transfert du blastocyste J5."
}
