package com.offgrid.android.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.offgrid.android.R
import kotlin.math.*
import kotlin.random.Random

// 3D Network Node data class
data class NetworkNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    var pulsePhase: Float = Random.nextFloat() * 2 * PI.toFloat(),
    var connections: List<Int> = emptyList()
)

// Network connection data class
data class NetworkConnection(
    val from: Int,
    val to: Int,
    var strength: Float = Random.nextFloat(),
    var pulseProgress: Float = 0f,
    var isActive: Boolean = true
)

/**
 * Creates a 3D icosphere-like network mesh
 */
fun createNetworkMesh(): Pair<List<NetworkNode>, List<NetworkConnection>> {
    val nodes = mutableListOf<NetworkNode>()
    val connections = mutableListOf<NetworkConnection>()
    
    // Create nodes in a roughly spherical distribution
    val radius = 1f
    val nodeCount = 16 // Reduced from 24 to 16
    
    // Generate nodes using fibonacci sphere algorithm for even distribution
    for (i in 0 until nodeCount) {
        val theta = 2 * PI * i / ((1 + sqrt(5.0)) / 2) // Golden angle
        val phi = acos(1 - 2 * i.toDouble() / nodeCount) // Latitude
        
        val x = (radius * sin(phi) * cos(theta)).toFloat()
        val y = (radius * sin(phi) * sin(theta)).toFloat()
        val z = (radius * cos(phi)).toFloat()
        
        nodes.add(NetworkNode(i, x, y, z))
    }
    
    // Create connections between nearby nodes
    for (i in nodes.indices) {
        val connectionsForNode = mutableListOf<Int>()
        for (j in nodes.indices) {
            if (i != j) {
                val distance = sqrt(
                    (nodes[i].x - nodes[j].x).pow(2) +
                    (nodes[i].y - nodes[j].y).pow(2) +
                    (nodes[i].z - nodes[j].z).pow(2)
                )
                
                // Connect nodes that are close enough (creates natural mesh)
                if (distance < 1.2f && connectionsForNode.size < 6) {
                    connectionsForNode.add(j)
                    connections.add(NetworkConnection(i, j))
                }
            }
        }
        nodes[i] = nodes[i].copy(connections = connectionsForNode)
    }
    
    return Pair(nodes, connections)
}

/**
 * Projects 3D coordinates to 2D screen coordinates with perspective
 */
fun project3DTo2D(
    x: Float, y: Float, z: Float,
    centerX: Float, centerY: Float,
    scale: Float,
    cameraDistance: Float = 4f
): Offset {
    val perspective = cameraDistance / (cameraDistance + z)
    return Offset(
        centerX + x * scale * perspective,
        centerY + y * scale * perspective
    )
}

/**
 * Cinematic 3D Network Mesh Animation Component
 */
@Composable
fun CinematicNetworkMesh(
    modifier: Modifier = Modifier,
    size: Float = 200f
) {
    // Create the network mesh once
    val (nodes, connections) = remember { createNetworkMesh() }
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "network_animation")
    
    // Slow rotation animation (faster)
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_y"
    )
    
    // Subtle rotation on X axis for more dynamic movement (faster)
    val rotationX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_x"
    )
    
    // Camera push-in effect (zoom) - starts larger
    val cameraZoom by infiniteTransition.animateFloat(
        initialValue = 1.1f, // Start bigger
        targetValue = 1.4f,  // End bigger
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera_zoom"
    )
    
    // Pulse animation for nodes (faster)
    val pulseTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_time"
    )
    
    // Remove data packet animation - no more sparkle dots
    // val packetProgress by infiniteTransition.animateFloat(
    //     initialValue = 0f,
    //     targetValue = 1f,
    //     animationSpec = infiniteRepeatable(
    //         animation = tween(3500, easing = LinearEasing),
    //         repeatMode = RepeatMode.Restart
    //     ),
    //     label = "packet_progress"
    // )
    
    Canvas(modifier = modifier.size(size.dp)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val scale = this.size.width * 0.15f * cameraZoom
        
        drawNetworkMesh(
            nodes = nodes,
            connections = connections,
            centerX = centerX,
            centerY = centerY,
            scale = scale,
            rotationX = rotationX,
            rotationY = rotationY,
            pulseTime = pulseTime
            // Removed packetProgress parameter
        )
    }
}

/**
 * Draws the 3D network mesh with all animations (no data packets)
 */
fun DrawScope.drawNetworkMesh(
    nodes: List<NetworkNode>,
    connections: List<NetworkConnection>,
    centerX: Float,
    centerY: Float,
    scale: Float,
    rotationX: Float,
    rotationY: Float,
    pulseTime: Float
) {
    // Transform nodes with rotation
    val transformedNodes = nodes.map { node ->
        val (x, y, z) = rotatePoint(node.x, node.y, node.z, rotationX, rotationY)
        node.copy(x = x, y = y, z = z)
    }
    
    // Sort nodes by Z-depth for proper rendering order
    val sortedNodes = transformedNodes.sortedBy { it.z }
    val nodePositions = transformedNodes.map { node ->
        project3DTo2D(node.x, node.y, node.z, centerX, centerY, scale)
    }
    
    // Draw connections first (behind nodes) - no data packets
    connections.forEach { connection ->
        val fromPos = nodePositions[connection.from]
        val toPos = nodePositions[connection.to]
        val fromZ = transformedNodes[connection.from].z
        val toZ = transformedNodes[connection.to].z
        
        // Calculate depth-based alpha (further = more transparent)
        val avgZ = (fromZ + toZ) / 2
        val depthAlpha = (1f - (avgZ + 2f) / 4f).coerceIn(0.1f, 0.8f)
        
        // Base connection line only
        drawLine(
            color = Color(0xFF00FF7F).copy(alpha = depthAlpha * 0.3f),
            start = fromPos,
            end = toPos,
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Draw nodes with pulsing animation
    sortedNodes.forEach { node ->
        val pos = project3DTo2D(node.x, node.y, node.z, centerX, centerY, scale)
        
        // Calculate depth-based effects
        val depthAlpha = (1f - (node.z + 2f) / 4f).coerceIn(0.2f, 1f)
        val depthSize = (1f - (node.z + 2f) / 6f).coerceIn(0.5f, 1f)
        
        // Pulsing effect (slower and more subtle)
        val pulseIntensity = sin(pulseTime + node.pulsePhase) * 0.4f + 0.6f // Reduced pulse range
        val nodeRadius = (4f + pulseIntensity * 2f) * depthSize // Slightly larger base size
        val glowRadius = nodeRadius * 2.5f // Larger glow
        
        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00FF7F).copy(alpha = depthAlpha * pulseIntensity * 0.3f),
                    Color.Transparent
                ),
                radius = glowRadius.dp.toPx()
            ),
            radius = glowRadius.dp.toPx(),
            center = pos
        )
        
        // Main node
        drawCircle(
            color = Color(0xFF00FF7F).copy(alpha = depthAlpha * (0.7f + pulseIntensity * 0.3f)),
            radius = nodeRadius.dp.toPx(),
            center = pos
        )
        
        // Core highlight
        drawCircle(
            color = Color.White.copy(alpha = depthAlpha * pulseIntensity * 0.8f),
            radius = (nodeRadius * 0.3f).dp.toPx(),
            center = pos
        )
    }
}

/**
 * Rotates a 3D point around X and Y axes
 */
fun rotatePoint(x: Float, y: Float, z: Float, rotX: Float, rotY: Float): Triple<Float, Float, Float> {
    val radX = Math.toRadians(rotX.toDouble())
    val radY = Math.toRadians(rotY.toDouble())
    
    // Rotate around Y axis first
    val cosY = cos(radY).toFloat()
    val sinY = sin(radY).toFloat()
    val x1 = x * cosY - z * sinY
    val z1 = x * sinY + z * cosY
    
    // Then rotate around X axis
    val cosX = cos(radX).toFloat()
    val sinX = sin(radX).toFloat()
    val y2 = y * cosX - z1 * sinX
    val z2 = y * sinX + z1 * cosX
    
    return Triple(x1, y2, z2)
}
/**
 * Cinematic loading screen with 3D animated network mesh
 */
@Composable
fun InitializingScreen(modifier: Modifier) {
    // Dark, high-tech background
    val backgroundColor = Color(0xFF0A0A0A)
    val primaryGreen = Color(0xFF00FF7F)
    
    // Animated dots for loading text (faster)
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    val dotCount = 3
    val animationDelay = 500
    val dots = (0 until dotCount).map { index ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDelay * dotCount),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(animationDelay * index)
            ),
            label = "dot_$index"
        )
        alpha
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(40.dp), // Reduced spacing to move title down
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title with subtle glow (moved down)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    fontSize = 32.sp,
                    color = primaryGreen,
                    letterSpacing = 4.sp
                ),
                textAlign = TextAlign.Center
            )

            // Cinematic 3D Network Mesh Animation
            CinematicNetworkMesh(
                modifier = Modifier.size(400.dp),
                size = 400f
            )

            // Loading text with animated dots and status below
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp), // Tight spacing between lines
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading text with animated dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.initializing_mesh_network),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = primaryGreen.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    )
                    
                    // Animated dots
                    dots.forEach { alpha ->
                        Text(
                            text = stringResource(R.string.dot),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = primaryGreen.copy(alpha = alpha),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Status indicator right below
                Text(
                    text = "Establishing secure connections",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = primaryGreen.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Subtle corner accent
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(8.dp)
                .background(
                    primaryGreen.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}

/**
 * Error screen shown if initialization fails
 */
@Composable
fun InitializationErrorScreen(
    modifier: Modifier,
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.warning_emoji),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Text(
                text = stringResource(R.string.setup_not_complete),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                ),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.errorContainer.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.try_again),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.open_settings),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
