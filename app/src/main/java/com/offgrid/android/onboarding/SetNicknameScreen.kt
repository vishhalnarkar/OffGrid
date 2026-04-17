package com.offgrid.android.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

/**
 * Screen for setting user's nickname
 */
@Composable
fun SetNicknameScreen(
    modifier: Modifier = Modifier,
    onContinue: (String) -> Unit,
    onSkip: () -> Unit
) {
    val primaryBlue = Color(0xFF64B5F6)
    val darkBg = Color(0xFF0A0E13)
    val surfaceColor = Color(0xFF1A1F26)
    
    var nicknameInput by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }
    
    // Neon glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "nickname_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    // Input focus animation
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "focus_scale"
    )
    
    // Update validity
    LaunchedEffect(nicknameInput) {
        isValid = nicknameInput.trim().length >= 2 && nicknameInput.trim().length <= 20
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title with icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon with glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    // Glow background
                    Canvas(
                        modifier = Modifier.size(64.dp)
                    ) {
                        drawCircle(
                            color = primaryBlue.copy(alpha = glowAlpha * 0.3f),
                            radius = 40f
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Set Nickname",
                        tint = primaryBlue.copy(alpha = 0.9f + glowAlpha * 0.1f),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Text(
                    text = "Set Your Identity",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
            
            // Input field with validation - moved closer to title
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Input box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(focusScale)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    surfaceColor.copy(alpha = 0.8f),
                                    surfaceColor.copy(alpha = 0.6f)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isFocused)
                                primaryBlue.copy(alpha = glowAlpha)
                            else
                                primaryBlue.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    TextField(
                        value = nicknameInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 20) {
                                nicknameInput = newValue
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                        placeholder = {
                            Text(
                                "Enter your nickname...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = primaryBlue
                        ),
                        singleLine = true
                    )
                }
                
                // Character count
                Text(
                    text = "${nicknameInput.length}/20",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isValid)
                            primaryBlue.copy(alpha = 0.7f)
                        else
                            Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
                
                // Validation message
                if (nicknameInput.isNotEmpty() && !isValid) {
                    Text(
                        text = "Nickname must be 2-20 characters",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFF6B6B).copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Continue button
                Button(
                    onClick = {
                        if (isValid) {
                            onContinue(nicknameInput.trim())
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue.copy(alpha = 0.8f),
                        disabledContainerColor = primaryBlue.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
                
                // Skip button
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = primaryBlue.copy(alpha = 0.8f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = primaryBlue.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Skip for Now",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
