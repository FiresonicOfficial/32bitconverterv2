package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.compat.AbiDetector
import com.example.compat.ApkAnalysisResult
import com.example.compat.ApkInspector
import com.example.compat.SystemAbiInfo
import com.example.compat.engine.GameRunnerEngine
import com.example.compat.engine.GameType
import com.example.compat.sandbox.CompatibilityBridge
import com.example.compat.sandbox.SandboxEnvironment
import com.example.compat.sandbox.TranslationMode
import com.example.compat.sandbox.VirtualContainerConfig
import com.example.data.db.AppDatabase
import com.example.data.db.GameEntity
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportDialogState(
    val isScanning: Boolean = false,
    val analysisResult: ApkAnalysisResult? = null,
    val showDialog: Boolean = false
)

data class ActiveSessionState(
    val activeGame: GameEntity? = null,
    val engine: GameRunnerEngine? = null,
    val bridge: CompatibilityBridge? = null,
    val sandboxEnv: SandboxEnvironment? = null,
    val isRunning: Boolean = false,
    val showSyscallDrawer: Boolean = false,
    val crtShader: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val systemAbiInfo: SystemAbiInfo = AbiDetector.getSystemAbiInfo()

    val games: StateFlow<List<GameEntity>>

    private val _importState = MutableStateFlow(ImportDialogState())
    val importState: StateFlow<ImportDialogState> = _importState.asStateFlow()

    private val _activeSession = MutableStateFlow(ActiveSessionState())
    val activeSession: StateFlow<ActiveSessionState> = _activeSession.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: All, 1: Installed 32-bit APKs, 2: Built-in 32-bit Demos
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _configGame = MutableStateFlow<GameEntity?>(null)
    val configGame: StateFlow<GameEntity?> = _configGame.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GameRepository(db.gameDao())
        games = repository.allGames.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.seedBuiltinDemoGamesIfEmpty()
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun openApkFile(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportDialogState(isScanning = true, showDialog = true)
            val result = withContext(Dispatchers.IO) {
                ApkInspector.inspectAndImportApk(getApplication(), uri)
            }
            _importState.value = ImportDialogState(
                isScanning = false,
                analysisResult = result,
                showDialog = true
            )
        }
    }

    fun dismissImportDialog() {
        _importState.value = ImportDialogState(showDialog = false)
    }

    fun confirmInstallApk() {
        val analysis = _importState.value.analysisResult ?: return
        if (!analysis.isValid) return

        viewModelScope.launch {
            val gameEntity = GameEntity(
                packageName = analysis.packageName,
                title = analysis.appTitle,
                versionName = analysis.versionName,
                targetSdk = analysis.targetSdk,
                abiType = analysis.primaryAbi,
                isPure32Bit = analysis.isPure32Bit,
                apkPath = analysis.apkFilePath,
                iconUri = analysis.iconFilePath,
                nativeLibsCount = analysis.nativeLibraries.size,
                nativeLibsList = analysis.nativeLibraries.joinToString(", "),
                virtualAndroidVersion = if (analysis.targetSdk <= 22) "Android 4.4 (KitKat)" else "Android 9.0 (Pie)",
                isBuiltinDemo = false,
                description = "Imported 32-bit APK. Sandboxed native libraries: ${analysis.nativeLibraries.joinToString()}"
            )
            repository.insertGame(gameEntity)
            _importState.value = ImportDialogState(showDialog = false)
            _snackbarMessage.value = "${analysis.appTitle} installed into 32-bit sandbox!"
        }
    }

    fun launchGame(game: GameEntity) {
        viewModelScope.launch {
            // Stop existing if any
            _activeSession.value.engine?.stop()

            val config = VirtualContainerConfig(
                packageName = game.packageName,
                appTitle = game.title,
                virtualAndroidVersion = game.virtualAndroidVersion,
                memoryLimitMb = game.memoryLimitMb,
                targetAbi = game.abiType
            )

            val sandboxEnv = SandboxEnvironment(getApplication(), config)
            val bridge = CompatibilityBridge(config)

            val gameType = when (game.packageName) {
                "com.retro.flappy32" -> GameType.FLAPPY_32
                "com.arcade.spaceraider32" -> GameType.SPACE_RAIDER_32
                "com.puzzle.pocket2048" -> GameType.POCKET_2048
                "com.retro.pixeldungeon32" -> GameType.PIXEL_DUNGEON_32
                else -> GameType.GENERIC_APK
            }

            val libsList = game.nativeLibsList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val engine = GameRunnerEngine(
                scope = viewModelScope,
                bridge = bridge,
                gameType = gameType,
                gameTitle = game.title,
                initialHighScore = game.highScore,
                nativeLibs = libsList
            )

            _activeSession.value = ActiveSessionState(
                activeGame = game,
                engine = engine,
                bridge = bridge,
                sandboxEnv = sandboxEnv,
                isRunning = true,
                crtShader = false
            )

            engine.start()

            // Update last played
            repository.updatePlaySession(game.id, System.currentTimeMillis(), 1)
        }
    }

    fun exitActiveSession() {
        val session = _activeSession.value
        val game = session.activeGame
        val engine = session.engine

        if (game != null && engine != null) {
            viewModelScope.launch {
                val currentScore = engine.state.value.score
                if (currentScore > game.highScore) {
                    repository.updateHighScore(game.id, currentScore)
                }
            }
        }

        session.engine?.stop()
        _activeSession.value = ActiveSessionState(isRunning = false)
    }

    fun toggleSyscallDrawer() {
        _activeSession.value = _activeSession.value.copy(
            showSyscallDrawer = !_activeSession.value.showSyscallDrawer
        )
    }

    fun toggleCrtShader() {
        _activeSession.value = _activeSession.value.copy(
            crtShader = !_activeSession.value.crtShader
        )
    }

    fun openGameConfig(game: GameEntity) {
        _configGame.value = game
    }

    fun closeGameConfig() {
        _configGame.value = null
    }

    fun updateGameConfig(updated: GameEntity) {
        viewModelScope.launch {
            repository.updateGame(updated)
            _configGame.value = null
            _snackbarMessage.value = "Updated sandbox configuration for ${updated.title}"
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            repository.deleteGame(game)
            _configGame.value = null
            _snackbarMessage.value = "Removed ${game.title} from sandbox"
        }
    }
}
