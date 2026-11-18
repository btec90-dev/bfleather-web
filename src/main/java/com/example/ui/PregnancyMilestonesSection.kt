package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeColors
import org.json.JSONArray
import org.json.JSONObject

data class PregnancyMilestoneItem(
  val week: Int,
  val sizeCm: String,
  val weightGrams: String,
  val fruitOrObject: String,
  val fruitEmoji: String,
  val trimester: Int, // 1, 2, 3
  val title: String,
  val description: String,
  val maternalTip: String,
  val tags: List<String>,
  val userNotes: String = "",
  val isCompleted: Boolean = false
)

object PregnancyMilestonesDefaults {
  fun getDefaultList(isFr: Boolean): List<PregnancyMilestoneItem> = listOf(
    // -------------------------------------------------------------
    // TRIMESTRE 1 (Semaines 1 à 13)
    // -------------------------------------------------------------
    PregnancyMilestoneItem(
      week = 1, sizeCm = "0.1 mm", weightGrams = "< 1 mg",
      fruitOrObject = if (isFr) "Ovule fécondé" else "Fertilized Ovum", fruitEmoji = "✨",
      trimester = 1,
      title = if (isFr) "Fécondation & Fusion cellulaire" else "Fertilization & Genetic Fusion",
      description = if (isFr) "Fécondation et formation du zygote. Les 46 chromosomes sont réunis pour fixer l'ADN unique du futur bébé." else "Fertilization and zygote formation uniting the 46 chromosomes.",
      maternalTip = if (isFr) "Prendre de l'acide folique (400–800 mcg) quotidiennement pour soutenir le tube neural." else "Take daily folic acid to support neural tube development.",
      tags = listOf("🧬 Caryotype fixé", "✨ Stade zygote", "🔬 Division cellulaire")
    ),
    PregnancyMilestoneItem(
      week = 2, sizeCm = "0.2 mm", weightGrams = "< 1 mg",
      fruitOrObject = if (isFr) "Blastocyste J5" else "Day 5 Blastocyst", fruitEmoji = "🔬",
      trimester = 1,
      title = if (isFr) "Stade blastocyste & bouton embryonnaire" else "Blastocyst & Inner Cell Mass",
      description = if (isFr) "Division cellulaire exponentielle jusqu'à une centaine de cellules (blastocyste J5/J6). La masse interne deviendra le bébé et le trophoblaste formera le placenta." else "Rapid cell division reaching 100+ cells (Day 5 blastocyst ready for transfer).",
      maternalTip = if (isFr) "Soutien progestatif (PIO) régulier pour favoriser la réceptivité endométriale." else "Maintain regular progesterone injections for endometrial receptivity.",
      tags = listOf("🔬 100+ cellules", "🛡️ Trophoblaste actif", "🎯 Prêt au transfert")
    ),
    PregnancyMilestoneItem(
      week = 3, sizeCm = "0.4 mm", weightGrams = "< 1 mg",
      fruitOrObject = if (isFr) "Graine de vanille" else "Vanilla Seed", fruitEmoji = "🌱",
      trimester = 1,
      title = if (isFr) "Implantation & Nidation utérine" else "Implantation & Uterine Nidation",
      description = if (isFr) "Le blastocyste s'enfouit doucement dans l'endomètre réceptif. Les cellules trophoblastiques commencent à sécréter la précieuse hormone hCG." else "The blastocyst implants in the endometrial wall and begins secreting hCG.",
      maternalTip = if (isFr) "Repos relatif, bonne hydratation et éviter les bains très chauds ou saunas." else "Gentle rest, hydration, and avoid excessive heat sources.",
      tags = listOf("🌱 Nidation active", "🧪 Sécrétion Bêta-hCG", "🛡️ Accueil endométrial")
    ),
    PregnancyMilestoneItem(
      week = 4, sizeCm = "1.0 mm", weightGrams = "< 1 g",
      fruitOrObject = if (isFr) "Graine de pavot" else "Poppy Seed", fruitEmoji = "🌾",
      trimester = 1,
      title = if (isFr) "Disque embryonnaire & sac amniotique" else "Embryonic Disk & Amniotic Sac",
      description = if (isFr) "Différenciation en trois feuillets primordiaux (ectoderme, mésoderme, endoderme) qui donneront naissance à tous les futurs organes." else "Differentiation into 3 germ layers forming all future bodily organs.",
      maternalTip = if (isFr) "Le test sanguin Bêta-hCG confirme la grossesse biologique avec certitude." else "Beta-hCG blood test reliably confirms biological pregnancy.",
      tags = listOf("🌾 3 feuillets embryonnaires", "🩸 Détection Bêta-hCG", "🫧 Sac gestationnel")
    ),
    PregnancyMilestoneItem(
      week = 5, sizeCm = "2.0 mm", weightGrams = "< 0.1 g",
      fruitOrObject = if (isFr) "Graine de sésame" else "Sesame Seed", fruitEmoji = "🌱",
      trimester = 1,
      title = if (isFr) "Tube neural & pulsations primitives" else "Neural Tube & Heart Pulsations",
      description = if (isFr) "Fermeture du tube neural (cerveau et moelle épinière). Le premier tube cardiaque primitif commence ses toutes premières contractions." else "Neural tube closure and initial rhythmic contractions of the primitive heart tube.",
      maternalTip = if (isFr) "Petits repas fractionnés et tisane de gingembre en cas de premières nausées." else "Small frequent meals and ginger tea for mild early nausea.",
      tags = listOf("❤️ Premières pulsations", "🧠 Tube neural fermé", "🌱 Ébauche vasculaire")
    ),
    PregnancyMilestoneItem(
      week = 6, sizeCm = "4.0 mm", weightGrams = "0.2 g",
      fruitOrObject = if (isFr) "Grain de lentille" else "Lentil", fruitEmoji = "🌿",
      trimester = 1,
      title = if (isFr) "Battements cardiaques & bourgeons des membres" else "Heartbeat & Limb Buds",
      description = if (isFr) "Le cœur bat à 100–120 bpm ! Les petites ébauches des bras et des jambes apparaissent sous forme de minuscules bourgeons." else "Heart is beating at 100–120 bpm. Limb buds for arms and legs become visible.",
      maternalTip = if (isFr) "Première échographie de viabilité pour visualiser le sac gestationnel et la vésicule vitelline." else "First viability ultrasound to observe gestational sac and yolk sac.",
      tags = listOf("💓 Cœur régulier 110 bpm", "👀 Vésicules optiques", "🌿 Bourgeons des membres")
    ),
    PregnancyMilestoneItem(
      week = 7, sizeCm = "1.0 cm", weightGrams = "0.5 g",
      fruitOrObject = if (isFr) "Myrtille" else "Blueberry", fruitEmoji = "🫐",
      trimester = 1,
      title = if (isFr) "Hémisphères cérébraux & visage" else "Cerebral Hemispheres & Facial Outline",
      description = if (isFr) "Croissance fulgurante du cerveau (100 000 neurones générés par minute !). Ébauche des narines, de la bouche et de la rétine des yeux." else "Rapid brain growth (100k neurons/min). Nostrils, mouth, and retina form.",
      maternalTip = if (isFr) "Boire 1.5L à 2L d'eau par jour pour soutenir l'augmentation du volume sanguin." else "Drink 1.5–2L of water daily to support blood volume expansion.",
      tags = listOf("🧠 100k neurones/min", "👁️ Rétines formées", "🦷 Bourgeons dentaires")
    ),
    PregnancyMilestoneItem(
      week = 8, sizeCm = "1.6 cm", weightGrams = "1.0 g",
      fruitOrObject = if (isFr) "Framboise" else "Raspberry", fruitEmoji = "🍓",
      trimester = 1,
      title = if (isFr) "Doigts et orteils en séparation" else "Fingers & Toes Separation",
      description = if (isFr) "Les bras et jambes s'allongent et se fléchissent. Les doigts palmés commencent à se séparer. Les paupières recouvrent délicatement les yeux." else "Arms and legs flex, webbed digits separate, eyelids cover developing eyes.",
      maternalTip = if (isFr) "Poursuivre les vitamines prénatales riches en fer, iode et vitamine B9." else "Continue prenatal vitamins rich in iron, iodine, and vitamin B9.",
      tags = listOf("🖐️ Séparation des doigts", "🫁 Arborescence bronchique", "🦵 Articulations")
    ),
    PregnancyMilestoneItem(
      week = 9, sizeCm = "2.3 cm", weightGrams = "2.0 g",
      fruitOrObject = if (isFr) "Cerise" else "Cherry", fruitEmoji = "🍒",
      trimester = 1,
      title = if (isFr) "Passage officiel au statut de Fœtus" else "Transition from Embryo to Fetus",
      description = if (isFr) "La queue embryonnaire a totalement disparu ! L'embryon devient officiellement un fœtus. Les coudes et genoux peuvent fléchir." else "Embryonic tail is gone; baby is now officially a fetus with movable joints.",
      maternalTip = if (isFr) "Le repos en début d'après-midi aide à lutter contre la somnolence hormonale." else "Afternoon rest helps combat progesterone-induced fatigue.",
      tags = listOf("👶 Statut fœtal officiel", "💪 Coudes et genoux", "🫀 4 cavités cardiaques")
    ),
    PregnancyMilestoneItem(
      week = 10, sizeCm = "3.1 cm", weightGrams = "4.0 g",
      fruitOrObject = if (isFr) "Fraise" else "Strawberry", fruitEmoji = "🍓",
      trimester = 1,
      title = if (isFr) "Ossification du squelette & déglutition" else "Bone Ossification & Swallowing",
      description = if (isFr) "Les organes vitaux (reins, foie, intestins) fonctionnent ensemble. Le fœtus déglutit ses premières gorgées de liquide amniotique." else "Vital organs are functional. Fetus swallows first sips of amniotic fluid.",
      maternalTip = if (isFr) "Les reins fœtaux éliminent leurs premières urines stériles dans la cavité amniotique." else "Fetal kidneys produce clear sterile urine into the amniotic sac.",
      tags = listOf("🦴 Début ossification", "💧 Déglutition réflexe", "🫁 Mouvements thoraciques")
    ),
    PregnancyMilestoneItem(
      week = 11, sizeCm = "4.1 cm", weightGrams = "7.0 g",
      fruitOrObject = if (isFr) "Figue" else "Fig", fruitEmoji = "🫐",
      trimester = 1,
      title = if (isFr) "Mouvements spontanés & ongles" else "Spontaneous Movement & Nails",
      description = if (isFr) "Le fœtus s'étire et fait de gracieux mouvements dans l'utérus. Les follicules pileux et les ongles microscopiques apparaissent." else "Active stretches in the amniotic fluid. Hair follicles and nail beds emerge.",
      maternalTip = if (isFr) "Période idéale pour planifier la prise de sang DPNI (dépistage prénatal non invasif)." else "Ideal window to schedule NIPT non-invasive prenatal testing.",
      tags = listOf("🤸 Mouvements spontanés", "💅 Ébauche des ongles", "🩸 Bilan DPNI possible")
    ),
    PregnancyMilestoneItem(
      week = 12, sizeCm = "5.4 cm", weightGrams = "14 g",
      fruitOrObject = if (isFr) "Citron vert" else "Lime", fruitEmoji = "🍋",
      trimester = 1,
      title = if (isFr) "Échographie T1 & Clarté nucale" else "First Trimester Scan & Nuchal Translucency",
      description = if (isFr) "Échographie capitale du 1er trimestre : mesure de la clarté nucale, profil facial, 4 membres et cœur battant à 150 bpm." else "Key T1 ultrasound: nuchal translucency measurement, full facial profile and heart at 150 bpm.",
      maternalTip = if (isFr) "Grand moment : vous pouvez entendre le galop vigoureux du cœur au Doppler !" else "Great moment: you can hear the heartbeat on Doppler!",
      tags = listOf("🩺 Clarté nucale mesurée", "❤️ Cœur 150 bpm au Doppler", "🧬 Dépistage T1")
    ),
    PregnancyMilestoneItem(
      week = 13, sizeCm = "7.4 cm", weightGrams = "23 g",
      fruitOrObject = if (isFr) "Prune" else "Plum", fruitEmoji = "🍑",
      trimester = 1,
      title = if (isFr) "Fin du 1er trimestre & empreintes digitales" else "End of 1st Trimester & Fingerprints",
      description = if (isFr) "Dernière semaine du premier trimestre ! Les empreintes digitales uniques et définitives se gravent sur la pulpe de ses petits doigts." else "Final week of T1. Unique permanent fingerprints form on tiny fingertips.",
      maternalTip = if (isFr) "Le placenta assure désormais l'autonomie hormonale de la grossesse." else "The placenta now fully assumes hormonal production.",
      tags = listOf("🖐️ Empreintes digitales uniques", "🛡️ Placenta autonome", "🎉 Fin du 1er trimestre")
    ),

    // -------------------------------------------------------------
    // TRIMESTRE 2 (Semaines 14 à 27)
    // -------------------------------------------------------------
    PregnancyMilestoneItem(
      week = 14, sizeCm = "8.7 cm", weightGrams = "43 g",
      fruitOrObject = if (isFr) "Citron jaune" else "Lemon", fruitEmoji = "🍋",
      trimester = 2,
      title = if (isFr) "Bienvenue au 2ème trimestre ! Expressions faciales" else "Welcome to 2nd Trimester! Facial Expressions",
      description = if (isFr) "Le fœtus fronce les sourcils, sourit et suce son pouce ! Les muscles du visage s'exercent activement." else "Fetus makes facial expressions, smiles, frowns, and sucks thumb.",
      maternalTip = if (isFr) "Gain d'énergie fréquent chez la future maman (la lune de miel de la grossesse)." else "Energy returns for the mother during this honeymoon period.",
      tags = listOf("😊 Mimiques faciales", "👍 Succion du pouce", "🌟 Bienvenue au 2ème trimestre")
    ),
    PregnancyMilestoneItem(
      week = 15, sizeCm = "10.1 cm", weightGrams = "70 g",
      fruitOrObject = if (isFr) "Pomme" else "Apple", fruitEmoji = "🍎",
      trimester = 2,
      title = if (isFr) "Audition des sons graves & perception lumineuse" else "Auditory Development & Light Perception",
      description = if (isFr) "Les petits osselets de l'oreille interne se consolident. Le fœtus perçoit les voix graves et la lumière à travers la peau abdominale." else "Inner ear bones harden; baby can perceive low-pitch parental voices.",
      maternalTip = if (isFr) "Parler ou chantonner doucement au bébé : les fréquences apaisantes traversent le liquide amniotique." else "Talk gently to the baby as soothing frequencies cross amniotic fluid.",
      tags = listOf("👂 Osselets auditifs", "💡 Perception lumineuse", "🦴 Squelette articulé")
    ),
    PregnancyMilestoneItem(
      week = 16, sizeCm = "11.6 cm", weightGrams = "100 g",
      fruitOrObject = if (isFr) "Avocat" else "Avocado", fruitEmoji = "🥑",
      trimester = 2,
      title = if (isFr) "Pompe cardiaque 28L/jour & cordon robuste" else "Cardiac Pump (28L/day) & Strong Cord",
      description = if (isFr) "Le cœur fœtal pompe près de 28 litres de sang par jour ! Le cordon ombilical est vigoureux avec ses 2 artères et sa veine." else "Heart pumps 28L of blood daily. The umbilical cord is strong with 3 vessels.",
      maternalTip = if (isFr) "De légers frémissements (comme des ailes de papillon) peuvent être ressentis dans le bas-ventre." else "Light flutters (like butterfly wings) may be felt in lower abdomen.",
      tags = listOf("❤️ 28L de sang pompés/j", "🌀 Cordon à 3 vaisseaux", "🦋 Premiers frémissements")
    ),
    PregnancyMilestoneItem(
      week = 17, sizeCm = "13.0 cm", weightGrams = "140 g",
      fruitOrObject = if (isFr) "Navet" else "Turnip", fruitEmoji = "🥔",
      trimester = 2,
      title = if (isFr) "Graisse brune thermorégulatrice" else "Brown Adipose Tissue",
      description = if (isFr) "Accumulation de tissu adipeux brun sous la peau, indispensable pour maintenir sa température corporelle à la naissance." else "Brown fat accumulates to keep baby warm after birth.",
      maternalTip = if (isFr) "Adopter de bonnes postures assises pour soulager les ligaments utérins et le dos." else "Adopt good sitting posture to relieve uterine ligaments and back.",
      tags = listOf("🔥 Graisse brune", "🦵 Élongation des membres", "🧘 Étirements doux")
    ),
    PregnancyMilestoneItem(
      week = 18, sizeCm = "14.2 cm", weightGrams = "190 g",
      fruitOrObject = if (isFr) "Poire" else "Pear", fruitEmoji = "🍐",
      trimester = 2,
      title = if (isFr) "Perception des mouvements (Quickening)" else "Fetal Movements (Quickening)",
      description = if (isFr) "Le fœtus est très actif : coups de pieds, flexions et cabrioles ! Les mouvements sont désormais nettement perceptibles." else "Active kicks and somersaults are now clearly felt by the mother.",
      maternalTip = if (isFr) "Fatou peut guider la main de Bala sur le ventre pour ressentir ensemble ces premiers coups !" else "Fatou can guide Bala's hand to feel these first movements together!",
      tags = listOf("🦶 Premiers coups nets", "🧠 Gaine de myéline", "❤️ Moment magique à deux")
    ),
    PregnancyMilestoneItem(
      week = 19, sizeCm = "15.3 cm", weightGrams = "240 g",
      fruitOrObject = if (isFr) "Mangue" else "Mango", fruitEmoji = "🥭",
      trimester = 2,
      title = if (isFr) "Vernix caseosa protecteur" else "Protective Vernix Caseosa",
      description = if (isFr) "Une substance blanche crémeuse (le vernix) recouvre et protège la peau délicate du bébé contre l'immersion prolongée dans le liquide amniotique." else "Creamy white vernix protects baby's delicate skin in amniotic fluid.",
      maternalTip = if (isFr) "Hydrater la peau du ventre avec de l'huile d'amande douce ou beurre de karité." else "Moisturize belly skin with sweet almond oil or shea butter.",
      tags = listOf("🛡️ Manteau de vernix", "👂 Reconnaissance des voix", "💧 Peau protégée")
    ),
    PregnancyMilestoneItem(
      week = 20, sizeCm = "16.4 cm", weightGrams = "300 g",
      fruitOrObject = if (isFr) "Banane" else "Banana", fruitEmoji = "🍌",
      trimester = 2,
      title = if (isFr) "Mi-parcours exact ! Échographie morphologique T2" else "Mid-Pregnancy Milestone! T2 Anatomy Scan",
      description = if (isFr) "20 semaines : la moitié exacte de la grossesse est franchie ! Examen morphologique minutieux de chaque organe, du cerveau et des 4 cavités cardiaques." else "Halfway there! Complete organ anatomy scan and cardiac four-chamber review.",
      maternalTip = if (isFr) "Émotion garantie : vous admirez en direct le profil complet et les mimiques de votre petit garçon !" else "Emotional milestone: admire the complete anatomy of your baby boy!",
      tags = listOf("🎯 50% du voyage atteint", "🩺 Écho morphologique T2", "💙 Garçon confirmé")
    ),
    PregnancyMilestoneItem(
      week = 21, sizeCm = "26.7 cm", weightGrams = "360 g",
      fruitOrObject = if (isFr) "Carotte" else "Carrot", fruitEmoji = "🥕",
      trimester = 2,
      title = if (isFr) "Mesure tête-talons & cycles de sommeil" else "Crown-to-Heel Measurement & Sleep Cycles",
      description = if (isFr) "Mesuré de la tête aux talons ! Le fœtus a des cycles de sommeil bien réglés et bouge avec coordination." else "Measured from head to heel. Regulated sleep cycles and coordinated limb movements.",
      maternalTip = if (isFr) "Le système digestif fœtal absorbe des petites quantités de sucres du liquide amniotique dégluti." else "Fetal digestive tract absorbs small amounts of swallowed sugars.",
      tags = listOf("📏 Mesure tête-talons", "😴 Sommeil régulier", "🥕 Prise de longueur")
    ),
    PregnancyMilestoneItem(
      week = 22, sizeCm = "27.8 cm", weightGrams = "430 g",
      fruitOrObject = if (isFr) "Papaye" else "Papaya", fruitEmoji = "🍈",
      trimester = 2,
      title = if (isFr) "Sens du toucher & préhension du cordon" else "Sense of Touch & Cord Grasping",
      description = if (isFr) "Bébé explore son environnement : il touche son visage, ses pieds et serre le cordon ombilical avec ses petits doigts." else "Baby touches face, feet, and firmly grasps the umbilical cord.",
      maternalTip = if (isFr) "Prendre des respirations profondes et relaxer le plancher pelvien." else "Take deep breaths and relax pelvic floor muscles.",
      tags = listOf("🖐️ Préhension active", "👃 Narines ouvertes", "🦶 Caresse du visage")
    ),
    PregnancyMilestoneItem(
      week = 23, sizeCm = "28.9 cm", weightGrams = "500 g",
      fruitOrObject = if (isFr) "Pamplemousse" else "Grapefruit", fruitEmoji = "🍊",
      trimester = 2,
      title = if (isFr) "Cap des 500 grammes & mouvements respiratoires" else "500g Milestone & Breathing Practice",
      description = if (isFr) "Cap symbolique du demi-kilo franchi ! Le thorax s'exerce aux mouvements respiratoires en soulevant doucement le diaphragme." else "Symbolic half-kilo mark passed. Chest practices rhythmic respiratory exercises.",
      maternalTip = if (isFr) "Le bébé peut avoir de petits hoquets perceptibles sous forme de soubresauts rythmiques." else "Baby may experience harmless rhythmic hiccups.",
      tags = listOf("⚖️ Poids 500g atteint", "🫁 Entraînement pulmonaire", "🎈 Petits hoquets bénins")
    ),
    PregnancyMilestoneItem(
      week = 24, sizeCm = "30.0 cm", weightGrams = "600 g",
      fruitOrObject = if (isFr) "Épi de maïs" else "Corn Ear", fruitEmoji = "🌽",
      trimester = 2,
      title = if (isFr) "Seuil de viabilité médicale & surfactant" else "Viability Threshold & Surfactant",
      description = if (isFr) "Étape fondamentale : seuil de viabilité médicale franchi. Les poumons commencent à fabriquer le surfactant pour garder les alvéoles ouvertes." else "Medical viability threshold reached. Lungs begin producing alveolar surfactant.",
      maternalTip = if (isFr) "Test d'hyperglycémie provoquée (HGPO) au laboratoire entre 24 et 28 SA pour dépister le diabète gestationnel." else "Glucose screening test (OGTT) at lab between 24 and 28 weeks.",
      tags = listOf("⭐ Seuil de viabilité franchi", "🫁 Début du surfactant", "🧪 Dépistage glucose HGPO")
    ),
    PregnancyMilestoneItem(
      week = 25, sizeCm = "34.6 cm", weightGrams = "660 g",
      fruitOrObject = if (isFr) "Rutabaga" else "Rutabaga", fruitEmoji = "🥔",
      trimester = 2,
      title = if (isFr) "Moelle osseuse active & capillaires sanguins" else "Bone Marrow Activity & Capillaries",
      description = if (isFr) "La moelle osseuse assure l'essentiel de la production de globules rouges. Les capillaires sanguins donnent une belle couleur rosée à la peau." else "Bone marrow produces red blood cells. Capillaries give skin a healthy pink glow.",
      maternalTip = if (isFr) "Maintenir des apports en fer satisfaisants pour prévenir l'anémie maternelle." else "Maintain adequate iron intake to prevent maternal anemia.",
      tags = listOf("🩸 Hématopoïèse active", "👂 Réagit aux voix familières", "🌸 Teint rosé")
    ),
    PregnancyMilestoneItem(
      week = 26, sizeCm = "35.6 cm", weightGrams = "760 g",
      fruitOrObject = if (isFr) "Courgette" else "Zucchini", fruitEmoji = "🥒",
      trimester = 2,
      title = if (isFr) "Ouverture des paupières & cils" else "Eye Opening & Eyelashes",
      description = if (isFr) "Les yeux s'ouvrent ! Les cils et sourcils sont nettement dessinés. Bébé cligne des yeux et perçoit les faisceaux de lumière." else "Eyes open! Eyelashes and eyebrows formed; baby blinks in response to light.",
      maternalTip = if (isFr) "De légères marches en plein air favorisent le retour veineux et le sommeil." else "Light outdoor walks promote venous return and restful sleep.",
      tags = listOf("👁️ Yeux grands ouverts", "👀 Réflexe de clignement", "🚶 Marche quotidienne")
    ),
    PregnancyMilestoneItem(
      week = 27, sizeCm = "36.6 cm", weightGrams = "875 g",
      fruitOrObject = if (isFr) "Chou-fleur" else "Cauliflower", fruitEmoji = "🥦",
      trimester = 2,
      title = if (isFr) "Fin du 2ème trimestre ! Cortex cérébral plissé" else "End of 2nd Trimester! Complex Brain Cortex",
      description = if (isFr) "Dernière semaine du deuxième trimestre ! Le cerveau multiplie ses connexions. Le bébé reconnaît instantanément la voix de Fatou et de Bala." else "Final week of T2. Brain forms intricate neural connections, recognizing parents' voices.",
      maternalTip = if (isFr) "Commencer à rassembler les éléments de la chambre de bébé en toute sérénité." else "Begin preparing the nursery room with peace of mind.",
      tags = listOf("🧠 Cortex cérébral affiné", "🎵 Reconnaissance vocale", "🎉 Fin du 2ème trimestre")
    ),

    // -------------------------------------------------------------
    // TRIMESTRE 3 (Semaines 28 à 41)
    // -------------------------------------------------------------
    PregnancyMilestoneItem(
      week = 28, sizeCm = "37.6 cm", weightGrams = "1 005 g",
      fruitOrObject = if (isFr) "Aubergine" else "Eggplant", fruitEmoji = "🍆",
      trimester = 3,
      title = if (isFr) "Bienvenue au 3ème trimestre ! Cap des 1 kg" else "Welcome to 3rd Trimester! 1 kg Milestone",
      description = if (isFr) "Entrée dans le troisième trimestre ! Le bébé pèse plus d'un kilo. Ses cycles d'éveil et de repos sont très perceptibles." else "Entering third trimester! Weight exceeds 1 kg with distinct wake and rest patterns.",
      maternalTip = if (isFr) "Consultation mensuelle du T3 et bilan sanguin de contrôle." else "Monthly T3 consultation and routine blood checkup.",
      tags = listOf("⚖️ Cap 1 kg dépassé", "🥉 Bienvenue au T3", "👶 Présence utérine affirmée")
    ),
    PregnancyMilestoneItem(
      week = 29, sizeCm = "38.6 cm", weightGrams = "1 150 g",
      fruitOrObject = if (isFr) "Butternut" else "Butternut Squash", fruitEmoji = "🎃",
      trimester = 3,
      title = if (isFr) "Minéralisation osseuse & réserves de calcium" else "Bone Mineralization & Calcium Stores",
      description = if (isFr) "Les os s'enrichissent en calcium pour durcir. Le crâne reste volontairement malléable pour permettre le futur passage lors de l'accouchement." else "Bones absorb calcium to harden; skull remains flexible for delivery.",
      maternalTip = if (isFr) "Consommer des sources saines de calcium et de vitamine D." else "Consume healthy sources of calcium and vitamin D.",
      tags = listOf("🦴 Minéralisation du squelette", "🧠 Contrôle autonome", "🥛 Calcium & Vitamine D")
    ),
    PregnancyMilestoneItem(
      week = 30, sizeCm = "39.9 cm", weightGrams = "1 320 g",
      fruitOrObject = if (isFr) "Concombre" else "Cucumber", fruitEmoji = "🥒",
      trimester = 3,
      title = if (isFr) "Maturation cérébrale & rêves fœtaux" else "Brain Development & Fetal Dreams",
      description = if (isFr) "Le cerveau forme de profondes circonvolutions. Les tracés électriques montrent des phases de sommeil paradoxal correspondant aux rêves fœtaux." else "Brain forms deep convolutions; EEG shows REM sleep indicative of dreaming.",
      maternalTip = if (isFr) "Dormir sur le côté gauche avec un coussin entre les jambes pour maximiser le flux sanguin placentaire." else "Sleep on left side with pillow between knees for optimal blood flow.",
      tags = listOf("💭 Phases de rêves fœtaux", "👁️ Pupilles contractiles", "🛏️ Sommeil côté gauche")
    ),
    PregnancyMilestoneItem(
      week = 31, sizeCm = "41.1 cm", weightGrams = "1 500 g",
      fruitOrObject = if (isFr) "Noix de coco" else "Coconut", fruitEmoji = "🥥",
      trimester = 3,
      title = if (isFr) "Les 5 sens pleinement connectés" else "All 5 Senses Connected",
      description = if (isFr) "Bébé goûte le liquide amniotique, écoute les voix, ressent la lumière et réagit aux caresses bienveillantes sur le ventre." else "All senses active: taste, hearing, light perception, and tactile bonding.",
      maternalTip = if (isFr) "Séances d'haptonomie : poser des mains chaleureuses sur le ventre pour communiquer avec lui." else "Haptonomy sessions: place warm hands on belly to bond with baby.",
      tags = listOf("✋ 5 sens connectés", "🍯 Goût des saveurs", "❤️ Haptonomie en couple")
    ),
    PregnancyMilestoneItem(
      week = 32, sizeCm = "42.4 cm", weightGrams = "1 700 g",
      fruitOrObject = if (isFr) "Chou frisé" else "Kale", fruitEmoji = "🥬",
      trimester = 3,
      title = if (isFr) "Échographie T3 & chute du lanugo" else "T3 Ultrasound & Lanugo Shedding",
      description = if (isFr) "Dernière échographie de référence : biométrie fœtale, estimation du poids à terme, évaluation du liquide amniotique et du placenta." else "Final official ultrasound: fetal biometry, growth estimation, fluid and placental health.",
      maternalTip = if (isFr) "Le fin duvet (lanugo) commence à tomber, laissant place à une peau douce et dodue." else "Fine lanugo hair begins shedding, revealing soft chubby skin.",
      tags = listOf("🩺 Échographie T3 de croissance", "👶 Peau rose et dodue", "⚖️ Poids 1.7 kg")
    ),
    PregnancyMilestoneItem(
      week = 33, sizeCm = "43.7 cm", weightGrams = "1 920 g",
      fruitOrObject = if (isFr) "Ananas" else "Pineapple", fruitEmoji = "🍍",
      trimester = 3,
      title = if (isFr) "Transfert massif d'anticorps maternels" else "Maternal Antibody Transfer",
      description = if (isFr) "La maman transmet par le placenta une grande quantité d'anticorps protecteurs (IgG) pour bâtir le système immunitaire du nouveau-né." else "Placenta transfers maternal IgG antibodies to build newborn immunity.",
      maternalTip = if (isFr) "Boire régulièrement de l'eau : le volume de liquide amniotique se renouvelle toutes les 3 heures." else "Drink water frequently; amniotic fluid refreshes every 3 hours.",
      tags = listOf("🛡️ Bouclier immunitaire", "💧 Liquide renouvelé", "🍍 Longueur 43 cm")
    ),
    PregnancyMilestoneItem(
      week = 34, sizeCm = "45.0 cm", weightGrams = "2 150 g",
      fruitOrObject = if (isFr) "Cantaloup" else "Cantaloupe", fruitEmoji = "🍈",
      trimester = 3,
      title = if (isFr) "Poumons matures & valise de maternité" else "Mature Lungs & Hospital Bag",
      description = if (isFr) "Les alvéoles pulmonaires ont atteint une maturité quasi-complète. Le bébé est prêt pour ses premiers échanges gazeux à l'air libre." else "Lungs are mature and ready for breathing ambient air at birth.",
      maternalTip = if (isFr) "Préparer la valise de maternité pour la maman et le nouveau-né en toute quiétude." else "Prepare the hospital maternity bag calmly with essentials.",
      tags = listOf("🫁 Poumons quasi-matures", "👜 Valise de maternité", "👶 Bébé tonique")
    ),
    PregnancyMilestoneItem(
      week = 35, sizeCm = "46.2 cm", weightGrams = "2 380 g",
      fruitOrObject = if (isFr) "Melon miel" else "Honeydew", fruitEmoji = "🍈",
      trimester = 3,
      title = if (isFr) "Prise de poids accélérée (230g/semaine)" else "Rapid Weight Gain (+230g/week)",
      description = if (isFr) "L'espace devient douillettement serré : bébé ne fait plus de culbutes mais pousse vigoureusement avec ses talons et ses fesses." else "Tight womb space: baby pushes firmly with feet and buttocks.",
      maternalTip = if (isFr) "Prélèvement de dépistage du Streptocoque du groupe B (SGB) entre 35 et 37 SA." else "Group B Streptococcus (GBS) screening between 35 and 37 weeks.",
      tags = listOf("📈 +230g par semaine", "🦶 Poussées vigoureuses", "🩺 Dépistage Streptocoque B")
    ),
    PregnancyMilestoneItem(
      week = 36, sizeCm = "47.4 cm", weightGrams = "2 620 g",
      fruitOrObject = if (isFr) "Laitue romaine" else "Romaine Lettuce", fruitEmoji = "🥬",
      trimester = 3,
      title = if (isFr) "Position céphalique tête en bas" else "Cephalic Head-Down Position",
      description = if (isFr) "La grande majorité des bébés sont désormais en position céphalique (tête en bas, menton fléchi), prête pour l'engagement dans le bassin." else "Most babies are in head-down vertex position preparing to engage pelvis.",
      maternalTip = if (isFr) "Consultation pré-anesthésique obligatoire en maternité." else "Required pre-anesthesia consultation at the maternity hospital.",
      tags = listOf("👶 Tête en bas engagée", "🩺 Consultation anesthésie", "🌿 Fin du 8ème mois")
    ),
    PregnancyMilestoneItem(
      week = 37, sizeCm = "48.6 cm", weightGrams = "2 860 g",
      fruitOrObject = if (isFr) "Bette à carde" else "Swiss Chard", fruitEmoji = "🌿",
      trimester = 3,
      title = if (isFr) "Plein terme précoce atteint ! (Early Term)" else "Early Full Term Reached!",
      description = if (isFr) "Cap médical extraordinaire : bébé est désormais à terme ! S'il naît aujourd'hui, il n'est plus considéré comme prématuré." else "Major milestone: baby is officially full term! No longer premature.",
      maternalTip = if (isFr) "Surveiller les signes annonciateurs : contractions rythmées et perte des eaux." else "Watch for signs of early labor: rhythmic contractions and water break.",
      tags = listOf("🎉 Plein terme précoce", "✅ Zéro prématurité", "👶 Prêt pour la naissance")
    ),
    PregnancyMilestoneItem(
      week = 38, sizeCm = "49.8 cm", weightGrams = "3 080 g",
      fruitOrObject = if (isFr) "Poireau géant" else "Giant Leek", fruitEmoji = "🪴",
      trimester = 3,
      title = if (isFr) "Cap des 3 kg & prise ferme" else "3 kg Milestone & Firm Grasp",
      description = if (isFr) "Le bébé dépasse les 3 kilogrammes ! Sa prise en main est ferme, ses ongles dépassent le bout de ses doigts et son regard est vif." else "Baby surpasses 3 kg. Strong grasp reflex and alert senses.",
      maternalTip = if (isFr) "Installer définitivement le siège-auto homologué dans la voiture." else "Firmly install certified car seat in vehicle for safe homecoming.",
      tags = listOf("⚖️ Poids > 3 kg", "🚗 Siège-auto installé", "🪴 Cap des 38 SA")
    ),
    PregnancyMilestoneItem(
      week = 39, sizeCm = "50.7 cm", weightGrams = "3 290 g",
      fruitOrObject = if (isFr) "Mini-pastèque" else "Mini Watermelon", fruitEmoji = "🍉",
      trimester = 3,
      title = if (isFr) "100% prêt à naître" else "100% Ready for Birth",
      description = if (isFr) "Le développement physique est entièrement accompli. Le bébé continue uniquement de stocker du glycogène pour l'effort de la naissance." else "Physical growth complete; baby stores liver glycogen for labor.",
      maternalTip = if (isFr) "Se reposer, se relaxer sur le ballon de grossesse (gym ball) pour bercer le bassin." else "Rest and relax on pregnancy gym ball to gently rock pelvis.",
      tags = listOf("🌟 100% prêt à naître", "🧘 Relaxation bassin", "🍉 Plein terme accompli")
    ),
    PregnancyMilestoneItem(
      week = 40, sizeCm = "51.2 cm", weightGrams = "3 460 g",
      fruitOrObject = if (isFr) "Pastèque à terme" else "Full Term Watermelon", fruitEmoji = "🍉",
      trimester = 3,
      title = if (isFr) "DPA Officielle (Mercredi 6 août 2027) !" else "Official Due Date (Wednesday, August 6, 2027)!",
      description = if (isFr) "Le jour J tant attendu est arrivé ! Votre petit garçon est prêt à rencontrer ses parents aimants Fatou et Bala." else "The big day is here! Your baby boy is ready to meet Fatou and Bala.",
      maternalTip = if (isFr) "Rester sereine, respirer calmement et se rendre en salle de naissance dès des contractions toutes les 5 minutes." else "Stay calm, breathe deeply, and go to maternity when contractions are 5 min apart.",
      tags = listOf("🎉 DPA 06 Août 2027", "👶 Rencontre magique", "❤️ Bienvenue au monde")
    ),
    PregnancyMilestoneItem(
      week = 41, sizeCm = "51.7 cm", weightGrams = "3 600 g",
      fruitOrObject = if (isFr) "Pastèque mûre" else "Ripe Watermelon", fruitEmoji = "🍉",
      trimester = 3,
      title = if (isFr) "Surveillance rapprochée post-terme" else "Post-Term Close Monitoring",
      description = if (isFr) "Bébé profite encore un peu du nid maternel ! Suivi rapproché par cardiotocographie et contrôle régulier du liquide amniotique." else "Baby enjoys extra womb time under close NST and amniotic fluid monitoring.",
      maternalTip = if (isFr) "Contrôle à la maternité toutes les 48 heures pour vérifier le bien-être fœtal optimal." else "Maternity checkup every 48 hours for optimal fetal well-being.",
      tags = listOf("🩺 Monitoring rapproché", "❤️ Surveillance attentive", "👶 Arrivée imminente")
    )
  )

  fun loadMilestones(context: Context, isFr: Boolean): List<PregnancyMilestoneItem> {
    val prefs = context.getSharedPreferences("pregnancy_milestones_cache", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("custom_milestones_json", null)
    val defaults = getDefaultList(isFr)
    if (jsonStr.isNullOrBlank()) return defaults

    return try {
      val jsonArray = JSONArray(jsonStr)
      val customMap = mutableMapOf<Int, JSONObject>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        customMap[obj.getInt("week")] = obj
      }
      defaults.map { def ->
        val custom = customMap[def.week]
        if (custom != null) {
          def.copy(
            title = custom.optString("title", def.title),
            sizeCm = custom.optString("sizeCm", def.sizeCm),
            weightGrams = custom.optString("weightGrams", def.weightGrams),
            fruitOrObject = custom.optString("fruitOrObject", def.fruitOrObject),
            fruitEmoji = custom.optString("fruitEmoji", def.fruitEmoji),
            description = custom.optString("description", def.description),
            maternalTip = custom.optString("maternalTip", def.maternalTip),
            userNotes = custom.optString("userNotes", def.userNotes),
            isCompleted = custom.optBoolean("isCompleted", def.isCompleted)
          )
        } else {
          def
        }
      }
    } catch (_: Exception) {
      defaults
    }
  }

  fun saveMilestones(context: Context, milestones: List<PregnancyMilestoneItem>) {
    val prefs = context.getSharedPreferences("pregnancy_milestones_cache", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    milestones.forEach { m ->
      val obj = JSONObject().apply {
        put("week", m.week)
        put("title", m.title)
        put("sizeCm", m.sizeCm)
        put("weightGrams", m.weightGrams)
        put("fruitOrObject", m.fruitOrObject)
        put("fruitEmoji", m.fruitEmoji)
        put("description", m.description)
        put("maternalTip", m.maternalTip)
        put("userNotes", m.userNotes)
        put("isCompleted", m.isCompleted)
      }
      jsonArray.put(obj)
    }
    prefs.edit().putString("custom_milestones_json", jsonArray.toString()).apply()
  }
}

@Composable
fun PregnancyMilestonesSection(
  currentWeek: Int,
  theme: ThemeColors,
  language: AppLanguage,
  onWeekSelected: (Int) -> Unit = {}
) {
  val context = LocalContext.current
  val isFr = language == AppLanguage.FR

  var milestonesList by remember {
    mutableStateOf(PregnancyMilestonesDefaults.loadMilestones(context, isFr))
  }

  var selectedTrimesterFilter by remember { mutableIntStateOf(0) } // 0: All, 1: T1, 2: T2, 3: T3
  var milestoneSearchQuery by remember { mutableStateOf("") }
  var editingMilestone by remember { mutableStateOf<PregnancyMilestoneItem?>(null) }

  val filteredMilestones = remember(milestonesList, selectedTrimesterFilter, milestoneSearchQuery) {
    milestonesList.filter { item ->
      val matchesTrimester = when (selectedTrimesterFilter) {
        1 -> item.trimester == 1
        2 -> item.trimester == 2
        3 -> item.trimester == 3
        else -> true
      }
      val matchesQuery = milestoneSearchQuery.isBlank() ||
        item.week.toString() == milestoneSearchQuery.trim() ||
        item.title.contains(milestoneSearchQuery, ignoreCase = true) ||
        item.fruitOrObject.contains(milestoneSearchQuery, ignoreCase = true) ||
        item.description.contains(milestoneSearchQuery, ignoreCase = true) ||
        item.userNotes.contains(milestoneSearchQuery, ignoreCase = true)
      matchesTrimester && matchesQuery
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pregnancy_milestones_section"),
    shape = RoundedCornerShape(22.dp),
    color = theme.cardSurface,
    border = BorderStroke(1.dp, theme.border)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header: Title & Total Count
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Timeline, contentDescription = null, tint = theme.accent, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = if (isFr) "Jalons de Grossesse (Semaines 1 à 41)" else "Pregnancy Milestones (Weeks 1 to 41)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = theme.primaryText
            )
            Text(
              text = if (isFr) "Suivi clinique personnalisé • Cliquez pour modifier" else "Personalized clinical timeline • Tap to edit",
              style = MaterialTheme.typography.labelSmall,
              color = theme.mutedText
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(100.dp),
          color = theme.accent.copy(alpha = 0.15f),
          border = BorderStroke(0.6.dp, theme.accent.copy(alpha = 0.4f))
        ) {
          Text(
            text = "SA $currentWeek",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = theme.accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      // Trimester Tabs Filter
      val trimesterTabs = listOf(
        0 to (if (isFr) "Toutes (1–41)" else "All (1–41)"),
        1 to (if (isFr) "🌸 1er Trim. (1–13)" else "🌸 1st Tri (1–13)"),
        2 to (if (isFr) "🌿 2ème Trim. (14–27)" else "🌿 2nd Tri (14–27)"),
        3 to (if (isFr) "🍇 3ème Trim. (28–41)" else "🍇 3rd Tri (28–41)")
      )
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(trimesterTabs) { (triId, label) ->
          val isSel = selectedTrimesterFilter == triId
          FilterChip(
            selected = isSel,
            onClick = { selectedTrimesterFilter = triId },
            label = {
              Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = theme.accent,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.height(32.dp)
          )
        }
      }

      // Search & Jump to current week row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = milestoneSearchQuery,
          onValueChange = { milestoneSearchQuery = it },
          placeholder = { Text(if (isFr) "Filtrer semaine ou mot-clé..." else "Search week or keyword...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
          trailingIcon = {
            if (milestoneSearchQuery.isNotEmpty()) {
              IconButton(onClick = { milestoneSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          shape = RoundedCornerShape(10.dp)
        )

        FilledTonalButton(
          onClick = {
            val tri = when {
              currentWeek <= 13 -> 1
              currentWeek <= 27 -> 2
              else -> 3
            }
            selectedTrimesterFilter = tri
            milestoneSearchQuery = currentWeek.toString()
            onWeekSelected(currentWeek)
          },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(48.dp)
        ) {
          Text(if (isFr) "Ma Semaine" else "My Week", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      // List of 1 to 41 Milestone Cards
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (filteredMilestones.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(if (isFr) "Aucun jalon trouvé pour cette recherche." else "No milestones found for this search.", color = theme.mutedText)
          }
        } else {
          filteredMilestones.forEach { item ->
            val isCurrent = item.week == currentWeek
            val triColor = when (item.trimester) {
              1 -> Color(0xFFE11D48)
              2 -> Color(0xFF0D9488)
              else -> Color(0xFF7C3AED)
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isCurrent) theme.accent.copy(alpha = 0.08f) else theme.background,
              border = BorderStroke(
                width = if (isCurrent) 1.6.dp else 0.8.dp,
                color = if (isCurrent) theme.accent else theme.border
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pregnancy_milestone_card_${item.week}")
                .clickable { editingMilestone = item }
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top Row: Week badge, Fruit emoji + comparison, Trimester badge, Edit button
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = triColor.copy(alpha = 0.16f),
                      border = BorderStroke(0.6.dp, triColor)
                    ) {
                      Text(
                        text = "Semaine ${item.week} SA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = triColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                      )
                    }

                    if (isCurrent) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.accent,
                      ) {
                        Text(
                          text = if (isFr) "Actuelle" else "Current",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          fontSize = 10.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = RoundedCornerShape(100.dp),
                      color = theme.cardSurface,
                      border = BorderStroke(0.6.dp, theme.border)
                    ) {
                      Text(
                        text = "${item.fruitEmoji} ${item.fruitOrObject}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                      onClick = { editingMilestone = item },
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("pregnancy_edit_milestone_${item.week}")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = theme.accent, modifier = Modifier.size(16.dp))
                    }
                  }
                }

                // Biometrics: Length & Weight
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.cardSurface,
                    border = BorderStroke(0.5.dp, theme.border)
                  ) {
                    Text(
                      text = "📏 ${item.sizeCm}",
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.primaryText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.cardSurface,
                    border = BorderStroke(0.5.dp, theme.border)
                  ) {
                    Text(
                      text = "⚖️ ${item.weightGrams}",
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.primaryText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text(
                    text = "${if (isFr) "Trimestre" else "Trimester"} ${item.trimester}",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.mutedText,
                    modifier = Modifier.align(Alignment.CenterVertically)
                  )
                }

                // Title & Medical Description
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = theme.primaryText
                )
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = theme.mutedText,
                  lineHeight = 17.sp
                )

                // Maternal Health Tip
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = triColor.copy(alpha = 0.08f),
                  border = BorderStroke(0.5.dp, triColor.copy(alpha = 0.25f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Text("🤰", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = item.maternalTip,
                      style = MaterialTheme.typography.labelSmall,
                      color = theme.primaryText,
                      lineHeight = 15.sp
                    )
                  }
                }

                // Custom User Notes (if entered)
                if (item.userNotes.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.Top
                    ) {
                      Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = item.userNotes,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                      )
                    }
                  }
                }

                // Bottom row: Feature tags and completion toggle
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    item.tags.take(2).forEach { tag ->
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = theme.cardSurface,
                        border = BorderStroke(0.4.dp, theme.border)
                      ) {
                        Text(
                          text = tag,
                          fontSize = 10.sp,
                          color = theme.mutedText,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }

                  TextButton(
                    onClick = {
                      val updated = milestonesList.map { m ->
                        if (m.week == item.week) m.copy(isCompleted = !m.isCompleted) else m
                      }
                      milestonesList = updated
                      PregnancyMilestonesDefaults.saveMilestones(context, updated)
                      Toast.makeText(
                        context,
                        if (item.isCompleted) (if (isFr) "Jalon marqué en cours" else "Milestone unmarked") else (if (isFr) "Jalon validé !" else "Milestone completed!"),
                        Toast.LENGTH_SHORT
                      ).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Icon(
                      imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                      contentDescription = null,
                      tint = if (item.isCompleted) Color(0xFF16A34A) else theme.mutedText,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (item.isCompleted) (if (isFr) "Validé" else "Completed") else (if (isFr) "À valider" else "Mark Done"),
                      style = MaterialTheme.typography.labelSmall,
                      color = if (item.isCompleted) Color(0xFF16A34A) else theme.mutedText,
                      fontWeight = if (item.isCompleted) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Edit Milestone Dialog (Allows fully customizing any week 1 to 41 entry)
  if (editingMilestone != null) {
    val m = editingMilestone!!
    var editTitle by remember(m.week) { mutableStateOf(m.title) }
    var editSize by remember(m.week) { mutableStateOf(m.sizeCm) }
    var editWeight by remember(m.week) { mutableStateOf(m.weightGrams) }
    var editFruit by remember(m.week) { mutableStateOf(m.fruitOrObject) }
    var editEmoji by remember(m.week) { mutableStateOf(m.fruitEmoji) }
    var editDesc by remember(m.week) { mutableStateOf(m.description) }
    var editTip by remember(m.week) { mutableStateOf(m.maternalTip) }
    var editUserNotes by remember(m.week) { mutableStateOf(m.userNotes) }

    AlertDialog(
      onDismissRequest = { editingMilestone = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EditCalendar, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${if (isFr) "Modifier Semaine" else "Edit Week"} ${m.week} SA",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            label = { Text(if (isFr) "Titre du Jalon Fœtal" else "Milestone Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = editSize,
              onValueChange = { editSize = it },
              label = { Text(if (isFr) "Taille" else "Size") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = editWeight,
              onValueChange = { editWeight = it },
              label = { Text(if (isFr) "Poids" else "Weight") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = editEmoji,
              onValueChange = { editEmoji = it },
              label = { Text("Emoji") },
              singleLine = true,
              modifier = Modifier.weight(0.35f)
            )
            OutlinedTextField(
              value = editFruit,
              onValueChange = { editFruit = it },
              label = { Text(if (isFr) "Comparaison (Fruit/Objet)" else "Comparison (Fruit/Object)") },
              singleLine = true,
              modifier = Modifier.weight(0.65f)
            )
          }

          OutlinedTextField(
            value = editDesc,
            onValueChange = { editDesc = it },
            label = { Text(if (isFr) "Description Médicale Fœtale" else "Fetal Medical Description") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editTip,
            onValueChange = { editTip = it },
            label = { Text(if (isFr) "Conseils Corps de Maman" else "Maternal Body Tip") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editUserNotes,
            onValueChange = { editUserNotes = it },
            label = { Text(if (isFr) "Mes Notes Personnelles & Écho" else "My Personal & Scan Notes") },
            placeholder = { Text(if (isFr) "Mesures échographiques, souvenirs, ressentis..." else "Ultrasound measurements, feelings...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val updated = milestonesList.map { item ->
              if (item.week == m.week) {
                item.copy(
                  title = editTitle,
                  sizeCm = editSize,
                  weightGrams = editWeight,
                  fruitOrObject = editFruit,
                  fruitEmoji = editEmoji,
                  description = editDesc,
                  maternalTip = editTip,
                  userNotes = editUserNotes
                )
              } else {
                item
              }
            }
            milestonesList = updated
            PregnancyMilestonesDefaults.saveMilestones(context, updated)
            editingMilestone = null
            Toast.makeText(context, if (isFr) "Jalon sauvegardé !" else "Milestone saved!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
          Text(if (isFr) "Enregistrer" else "Save", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingMilestone = null }) {
          Text(if (isFr) "Annuler" else "Cancel")
        }
      }
    )
  }
}
