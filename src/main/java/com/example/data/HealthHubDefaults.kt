package com.example.data

object HealthHubDefaults {
  val initialMedReminders = listOf(
    SyncedMedReminder(
      id = "mr_estrace_am",
      title = "Estrace 2 mg (Matin)",
      medication = "Estradiol (Estrace)",
      dosage = "2 mg PO (1 comprimé)",
      hour = 8L,
      minute = 0L,
      repeatDaily = true,
      isEnabled = true,
      protocolPhase = "Phase 2 & 3 • Prépa & TEC (TID)",
      notes = "Prendre le matin à 08:00 avec un grand verre d'eau (1re prise TID)."
    ),
    SyncedMedReminder(
      id = "mr_estrace_midi",
      title = "Estrace 2 mg (Midi)",
      medication = "Estradiol (Estrace)",
      dosage = "2 mg PO (1 comprimé)",
      hour = 14L,
      minute = 0L,
      repeatDaily = true,
      isEnabled = true,
      protocolPhase = "Phase 2 & 3 • Prépa & TEC (TID)",
      notes = "Prendre le midi à 14:00 (2e prise TID, débutée le 09 nov.)."
    ),
    SyncedMedReminder(
      id = "mr_prenatal_am",
      title = "Vitamines Prénatales & Acide Folique",
      medication = "Vitamines Prénatales + B9",
      dosage = "1 gélule (400–800 mcg)",
      hour = 9L,
      minute = 0L,
      repeatDaily = true,
      isEnabled = true,
      protocolPhase = "Soutien Quotidien",
      notes = "Prendre pendant le petit-déjeuner ou déjeuner pour éviter les nausées."
    ),
    SyncedMedReminder(
      id = "mr_estrace_pm",
      title = "Estrace 2 mg (Soir)",
      medication = "Estradiol (Estrace)",
      dosage = "2 mg PO (1 comprimé)",
      hour = 20L,
      minute = 0L,
      repeatDaily = true,
      isEnabled = true,
      protocolPhase = "Phase 2 & 3 • Prépa & TEC (TID)",
      notes = "Prendre le soir à 20:00 (3e prise TID)."
    ),
    SyncedMedReminder(
      id = "mr_progesterone_im",
      title = "Progestérone IM (PIO)",
      medication = "Progestérone dans l'huile",
      dosage = "1.0 mL (17-18 nov.) -> 1.5 mL (dès 19 nov.)",
      hour = 20L,
      minute = 0L,
      repeatDaily = true,
      isEnabled = true,
      protocolPhase = "Phase 2 & 3 • Soutien Lutéal",
      pillsRemaining = 20L,
      notes = "17 & 18 nov. : 1.0 mL IM le soir. Dès le 19 nov. : 1.5 mL IM le soir (à ajuster selon prise de sang du 20 nov.). Chauffer la fiole 5 min avant injection."
    ),
    SyncedMedReminder(
      id = "mr_azithromycine",
      title = "Azithromycine (1 000 mg)",
      medication = "Azithromycine",
      dosage = "2 comprimés (1 000 mg PO)",
      hour = 19L,
      minute = 0L,
      repeatDaily = false,
      isEnabled = false,
      protocolPhase = "13 nov. • Antibiothérapie TEC (Complété)",
      pillsRemaining = 0L,
      notes = "Prise unique pré-transfert le 13 nov. avec nourriture (auto-archivée)."
    )
  )

  val initialPregnancyMeds = listOf(
    SyncedPregnancyMed(
      id = "pm_estrace",
      name = "Estrace (Estradiol 2 mg)",
      activeIngredient = "Estradiol micronisé",
      category = "Protocole PMA / TEC",
      safetyLevel = "SAFE",
      trimester1 = "Prescrit (RMA jusqu'à 8–10 SA)",
      trimester2 = "Sevrage terminé",
      trimester3 = "Non requis",
      clinicalNotes = "Préparation et maintien de la muqueuse endométriale pendant la phase pré-TEC et le début du 1er trimestre sous protocole Dr. Anate Brauer."
    ),
    SyncedPregnancyMed(
      id = "pm_progesterone",
      name = "Progestérone IM (50 mg/mL)",
      activeIngredient = "Progestérone bio-identique",
      category = "Protocole PMA / TEC",
      safetyLevel = "SAFE",
      trimester1 = "Essentiel (1 mL/soir)",
      trimester2 = "Selon avis obstétrical",
      trimester3 = "Sur prescription spécialisée",
      clinicalNotes = "Maintien de la phase lutéale et prévention des fausses couches précoces. Injection chaque soir à heure fixe."
    ),
    SyncedPregnancyMed(
      id = "pm_paracetamol",
      name = "Paracétamol / Acétaminophène (Tylenol)",
      activeIngredient = "Acétaminophène",
      category = "Antalgique & Fièvre",
      safetyLevel = "SAFE",
      trimester1 = "Sûr (dose min. efficace)",
      trimester2 = "Sûr (dose min. efficace)",
      trimester3 = "Sûr (dose min. efficace)",
      clinicalNotes = "Antalgique et antipyrétique de premier choix pendant toute la grossesse (max 3 g/24h sauf avis contraire)."
    ),
    SyncedPregnancyMed(
      id = "pm_folic_acid",
      name = "Acide Folique & Vitamines Prénatales",
      activeIngredient = "Vitamine B9 / L-Méthylfolate + Fer",
      category = "Vitamines & Suppléments",
      safetyLevel = "SAFE",
      trimester1 = "Indispensable (400–800 mcg/j)",
      trimester2 = "Recommandé",
      trimester3 = "Recommandé",
      clinicalNotes = "Prévention des anomalies du tube neural et soutien hématologique. Prendre à distance du thé ou café."
    ),
    SyncedPregnancyMed(
      id = "pm_doxylamine",
      name = "Doxylamine + Vitamine B6 (Diclegis / Bonjesta)",
      activeIngredient = "Succinate de doxylamine + Pyridoxine",
      category = "Nausées & Vomissements",
      safetyLevel = "SAFE",
      trimester1 = "Sûr (1re intention)",
      trimester2 = "Sûr",
      trimester3 = "Sûr",
      clinicalNotes = "Traitement de référence validé ACOG contre les nausées gravidiques matinales."
    ),
    SyncedPregnancyMed(
      id = "pm_omeprazole",
      name = "Oméprazole / Famotidine",
      activeIngredient = "IPP / Antihistaminique H2",
      category = "Reflux & Acidité",
      safetyLevel = "SAFE",
      trimester1 = "Prudence / Sur avis",
      trimester2 = "Sûr",
      trimester3 = "Sûr",
      clinicalNotes = "Efficace contre les brûlures d'estomac et reflux gastro-œsophagiens fréquents au T2 et T3."
    ),
    SyncedPregnancyMed(
      id = "pm_azithromycine",
      name = "Azithromycine (1 000 mg)",
      activeIngredient = "Azithromycine (macrolide)",
      category = "Antibiotiques",
      safetyLevel = "SAFE",
      trimester1 = "Sûr sur prescription",
      trimester2 = "Sûr sur prescription",
      trimester3 = "Sûr sur prescription",
      clinicalNotes = "Utilisée dans le protocole pré-transfert RMA (13 nov.). Toujours prendre avec de la nourriture."
    ),
    SyncedPregnancyMed(
      id = "pm_low_aspirin",
      name = "Aspirine faible dose (81 mg / Bébé Aspirine)",
      activeIngredient = "Acide acétylsalicylique 81 mg",
      category = "Vasculaire & Pré-éclampsie",
      safetyLevel = "CAUTION",
      trimester1 = "Uniquement si prescrit",
      trimester2 = "Sur prescription (12–36 SA)",
      trimester3 = "Arrêt selon OB (~36 SA)",
      clinicalNotes = "Parfois prescrite en FIV ou prévention de la pré-éclampsie. Ne pas modifier la dose sans accord du Dr. Brauer."
    ),
    SyncedPregnancyMed(
      id = "pm_ibuprofen",
      name = "Ibuprofène / AINS (Advil, Motrin, Naproxène)",
      activeIngredient = "Ibuprofène / Naproxène",
      category = "Anti-inflammatoire (AINS)",
      safetyLevel = "AVOID",
      trimester1 = "À ÉVITER (risque fausse couche)",
      trimester2 = "CONTRE-INDIQUÉ (>20 SA)",
      trimester3 = "STRICTEMENT INTERDIT",
      clinicalNotes = "À éviter strictement dès la préparation TEC et pendant toute la grossesse (risque d'oligoamnios et fermeture prématurée du canal artériel)."
    ),
    SyncedPregnancyMed(
      id = "pm_pseudoephedrine",
      name = "Pseudoéphédrine (Sudafed / Décongestionnants)",
      activeIngredient = "Chlorhydrate de pseudoéphédrine",
      category = "ORL & Rhume",
      safetyLevel = "AVOID",
      trimester1 = "À ÉVITER (vasoconstriction)",
      trimester2 = "Déconseillé",
      trimester3 = "À ÉVITER",
      clinicalNotes = "Préférer les lavages au sérum physiologique ou spray nasal d'eau de mer."
    ),
    SyncedPregnancyMed(
      id = "pm_retinoids",
      name = "Rétinoïdes (Isotrétinoïne / Rétinol oral ou fort)",
      activeIngredient = "Isotrétinoïne / Trétinoïne",
      category = "Dermatologie",
      safetyLevel = "AVOID",
      trimester1 = "STRICTEMENT INTERDIT",
      trimester2 = "STRICTEMENT INTERDIT",
      trimester3 = "STRICTEMENT INTERDIT",
      clinicalNotes = "Tératogène majeur. Remplacer par acide azélaïque ou vitamine C topique."
    )
  )

  val initialNutritionFoods = listOf(
    SyncedNutritionFood(
      id = "nf_salmon",
      name = "Saumon sauvage & Sardines bien cuits",
      category = "Poissons Sûrs",
      safetyStatus = "EAT",
      highlights = "Faible en mercure, cuisson à cœur ≥ 63°C. 2 portions/semaine recommandées.",
      nutrients = "Oméga-3 DHA/EPA, Vitamine D, Protéines"
    ),
    SyncedNutritionFood(
      id = "nf_leafy_greens",
      name = "Épinards, Brocolis & Lentilles (bien lavés)",
      category = "Légumes & Légumineuses",
      safetyStatus = "EAT",
      highlights = "Rincer abondamment sous l'eau claire. Idéal pour la nidation et la croissance fœtale.",
      nutrients = "Folates (B9), Fer, Fibres, Calcium"
    ),
    SyncedNutritionFood(
      id = "nf_pasteurized_dairy",
      name = "Yaourt grec, Lait & Fromages pasteurisés (Cheddar, Mozzarella)",
      category = "Produits Laitiers",
      safetyStatus = "EAT",
      highlights = "Vérifier la mention « lait pasteurisé » sur l'emballage.",
      nutrients = "Calcium, Iode, Protéines, Probiotiques"
    ),
    SyncedNutritionFood(
      id = "nf_eggs_cooked",
      name = "Œufs bien cuits (jaune et blanc fermes)",
      category = "Protéines",
      safetyStatus = "EAT",
      highlights = "Éviter les œufs crus ou coulants (mayonnaise maison, œuf mollet).",
      nutrients = "Choline (cerveau bébé), Protéines, B12"
    ),
    SyncedNutritionFood(
      id = "nf_berries_avocado",
      name = "Avocat, Baies, Noix & Amandes",
      category = "Fruits & Bons Lipides",
      safetyStatus = "EAT",
      highlights = "Excellentes graisses insaturées et antioxydants pour l'endomètre et bébé.",
      nutrients = "Acides gras mono-insaturés, Potassium, Vitamine E"
    ),
    SyncedNutritionFood(
      id = "nf_caffeine",
      name = "Café, Thé vert / noir & Chocolat noir",
      category = "Boissons & Caféine",
      safetyStatus = "LIMIT",
      highlights = "Limiter strictement à ≤ 200 mg de caféine/jour (environ 1 petite tasse de café filtre).",
      nutrients = "Polyphénols (modération requise)"
    ),
    SyncedNutritionFood(
      id = "nf_raw_dairy",
      name = "Fromages au lait cru & pâte molle non pasteurisés (Brie, Roquefort, Feta artisanale)",
      category = "Produits Laitiers à Risque",
      safetyStatus = "AVOID",
      highlights = "Risque de listériose (Listeria monocytogenes). N'utiliser que s'ils sont cuits bouillants (ex. gratin).",
      nutrients = "⚠️ Risque infectieux"
    ),
    SyncedNutritionFood(
      id = "nf_raw_fish",
      name = "Poissons crus, Sushis, Tartares & Coquillages crus",
      category = "Poissons & Fruits de Mer",
      safetyStatus = "AVOID",
      highlights = "Risque de parasites et bactéries. Préférer des makis végétariens ou crevettes bien cuites.",
      nutrients = "⚠️ Risque toxoplasmose / listeria"
    ),
    SyncedNutritionFood(
      id = "nf_high_mercury",
      name = "Espadon, Requin, Thazard & Thon rouge (mercure élevé)",
      category = "Poissons à Éviter",
      safetyStatus = "AVOID",
      highlights = "Neurotoxicité fœtale liée au méthylmercure. Limiter aussi le thon blanc en boîte.",
      nutrients = "⚠️ Mercure élevé"
    ),
    SyncedNutritionFood(
      id = "nf_deli_meats",
      name = "Charcuteries froides, Rillettes, Pâtés & Viandes saignantes",
      category = "Viandes & Charcuteries",
      safetyStatus = "AVOID",
      highlights = "À éviter sauf si réchauffées à la vapeur (> 74°C) juste avant dégustation.",
      nutrients = "⚠️ Risque listeria & toxoplasmose"
    )
  )

  val initialGroceryItems = listOf(
    SyncedGroceryItem(id = "gr_1", label = "Épinards frais bio & avocats mûrs", category = "Fruits & Légumes", quantity = "2 sachets + 3", isChecked = false),
    SyncedGroceryItem(id = "gr_2", label = "Myrtilles, fraises & oranges", category = "Fruits & Légumes", quantity = "500 g", isChecked = false),
    SyncedGroceryItem(id = "gr_3", label = "Filets de saumon sauvage", category = "Protéines", quantity = "2 pavés", isChecked = false),
    SyncedGroceryItem(id = "gr_4", label = "Œufs frais catégorie A (cuisson dure)", category = "Protéines", quantity = "12 œufs", isChecked = true),
    SyncedGroceryItem(id = "gr_5", label = "Yaourts grecs pasteurisés nature", category = "Laitiers Pasteur.", quantity = "1 pack (x4)", isChecked = false),
    SyncedGroceryItem(id = "gr_6", label = "Lentilles corail & quinoa complet", category = "Céréales & Folates", quantity = "500 g", isChecked = false),
    SyncedGroceryItem(id = "gr_7", label = "Eau minérale faiblement minéralisée", category = "Hydratation & Collations", quantity = "2 packs", isChecked = true),
    SyncedGroceryItem(id = "gr_8", label = "Noix, amandes non salées & gingembre frais", category = "Hydratation & Collations", quantity = "250 g", isChecked = false)
  )

  val initialHealthIssues = listOf(
    SyncedHealthIssue(
      id = "hi_1",
      title = "Préparation endométriale pré-TEC",
      trimester = "Protocole TEC",
      severity = "Léger",
      dateStr = "13 nov. 2026",
      symptoms = "Légère fatigue et sensibilité mammaire sous Estrace 2 mg PO AM/PM",
      doctorNotes = "Réponse attendue aux œstrogènes. Vérifier l'épaisseur endométriale le 17 nov.",
      prescribedTreatment = "Estrace 2 mg 2x/j + Progestérone IM 50 mg dès ce soir",
      isResolved = false
    ),
    SyncedHealthIssue(
      id = "hi_2",
      title = "Sensibilité au point d'injection IM",
      trimester = "Protocole TEC",
      severity = "Léger",
      dateStr = "13 nov. 2026",
      symptoms = "Prévention douleur Progestérone IM huileuse",
      doctorNotes = "Réchauffer la seringue entre les mains 2 min avant injection + compresse chaude + marcher 10 min après.",
      prescribedTreatment = "Application chaleur locale douce (sans AINS)",
      isResolved = false
    )
  )

  val initialExpenses = listOf(
    SyncedExpenseRecord(
      id = "exp_1",
      title = "Progyny / Pharmacy Statement",
      amount = 30.0,
      insurancePaid = 3547.44,
      totalBill = 3577.44,
      currency = "USD",
      category = "Medical",
      phase = "IVF / Clinic",
      paymentStatus = "Insurance Copay",
      dateStr = "05 Nov 2026",
      notes = "Progyny medical statement & pharmacy statement",
      addedBy = "Fatou"
    ),
    SyncedExpenseRecord(
      id = "exp_2",
      title = "Ordonnance Estrace 2 mg, Progestérone IM & Azithromycine",
      amount = 85.0,
      insurancePaid = 0.0,
      totalBill = 85.0,
      currency = "USD",
      category = "Medical",
      phase = "IVF / Clinic",
      paymentStatus = "Out-of-Pocket",
      dateStr = "06 Nov 2026",
      notes = "Pharmacie spécialisée fertilité",
      addedBy = "Partner"
    ),
    SyncedExpenseRecord(
      id = "exp_3",
      title = "Vitamines Prénatales, Oméga-3 DHA & Fer doux",
      amount = 38.00,
      insurancePaid = 0.0,
      totalBill = 38.00,
      currency = "USD",
      category = "For the Mom",
      phase = "Pregnancy & Baby",
      paymentStatus = "Out-of-Pocket",
      dateStr = "08 Nov 2026",
      notes = "Cure trimestrielle",
      addedBy = "Fatou"
    ),
    SyncedExpenseRecord(
      id = "exp_4",
      title = "Berceau Cododo & Moniteur Bébé",
      amount = 315.00,
      insurancePaid = 0.0,
      totalBill = 315.00,
      currency = "USD",
      category = "Baby",
      phase = "Pregnancy & Baby",
      paymentStatus = "Out-of-Pocket",
      dateStr = "10 Nov 2026",
      notes = "Préparation chambre bébé",
      addedBy = "Partner"
    )
  )

  val initialPartnerTodoItems = listOf(
    SyncedChecklistItem(id = "pp_1", listKey = "partner_todo", label = "Pick up Estrace 2 mg & Progesterone IM refill", isChecked = false, sortOrder = 50L, addedBy = "Partner"),
    SyncedChecklistItem(id = "pp_2", listKey = "partner_todo", label = "Confirm RMA East Side ultrasound insurance copay", isChecked = true, sortOrder = 51L, addedBy = "Fatou"),
    SyncedChecklistItem(id = "pp_3", listKey = "partner_todo", label = "Prepare fragrance-free checklist for Nov 18 TEC day", isChecked = false, sortOrder = 52L, addedBy = "Fatou"),
    SyncedChecklistItem(id = "pp_4", listKey = "partner_todo", label = "Scan pharmacy receipts into Expenses / Wallet", isChecked = false, sortOrder = 53L, addedBy = "Partner")
  )

  val initialCareProviders = listOf(
    SyncedCareProvider(
      id = "cp_1",
      providerType = "Médecin Référent PMA",
      name = "Dr. Anate Brauer — RMA East Side",
      affiliation = "Reproductive Endocrinologist (IVF / FET)",
      phone = "212-756-5777",
      emergencyContact = "212-756-5777",
      address = "635 Madison Ave, 10th Floor, New York, NY 10022",
      portalLink = "https://www.rmany.com",
      email = "abrauer@rmany.com",
      notes = "Médecin Référent PMA & FIV. Prises de sang & échos le matin 7h-9h30.",
      isEmergency = true,
      category = "MEDICAL"
    ),
    SyncedCareProvider(
      id = "cp_2",
      providerType = "Embryologiste",
      name = "Alex — Laboratoire Embryologie",
      affiliation = "RMA of New York — Labo FIV",
      phone = "212-756-5777",
      emergencyContact = "212-756-5777",
      address = "635 Madison Ave, New York, NY 10022",
      portalLink = "https://www.rmany.com",
      email = "embryology@rmany.com",
      notes = "En charge du blastocyste J5 euploïde masculin (Transfert TEC 18 nov.).",
      isEmergency = false,
      category = "MEDICAL"
    ),
    SyncedCareProvider(
      id = "cp_3",
      providerType = "Centre Ambulatoire",
      name = "Gramercy Surgery Center",
      affiliation = "Ambulatory Surgery (Hystéroscopie)",
      phone = "212-674-2000",
      emergencyContact = "212-674-2000",
      address = "380 2nd Avenue, Suite 1000, New York, NY 10010",
      portalLink = "",
      email = "info@gramercysurgery.com",
      notes = "Interventions chirurgicales ambulatoires et contrôle endométrial.",
      isEmergency = false,
      category = "CLINIC"
    ),
    SyncedCareProvider(
      id = "cp_4",
      providerType = "Gynécologue-Obstétricien",
      name = "Dr. Laurent (OB/GYN)",
      affiliation = "Gynécologue-Obstétricien • Centre de Santé Maternelle",
      phone = "+1 (212) 555-0144",
      emergencyContact = "911",
      address = "Centre de Santé Maternelle, New York, NY",
      portalLink = "Portail Maternité",
      email = "dr.laurent@maternite-nyc.org",
      notes = "Suivi maternité et grossesse après diplôme RMA (Phase 4).",
      isEmergency = true,
      category = "MEDICAL"
    ),
    SyncedCareProvider(
      id = "cp_5",
      providerType = "Pharmacie Spécialisée FIV",
      name = "Mandell's Clinical Pharmacy",
      affiliation = "Fertility Specialty Pharmacy",
      phone = "877-252-0553",
      emergencyContact = "877-252-0553",
      address = "Somerset, NJ (Livraison express coursier NYC)",
      portalLink = "https://mymandellspharmacy.com",
      email = "orders@mymandellspharmacy.com",
      notes = "Fournisseur officiel Estrace, PIO (Progestérone IM), aiguilles et seringues.",
      isEmergency = false,
      category = "PHARMACY"
    ),
    SyncedCareProvider(
      id = "cp_6",
      providerType = "Assurance PMA / Progyny",
      name = "Progyny Patient Care Advocate (PCA)",
      affiliation = "Fertility Insurance & Prior Authorizations (Member ID •••• 4071)",
      phone = "888-597-5065",
      emergencyContact = "888-597-5065",
      address = "1359 Broadway, 2nd Floor, New York, NY 10018",
      portalLink = "https://mycompany.progyny.com",
      email = "pca@progyny.com",
      notes = "Dossier Fertilité Smart Cycles & approbations préalables.",
      isEmergency = false,
      category = "INSURANCE"
    ),
    SyncedCareProvider(
      id = "cp_7",
      providerType = "Conjoint & Partenaire",
      name = "Bala — Partenaire PMA",
      affiliation = "Contact d'Urgence Prioritaire",
      phone = "929-900-5110",
      emergencyContact = "929-900-5110",
      address = "New York, NY",
      portalLink = "",
      email = "bala@balatec.com",
      notes = "Conjoint de Fatou, disponible 24h/24 pour urgences ou transports.",
      isEmergency = true,
      category = "FAMILY"
    )
  )
}
