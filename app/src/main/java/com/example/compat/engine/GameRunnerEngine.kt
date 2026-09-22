package com.example.compat.engine

import android.os.SystemClock
import com.example.compat.sandbox.CompatibilityBridge
import com.example.compat.sandbox.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameType {
    FLAPPY_32,
    SPACE_RAIDER_32,
    POCKET_2048,
    PIXEL_DUNGEON_32,
    GENERIC_APK
}

// Data structures for Flappy
data class Pipe(val x: Float, val topHeight: Float, val gap: Float = 220f, var passed: Boolean = false)

// Data structures for Space Raider
data class Laser(val x: Float, var y: Float)
data class Alien(var x: Float, var y: Float, val speed: Float, var hp: Int = 1)
data class Star(val x: Float, var y: Float, val speed: Float, val size: Float)

// Data structures for Pixel Dungeon
data class Monster(var x: Int, var y: Int, val name: String, var hp: Int, val maxHp: Int)

data class GameState(
    val gameType: GameType,
    val title: String,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val score: Int = 0,
    val highScore: Int = 0,
    val fps: Int = 60,
    val frameCount: Long = 0,

    // Flappy 32 State
    val birdY: Float = 400f,
    val birdVelocity: Float = 0f,
    val pipes: List<Pipe> = emptyList(),

    // Space Raider State
    val playerShipX: Float = 360f,
    val lasers: List<Laser> = emptyList(),
    val aliens: List<Alien> = emptyList(),
    val stars: List<Star> = emptyList(),
    val playerHealth: Int = 3,

    // 2048 State
    val grid2048: List<List<Int>> = List(4) { List(4) { 0 } },

    // Pixel Dungeon State
    val dungeonPlayerX: Int = 2,
    val dungeonPlayerY: Int = 2,
    val dungeonFloor: Int = 1,
    val dungeonHp: Int = 20,
    val dungeonMaxHp: Int = 20,
    val dungeonGold: Int = 0,
    val monsters: List<Monster> = emptyList(),

    // Generic APK Sandbox state
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val activeNativeLibs: List<String> = emptyList()
)

class GameRunnerEngine(
    private val scope: CoroutineScope,
    private val bridge: CompatibilityBridge,
    val gameType: GameType,
    val gameTitle: String,
    initialHighScore: Int = 0,
    nativeLibs: List<String> = emptyList()
) {
    private val _state = MutableStateFlow(
        GameState(
            gameType = gameType,
            title = gameTitle,
            highScore = initialHighScore,
            activeNativeLibs = nativeLibs
        )
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var lastFpsTimestamp = SystemClock.uptimeMillis()
    private var framesThisSecond = 0
    private var spawnTimer = 0

    init {
        initGame()
    }

    fun initGame() {
        when (gameType) {
            GameType.FLAPPY_32 -> {
                _state.value = _state.value.copy(
                    birdY = 400f,
                    birdVelocity = 0f,
                    pipes = listOf(
                        Pipe(x = 600f, topHeight = 250f),
                        Pipe(x = 950f, topHeight = 350f)
                    ),
                    score = 0,
                    isGameOver = false
                )
            }
            GameType.SPACE_RAIDER_32 -> {
                val stars = List(35) {
                    Star(
                        x = Random.nextFloat() * 720f,
                        y = Random.nextFloat() * 1000f,
                        speed = Random.nextFloat() * 4f + 2f,
                        size = Random.nextFloat() * 2.5f + 1f
                    )
                }
                _state.value = _state.value.copy(
                    playerShipX = 360f,
                    lasers = emptyList(),
                    aliens = emptyList(),
                    stars = stars,
                    playerHealth = 3,
                    score = 0,
                    isGameOver = false
                )
            }
            GameType.POCKET_2048 -> {
                val grid = MutableList(4) { MutableList(4) { 0 } }
                spawn2048Tile(grid)
                spawn2048Tile(grid)
                _state.value = _state.value.copy(
                    grid2048 = grid,
                    score = 0,
                    isGameOver = false
                )
            }
            GameType.PIXEL_DUNGEON_32 -> {
                val monsters = listOf(
                    Monster(4, 4, "Armored Rat", 8, 8),
                    Monster(6, 2, "Cave Gnoll", 12, 12),
                    Monster(2, 6, "Sewer Crab", 15, 15)
                )
                _state.value = _state.value.copy(
                    dungeonPlayerX = 2,
                    dungeonPlayerY = 2,
                    dungeonHp = 20,
                    dungeonMaxHp = 20,
                    dungeonFloor = 1,
                    dungeonGold = 25,
                    monsters = monsters,
                    score = 25,
                    isGameOver = false
                )
            }
            GameType.GENERIC_APK -> {
                _state.value = _state.value.copy(
                    score = 100,
                    isGameOver = false
                )
            }
        }
    }

    fun start() {
        if (loopJob?.isActive == true) return
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
        bridge.log(LogType.SANDBOX_EVENT, "Game execution started: ${_state.value.title}")

        loopJob = scope.launch(Dispatchers.Default) {
            val targetFrameTimeMs = 16L // ~60 FPS
            while (isActive && _state.value.isRunning) {
                val frameStart = SystemClock.uptimeMillis()

                if (!_state.value.isPaused && !_state.value.isGameOver) {
                    updatePhysics()
                }

                // FPS & Telemetry Calculation
                framesThisSecond++
                val now = SystemClock.uptimeMillis()
                if (now - lastFpsTimestamp >= 1000L) {
                    val currentFps = framesThisSecond
                    framesThisSecond = 0
                    lastFpsTimestamp = now
                    _state.value = _state.value.copy(fps = currentFps)
                    // Trigger translation bridge telemetry batch
                    bridge.recordSyscallBatch(currentFps * 8)
                }

                val frameDuration = SystemClock.uptimeMillis() - frameStart
                val delayTime = targetFrameTimeMs - frameDuration
                if (delayTime > 0) {
                    delay(delayTime)
                }
            }
        }
    }

    fun pause() {
        _state.value = _state.value.copy(isPaused = true)
        bridge.log(LogType.SANDBOX_EVENT, "Game execution suspended (pause)")
    }

    fun resume() {
        _state.value = _state.value.copy(isPaused = false)
        bridge.log(LogType.SANDBOX_EVENT, "Game execution resumed")
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        _state.value = _state.value.copy(isRunning = false)
        bridge.log(LogType.SANDBOX_EVENT, "Game process terminated in sandbox")
    }

    // Input Actions
    fun onFlap() {
        if (_state.value.isGameOver) {
            initGame()
            return
        }
        _state.value = _state.value.copy(birdVelocity = -13f)
        RetroAudioSynth.playJump()
    }

    fun onMoveShip(deltaX: Float) {
        val newX = (_state.value.playerShipX + deltaX).coerceIn(40f, 680f)
        _state.value = _state.value.copy(playerShipX = newX)
    }

    fun onFireLaser() {
        if (_state.value.isGameOver) {
            initGame()
            return
        }
        val currentLasers = _state.value.lasers.toMutableList()
        currentLasers.add(Laser(_state.value.playerShipX, 780f))
        _state.value = _state.value.copy(lasers = currentLasers)
        RetroAudioSynth.playLaser()
    }

    fun onMoveDungeon(dx: Int, dy: Int) {
        if (_state.value.isGameOver) {
            initGame()
            return
        }
        val current = _state.value
        val newX = (current.dungeonPlayerX + dx).coerceIn(0, 7)
        val newY = (current.dungeonPlayerY + dy).coerceIn(0, 7)

        // Check monster collision
        val monsterAtTarget = current.monsters.find { it.x == newX && it.y == newY }
        if (monsterAtTarget != null) {
            // Combat attack
            val damage = Random.nextInt(4, 9)
            monsterAtTarget.hp -= damage
            RetroAudioSynth.playHit()
            bridge.log(LogType.JNI_HOOK, "Attack on ${monsterAtTarget.name} for $damage dmg", "Native Box2D hit registered")

            val updatedMonsters = current.monsters.filter { it.hp > 0 }
            val goldGained = if (monsterAtTarget.hp <= 0) 15 else 0
            if (goldGained > 0) RetroAudioSynth.playPowerup()

            // Monster counter-attacks
            val retaliation = if (monsterAtTarget.hp > 0) Random.nextInt(1, 5) else 0
            val newHp = (current.dungeonHp - retaliation).coerceAtLeast(0)

            _state.value = current.copy(
                monsters = updatedMonsters,
                dungeonHp = newHp,
                dungeonGold = current.dungeonGold + goldGained,
                score = current.score + goldGained + damage,
                isGameOver = newHp <= 0
            )
        } else {
            _state.value = current.copy(dungeonPlayerX = newX, dungeonPlayerY = newY)
            RetroAudioSynth.playBeep(330f, 40, RetroAudioSynth.WaveType.SQUARE)
        }
    }

    fun onSwipe2048(direction: SwipeDirection) {
        if (_state.value.isGameOver) {
            initGame()
            return
        }
        val grid = _state.value.grid2048.map { it.toMutableList() }.toMutableList()
        var moved = false
        var scoreAdd = 0

        when (direction) {
            SwipeDirection.LEFT -> {
                for (r in 0 until 4) {
                    val (newRow, added) = slideAndMergeRow(grid[r])
                    if (newRow != grid[r]) moved = true
                    grid[r] = newRow.toMutableList()
                    scoreAdd += added
                }
            }
            SwipeDirection.RIGHT -> {
                for (r in 0 until 4) {
                    val reversed = grid[r].reversed()
                    val (newRow, added) = slideAndMergeRow(reversed)
                    val back = newRow.reversed()
                    if (back != grid[r]) moved = true
                    grid[r] = back.toMutableList()
                    scoreAdd += added
                }
            }
            SwipeDirection.UP -> {
                for (c in 0 until 4) {
                    val col = (0 until 4).map { grid[it][c] }
                    val (newCol, added) = slideAndMergeRow(col)
                    if (newCol != col) moved = true
                    for (r in 0 until 4) grid[r][c] = newCol[r]
                    scoreAdd += added
                }
            }
            SwipeDirection.DOWN -> {
                for (c in 0 until 4) {
                    val col = (0 until 4).map { grid[it][c] }.reversed()
                    val (newCol, added) = slideAndMergeRow(col)
                    val back = newCol.reversed()
                    if (back != (0 until 4).map { grid[it][c] }) moved = true
                    for (r in 0 until 4) grid[r][c] = back[r]
                    scoreAdd += added
                }
            }
        }

        if (moved) {
            spawn2048Tile(grid)
            val newScore = _state.value.score + scoreAdd
            val newHigh = maxOf(newScore, _state.value.highScore)
            _state.value = _state.value.copy(
                grid2048 = grid,
                score = newScore,
                highScore = newHigh
            )
            if (scoreAdd > 0) RetroAudioSynth.playScore() else RetroAudioSynth.playJump()
        }
    }

    fun onTouchGeneric(x: Float, y: Float) {
        _state.value = _state.value.copy(touchX = x, touchY = y, score = _state.value.score + 1)
        RetroAudioSynth.playBeep(440f + (x % 300), 50, RetroAudioSynth.WaveType.SINE)
        bridge.log(LogType.JNI_HOOK, "NativeMotionEvent: x=${x.toInt()}, y=${y.toInt()}", "Mapped to ARM32 Viewport")
    }

    private fun updatePhysics() {
        val current = _state.value
        when (gameType) {
            GameType.FLAPPY_32 -> {
                val newVel = current.birdVelocity + 0.65f // gravity
                val newY = (current.birdY + newVel).coerceIn(0f, 900f)

                // Move pipes
                val updatedPipes = mutableListOf<Pipe>()
                var score = current.score
                var hit = newY >= 880f || newY <= 10f

                for (pipe in current.pipes) {
                    val newX = pipe.x - 4f
                    if (newX < 180f && !pipe.passed) {
                        pipe.passed = true
                        score++
                        RetroAudioSynth.playScore()
                        bridge.log(LogType.SYSCALL_TRANSLATE, "Pipe passed: score=$score", "Syscall notify audio")
                    }

                    // Collision check (Bird X is 140f, size 32f)
                    val birdLeft = 140f
                    val birdRight = 172f
                    val birdTop = newY
                    val birdBottom = newY + 32f

                    if (birdRight > newX && birdLeft < newX + 60f) {
                        if (birdTop < pipe.topHeight || birdBottom > pipe.topHeight + pipe.gap) {
                            hit = true
                        }
                    }

                    if (newX > -80f) {
                        updatedPipes.add(pipe.copy(x = newX))
                    }
                }

                // Spawn new pipe if needed
                if (updatedPipes.isEmpty() || updatedPipes.last().x < 450f) {
                    val topHeight = Random.nextFloat() * 320f + 120f
                    updatedPipes.add(Pipe(x = 750f, topHeight = topHeight))
                }

                if (hit && !current.isGameOver) {
                    RetroAudioSynth.playHit()
                    bridge.log(LogType.SANDBOX_EVENT, "Game Over collision detected. Final Score: $score")
                }

                _state.value = current.copy(
                    birdY = newY,
                    birdVelocity = newVel,
                    pipes = updatedPipes,
                    score = score,
                    highScore = maxOf(score, current.highScore),
                    isGameOver = hit,
                    frameCount = current.frameCount + 1
                )
            }
            GameType.SPACE_RAIDER_32 -> {
                // Update stars
                current.stars.forEach {
                    it.y += it.speed
                    if (it.y > 1000f) it.y = 0f
                }

                // Update lasers
                val activeLasers = current.lasers.mapNotNull {
                    it.y -= 14f
                    if (it.y > -20f) it else null
                }.toMutableList()

                // Spawn aliens
                spawnTimer++
                val activeAliens = current.aliens.toMutableList()
                if (spawnTimer % 45 == 0) {
                    activeAliens.add(
                        Alien(
                            x = Random.nextFloat() * 600f + 50f,
                            y = -40f,
                            speed = Random.nextFloat() * 3f + 2f
                        )
                    )
                }

                var score = current.score
                var health = current.playerHealth

                // Update aliens & collisions
                val remainingAliens = mutableListOf<Alien>()
                for (alien in activeAliens) {
                    alien.y += alien.speed

                    // Check hit with lasers
                    var alienHit = false
                    val laserIterator = activeLasers.iterator()
                    while (laserIterator.hasNext()) {
                        val laser = laserIterator.next()
                        if (Math.abs(laser.x - alien.x) < 32f && Math.abs(laser.y - alien.y) < 32f) {
                            alienHit = true
                            laserIterator.remove()
                            break
                        }
                    }

                    if (alienHit) {
                        score += 50
                        RetroAudioSynth.playExplosion()
                        bridge.log(LogType.GL_ES_EMU, "Alien destroyed at (${alien.x.toInt()}, ${alien.y.toInt()})", "Particle FX emitted")
                    } else if (alien.y > 880f) {
                        // Alien passed or hit ship
                        if (Math.abs(alien.x - current.playerShipX) < 50f) {
                            health--
                            RetroAudioSynth.playHit()
                        }
                    } else {
                        remainingAliens.add(alien)
                    }
                }

                val gameOver = health <= 0
                _state.value = current.copy(
                    lasers = activeLasers,
                    aliens = remainingAliens,
                    score = score,
                    playerHealth = health,
                    highScore = maxOf(score, current.highScore),
                    isGameOver = gameOver,
                    frameCount = current.frameCount + 1
                )
            }
            GameType.GENERIC_APK, GameType.POCKET_2048, GameType.PIXEL_DUNGEON_32 -> {
                _state.value = current.copy(frameCount = current.frameCount + 1)
            }
        }
    }

    private fun spawn2048Tile(grid: MutableList<MutableList<Int>>) {
        val emptyCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == 0) emptyCoords.add(Pair(r, c))
            }
        }
        if (emptyCoords.isNotEmpty()) {
            val (r, c) = emptyCoords.random()
            grid[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
        }
    }

    private fun slideAndMergeRow(row: List<Int>): Pair<List<Int>, Int> {
        val nonZeros = row.filter { it != 0 }.toMutableList()
        var addedScore = 0
        val merged = mutableListOf<Int>()
        var i = 0
        while (i < nonZeros.size) {
            if (i + 1 < nonZeros.size && nonZeros[i] == nonZeros[i + 1]) {
                val newVal = nonZeros[i] * 2
                merged.add(newVal)
                addedScore += newVal
                i += 2
            } else {
                merged.add(nonZeros[i])
                i++
            }
        }
        while (merged.size < 4) merged.add(0)
        return Pair(merged, addedScore)
    }

    enum class SwipeDirection {
        LEFT, RIGHT, UP, DOWN
    }
}
