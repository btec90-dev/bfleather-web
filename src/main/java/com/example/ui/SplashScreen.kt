package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PmaRepository
import com.example.ui.theme.SoftChalkTheme
import kotlinx.coroutines.delay

/**
 * Modern, soothing splash screen:
 * - Pure white & soft porcelain background
 * - White leather B&F logo with subtle light border
 * - Medium slate typography, zero harsh black text
 * - Auto-transitions smoothly to main dashboard
 */
@Composable
fun SplashScreen(
  onTimeout: () -> Unit,
  delayMillis: Long = 2200L
) {
  var startAnimation by remember { mutableStateOf(false) }

  val scale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.88f,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "logoScale"
  )

  val alpha by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    label = "logoAlpha"
  )

  LaunchedEffect(Unit) {
    startAnimation = true
    delay(delayMillis)
    onTimeout()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFFFAFAFA))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        onTimeout()
      }
      .testTag("splash_screen_container"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp)
        .scale(scale)
        .alpha(alpha)
    ) {
      // Centered Luxury White Leather Logo Card
      Surface(
        modifier = Modifier
          .size(170.dp)
          .testTag("splash_logo"),
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, SoftChalkTheme.border)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.white_leather_bf),
            contentDescription = "B&F Parcours Logo",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = "B&F PARCOURS",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = SoftChalkTheme.primaryText,
        letterSpacing = 2.5.sp,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Protocole PMA & Étapes de Grossesse",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = SoftChalkTheme.mutedText,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Calendrier Directeur — ${PmaRepository.patientName}",
        style = MaterialTheme.typography.bodySmall,
        color = SoftChalkTheme.mutedText.copy(alpha = 0.8f),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Calming low-saturation indicator
      CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = SoftChalkTheme.accent,
        strokeWidth = 2.dp
      )
    }

    // Footnote
    Text(
      text = PmaRepository.clinicName,
      style = MaterialTheme.typography.labelSmall,
      color = SoftChalkTheme.mutedText.copy(alpha = 0.7f),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 28.dp)
    )
  }
}
