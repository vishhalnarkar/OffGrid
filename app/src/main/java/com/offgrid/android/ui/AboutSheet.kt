package com.offgrid.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offgrid.android.nostr.PoWPreferenceManager
import androidx.compose.ui.res.stringResource
import com.offgrid.android.R
import com.offgrid.android.core.ui.component.button.CloseButton
import com.offgrid.android.core.ui.component.sheet.OffGridBottomSheet
import com.offgrid.android.net.TorPreferenceManager
import com.offgrid.android.net.ArtiTorManager
import com.offgrid.android.services.meshgraph.MeshGraphService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgrid.android.ui.debug.ForceDirectedMeshGraph

/**
 * Real mesh network visualization showing actual peer connections
 */
@Composable
private fun MeshNetworkVisualization(
    modifier: Modifier = Modifier
) {
    val meshGraphService = remember { MeshGraphService.getInstance() }
    val graphSnapshot by meshGraphService.graphState.collectAsStateWithLifecycle()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        if (graphSnapshot.nodes.isNotEmpty()) {
            ForceDirectedMeshGraph(
                nodes = graphSnapshot.nodes,
                edges = graphSnapshot.edges,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No peers connected yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Unified settings toggle row with icon, title, subtitle, and switch
 * Apple-like design with proper spacing
 */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    statusIndicator: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.red + colorScheme.background.green + colorScheme.background.blue < 1.5f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.4f)
                )
                statusIndicator?.invoke()
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = if (enabled) 0.6f else 0.3f),
                lineHeight = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF64B5F6), // Changed from green to blue
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Apple-like About/Settings Sheet with high-quality design
 * Professional UX optimized for checkout scenarios
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    onShowDebug: (() -> Unit)? = null,
    nickname: String? = null,
    onNicknameChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Get version name from package info
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0" // fallback version
        }
    }

    val lazyListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.98f else 0f,
        label = "topBarAlpha"
    )

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.red + colorScheme.background.green + colorScheme.background.blue < 1.5f
    
    if (isPresented) {
        OffGridBottomSheet(
            modifier = modifier,
            onDismissRequest = onDismiss,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 80.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Section - App Identity
                    item(key = "header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.version_prefix, versionName ?: ""),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = stringResource(R.string.about_tagline),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }



                    // User Profile Section - Username
                    if (nickname != null && onNicknameChange != null) {
                        item(key = "user_profile") {
                            var editedNickname by remember(nickname) { mutableStateOf(nickname) }
                            var isEditing by remember { mutableStateOf(false) }

                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Text(
                                    text = "USER PROFILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onBackground.copy(alpha = 0.5f),
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                )
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Username",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface
                                        )
                                        
                                        OutlinedTextField(
                                            value = editedNickname,
                                            onValueChange = { 
                                                editedNickname = it
                                                isEditing = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            placeholder = {
                                                Text(
                                                    text = "Enter your username",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = colorScheme.primary,
                                                unfocusedBorderColor = colorScheme.outline
                                            ),
                                            singleLine = true
                                        )
                                        
                                        if (isEditing && editedNickname.isNotBlank() && editedNickname != nickname) {
                                            Button(
                                                onClick = {
                                                    onNicknameChange(editedNickname.trim())
                                                    isEditing = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorScheme.primary
                                                )
                                            ) {
                                                Text(
                                                    text = "Save Username",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = "This is how other users will see you in the mesh network.",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Appearance Section - Mesh Map
                    item(key = "appearance") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "APPEARANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onBackground.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Mesh Network",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface
                                    )
                                    MeshNetworkVisualization()
                                    Text(
                                        text = "Your device is connected to the mesh network. Nodes represent peers in your network.",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Settings Section - Unified Card with Toggles
                    item(key = "settings") {
                        LaunchedEffect(Unit) { PoWPreferenceManager.init(context) }
                        val powEnabled by PoWPreferenceManager.powEnabled.collectAsState()
                        val powDifficulty by PoWPreferenceManager.powDifficulty.collectAsState()
                        var backgroundEnabled by remember { mutableStateOf(com.offgrid.android.service.MeshServicePreferences.isBackgroundEnabled(true)) }
                        val torMode = remember { mutableStateOf(TorPreferenceManager.get(context)) }
                        val torProvider = remember { ArtiTorManager.getInstance() }
                        val torStatus by torProvider.statusFlow.collectAsState()
                        val torAvailable = remember { torProvider.isTorAvailable() }

                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "SETTINGS",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onBackground.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    // Background Mode Toggle
                                    SettingsToggleRow(
                                        icon = Icons.Filled.Bluetooth,
                                        title = stringResource(R.string.about_background_title),
                                        subtitle = stringResource(R.string.about_background_desc),
                                        checked = backgroundEnabled,
                                        onCheckedChange = { enabled ->
                                            backgroundEnabled = enabled
                                            com.offgrid.android.service.MeshServicePreferences.setBackgroundEnabled(enabled)
                                            if (!enabled) {
                                                com.offgrid.android.service.MeshForegroundService.stop(context)
                                            } else {
                                                com.offgrid.android.service.MeshForegroundService.start(context)
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Tor unavailable hint
                            if (!torAvailable) {
                                Text(
                                    text = stringResource(R.string.tor_not_available_in_this_build),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                )
                            }
                        }
                    }



                    // Footer
                    item(key = "footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (onShowDebug != null) {
                                TextButton(onClick = onShowDebug) {
                                    Text(
                                        text = stringResource(R.string.about_debug_settings),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.about_footer),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }

                // TopBar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = topBarAlpha))
                ) {
                    CloseButton(
                        onClick = onDismiss,
                        modifier = modifier
                            .align(Alignment.CenterEnd)
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Password prompt dialog for password-protected channels
 * Kept as dialog since it requires user input
 */
@Composable
fun PasswordPromptDialog(
    show: Boolean,
    channelName: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show && channelName != null) {
        val colorScheme = MaterialTheme.colorScheme
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.pwd_prompt_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.pwd_prompt_message, channelName ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.pwd_label), style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.join),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                }
            },
            containerColor = colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}
