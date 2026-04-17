package com.offgrid.android.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
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

// Particle system for background effects
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var alpha: Float = 1f
)

// Interactive touch ripple effect
data class TouchRipple(
    val x: Float,
    val y: Float,
    var radius: Float = 0f,
    var alpha: Float = 1f,
    val maxRadius: Float = 200f
)

// Enhanced 3D Network Node with interactive features
data class NetworkNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    var pulsePhase: Float = Random.nextFloat() * 2 * PI.toFloat(),
    var connections: List<Int> = emptyList(),
    var isHighlighted: Boolean = false,
    var highlightIntensity: Float = 0f
)

// Holographic scan line effect - REMOVED
// data class ScanLine(
//     var y: Float,
//     var opacity: Float,
//     var speed: Float
// )

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
 * Advanced Futuristic Network Mesh with Interactive Features
 */
@Composable
fun FuturisticNetworkMesh(
    modifier: Modifier = Modifier,
    size: Float = 400f
) {
    // Create the network mesh and particles
    val (nodes, connections) = remember { createNetworkMesh() }
    val particles = remember { mutableStateListOf<Particle>() }
    val touchRipples = remember { mutableStateListOf<TouchRipple>() }
    
    // Interactive state
    var lastTouchTime by remember { mutableStateOf(0L) }
    
    // Advanced animation states
    val infiniteTransition = rememberInfiniteTransition(label = "futuristic_animation")
    
    // Multi-layered rotation system
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_y"
    )
    
    val rotationX by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation_x"
    )
    
    // Dynamic camera system with multiple zoom layers
    val primaryZoom by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "primary_zoom"
    )
    
    val secondaryZoom by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondary_zoom"
    )
    
    // Advanced pulsing with harmonic frequencies
    val primaryPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "primary_pulse"
    )
    
    val secondaryPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondary_pulse"
    )
    
    // Holographic scan effect - REMOVED
    // val scanProgress by infiniteTransition.animateFloat(
    //     initialValue = 0f,
    //     targetValue = 1f,
    //     animationSpec = infiniteRepeatable(
    //         animation = tween(3000, easing = LinearEasing),
    //         repeatMode = RepeatMode.Restart
    //     ),
    //     label = "scan_progress"
    // )
    
    // Particle system animation
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )
    
    // Update particles and effects
    LaunchedEffect(particleTime) {
        // Add new particles
        if (particles.size < 50) {
            repeat(2) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * size,
                        y = Random.nextFloat() * size,
                        vx = (Random.nextFloat() - 0.5f) * 0.5f,
                        vy = (Random.nextFloat() - 0.5f) * 0.5f,
                        life = Random.nextFloat() * 5f + 2f,
                        maxLife = Random.nextFloat() * 5f + 2f,
                        size = Random.nextFloat() * 2f + 1f
                    )
                )
            }
        }
        
        // Update existing particles
        particles.removeAll { particle ->
            particle.life -= 0.016f // ~60fps
            particle.x += particle.vx
            particle.y += particle.vy
            particle.alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            particle.life <= 0f
        }
        
        // Update touch ripples
        touchRipples.removeAll { ripple ->
            ripple.radius += 3f
            ripple.alpha = 1f - (ripple.radius / ripple.maxRadius)
            ripple.radius >= ripple.maxRadius
        }
        
        // Update scan lines - REMOVED
        // if (scanLines.isEmpty() || Random.nextFloat() < 0.02f) {
        //     scanLines.add(
        //         ScanLine(
        //             y = -50f,
        //             opacity = 0.8f,
        //             speed = Random.nextFloat() * 2f + 1f
        //         )
        //     )
        // }
        //
        // scanLines.removeAll { scanLine ->
        //     scanLine.y += scanLine.speed
        //     scanLine.opacity *= 0.995f
        //     scanLine.y > size + 50f || scanLine.opacity < 0.1f
        // }
    }

    Canvas(
        modifier = modifier
            .size(size.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Add interactive touch ripple
                    touchRipples.add(
                        TouchRipple(
                            x = offset.x,
                            y = offset.y
                        )
                    )
                    lastTouchTime = System.currentTimeMillis()
                }
            }
    ) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val finalZoom = primaryZoom * secondaryZoom
        val scale = this.size.width * 0.15f * finalZoom
        
        drawFuturisticMesh(
            nodes = nodes,
            connections = connections,
            particles = particles,
            touchRipples = touchRipples,
            centerX = centerX,
            centerY = centerY,
            scale = scale,
            rotationX = rotationX,
            rotationY = rotationY,
            primaryPulse = primaryPulse,
            secondaryPulse = secondaryPulse,
            canvasSize = this.size
        )
    }
}

/**
 * Advanced futuristic mesh drawing with multiple visual layers
 */
fun DrawScope.drawFuturisticMesh(
    nodes: List<NetworkNode>,
    connections: List<NetworkConnection>,
    particles: List<Particle>,
    touchRipples: List<TouchRipple>,
    centerX: Float,
    centerY: Float,
    scale: Float,
    rotationX: Float,
    rotationY: Float,
    primaryPulse: Float,
    secondaryPulse: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    // Transform nodes with rotation
    val transformedNodes = nodes.map { node ->
        val (x, y, z) = rotatePoint(node.x, node.y, node.z, rotationX, rotationY)
        node.copy(x = x, y = y, z = z)
    }
    
    val nodePositions = transformedNodes.map { node ->
        project3DTo2D(node.x, node.y, node.z, centerX, centerY, scale)
    }
    
    // Layer 1: Background particles
    particles.forEach { particle ->
        val particleColor = Color(0xFF64B5F6).copy(alpha = particle.alpha * 0.3f)
        drawCircle(
            color = particleColor,
            radius = particle.size,
            center = Offset(particle.x, particle.y)
        )
    }
    
    // Layer 2: Holographic scan lines - REMOVED
    // scanLines.forEach { scanLine ->
    //     val gradient = Brush.verticalGradient(
    //         colors = listOf(
    //             Color.Transparent,
    //             Color(0xFF64B5F6).copy(alpha = scanLine.opacity * 0.5f),
    //             Color(0xFF90CAF9).copy(alpha = scanLine.opacity * 0.8f),
    //             Color(0xFF64B5F6).copy(alpha = scanLine.opacity * 0.5f),
    //             Color.Transparent
    //         ),
    //         startY = scanLine.y - 10f,
    //         endY = scanLine.y + 10f
    //     )
    //
    //     drawRect(
    //         brush = gradient,
    //         topLeft = Offset(0f, scanLine.y - 10f),
    //         size = androidx.compose.ui.geometry.Size(canvasSize.width, 20f)
    //     )
    // }
    
    // Layer 3: Enhanced connections with energy flow
    connections.forEach { connection ->
        val fromPos = nodePositions[connection.from]
        val toPos = nodePositions[connection.to]
        val fromZ = transformedNodes[connection.from].z
        val toZ = transformedNodes[connection.to].z
        
        val avgZ = (fromZ + toZ) / 2
        val depthAlpha = (1f - (avgZ + 2f) / 4f).coerceIn(0.1f, 0.9f)
        
        // Main connection line with glow
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF64B5F6).copy(alpha = depthAlpha * 0.2f),
                    Color(0xFF90CAF9).copy(alpha = depthAlpha * 0.6f),
                    Color(0xFF64B5F6).copy(alpha = depthAlpha * 0.2f)
                ),
                start = fromPos,
                end = toPos
            ),
            start = fromPos,
            end = toPos,
            strokeWidth = 2.dp.toPx()
        )
        
        // Energy pulse along connection
        val pulsePosition = (sin(primaryPulse + connection.from * 0.5f) + 1f) / 2f
        val energyPos = Offset(
            fromPos.x + (toPos.x - fromPos.x) * pulsePosition,
            fromPos.y + (toPos.y - fromPos.y) * pulsePosition
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF90CAF9).copy(alpha = depthAlpha * 0.8f),
                    Color.Transparent
                ),
                radius = 8f
            ),
            radius = 8f,
            center = energyPos
        )
    }
    
    // Layer 4: Interactive touch ripples
    touchRipples.forEach { ripple ->
        drawCircle(
            color = Color(0xFF64B5F6).copy(alpha = ripple.alpha * 0.3f),
            radius = ripple.radius,
            center = Offset(ripple.x, ripple.y),
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawCircle(
            color = Color(0xFF90CAF9).copy(alpha = ripple.alpha * 0.6f),
            radius = ripple.radius * 0.5f,
            center = Offset(ripple.x, ripple.y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
    
    // Layer 5: Enhanced nodes with multiple pulse frequencies
    transformedNodes.sortedBy { it.z }.forEach { node ->
        val pos = project3DTo2D(node.x, node.y, node.z, centerX, centerY, scale)
        
        val depthAlpha = (1f - (node.z + 2f) / 4f).coerceIn(0.2f, 1f)
        val depthSize = (1f - (node.z + 2f) / 6f).coerceIn(0.5f, 1f)
        
        // Multi-frequency pulsing
        val pulse1 = sin(primaryPulse + node.pulsePhase) * 0.3f + 0.7f
        val pulse2 = sin(secondaryPulse + node.pulsePhase * 1.618f) * 0.2f + 0.8f
        val combinedPulse = (pulse1 + pulse2) / 2f
        
        val nodeRadius = (5f + combinedPulse * 3f) * depthSize
        val glowRadius = nodeRadius * 3f
        
        // Outer energy field
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF64B5F6).copy(alpha = depthAlpha * combinedPulse * 0.1f),
                    Color(0xFF42A5F5).copy(alpha = depthAlpha * combinedPulse * 0.3f),
                    Color.Transparent
                ),
                radius = glowRadius * 1.5f
            ),
            radius = glowRadius * 1.5f,
            center = pos
        )
        
        // Main glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF90CAF9).copy(alpha = depthAlpha * combinedPulse * 0.6f),
                    Color(0xFF64B5F6).copy(alpha = depthAlpha * combinedPulse * 0.4f),
                    Color.Transparent
                ),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = pos
        )
        
        // Core node
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE3F2FD).copy(alpha = depthAlpha * 0.9f),
                    Color(0xFF90CAF9).copy(alpha = depthAlpha * 0.7f),
                    Color(0xFF64B5F6).copy(alpha = depthAlpha * 0.5f)
                ),
                radius = nodeRadius
            ),
            radius = nodeRadius,
            center = pos
        )
        
        // Inner highlight
        drawCircle(
            color = Color.White.copy(alpha = depthAlpha * combinedPulse * 0.8f),
            radius = nodeRadius * 0.3f,
            center = pos
        )
        
        // Rotating ring effect
        val ringRotation = (primaryPulse * 2f + node.id * 30f) % 360f
        rotate(ringRotation, pos) {
            drawCircle(
                color = Color(0xFF64B5F6).copy(alpha = depthAlpha * 0.4f),
                radius = nodeRadius * 1.5f,
                center = pos,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
    
    // Layer 6: Holographic overlay effect - REMOVED
    // val overlayAlpha = (sin(scanProgress * 2 * PI.toFloat()) * 0.1f + 0.05f).coerceIn(0f, 0.15f)
    // drawRect(
    //     brush = Brush.verticalGradient(
    //         colors = listOf(
    //             Color(0xFF64B5F6).copy(alpha = overlayAlpha),
    //             Color.Transparent,
    //             Color(0xFF90CAF9).copy(alpha = overlayAlpha * 0.5f),
    //             Color.Transparent,
    //             Color(0xFF64B5F6).copy(alpha = overlayAlpha)
    //         )
    //     ),
    //     size = canvasSize
    // )
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
/**
 * Advanced Futuristic Loading Screen with Interactive Elements
 */
@Composable
fun InitializingScreen(modifier: Modifier) {
    // Theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryBlue = MaterialTheme.colorScheme.primary
    val surfaceBlue = MaterialTheme.colorScheme.surface
    
    // Interactive state
    var isInteracting by remember { mutableStateOf(false) }
    var interactionIntensity by remember { mutableStateOf(0f) }
    
    // Advanced animations
    val infiniteTransition = rememberInfiniteTransition(label = "futuristic_loading")
    
    // Holographic title animation
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_glow"
    )
    
    // Animated typing effect for dots
    val dotCount = 3
    val animationDelay = 400
    val dots = (0 until dotCount).map { index ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDelay * dotCount, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(animationDelay * index)
            ),
            label = "dot_$index"
        )
        alpha
    }
    
    // Progress simulation
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    // Background pulse
    val backgroundPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_pulse"
    )
    
    // Interaction animation
    val interactionScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "interaction_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isInteracting = true
                        tryAwaitRelease()
                        isInteracting = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(interactionScale)
        ) {
            // Minimal app title
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    fontSize = 32.sp,
                    color = primaryBlue.copy(alpha = 0.9f),
                    letterSpacing = 4.sp
                ),
                textAlign = TextAlign.Center
            )

            // Advanced 3D Network Mesh
            FuturisticNetworkMesh(
                modifier = Modifier.size(380.dp),
                size = 380f
            )

            // Minimal loading section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main loading text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.initializing_mesh_network),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    )
                    
                    // Animated dots
                    dots.forEach { alpha ->
                        Text(
                            text = stringResource(R.string.dot),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = primaryBlue.copy(alpha = alpha),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Simple progress bar
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                primaryBlue.copy(alpha = 0.7f),
                                RoundedCornerShape(1.dp)
                            )
                    )
                }

                // Status text
                Text(
                    text = "Establishing secure connections",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
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
                    containerColor = colorScheme.errorContainer
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
                    containerColor = colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onErrorContainer
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
