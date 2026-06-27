package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VpnViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isIpRotationEnabled by viewModel.isIpRotationEnabled.collectAsStateWithLifecycle()
    val currentIp by viewModel.currentIp.collectAsStateWithLifecycle()

    // Activity launcher for system VPN permission
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn(vpnPrepared = true)
        } else {
            viewModel.addTerminalLog("SYSTEM: VPN permission rejected by user.")
        }
    }

    fun handleVpnToggle() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPrepareLauncher.launch(intent)
        } else {
            viewModel.toggleVpn(vpnPrepared = true)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GHOST_LINE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = CyberWhite,
                            letterSpacing = 2.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CyberBlack,
                    titleContentColor = CyberWhite
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = CyberGray
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberCard,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Tunnel") },
                    label = { Text("Shield", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBlack,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberEmerald,
                        unselectedIconColor = CyberGray,
                        unselectedTextColor = CyberGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Shuffle, contentDescription = "Rotator") },
                    label = { Text("IP Rotate", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBlack,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberEmerald,
                        unselectedIconColor = CyberGray,
                        unselectedTextColor = CyberGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "WireGuard") },
                    label = { Text("WireGuard", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBlack,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberEmerald,
                        unselectedIconColor = CyberGray,
                        unselectedTextColor = CyberGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Public, contentDescription = "Relays") },
                    label = { Text("Relays", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberBlack,
                        selectedTextColor = CyberEmerald,
                        indicatorColor = CyberEmerald,
                        unselectedIconColor = CyberGray,
                        unselectedTextColor = CyberGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            // Main Connection status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .border(1.dp, Brush.linearGradient(listOf(CyberEmerald.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(0.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (connectionState) {
                                        ConnectionState.CONNECTED -> CyberEmerald
                                        ConnectionState.CONNECTING -> CyberCyan
                                        ConnectionState.SHUFFLING -> CyberCyan
                                        ConnectionState.DISCONNECTED -> CyberRed
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "SECURE STEALTH TUNNEL ACTIVE"
                                ConnectionState.CONNECTING -> "NEGOTIATING HANDSHAKE..."
                                ConnectionState.SHUFFLING -> "ROTATING EXIT NODE..."
                                ConnectionState.DISCONNECTED -> "TUNNEL OFFLINE (UNPROTECTED)"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> CyberEmerald
                                ConnectionState.CONNECTING -> CyberCyan
                                ConnectionState.SHUFFLING -> CyberCyan
                                ConnectionState.DISCONNECTED -> CyberRed
                            }
                        )
                    }
                    if (isIpRotationEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ROTATOR_ON",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Split pane: Top is active tab screen, Bottom is terminal log
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(viewModel, ::handleVpnToggle)
                    1 -> RotatorTab(viewModel)
                    2 -> WireGuardTab(viewModel)
                    3 -> RelaysTab(viewModel)
                }
            }

            // Real-time terminal output
            TerminalConsole(viewModel)
        }
    }
}

@Composable
fun DashboardTab(viewModel: VpnViewModel, onToggleVpn: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val currentIp by viewModel.currentIp.collectAsStateWithLifecycle()
    val downloadSpeed by viewModel.downloadSpeed.collectAsStateWithLifecycle()
    val uploadSpeed by viewModel.uploadSpeed.collectAsStateWithLifecycle()
    val isSingleIpMode by viewModel.isSingleIpMode.collectAsStateWithLifecycle()
    val isIpRotationEnabled by viewModel.isIpRotationEnabled.collectAsStateWithLifecycle()
    val isTrafficObfuscationEnabled by viewModel.isTrafficObfuscationEnabled.collectAsStateWithLifecycle()

    // Animating circle pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val radarRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Glowing Connection Circle Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(230.dp)
                .padding(12.dp)
        ) {
            // Pulse rings
            if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.SHUFFLING) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .rotate(radarRotate)
                        .drawBehind {
                            drawCircle(
                                color = CyberEmerald.copy(alpha = 0.08f),
                                radius = size.minDimension / 2 * pulseScale
                            )
                            drawCircle(
                                color = CyberEmerald,
                                radius = size.minDimension / 2,
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 45f), 0f)
                                )
                            )
                        }
                )
            }

            // Core circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = if (connectionState == ConnectionState.CONNECTED) CyberEmerald else CyberRed,
                        spotColor = if (connectionState == ConnectionState.CONNECTED) CyberEmerald else CyberRed
                    )
                    .clip(CircleShape)
                    .background(CyberCard)
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                if (connectionState == ConnectionState.CONNECTED) CyberEmerald else CyberRed,
                                CyberCyan
                            )
                        ),
                        CircleShape
                    )
                    .clickable { onToggleVpn() }
                    .testTag("connect_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Power",
                        tint = if (connectionState == ConnectionState.CONNECTED) CyberEmerald else CyberRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "SECURE"
                            ConnectionState.CONNECTING -> "WAIT..."
                            ConnectionState.SHUFFLING -> "SHUFFLE"
                            ConnectionState.DISCONNECTED -> "SECURE"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (connectionState == ConnectionState.CONNECTED) CyberEmerald else CyberWhite
                    )
                }
            }
        }

        // IP Display Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VIRTUAL CLOAKED IP",
                    color = CyberGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentIp,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyberWhite,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WireGuard Noise Protocol Layer Active",
                    color = CyberEmerald.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        // Real-time speed metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.1f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "DL",
                                tint = CyberEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "DOWNSTREAM",
                                fontSize = 9.sp,
                                color = CyberGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isTrafficObfuscationEnabled && connectionState == ConnectionState.CONNECTED) {
                            Box(
                                modifier = Modifier
                                    .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "OBFUSCATED",
                                    color = CyberCyan,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f MB/s", downloadSpeed),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberWhite
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.1f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "UL",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "UPSTREAM",
                                fontSize = 9.sp,
                                color = CyberGray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isTrafficObfuscationEnabled && connectionState == ConnectionState.CONNECTED) {
                            Box(
                                modifier = Modifier
                                    .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "OBFUSCATED",
                                    color = CyberCyan,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f MB/s", uploadSpeed),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberWhite
                    )
                }
            }
        }

        // Active node description card
        selectedServer?.let { server ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CURRENT ENCRYPTED GATEWAY",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = server.countryLong,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberWhite
                            )
                            Text(
                                text = server.hostName,
                                fontSize = 11.sp,
                                color = CyberGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(220.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${server.ping} ms",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Tunnel Operation Mode Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CONNECTION ROUTING MODE",
                    color = CyberEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSingleIpMode) "Static Single IP Tunnel" else "Dynamic IP Rotator",
                            color = CyberWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSingleIpMode) 
                                "Routing traffic stably via a single static VPN gateway IP. Safest for online banking & gaming." 
                            else 
                                "IP rotating automatically to bypass tracking. Perfect for high-grade anonymity.",
                            color = CyberGray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isSingleIpMode,
                        onCheckedChange = { viewModel.toggleSingleIpMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberEmerald,
                            uncheckedThumbColor = CyberBlack,
                            uncheckedTrackColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("single_ip_connection_switch")
                    )
                }
            }
        }

        // WiFi Router & ISP Cloaking Shield Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WIFI & ISP CLOAKING SHIELD",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DNS Query Hijack Protection",
                                color = CyberWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Upstream DNS requests (Cloudflare 1.1.1.1 & Quad9 9.9.9.9) are routed inside the TLS-encrypted VPN block. Your router/ISP only sees encrypted payloads, keeping your search history 100% private.",
                                color = CyberGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "IPv6 Back-Channel Leak Prevention",
                                color = CyberWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Blocks stealth background data leaks on local IPv6 configurations by forcing all IPv6 traffic into the VPN tunnel's virtual null route (::/0).",
                                color = CyberGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Packet Size Padding & Obfuscation",
                                color = CyberWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Limits MTU to 1400 and pads headers dynamically. This scrambles your data footprint, preventing deep packet inspection and side-channel traffic size/pattern matching.",
                                color = CyberGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotatorTab(viewModel: VpnViewModel) {
    val isIpRotationEnabled by viewModel.isIpRotationEnabled.collectAsStateWithLifecycle()
    val rotationInterval by viewModel.rotationInterval.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val isManualHostsEnabled by viewModel.isManualHostsEnabled.collectAsStateWithLifecycle()
    val manualHostsInput by viewModel.manualHostsInput.collectAsStateWithLifecycle()
    val isJitterEnabled by viewModel.isJitterEnabled.collectAsStateWithLifecycle()
    val isMultiHopEnabled by viewModel.isMultiHopEnabled.collectAsStateWithLifecycle()
    val isFingerprintScramblingEnabled by viewModel.isFingerprintScramblingEnabled.collectAsStateWithLifecycle()
    val isPayloadPaddingEnabled by viewModel.isPayloadPaddingEnabled.collectAsStateWithLifecycle()
    val isTrafficObfuscationEnabled by viewModel.isTrafficObfuscationEnabled.collectAsStateWithLifecycle()
    val isKillSwitchEnabled by viewModel.isKillSwitchEnabled.collectAsStateWithLifecycle()
    val isDynamicPacketSizingEnabled by viewModel.isDynamicPacketSizingEnabled.collectAsStateWithLifecycle()
    val isWebIPMaskingEnabled by viewModel.isWebIPMaskingEnabled.collectAsStateWithLifecycle()
    val isDecoyShuffleEnabled by viewModel.isDecoyShuffleEnabled.collectAsStateWithLifecycle()
    val isVpsRelayEnabled by viewModel.isVpsRelayEnabled.collectAsStateWithLifecycle()
    val vpsIp by viewModel.vpsIp.collectAsStateWithLifecycle()
    val vpsPort by viewModel.vpsPort.collectAsStateWithLifecycle()
    val vpsSecretKey by viewModel.vpsSecretKey.collectAsStateWithLifecycle()
    val decoyHost by viewModel.decoyHost.collectAsStateWithLifecycle()
    val decoyOptions = viewModel.decoyOptions

    val intervals = listOf(1, 5, 10, 30, 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "IP AUTO-SHUFFLE ENGINE",
                            color = CyberEmerald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Rotates gateway IP dynamically at chosen intervals using WireGuard peer roaming to bypass deep packet inspection without dropping connection.",
                            color = CyberGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isIpRotationEnabled,
                        onCheckedChange = { viewModel.toggleIpRotation() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberEmerald,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("ip_rotation_toggle")
                    )
                }
            }
        }

        // Selection of rotation intervals
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SHUFFLE FREQUENCY INTERVAL",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    intervals.forEach { sec ->
                        val isSelected = rotationInterval == sec
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberEmerald else CyberCard)
                                .border(
                                    1.dp,
                                    if (isSelected) CyberEmerald else CyberGray.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setRotationInterval(sec) }
                                .padding(vertical = 8.dp)
                                .testTag("interval_$sec")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${sec}s",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = if (isSelected) CyberBlack else CyberWhite
                                )
                                if (sec == 1) {
                                    Text(
                                        "HYPER",
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) CyberBlack else CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Advanced Ghost Obfuscation Features Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ADVANCED GHOST PRIVACY PROTOCOLS",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Jitter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "TEMPORAL TIMING JITTER",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Applies random dynamic delay skew (+/- 35%) to break predictable periodic traffic analysis patterns.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isJitterEnabled,
                        onCheckedChange = { viewModel.toggleJitter() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_timing_jitter")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Multi-Hop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MULTI-HOP CIRCUIT (CHAINING)",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Routes packets through entry, middle relays, and exits to ensure zero direct correlations.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isMultiHopEnabled,
                        onCheckedChange = { viewModel.toggleMultiHop() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_multi_hop")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Fingerprint Scrambling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "DYNAMIC IDENTITY SCRAMBLING",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Rotates HTTP/TLS user-agent, headers, and TCP window parameter fingerprints.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isFingerprintScramblingEnabled,
                        onCheckedChange = { viewModel.toggleFingerprintScrambling() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_fingerprint_scrambling")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Payload Padding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RANDOM PACKET PAYLOAD PADDING",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Appends high-entropy randomized dummy bytes to shield outgoing frame-size patterns.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isPayloadPaddingEnabled,
                        onCheckedChange = { viewModel.togglePayloadPadding() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_payload_padding")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Traffic Obfuscation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "HIGH-BANDWIDTH TRAFFIC OBFUSCATION",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Masks large downloads/uploads into constant-bitrate flows. Injects entropy padding to prevent local admins from analyzing pattern sizes or identifying file types.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isTrafficObfuscationEnabled,
                        onCheckedChange = { viewModel.toggleTrafficObfuscation() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_traffic_obfuscation")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // VPN Kill Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "VPN CONNECTION KILL SWITCH",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Strictly blocks all unencrypted internet traffic if the VPN connection drops or fails to authenticate, preventing bypass leaks.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isKillSwitchEnabled,
                        onCheckedChange = { viewModel.toggleKillSwitch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_kill_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Packet Sizing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "DYNAMIC PACKET SIZING",
                            color = CyberWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Fluctuates the MTU session sizes dynamically between 1280 and 1420 bytes to obscure deep packet size/volume fingerprinting signatures.",
                            color = CyberGray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isDynamicPacketSizingEnabled,
                        onCheckedChange = { viewModel.toggleDynamicPacketSizing() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("toggle_dynamic_packet_sizing")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Web IP Masking (SNI Fronting)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "WEB IP MASKING (SNI FRONTING)",
                                color = CyberWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Tricks local routers and firewall inspectors into thinking you are visiting a safe standard web domain instead of connecting to a VPN gateway.",
                                color = CyberGray,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isWebIPMaskingEnabled,
                            onCheckedChange = { viewModel.toggleWebIPMasking() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = CyberGray,
                                uncheckedTrackColor = CyberCard
                            ),
                            modifier = Modifier.testTag("toggle_web_ip_masking")
                        )
                    }

                    if (isWebIPMaskingEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "DECOY WEB DESTINATION",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // First Row of Decoys
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val row1 = decoyOptions.take(4)
                            row1.forEach { option ->
                                val isSelected = decoyHost == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) CyberCyan else CyberGray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { viewModel.decoyHost.value = option }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.substringBefore(".com")
                                            .substringBefore(".io")
                                            .substringBefore(".org")
                                            .removePrefix("www.")
                                            .removePrefix("cdn."),
                                        color = if (isSelected) CyberCyan else CyberGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Second Row of Decoys
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val row2 = decoyOptions.drop(4)
                            row2.forEach { option ->
                                val isSelected = decoyHost == option
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) CyberCyan else CyberGray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { viewModel.decoyHost.value = option }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.substringBefore(".com")
                                            .substringBefore(".io")
                                            .substringBefore(".org")
                                            .removePrefix("www.")
                                            .removePrefix("cdn."),
                                        color = if (isSelected) CyberCyan else CyberGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CyberGray.copy(alpha = 0.1f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Decoy Auto-Shuffle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "DECOY AUTO-SHUFFLE",
                                    color = CyberWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Automatically rotates the fronting decoy target during session shuffles or handshakes to bypass behavior and pattern-matching signature firewalls.",
                                    color = CyberGray,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isDecoyShuffleEnabled,
                                onCheckedChange = { viewModel.toggleDecoyShuffle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberBlack,
                                    checkedTrackColor = CyberCyan,
                                    uncheckedThumbColor = CyberGray,
                                    uncheckedTrackColor = CyberCard
                                ),
                                modifier = Modifier.testTag("toggle_decoy_shuffle")
                            )
                        }
                    }
                }
            }
        }

        // Custom VPS Relay Gateway
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "CUSTOM VPS RELAY GATEWAY",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Route all outgoing session tunnels through your personal VPS relay node for customized hops.",
                            color = CyberGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isVpsRelayEnabled,
                        onCheckedChange = { viewModel.toggleVpsRelay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("vps_relay_toggle")
                    )
                }

                if (isVpsRelayEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // VPS Server IP Configuration
                    Text(
                        "VPS SERVER IP ADDRESS",
                        color = CyberWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = vpsIp,
                        onValueChange = { viewModel.vpsIp.value = it },
                        placeholder = { Text("e.g. 194.26.135.12", color = CyberGray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.3f),
                            focusedTextColor = CyberWhite,
                            unfocusedTextColor = CyberWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("vps_ip_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // VPS Port & Secret Key
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(0.35f)) {
                            Text(
                                "PORT",
                                color = CyberWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = vpsPort,
                                onValueChange = { viewModel.vpsPort.value = it },
                                placeholder = { Text("51820", color = CyberGray) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberGray.copy(alpha = 0.3f),
                                    focusedTextColor = CyberWhite,
                                    unfocusedTextColor = CyberWhite
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("vps_port_input"),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(0.65f)) {
                            Text(
                                "WIREGUARD SECRET KEY",
                                color = CyberWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = vpsSecretKey,
                                onValueChange = { viewModel.vpsSecretKey.value = it },
                                placeholder = { Text("Curve25519 Secret Key", color = CyberGray) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberGray.copy(alpha = 0.3f),
                                    focusedTextColor = CyberWhite,
                                    unfocusedTextColor = CyberWhite
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("vps_secret_key_input"),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // Optional feature: Manual hosts input
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MANUAL HOSTS LIST (OPTIONAL)",
                            color = CyberEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Provide custom IP addresses or server hostnames (comma-separated) to shuffle between.",
                            color = CyberGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isManualHostsEnabled,
                        onCheckedChange = { viewModel.toggleManualHosts() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = CyberEmerald,
                            uncheckedThumbColor = CyberGray,
                            uncheckedTrackColor = CyberCard
                        ),
                        modifier = Modifier.testTag("manual_hosts_toggle")
                    )
                }

                if (isManualHostsEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualHostsInput,
                        onValueChange = { viewModel.manualHostsInput.value = it },
                        placeholder = { Text("e.g. 1.1.1.1, 8.8.8.8, my.custom.vpn", color = CyberGray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.3f),
                            focusedTextColor = CyberWhite,
                            unfocusedTextColor = CyberWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_hosts_field")
                    )
                }
            }
        }

        // IP History / Rotation Log Sub-Terminal
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberGray.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ACTIVE SHUFFLE LOG",
                    color = CyberWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val shuffles = terminalLogs.filter { 
                        it.contains("STEALTH-ROTATOR") || 
                        it.contains("ROTATOR") || 
                        it.contains("GHOST-ROUTING") || 
                        it.contains("OBFUSCATION") || 
                        it.contains("FINGERPRINT") || 
                        it.contains("CLOAKING")
                    }
                    if (shuffles.isEmpty()) {
                        Text(
                            "No rotative handshakes recorded yet.\nToggle the Switch and select a fast interval to witness dynamic IP shuffling.",
                            color = CyberGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        )
                    } else {
                        shuffles.forEach { log ->
                            Text(
                                text = log,
                                color = CyberEmerald,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireGuardTab(viewModel: VpnViewModel) {
    val clientPrivateKey by viewModel.clientPrivateKey.collectAsStateWithLifecycle()
    val clientPublicKey by viewModel.clientPublicKey.collectAsStateWithLifecycle()
    val serverPublicKey by viewModel.serverPublicKey.collectAsStateWithLifecycle()
    val presharedKey by viewModel.presharedKey.collectAsStateWithLifecycle()
    val wireguardInterfaceAddress by viewModel.wireguardInterfaceAddress.collectAsStateWithLifecycle()
    val wireguardDns by viewModel.wireguardDns.collectAsStateWithLifecycle()
    val allowedIps by viewModel.allowedIps.collectAsStateWithLifecycle()
    val persistentKeepalive by viewModel.persistentKeepalive.collectAsStateWithLifecycle()
    val endpointAddress by viewModel.endpointAddress.collectAsStateWithLifecycle()
    val wireguardConfig by viewModel.wireguardConfig.collectAsStateWithLifecycle()
    val handshakeLogs by viewModel.handshakeLogs.collectAsStateWithLifecycle()
    val isHandshaking by viewModel.isHandshaking.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "STANDARD WIREGUARD® PROTOCOL LAB",
            color = CyberEmerald,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = "GhostLine utilizes standard, kernel-optimized WireGuard tunneling for near-zero battery overhead and blazing network speed. Fine-tune peer keys and trigger live cryptographic handshakes below.",
            color = CyberGray,
            fontSize = 11.sp
        )

        // 1. Peer Profile Configurator Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "WIREGUARD CRYPTOGRAPHIC KEYS",
                    color = CyberEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Client Keys Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = clientPrivateKey,
                        onValueChange = { 
                            viewModel.clientPrivateKey.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Client Private Key") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberEmerald,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_client_private_key_input")
                    )
                    OutlinedTextField(
                        value = clientPublicKey,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Client Public Key") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberGray),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGray,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.3f),
                            focusedLabelColor = CyberGray,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_client_public_key_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Peer Keys Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = serverPublicKey,
                        onValueChange = { 
                            viewModel.serverPublicKey.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Peer/Server Public Key") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberEmerald,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_server_public_key_input")
                    )
                    OutlinedTextField(
                        value = presharedKey,
                        onValueChange = { 
                            viewModel.presharedKey.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Preshared Key (Optional)") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberEmerald,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_preshared_key_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.generateWireGuardKeys() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_wireguard_keys_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE NEW WIRE-KEYPAIR", color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 2. Network Configurations Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INTERFACE & ENDPOINT DEFAULTS",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = wireguardInterfaceAddress,
                    onValueChange = { 
                        viewModel.wireguardInterfaceAddress.value = it
                        viewModel.updateWireGuardConfig()
                    },
                    label = { Text("Interface Address(es)") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = CyberWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = CyberGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wireguard_address_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = wireguardDns,
                        onValueChange = { 
                            viewModel.wireguardDns.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("DNS Servers") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_dns_input")
                    )
                    OutlinedTextField(
                        value = allowedIps,
                        onValueChange = { 
                            viewModel.allowedIps.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Allowed IPs") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wireguard_allowed_ips_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endpointAddress,
                        onValueChange = { 
                            viewModel.endpointAddress.value = it
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Endpoint Node Address") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("wireguard_endpoint_input")
                    )
                    OutlinedTextField(
                        value = if (persistentKeepalive > 0) persistentKeepalive.toString() else "",
                        onValueChange = { 
                            viewModel.persistentKeepalive.value = it.toIntOrNull() ?: 0
                            viewModel.updateWireGuardConfig()
                        },
                        label = { Text("Keepalive") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CyberWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberGray.copy(alpha = 0.5f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = CyberGray
                        ),
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("wireguard_keepalive_input")
                    )
                }
            }
        }

        // 3. Noise Handshake Simulator Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "NOISE PROTOCOL HANDSHAKE SIMULATOR (1-RTT UDP)",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    "WireGuard establishes zero-state sessions using a single Round-Trip Time handshake. Click below to execute and visualize the Noise_IKpsk2 authentication loop.",
                    color = CyberGray,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.performInteractiveHandshake() },
                    enabled = !isHandshaking,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_handshake_button")
                ) {
                    if (isHandshaking) {
                        CircularProgressIndicator(
                            color = CyberBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUTHENTICATING WIREGUARD PEER...", color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = CyberBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PERFORM NOISE HANDSHAKE", color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                if (handshakeLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("NOISE PROTOCOL WIRE PACKET LOGS:", color = CyberGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberBlack)
                            .padding(10.dp)
                    ) {
                        handshakeLogs.forEach { log ->
                            Text(
                                text = log,
                                color = when {
                                    log.contains("[INIT]") || log.contains("[MSG 1]") -> CyberEmerald
                                    log.contains("[MSG 2]") -> CyberCyan
                                    log.contains("[DERIVE]") || log.contains("successful") -> CyberWhite
                                    else -> CyberGray
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Config File Exporter
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberGray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "GENERATED .CONF PROFILE",
                        color = CyberWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(wireguardConfig)) },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("copy_config_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Config", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberBlack)
                        .padding(10.dp)
                ) {
                    Text(
                        text = wireguardConfig,
                        color = CyberGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // 5. Why WireGuard Comparison Matrix
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, CyberGray.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "WIREGUARD® PROTOCOL ADVANTAGES",
                    color = CyberWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FEATURE", color = CyberCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Handshake", color = CyberWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Max Speeds", color = CyberWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("CPU & Battery", color = CyberWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("IP Roaming", color = CyberWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("CUSTOM JAVA AES (OLD)", color = CyberGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Heavy SSL/TCP (120ms)", color = CyberGray, fontSize = 10.sp)
                        Text("~45 Mbps Limit (Heavy)", color = CyberGray, fontSize = 10.sp)
                        Text("High (User-space context)", color = CyberGray, fontSize = 10.sp)
                        Text("Drops connection on handoff", color = CyberGray, fontSize = 10.sp)
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("WIREGUARD UDP (NEW)", color = CyberEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("1-RTT Noise (20ms)", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("150+ Mbps (Ultra fast)", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Ultra-Low (<1% CPU)", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Seamless Roaming (Stateful)", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelaysTab(viewModel: VpnViewModel) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PUBLIC STEALTH RELAYS",
                    color = CyberWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${servers.size} available routes active",
                    color = CyberGray,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = { viewModel.loadServers() },
                modifier = Modifier
                    .background(CyberCard, CircleShape)
                    .testTag("refresh_servers_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = CyberEmerald
                )
            }
        }

        // Search Outlined Box
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Filter country (e.g. US, Japan)", color = CyberGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberGray) },
            textStyle = androidx.compose.ui.text.TextStyle(color = CyberWhite, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberEmerald,
                unfocusedBorderColor = CyberCard,
                focusedTextColor = CyberWhite,
                unfocusedTextColor = CyberWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field")
        )

        val filtered = servers.filter {
            it.countryLong.contains(query, ignoreCase = true) ||
            it.countryShort.contains(query, ignoreCase = true)
        }

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberEmerald)
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No relays found matching \"$query\"",
                    color = CyberGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { server ->
                    val isSelected = selectedServer?.ip == server.ip
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyberCard else CyberCard.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyberEmerald else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectServer(server) }
                            .testTag("server_item_${server.ip}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(CyberEmerald.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = server.countryShort,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberEmerald
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = server.countryLong,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CyberWhite
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "IP: ${server.ip} • Host: ${server.hostName}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${server.ping} ms",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        server.ping < 50 -> CyberEmerald
                                        server.ping < 150 -> CyberCyan
                                        else -> CyberGray
                                    }
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f Mbps", server.speed / 1000000f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalConsole(viewModel: VpnViewModel) {
    val logs by viewModel.terminalLogs.collectAsStateWithLifecycle()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF030508)),
        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE_TUNNEL_CONSOLE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberEmerald)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = CyberEmerald.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = when {
                            log.contains("ERROR") -> CyberRed
                            log.contains("STEALTH") -> CyberEmerald
                            log.contains("WIREGUARD") -> CyberCyan
                            else -> CyberGray
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
