package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.compat.engine.GameRunnerEngine
import com.example.compat.engine.GameState
import com.example.compat.engine.GameType
import com.example.compat.sandbox.LogType
import com.example.compat.sandbox.SyscallLogEntry
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GameRunnerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    val engine = session.engine ?: return
    val bridge = session.bridge ?: return
    val gameState by engine.state.collectAsStateWithLifecycle()
    val logs by bridge.logs.collectAsStateWithLifecycle()
    val metrics by bridge.metrics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Handle Android system back gesture to safely exit the sandbox
    BackHandler {
        viewModel.exitActiveSession()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("game_runner_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.exitActiveSession() },
                        modifier = Modifier.testTag("exit_sandbox_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Sandbox",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = gameState.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ARM32 SANDBOX",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // FPS & Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // FPS Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${gameState.fps} FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (gameState.fps >= 55) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // CRT filter toggle
                    IconButton(onClick = { viewModel.toggleCrtShader() }) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "CRT Filter",
                            tint = if (session.crtShader) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Syscall Console toggle
                    IconButton(
                        onClick = { viewModel.toggleSyscallDrawer() },
                        modifier = Modifier.testTag("toggle_syscall_console_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Syscall Bridge Console",
                            tint = if (session.showSyscallDrawer) NeonEmerald else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pause / Resume
                    IconButton(onClick = {
                        if (gameState.isPaused) engine.resume() else engine.pause()
                    }) {
                        Icon(
                            imageVector = if (gameState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Viewport Box: Game Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when (gameState.gameType) {
                    GameType.FLAPPY_32 -> FlappyGameCanvas(
                        state = gameState,
                        onFlap = {
                            vibrateShort()
                            engine.onFlap()
                        }
                    )
                    GameType.SPACE_RAIDER_32 -> SpaceRaiderCanvas(
                        state = gameState,
                        onDrag = { dx -> engine.onMoveShip(dx) },
                        onFire = {
                            vibrateShort()
                            engine.onFireLaser()
                        }
                    )
                    GameType.POCKET_2048 -> Pocket2048Canvas(
                        state = gameState,
                        onSwipe = { dir ->
                            vibrateShort()
                            engine.onSwipe2048(dir)
                        }
                    )
                    GameType.PIXEL_DUNGEON_32 -> PixelDungeonCanvas(
                        state = gameState,
                        onMove = { dx, dy ->
                            vibrateShort()
                            engine.onMoveDungeon(dx, dy)
                        }
                    )
                    GameType.GENERIC_APK -> GenericApkCanvas(
                        state = gameState,
                        onTouch = { x, y ->
                            vibrateShort()
                            engine.onTouchGeneric(x, y)
                        }
                    )
                }

                // CRT Scanline Overlay if enabled
                if (session.crtShader) {
                    CrtScanlineOverlay()
                }

                // Pause Overlay
                if (gameState.isPaused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "EMULATION PAUSED",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { engine.resume() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("Resume Execution", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Virtual Gamepad Controls Section
            VirtualGamepad(
                gameState = gameState,
                onDpadPress = { dx, dy ->
                    vibrateShort()
                    when (gameState.gameType) {
                        GameType.SPACE_RAIDER_32 -> engine.onMoveShip(dx * 25f)
                        GameType.PIXEL_DUNGEON_32 -> engine.onMoveDungeon(dx, dy)
                        GameType.POCKET_2048 -> {
                            val dir = when {
                                dx < 0 -> GameRunnerEngine.SwipeDirection.LEFT
                                dx > 0 -> GameRunnerEngine.SwipeDirection.RIGHT
                                dy < 0 -> GameRunnerEngine.SwipeDirection.UP
                                else -> GameRunnerEngine.SwipeDirection.DOWN
                            }
                            engine.onSwipe2048(dir)
                        }
                        GameType.FLAPPY_32 -> engine.onFlap()
                        GameType.GENERIC_APK -> engine.onTouchGeneric(360f + dx * 60f, 400f + dy * 60f)
                    }
                },
                onActionA = {
                    vibrateShort()
                    when (gameState.gameType) {
                        GameType.FLAPPY_32 -> engine.onFlap()
                        GameType.SPACE_RAIDER_32 -> engine.onFireLaser()
                        GameType.PIXEL_DUNGEON_32 -> engine.onMoveDungeon(0, 0)
                        GameType.POCKET_2048 -> engine.initGame()
                        GameType.GENERIC_APK -> engine.onTouchGeneric(360f, 600f)
                    }
                },
                onActionB = {
                    vibrateShort()
                    when (gameState.gameType) {
                        GameType.SPACE_RAIDER_32 -> {
                            // Double fire laser
                            engine.onFireLaser()
                            engine.onFireLaser()
                        }
                        GameType.FLAPPY_32 -> engine.onFlap()
                        else -> engine.initGame()
                    }
                }
            )
        }

        // Collapsible Syscall & JNI Bridge Console Drawer
        AnimatedVisibility(
            visible = session.showSyscallDrawer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SyscallConsolePanel(
                logs = logs,
                metrics = metrics,
                onClose = { viewModel.toggleSyscallDrawer() },
                onClear = { bridge.clearLogs() }
            )
        }
    }
}

// ----------------- Game Canvases -----------------

@Composable
fun FlappyGameCanvas(
    state: GameState,
    onFlap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onFlap() }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Sky Background
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                ),
                size = size
            )

            // Scale factors
            val scaleX = canvasW / 720f
            val scaleY = canvasH / 1000f

            // Pipes
            state.pipes.forEach { pipe ->
                val px = pipe.x * scaleX
                val topH = pipe.topHeight * scaleY
                val gapH = pipe.gap * scaleY
                val pw = 65f * scaleX

                // Top Pipe
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF15803D), Color(0xFF22C55E), Color(0xFF166534)),
                        startX = px,
                        endX = px + pw
                    ),
                    topLeft = Offset(px, 0f),
                    size = Size(pw, topH)
                )
                // Bottom Pipe
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF15803D), Color(0xFF22C55E), Color(0xFF166534)),
                        startX = px,
                        endX = px + pw
                    ),
                    topLeft = Offset(px, topH + gapH),
                    size = Size(pw, canvasH - (topH + gapH))
                )
            }

            // Bird (Yellow & Cyan Retro Cyber Bird)
            val bx = 140f * scaleX
            val by = state.birdY * scaleY
            val birdRadius = 18f * scaleX

            drawCircle(
                color = Color(0xFFFBBF24),
                radius = birdRadius,
                center = Offset(bx, by)
            )
            // Eye
            drawCircle(
                color = Color.White,
                radius = 5f * scaleX,
                center = Offset(bx + 8f * scaleX, by - 6f * scaleY)
            )
            drawCircle(
                color = Color.Black,
                radius = 2.5f * scaleX,
                center = Offset(bx + 10f * scaleX, by - 6f * scaleY)
            )
            // Beak
            val beakPath = Path().apply {
                moveTo(bx + birdRadius, by - 2f)
                lineTo(bx + birdRadius + 12f * scaleX, by + 4f * scaleY)
                lineTo(bx + birdRadius, by + 10f * scaleY)
                close()
            }
            drawPath(beakPath, color = Color(0xFFF97316))

            // Ground Bar
            drawRect(
                color = Color(0xFF78350F),
                topLeft = Offset(0f, canvasH - 30f),
                size = Size(canvasW, 30f)
            )
        }

        // Live Score Counter
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${state.score}",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        }

        // Game Over Banner
        if (state.isGameOver) {
            Card(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(NeonRose, NeonAmber))
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonRose,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "SCORE: ${state.score}", fontSize = 16.sp, color = TextPrimary)
                    Text(text = "BEST: ${state.highScore}", fontSize = 16.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onFlap,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart Round", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceRaiderCanvas(
    state: GameState,
    onDrag: (Float) -> Unit,
    onFire: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount -> onDrag(dragAmount.x) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { onFire() }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / 720f
            val scaleY = size.height / 1000f

            // Stars
            state.stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = star.size * scaleX,
                    center = Offset(star.x * scaleX, star.y * scaleY)
                )
            }

            // Lasers
            state.lasers.forEach { laser ->
                drawLine(
                    color = NeonCyan,
                    start = Offset(laser.x * scaleX, laser.y * scaleY),
                    end = Offset(laser.x * scaleX, (laser.y - 20f) * scaleY),
                    strokeWidth = 4f * scaleX
                )
            }

            // Aliens
            state.aliens.forEach { alien ->
                val ax = alien.x * scaleX
                val ay = alien.y * scaleY
                drawRect(
                    color = NeonRose,
                    topLeft = Offset(ax - 16f * scaleX, ay - 16f * scaleY),
                    size = Size(32f * scaleX, 32f * scaleY)
                )
            }

            // Player Starship
            val px = state.playerShipX * scaleX
            val py = 800f * scaleY
            val shipPath = Path().apply {
                moveTo(px, py - 24f * scaleY)
                lineTo(px - 22f * scaleX, py + 18f * scaleY)
                lineTo(px, py + 10f * scaleY)
                lineTo(px + 22f * scaleX, py + 18f * scaleY)
                close()
            }
            drawPath(shipPath, brush = Brush.verticalGradient(listOf(NeonCyan, Color(0xFF0284C7))))
        }

        // Space HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SCORE: ${state.score}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
            Text(
                text = "HP: ${"❤️".repeat(state.playerHealth)}",
                fontSize = 16.sp
            )
        }

        if (state.isGameOver) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "SHIP DESTROYED", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeonRose)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Final Score: ${state.score}", fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onFire,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Deploy New Ship", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun Pocket2048Canvas(
    state: GameState,
    onSwipe: (GameRunnerEngine.SwipeDirection) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y)) {
                            if (dragAmount.x > 18) onSwipe(GameRunnerEngine.SwipeDirection.RIGHT)
                            else if (dragAmount.x < -18) onSwipe(GameRunnerEngine.SwipeDirection.LEFT)
                        } else {
                            if (dragAmount.y > 18) onSwipe(GameRunnerEngine.SwipeDirection.DOWN)
                            else if (dragAmount.y < -18) onSwipe(GameRunnerEngine.SwipeDirection.UP)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2048 ARM32",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBox(label = "SCORE", value = state.score)
                    ScoreBox(label = "BEST", value = state.highScore)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4x4 Grid Container
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2433))
                    .border(2.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (r in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (c in 0 until 4) {
                                val value = state.grid2048.getOrNull(r)?.getOrNull(c) ?: 0
                                Tile2048(value = value)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Tile2048(value: Int) {
    val bgColor = when (value) {
        2 -> Color(0xFF334155)
        4 -> Color(0xFF475569)
        8 -> Color(0xFF0284C7)
        16 -> Color(0xFF00E5FF)
        32 -> Color(0xFF10B981)
        64 -> Color(0xFF059669)
        128 -> Color(0xFFF59E0B)
        256 -> Color(0xFFEA580C)
        512 -> Color(0xFFF43F5E)
        1024 -> Color(0xFF8B5CF6)
        2048 -> Color(0xFFEAB308)
        else -> Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = "$value",
                fontSize = if (value >= 1024) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (value in listOf(2, 4)) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun PixelDungeonCanvas(
    state: GameState,
    onMove: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Floor B${state.dungeonFloor} • HP ${state.dungeonHp}/${state.dungeonMaxHp}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.dungeonHp > 8) NeonEmerald else NeonRose
            )
            Text(
                text = "Gold: ${state.dungeonGold} 💰",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonAmber
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8x8 Dungeon Grid
        Box(
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (y in 0 until 8) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (x in 0 until 8) {
                            val isPlayer = state.dungeonPlayerX == x && state.dungeonPlayerY == y
                            val monster = state.monsters.find { it.x == x && it.y == y && it.hp > 0 }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isPlayer -> NeonCyan.copy(alpha = 0.3f)
                                            monster != null -> NeonRose.copy(alpha = 0.3f)
                                            else -> Color(0xFF1E293B)
                                        }
                                    )
                                    .clickable {
                                        val dx = (x - state.dungeonPlayerX).coerceIn(-1, 1)
                                        val dy = (y - state.dungeonPlayerY).coerceIn(-1, 1)
                                        onMove(dx, dy)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isPlayer -> "🧙‍♂️"
                                        monster != null -> "👾"
                                        (x + y) % 5 == 0 -> "🧱"
                                        else -> "·"
                                    },
                                    fontSize = 14.sp
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
fun GenericApkCanvas(
    state: GameState,
    onTouch: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050B14))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTouch(offset.x, offset.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ARM32",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SANDBOX CONTAINER RUNNING",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Virtual EGL Surface: 720x1280 @ 60 Hz\nAddress Space: 32-bit (0x00000000 - 0xC0000000)\nNative Libraries Loaded: ${state.activeNativeLibs.joinToString(", ")}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Tap surface to trigger ARM32 MotionEvent Syscalls\nLast Touch: (${state.touchX.toInt()}, ${state.touchY.toInt()})",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ScoreBox(label: String, value: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E2433))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(text = "$value", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}

@Composable
fun CrtScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += lineSpacing
        }
    }
}

// ----------------- Virtual Gamepad Controls -----------------

@Composable
fun VirtualGamepad(
    gameState: GameState,
    onDpadPress: (Int, Int) -> Unit,
    onActionA: () -> Unit,
    onActionB: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad Cross
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            // Up
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onDpadPress(0, -1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary)
            }
            // Down
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onDpadPress(0, 1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary)
            }
            // Left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onDpadPress(-1, 0) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary)
            }
            // Right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onDpadPress(1, 0) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary)
            }
            // Center Core
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
            )
        }

        // Action Buttons (A & B)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Button B (Cyan / Secondary)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0EA5E9), Color(0xFF0369A1))
                        )
                    )
                    .border(2.dp, NeonCyan, CircleShape)
                    .clickable { onActionB() }
                    .testTag("gamepad_btn_b"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Button A (Rose / Primary)
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
                        )
                    )
                    .border(2.dp, Color(0xFFFDA4AF), CircleShape)
                    .clickable { onActionA() }
                    .testTag("gamepad_btn_a"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

// ----------------- Syscall Bridge Console -----------------

@Composable
fun SyscallConsolePanel(
    logs: List<SyscallLogEntry>,
    metrics: com.example.compat.sandbox.BridgeMetrics,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .testTag("syscall_console_panel"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF00A0E17)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NeonEmerald, CyberBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Console Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ARM32 -> ARM64 SYSCALL BRIDGE LOG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonEmerald
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClear) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Real-time telemetry strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF131A2A))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Syscalls/s: ${metrics.syscallsPerSec}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
                Text(
                    text = "Mapped: ${String.format("%.1f", metrics.activeMemoryMappedMb)} MB",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonAmber
                )
                Text(
                    text = "JNI Hooks: ${metrics.hookedJniSymbolsCount}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonEmerald
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Logs stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(logs) { entry ->
                    val typeColor = when (entry.type) {
                        LogType.SYSCALL_TRANSLATE -> NeonCyan
                        LogType.JNI_HOOK -> NeonPurple
                        LogType.MEMORY_REMAP -> NeonAmber
                        LogType.GL_ES_EMU -> NeonEmerald
                        LogType.AUDIO_BRIDGE -> Color(0xFF38BDF8)
                        LogType.SANDBOX_EVENT -> NeonRose
                    }

                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.timestamp,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[${entry.type.name}]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        }
                        Text(
                            text = entry.message,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        if (entry.details != null) {
                            Text(
                                text = "  └ ${entry.details}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
