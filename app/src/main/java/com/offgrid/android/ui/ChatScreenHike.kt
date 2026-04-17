package com.offgrid.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.imePadding
import kotlin.math.sin
import kotlin.math.PI
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Hike-inspired ChatScreen with OffGrid futuristic texture
 */
@Composable
fun HikeChatScreen(viewModel: ChatViewModel, onSettingsClick: () -> Unit = {}) {
    val primaryBlue = Color(0xFF64B5F6)
    val darkBg = Color(0xFF0A0E13)
    val surfaceColor = Color(0xFF1A1F26)
    
    // Collect messages from ViewModel
    val messages by viewModel.messages.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    
    // Neon glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        darkBg,
                        darkBg.copy(alpha = 0.95f),
                        Color(0xFF0D1421)
                    ),
                    radius = 1000f
                )
            )
    ) {
        // Background grid effect
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(1.dp)
        ) {
            val gridSize = 50f
            val gridAlpha = 0.08f
            
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(
                    color = primaryBlue.copy(alpha = gridAlpha),
                    start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                    end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(
                    color = primaryBlue.copy(alpha = gridAlpha),
                    start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                    end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Hike-style Header with Neon Glow
            HikeStyleHeader(
                glowAlpha = glowAlpha,
                primaryBlue = primaryBlue,
                surfaceColor = surfaceColor,
                connectedPeers = connectedPeers,
                onSettingsClick = onSettingsClick
            )
            
            // Messages area
            val listState = rememberLazyListState()
            
            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    val isOwn = message.sender == nickname
                    val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)
                    
                    HikeChatBubble(
                        username = message.sender,
                        message = message.content,
                        timestamp = timestamp,
                        isOwn = isOwn,
                        primaryBlue = primaryBlue,
                        surfaceColor = surfaceColor
                    )
                }
            }
            
            // Input area - Hike style
            HikeInputArea(
                primaryBlue = primaryBlue,
                surfaceColor = surfaceColor,
                onSendMessage = { text ->
                    if (text.isNotEmpty()) {
                        viewModel.sendMessage(text)
                    }
                }
            )
        }
        
        // Neon glow effect in corners
        NeonCornerGlows(glowAlpha = glowAlpha, primaryBlue = primaryBlue)
    }
}

@Composable
private fun HikeStyleHeader(
    glowAlpha: Float,
    primaryBlue: Color,
    surfaceColor: Color,
    connectedPeers: List<String>,
    onSettingsClick: () -> Unit = {}
) {
    val isConnected = connectedPeers.isNotEmpty()
    val indicatorColor = if (isConnected) Color(0xFF81C784) else Color(0xFF6B7280) // Light green when connected, greyish blue when not
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.95f),
                        surfaceColor.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = primaryBlue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - Menu
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = primaryBlue.copy(alpha = glowAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Center - OffGrid Logo with Neon Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
            ) {
                // Glow background
                Canvas(
                    modifier = Modifier.size(40.dp)
                ) {
                    drawCircle(
                        color = primaryBlue.copy(alpha = glowAlpha * 0.3f),
                        radius = 25f
                    )
                }
                
                // Logo text with glow
                Text(
                    text = "OffGrid",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = primaryBlue.copy(alpha = 0.9f + glowAlpha * 0.1f),
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
            
            // Right side - Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection indicator with glow - GREEN when connected, GREYISH BLUE when not
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            indicatorColor.copy(alpha = glowAlpha),
                            CircleShape
                        )
                )
                
                // Person icon and connected count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Connected users",
                        tint = primaryBlue.copy(alpha = glowAlpha),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = connectedPeers.size.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = primaryBlue.copy(alpha = glowAlpha)
                        )
                    )
                }
                
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryBlue.copy(alpha = glowAlpha),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HikeChatBubble(
    username: String,
    message: String,
    timestamp: String,
    isOwn: Boolean,
    primaryBlue: Color,
    surfaceColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        // Username and timestamp row
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = primaryBlue.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
            
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            )
        }
        
        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isOwn)
                        Brush.linearGradient(
                            colors = listOf(
                                primaryBlue.copy(alpha = 0.3f),
                                primaryBlue.copy(alpha = 0.2f)
                            )
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(
                                surfaceColor.copy(alpha = 0.6f),
                                surfaceColor.copy(alpha = 0.4f)
                            )
                        ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = primaryBlue.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isOwn)
                        Color.White.copy(alpha = 0.9f)
                    else
                        Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
private fun HikeChatBubbleOld(
    message: String,
    isOwn: Boolean,
    primaryBlue: Color,
    surfaceColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isOwn)
                        Brush.linearGradient(
                            colors = listOf(
                                primaryBlue.copy(alpha = 0.3f),
                                primaryBlue.copy(alpha = 0.2f)
                            )
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(
                                surfaceColor.copy(alpha = 0.6f),
                                surfaceColor.copy(alpha = 0.4f)
                            )
                        ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = primaryBlue.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isOwn)
                        Color.White.copy(alpha = 0.9f)
                    else
                        Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
private fun HikeInputArea(
    primaryBlue: Color,
    surfaceColor: Color,
    onSendMessage: (String) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.8f),
                        surfaceColor.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = primaryBlue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = {
                    Text(
                        "Type a message...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = primaryBlue.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF0A0E13).copy(alpha = 0.7f),
                    unfocusedContainerColor = Color(0xFF0A0E13).copy(alpha = 0.5f),
                    cursorColor = primaryBlue
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            
            // Send button with glow
            IconButton(
                onClick = {
                    onSendMessage(inputText)
                    inputText = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryBlue.copy(alpha = 0.4f),
                                primaryBlue.copy(alpha = 0.2f)
                            )
                        ),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = primaryBlue.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = primaryBlue.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NeonCornerGlows(
    glowAlpha: Float,
    primaryBlue: Color
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top-left corner glow
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryBlue.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        ),
                        radius = 100f
                    )
                )
        )
        
        // Top-right corner glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryBlue.copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        ),
                        radius = 100f
                    )
                )
        )
        
        // Bottom-right corner glow
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryBlue.copy(alpha = glowAlpha * 0.1f),
                            Color.Transparent
                        ),
                        radius = 120f
                    )
                )
        )
    }
}
