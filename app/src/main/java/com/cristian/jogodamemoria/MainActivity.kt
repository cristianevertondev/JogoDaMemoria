package com.cristian.jogodamemoria

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristian.jogodamemoria.ui.theme.JogoDaMemoriaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JogoDaMemoriaTheme {
                MemoryGameApp()
            }
        }
    }
}

/* =========================================================
   SONS
   ========================================================= */

class SoundGenerator {

    companion object {

        fun playTone(
            frequency: Float,
            volume: Float,
            durationSeconds: Float,
            durationMs: Long
        ) {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * durationSeconds).toInt()
                val sound = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    val envelope = exp(-t * 3)
                    val tone = sin(2 * PI * frequency * t) * envelope

                    sound[i] = (
                            tone * Short.MAX_VALUE * volume
                            ).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build(),

                    android.media.AudioFormat.Builder()
                        .setEncoding(
                            android.media.AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setSampleRate(sampleRate)
                        .setChannelMask(
                            android.media.AudioFormat.CHANNEL_OUT_MONO
                        )
                        .build(),

                    numSamples * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                audioTrack.write(sound, 0, numSamples)
                audioTrack.play()

                Thread {
                    try {
                        Thread.sleep(durationMs)
                    } catch (_: InterruptedException) {
                    }

                    audioTrack.release()
                }.start()

            } catch (_: Exception) {
            }
        }

        fun playFlipSound() {
            playTone(
                frequency = 600f,
                volume = 0.3f,
                durationSeconds = 0.08f,
                durationMs = 100
            )
        }

        fun playMatchSound() {
            playTone(
                frequency = 880f,
                volume = 0.4f,
                durationSeconds = 0.18f,
                durationMs = 250
            )

            Thread {
                try {
                    Thread.sleep(90)
                } catch (_: InterruptedException) {
                }

                playTone(
                    frequency = 1100f,
                    volume = 0.4f,
                    durationSeconds = 0.18f,
                    durationMs = 250
                )
            }.start()
        }

        fun playErrorSound() {
            playTone(
                frequency = 300f,
                volume = 0.3f,
                durationSeconds = 0.2f,
                durationMs = 200
            )
        }

        fun playLifeLostSound() {
            playTone(
                frequency = 200f,
                volume = 0.3f,
                durationSeconds = 0.3f,
                durationMs = 300
            )
        }

        fun playComboSound(combo: Int) {
            val base = 800f + combo * 90f

            playTone(
                frequency = base,
                volume = 0.35f,
                durationSeconds = 0.12f,
                durationMs = 120
            )
        }

        fun playVictorySound() {

            val notes = floatArrayOf(
                523.25f,
                659.25f,
                783.99f,
                1046.50f
            )

            Thread {
                notes.forEach { note ->

                    playTone(
                        frequency = note,
                        volume = 0.4f,
                        durationSeconds = 0.25f,
                        durationMs = 250
                    )

                    try {
                        Thread.sleep(140)
                    } catch (_: InterruptedException) {
                    }
                }
            }.start()
        }
    }
}

/* =========================================================
   MÚSICA
   ========================================================= */

class MusicGenerator {

    companion object {

        private var musicThread: Thread? = null
        private var isPlaying = false

        fun startBackgroundMusic() {

            if (isPlaying) return

            isPlaying = true

            musicThread = Thread {

                val chords = listOf(
                    listOf(
                        261.63f,
                        329.63f,
                        392.00f
                    ),
                    listOf(
                        220.00f,
                        261.63f,
                        329.63f
                    ),
                    listOf(
                        174.61f,
                        220.00f,
                        261.63f
                    ),
                    listOf(
                        196.00f,
                        246.94f,
                        293.66f
                    )
                )

                while (isPlaying) {

                    try {

                        val chord = chords.random()

                        chord.forEach { note ->

                            SoundGenerator.playTone(
                                frequency = note,
                                volume = 0.07f,
                                durationSeconds = 0.55f,
                                durationMs = 550
                            )

                            Thread.sleep(150)
                        }

                        Thread.sleep(350)

                    } catch (_: Exception) {
                        break
                    }
                }
            }

            musicThread?.start()
        }

        fun stopBackgroundMusic() {

            isPlaying = false
            musicThread?.interrupt()
            musicThread = null
        }
    }
}

/* =========================================================
   IDIOMAS
   ========================================================= */

enum class Language(val code: String) {
    PT("pt"),
    EN("en"),
    ES("es")
}

object AppStrings {

    fun menuTitle(lang: Language) =
        when (lang) {
            Language.PT -> "Jogo da Memória"
            Language.EN -> "Memory Game"
            Language.ES -> "Juego de Memoria"
        }

    fun normal(lang: Language) =
        when (lang) {
            Language.PT -> "Normal"
            Language.EN -> "Normal"
            Language.ES -> "Normal"
        }

    fun hard(lang: Language) =
        when (lang) {
            Language.PT -> "Difícil"
            Language.EN -> "Hard"
            Language.ES -> "Difícil"
        }

    fun zen(lang: Language) =
        when (lang) {
            Language.PT -> "Zen"
            Language.EN -> "Zen"
            Language.ES -> "Zen"
        }

    fun settings(lang: Language) =
        when (lang) {
            Language.PT -> "Configurações"
            Language.EN -> "Settings"
            Language.ES -> "Configuración"
        }

    fun stats(lang: Language) =
        when (lang) {
            Language.PT -> "Estatísticas"
            Language.EN -> "Statistics"
            Language.ES -> "Estadísticas"
        }

    fun achievements(lang: Language) =
        when (lang) {
            Language.PT -> "Conquistas"
            Language.EN -> "Achievements"
            Language.ES -> "Logros"
        }

    fun music(lang: Language) =
        when (lang) {
            Language.PT -> "Música de fundo"
            Language.EN -> "Background music"
            Language.ES -> "Música de fondo"
        }

    fun soundEffects(lang: Language) =
        when (lang) {
            Language.PT -> "Efeitos sonoros"
            Language.EN -> "Sound effects"
            Language.ES -> "Efectos de sonido"
        }

    fun theme(lang: Language) =
        when (lang) {
            Language.PT -> "Tema do pano"
            Language.EN -> "Cloth theme"
            Language.ES -> "Tema del tapete"
        }

    fun red(lang: Language) =
        when (lang) {
            Language.PT -> "Vermelho"
            Language.EN -> "Red"
            Language.ES -> "Rojo"
        }

    fun green(lang: Language) =
        when (lang) {
            Language.PT -> "Verde"
            Language.EN -> "Green"
            Language.ES -> "Verde"
        }

    fun blue(lang: Language) =
        when (lang) {
            Language.PT -> "Azul"
            Language.EN -> "Blue"
            Language.ES -> "Azul"
        }

    fun purple(lang: Language) =
        when (lang) {
            Language.PT -> "Roxo"
            Language.EN -> "Purple"
            Language.ES -> "Morado"
        }

    fun language(lang: Language) =
        when (lang) {
            Language.PT -> "Idioma"
            Language.EN -> "Language"
            Language.ES -> "Idioma"
        }

    fun back(lang: Language) =
        when (lang) {
            Language.PT -> "Voltar"
            Language.EN -> "Back"
            Language.ES -> "Volver"
        }

    fun level(lang: Language) =
        when (lang) {
            Language.PT -> "Nível"
            Language.EN -> "Level"
            Language.ES -> "Nivel"
        }

    fun nextLevel(lang: Language) =
        when (lang) {
            Language.PT -> "Próximo Nível"
            Language.EN -> "Next Level"
            Language.ES -> "Siguiente Nivel"
        }

    fun totalStars(lang: Language) =
        when (lang) {
            Language.PT -> "Estrelas totais"
            Language.EN -> "Total stars"
            Language.ES -> "Estrellas totales"
        }

    fun levelsCompleted(lang: Language) =
        when (lang) {
            Language.PT -> "Níveis completados"
            Language.EN -> "Levels completed"
            Language.ES -> "Niveles completados"
        }

    fun records(lang: Language) =
        when (lang) {
            Language.PT -> "Recordes por nível"
            Language.EN -> "Records per level"
            Language.ES -> "Récords por nivel"
        }

    fun noRecords(lang: Language) =
        when (lang) {
            Language.PT -> "Nenhum recorde ainda"
            Language.EN -> "No records yet"
            Language.ES -> "Aún no hay récords"
        }

    fun firstPair(lang: Language) =
        when (lang) {
            Language.PT -> "Primeiro Par"
            Language.EN -> "First Pair"
            Language.ES -> "Primer Par"
        }

    fun tenPairs(lang: Language) =
        when (lang) {
            Language.PT -> "10 Pares"
            Language.EN -> "10 Pairs"
            Language.ES -> "10 Pares"
        }

    fun level5(lang: Language) =
        when (lang) {
            Language.PT -> "Alcance o nível 5"
            Language.EN -> "Reach level 5"
            Language.ES -> "Alcanza el nivel 5"
        }

    fun level10(lang: Language) =
        when (lang) {
            Language.PT -> "Alcance o nível 10"
            Language.EN -> "Reach level 10"
            Language.ES -> "Alcanza el nivel 10"
        }

    fun threeStars(lang: Language) =
        when (lang) {
            Language.PT -> "Ganhe 3 estrelas em um nível"
            Language.EN -> "Earn 3 stars in a level"
            Language.ES -> "Gana 3 estrellas en un nivel"
        }

    fun gameOver(lang: Language) =
        when (lang) {
            Language.PT -> "Fim de Jogo"
            Language.EN -> "Game Over"
            Language.ES -> "Fin del Juego"
        }

    fun tryAgain(lang: Language) =
        when (lang) {
            Language.PT -> "Tentar Novamente"
            Language.EN -> "Try Again"
            Language.ES -> "Intentar de nuevo"
        }

    fun menu(lang: Language) =
        when (lang) {
            Language.PT -> "Menu"
            Language.EN -> "Menu"
            Language.ES -> "Menú"
        }

    fun combo(lang: Language) =
        when (lang) {
            Language.PT -> "Combo"
            Language.EN -> "Combo"
            Language.ES -> "Combo"
        }

    fun tutorial(lang: Language) =
        when (lang) {
            Language.PT -> "Encontre todos os pares!"
            Language.EN -> "Find all the pairs!"
            Language.ES -> "¡Encuentra todas las parejas!"
        }

    fun gotIt(lang: Language) =
        when (lang) {
            Language.PT -> "Entendi"
            Language.EN -> "Got it"
            Language.ES -> "Entendido"
        }

    fun time(lang: Language) =
        when (lang) {
            Language.PT -> "Tempo"
            Language.EN -> "Time"
            Language.ES -> "Tiempo"
        }

    fun hint(lang: Language) =
        when (lang) {
            Language.PT -> "Dica"
            Language.EN -> "Hint"
            Language.ES -> "Pista"
        }
}

/* =========================================================
   MODELOS
   ========================================================= */

data class MemoryCard(
    val id: Int,
    val emoji: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

enum class GameMode {
    NORMAL,
    HARD,
    ZEN
}

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val alpha: Float
)

/* =========================================================
   APP
   ========================================================= */

@Composable
fun MemoryGameApp() {

    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf("menu")
    }

    var gameMode by remember {
        mutableStateOf(GameMode.NORMAL)
    }

    var selectedTheme by remember {
        mutableStateOf(loadTheme(context))
    }

    var musicEnabled by remember {
        mutableStateOf(loadMusicEnabled(context))
    }

    var soundEnabled by remember {
        mutableStateOf(loadSoundEnabled(context))
    }

    var language by remember {
        mutableStateOf(loadLanguage(context))
    }

    LaunchedEffect(musicEnabled) {

        if (musicEnabled) {
            MusicGenerator.startBackgroundMusic()
        } else {
            MusicGenerator.stopBackgroundMusic()
        }
    }

    when (currentScreen) {

        "menu" -> {

            MenuScreen(
                language = language,

                onLanguageChange = {
                    language = it
                    saveLanguage(context, it)
                },

                onPlay = {
                    gameMode = it
                    currentScreen = "game"
                },

                onSettings = {
                    currentScreen = "settings"
                },

                onStats = {
                    currentScreen = "stats"
                },

                onAchievements = {
                    currentScreen = "achievements"
                }
            )
        }

        "game" -> {

            GameScreen(
                gameMode = gameMode,
                theme = selectedTheme,
                soundEnabled = soundEnabled,
                language = language,

                onBack = {
                    currentScreen = "menu"
                }
            )
        }

        "settings" -> {

            SettingsScreen(
                musicEnabled = musicEnabled,
                soundEnabled = soundEnabled,
                theme = selectedTheme,
                language = language,

                onMusicChange = {
                    musicEnabled = it
                    saveMusicEnabled(context, it)
                },

                onSoundChange = {
                    soundEnabled = it
                    saveSoundEnabled(context, it)
                },

                onThemeChange = {
                    selectedTheme = it
                    saveTheme(context, it)
                },

                onLanguageChange = {
                    language = it
                    saveLanguage(context, it)
                },

                onBack = {
                    currentScreen = "menu"
                }
            )
        }

        "stats" -> {

            StatsScreen(
                language = language,

                onBack = {
                    currentScreen = "menu"
                }
            )
        }

        "achievements" -> {

            AchievementsScreen(
                language = language,

                onBack = {
                    currentScreen = "menu"
                }
            )
        }
    }
}

/* =========================================================
   MENU
   ========================================================= */

@Composable
fun MenuScreen(
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onPlay: (GameMode) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onAchievements: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition()

    val floatingY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,

        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        2800,
                        easing = FastOutSlowInEasing
                    ),

                repeatMode =
                    RepeatMode.Reverse
            )
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF1A0F08),
                                Color(0xFF3B2113),
                                Color(0xFF1E100A)
                            )
                    )
                )
    ) {

        FloatingCardsBackground()

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 20.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "🃏",

                fontSize = 54.sp,

                modifier =
                    Modifier
                        .offset(
                            y = floatingY.dp
                        )
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    AppStrings.menuTitle(language),

                fontSize = 38.sp,

                fontWeight =
                    FontWeight.Black,

                color =
                    Color.White,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Box(
                modifier =
                    Modifier
                        .width(90.dp)
                        .height(4.dp)
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFC107),
                                    Color(0xFFFF8F00)
                                )
                            )
                        )
            )

            Spacer(
                Modifier.height(24.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                LanguageButton(
                    flag = "🇧🇷",
                    language = Language.PT,
                    selected = language,
                    onClick = onLanguageChange
                )

                LanguageButton(
                    flag = "🇺🇸",
                    language = Language.EN,
                    selected = language,
                    onClick = onLanguageChange
                )

                LanguageButton(
                    flag = "🇪🇸",
                    language = Language.ES,
                    selected = language,
                    onClick = onLanguageChange
                )
            }

            Spacer(
                Modifier.height(24.dp)
            )

            GameModeButton(
                emoji = "😊",
                text =
                    AppStrings.normal(language),

                subtitle = "6–15 pares",

                colors =
                    listOf(
                        Color(0xFFE65100),
                        Color(0xFFFF9800)
                    ),

                onClick = {
                    onPlay(GameMode.NORMAL)
                }
            )

            Spacer(
                Modifier.height(12.dp)
            )

            GameModeButton(
                emoji = "🔥",
                text =
                    AppStrings.hard(language),

                subtitle = "60 segundos",

                colors =
                    listOf(
                        Color(0xFF9B1C1C),
                        Color(0xFFE53935)
                    ),

                onClick = {
                    onPlay(GameMode.HARD)
                }
            )

            Spacer(
                Modifier.height(12.dp)
            )

            GameModeButton(
                emoji = "🧘",
                text =
                    AppStrings.zen(language),

                subtitle = "Sem cronômetro",

                colors =
                    listOf(
                        Color(0xFF216B26),
                        Color(0xFF43A047)
                    ),

                onClick = {
                    onPlay(GameMode.ZEN)
                }
            )

            Spacer(
                Modifier.height(24.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                MenuIconButton(
                    icon = "⚙️",
                    onClick = onSettings
                )

                MenuIconButton(
                    icon = "📊",
                    onClick = onStats
                )

                MenuIconButton(
                    icon = "🏆",
                    onClick = onAchievements
                )
            }
        }
    }
}

@Composable
fun GameModeButton(
    emoji: String,
    text: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier =
            Modifier
                .fillMaxWidth(0.88f)
                .height(76.dp)
                .shadow(
                    elevation = 12.dp,
                    shape =
                        RoundedCornerShape(
                            24.dp
                        )
                ),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        contentPadding =
            PaddingValues(0.dp),

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    Color.Transparent
            )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color.White.copy(
                                alpha = 0.18f
                            ),
                        shape =
                            RoundedCornerShape(
                                24.dp
                            )
                    )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 22.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = emoji,
                    fontSize = 34.sp
                )

                Spacer(
                    Modifier.width(16.dp)
                )

                Column {

                    Text(
                        text = text,

                        color =
                            Color.White,

                        fontSize = 23.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text = subtitle,

                        color =
                            Color.White.copy(
                                alpha = 0.75f
                            ),

                        fontSize = 13.sp
                    )
                }

                Spacer(
                    Modifier.weight(1f)
                )

                Text(
                    text = "›",

                    color =
                        Color.White.copy(
                            alpha = 0.8f
                        ),

                    fontSize = 34.sp
                )
            }
        }
    }
}

@Composable
fun MenuIconButton(
    icon: String,
    onClick: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .size(56.dp)
                .shadow(
                    8.dp,
                    CircleShape
                )
                .clickable(
                    onClick = onClick
                ),

        shape = CircleShape,

        color =
            Color.White.copy(
                alpha = 0.10f
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(
                    alpha = 0.12f
                )
            )
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                icon,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun FloatingCardsBackground() {

    val cards =
        listOf(
            "🂡",
            "🂱",
            "🃁",
            "🃑",
            "🂶",
            "🃆",
            "🂪",
            "🃊"
        )

    cards.forEachIndexed { index, emoji ->

        val infiniteTransition =
            rememberInfiniteTransition()

        val x by infiniteTransition.animateFloat(
            initialValue =
                Random.nextInt(
                    -30,
                    250
                ).toFloat(),

            targetValue =
                Random.nextInt(
                    80,
                    320
                ).toFloat(),

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            5000 + index * 400,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Reverse
                )
        )

        val y by infiniteTransition.animateFloat(
            initialValue =
                Random.nextInt(
                    -50,
                    700
                ).toFloat(),

            targetValue =
                Random.nextInt(
                    40,
                    760
                ).toFloat(),

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            6200 + index * 300,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Reverse
                )
        )

        val rotation by infiniteTransition.animateFloat(
            initialValue = -25f,
            targetValue = 25f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            3000 + index * 250,
                            easing =
                                FastOutSlowInEasing
                        ),

                    repeatMode =
                        RepeatMode.Reverse
                )
        )

        Text(
            text = emoji,

            fontSize = 42.sp,

            color =
                Color.White.copy(
                    alpha = 0.08f
                ),

            modifier =
                Modifier
                    .offset(
                        x = x.dp,
                        y = y.dp
                    )
                    .rotate(rotation)
        )
    }
}

/* =========================================================
   CONFIGURAÇÕES
   ========================================================= */

@Composable
fun SettingsScreen(
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    theme: String,
    language: Language,
    onMusicChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onBack: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E100A),
                            Color(0xFF3B2113)
                        )
                    )
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "⚙️ ${AppStrings.settings(language)}",

                color =
                    Color.White,

                fontSize = 30.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(24.dp)
            )

            SettingsRow(
                title =
                    AppStrings.music(language),

                checked =
                    musicEnabled,

                onCheckedChange =
                    onMusicChange
            )

            Spacer(
                Modifier.height(10.dp)
            )

            SettingsRow(
                title =
                    AppStrings.soundEffects(language),

                checked =
                    soundEnabled,

                onCheckedChange =
                    onSoundChange
            )

            Spacer(
                Modifier.height(24.dp)
            )

            Text(
                text =
                    AppStrings.theme(language),

                color =
                    Color.White,

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                ThemeButton(
                    AppStrings.red(language),
                    "red",
                    theme,
                    onThemeChange
                )

                ThemeButton(
                    AppStrings.green(language),
                    "green",
                    theme,
                    onThemeChange
                )

                ThemeButton(
                    AppStrings.blue(language),
                    "blue",
                    theme,
                    onThemeChange
                )

                ThemeButton(
                    AppStrings.purple(language),
                    "purple",
                    theme,
                    onThemeChange
                )
            }

            Spacer(
                Modifier.height(24.dp)
            )

            Text(
                text =
                    AppStrings.language(language),

                color =
                    Color.White,

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                LanguageButton(
                    "🇧🇷",
                    Language.PT,
                    language,
                    onLanguageChange
                )

                LanguageButton(
                    "🇺🇸",
                    Language.EN,
                    language,
                    onLanguageChange
                )

                LanguageButton(
                    "🇪🇸",
                    Language.ES,
                    language,
                    onLanguageChange
                )
            }

            Spacer(
                Modifier.height(30.dp)
            )

            Button(
                onClick = onBack
            ) {

                Text(
                    AppStrings.back(
                        language
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            Color.White.copy(
                alpha = 0.07f
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = title,

                color =
                    Color.White,

                fontSize = 17.sp
            )

            Switch(
                checked = checked,
                onCheckedChange =
                    onCheckedChange
            )
        }
    }
}

@Composable
fun ThemeButton(
    name: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit
) {

    val selectedNow =
        selected == value

    Button(
        onClick = {
            onSelect(value)
        },

        modifier =
            Modifier
                .border(
                    width =
                        if (selectedNow) {
                            2.dp
                        } else {
                            0.dp
                        },

                    color =
                        Color.White,

                    shape =
                        CircleShape
                ),

        shape = CircleShape,

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    when (value) {
                        "red" ->
                            Color(0xFFB22222)

                        "green" ->
                            Color(0xFF2E7D32)

                        "blue" ->
                            Color(0xFF1565C0)

                        else ->
                            Color(0xFF6A1B9A)
                    }
            )
    ) {

        Text(
            name,
            fontSize = 12.sp
        )
    }
}

@Composable
fun LanguageButton(
    flag: String,
    language: Language,
    selected: Language,
    onClick: (Language) -> Unit
) {

    val selectedNow =
        language == selected

    Surface(
        modifier =
            Modifier
                .size(52.dp)
                .clickable {
                    onClick(language)
                },

        shape =
            CircleShape,

        color =
            if (selectedNow) {
                Color(0xFFFFC107)
            } else {
                Color.White.copy(
                    alpha = 0.08f
                )
            },

        border =
            androidx.compose.foundation.BorderStroke(
                width =
                    if (selectedNow) {
                        2.dp
                    } else {
                        1.dp
                    },

                color =
                    if (selectedNow) {
                        Color.White
                    } else {
                        Color.White.copy(
                            alpha = 0.12f
                        )
                    }
            )
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                flag,
                fontSize = 23.sp
            )
        }
    }
}

/* =========================================================
   ESTATÍSTICAS
   ========================================================= */

@Composable
fun StatsScreen(
    language: Language,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val bestMoves =
        loadBestMoves(context)

    val totalStars =
        loadTotalStars(context)

    val completed =
        loadLevelsCompleted(context)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E100A),
                            Color(0xFF3B2113)
                        )
                    )
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "📊 ${AppStrings.stats(language)}",

                color =
                    Color.White,

                fontSize = 30.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(24.dp)
            )

            StatCard(
                title =
                    AppStrings.levelsCompleted(
                        language
                    ),

                value =
                    completed.toString(),

                icon = "🏆"
            )

            Spacer(
                Modifier.height(12.dp)
            )

            StatCard(
                title =
                    AppStrings.totalStars(
                        language
                    ),

                value =
                    totalStars.toString(),

                icon = "⭐"
            )

            Spacer(
                Modifier.height(24.dp)
            )

            Text(
                text =
                    AppStrings.records(language),

                color =
                    Color.White,

                fontWeight =
                    FontWeight.Bold,

                fontSize = 19.sp
            )

            Spacer(
                Modifier.height(12.dp)
            )

            if (bestMoves.isEmpty()) {

                Text(
                    text =
                        AppStrings.noRecords(
                            language
                        ),

                    color =
                        Color.White.copy(
                            alpha = 0.6f
                        )
                )

            } else {

                bestMoves.forEach { (level, moves) ->

                    Text(
                        text =
                            "${AppStrings.level(language)} $level: $moves movimentos",

                        color =
                            Color.White.copy(
                                alpha = 0.85f
                            ),

                        fontSize = 16.sp,

                        modifier =
                            Modifier.padding(
                                vertical = 2.dp
                            )
                    )
                }
            }

            Spacer(
                Modifier.height(30.dp)
            )

            Button(
                onClick = onBack
            ) {

                Text(
                    AppStrings.back(
                        language
                    )
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            Color.White.copy(
                alpha = 0.08f
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                icon,
                fontSize = 34.sp
            )

            Spacer(
                Modifier.width(16.dp)
            )

            Column {

                Text(
                    title,

                    color =
                        Color.White.copy(
                            alpha = 0.75f
                        ),

                    fontSize = 14.sp
                )

                Text(
                    value,

                    color =
                        Color.White,

                    fontSize = 27.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* =========================================================
   CONQUISTAS
   ========================================================= */

@Composable
fun AchievementsScreen(
    language: Language,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val unlocked =
        loadAchievements(context)

    val achievements =
        listOf(
            "first_pair" to
                    AppStrings.firstPair(language),

            "ten_pairs" to
                    AppStrings.tenPairs(language),

            "level_5" to
                    AppStrings.level5(language),

            "level_10" to
                    AppStrings.level10(language),

            "three_stars" to
                    AppStrings.threeStars(language)
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E100A),
                            Color(0xFF3B2113)
                        )
                    )
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "🏆 ${AppStrings.achievements(language)}",

                color =
                    Color.White,

                fontSize = 30.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(22.dp)
            )

            achievements.forEach { (key, title) ->

                AchievementCard(
                    title = title,
                    unlocked =
                        unlocked.contains(key)
                )

                Spacer(
                    Modifier.height(10.dp)
                )
            }

            Spacer(
                Modifier.height(20.dp)
            )

            Button(
                onClick = onBack
            ) {

                Text(
                    AppStrings.back(
                        language
                    )
                )
            }
        }
    }
}

@Composable
fun AchievementCard(
    title: String,
    unlocked: Boolean
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            if (unlocked) {
                Color(0xFFFFB300)
                    .copy(
                        alpha = 0.12f
                    )
            } else {
                Color.White.copy(
                    alpha = 0.05f
                )
            },

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,

                if (unlocked) {
                    Color(0xFFFFD54F)
                } else {
                    Color.White.copy(
                        alpha = 0.08f
                    )
                }
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    if (unlocked) {
                        "🏆"
                    } else {
                        "🔒"
                    },

                fontSize = 26.sp
            )

            Spacer(
                Modifier.width(14.dp)
            )

            Text(
                title,

                color =
                    if (unlocked) {
                        Color.White
                    } else {
                        Color.Gray
                    },

                fontSize = 16.sp
            )
        }
    }
}

/* =========================================================
   JOGO
   ========================================================= */

@Composable
fun GameScreen(
    gameMode: GameMode,
    theme: String,
    soundEnabled: Boolean,
    language: Language,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    var level by remember {
        mutableStateOf(
            loadLevel(context)
        )
    }

    var cards by remember {
        mutableStateOf(
            createCardsForLevel(level)
        )
    }

    var firstSelectedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    var secondSelectedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    var moves by remember {
        mutableStateOf(0)
    }

    var matches by remember {
        mutableStateOf(0)
    }

    var isLevelComplete by remember {
        mutableStateOf(false)
    }

    var isLocked by remember {
        mutableStateOf(false)
    }

    var stars by remember {
        mutableStateOf(0)
    }

    var timeLeft by remember {
        mutableStateOf(
            if (gameMode == GameMode.HARD) {
                60
            } else {
                0
            }
        )
    }

    /* =====================================================
       VIDAS
       ===================================================== */

    var lives by remember {
        mutableStateOf(5f)
    }

    var combo by remember {
        mutableStateOf(0)
    }

    var gameOver by remember {
        mutableStateOf(false)
    }

    var showTutorial by remember {
        mutableStateOf(
            loadLevel(context) == 1 &&
                    !loadTutorialSeen(context)
        )
    }

    var particles by remember {
        mutableStateOf(
            emptyList<Particle>()
        )
    }

    var hintActive by remember {
        mutableStateOf(false)
    }

    var lifeLostPulse by remember {
        mutableStateOf(0)
    }

    val scope =
        rememberCoroutineScope()

    /* =====================================================
       TIMER
       ===================================================== */

    LaunchedEffect(
        gameMode,
        level,
        isLevelComplete,
        gameOver
    ) {

        if (
            gameMode == GameMode.HARD &&
            !isLevelComplete &&
            !gameOver
        ) {

            while (
                timeLeft > 0 &&
                !isLevelComplete &&
                !gameOver
            ) {

                delay(1000)

                if (
                    !isLevelComplete &&
                    !gameOver
                ) {
                    timeLeft--
                }
            }

            if (
                timeLeft == 0 &&
                !isLevelComplete &&
                !gameOver
            ) {

                /* TEMPO = -0.25 VIDA */

                lives =
                    (lives - 0.25f)
                        .coerceAtLeast(0f)

                lifeLostPulse++

                if (soundEnabled) {
                    SoundGenerator.playLifeLostSound()
                }

                if (lives <= 0f) {

                    gameOver = true

                    saveLevel(
                        context,
                        1
                    )

                } else {

                    cards =
                        createCardsForLevel(level)

                    firstSelectedIndex = null
                    secondSelectedIndex = null

                    moves = 0
                    matches = 0

                    isLocked = false

                    timeLeft = 60
                }
            }
        }
    }

    /* =====================================================
       LEVEL COMPLETO
       ===================================================== */

    LaunchedEffect(matches) {

        val totalPairs =
            cards.size / 2

        if (
            matches == totalPairs &&
            totalPairs > 0 &&
            !isLevelComplete
        ) {

            isLevelComplete = true

            val idealMoves =
                totalPairs * 1.5f

            val goodMoves =
                totalPairs * 2.5f

            stars =
                when {
                    moves <= idealMoves -> 3
                    moves <= goodMoves -> 2
                    else -> 1
                }

            saveStarsAndRecord(
                context,
                level,
                stars,
                moves
            )

            unlockAchievement(
                context,
                "first_pair"
            )

            if (matches >= 10) {
                unlockAchievement(
                    context,
                    "ten_pairs"
                )
            }

            if (level >= 5) {
                unlockAchievement(
                    context,
                    "level_5"
                )
            }

            if (level >= 10) {
                unlockAchievement(
                    context,
                    "level_10"
                )
            }

            if (stars == 3) {
                unlockAchievement(
                    context,
                    "three_stars"
                )
            }

            if (soundEnabled) {
                SoundGenerator.playVictorySound()
            }
        }
    }

    /* =====================================================
       RESET DO NÍVEL
       ===================================================== */

    LaunchedEffect(level) {

        lives = 5f
        combo = 0

        particles =
            emptyList()

        hintActive = false
        gameOver = false
        isLocked = false
    }

    /* =====================================================
       PARTÍCULAS
       ===================================================== */

    LaunchedEffect(particles) {

        if (particles.isNotEmpty()) {

            delay(120)

            particles =
                emptyList()
        }
    }

    /* =====================================================
       PULSO DOS CORAÇÕES
       ===================================================== */

    val heartScale by animateFloatAsState(

        targetValue =
            if (lifeLostPulse > 0) {
                1.15f
            } else {
                1f
            },

        animationSpec =
            spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy,

                stiffness =
                    Spring.StiffnessMedium
            ),

        label = "heartScale"
    )

    LaunchedEffect(lifeLostPulse) {

        if (lifeLostPulse > 0) {

            delay(280)

            lifeLostPulse = 0
        }
    }

    /* =====================================================
       FUNDO
       ===================================================== */

    val panoColor =
        getThemeColor(theme)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF20120C),
                            Color(0xFF3A2114)
                        )
                    )
                )
    ) {

        WoodenTable()

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 6.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /* =================================================
               HUD
               ================================================= */

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        20.dp
                    ),

                color =
                    Color.Black.copy(
                        alpha = 0.28f
                    ),

                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(
                            alpha = 0.08f
                        )
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 7.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onBack
                    ) {

                        Text(
                            "🏠",
                            fontSize = 23.sp
                        )
                    }

                    Column(
                        modifier =
                            Modifier.width(
                                72.dp
                            )
                    ) {

                        Text(
                            AppStrings.level(
                                language
                            ),

                            color =
                                Color.White.copy(
                                    alpha = 0.6f
                                ),

                            fontSize = 11.sp
                        )

                        Text(
                            level.toString(),

                            color =
                                Color.White,

                            fontSize = 20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        Modifier.weight(1f)
                    )

                    HeartDisplay(
                        lives = lives,

                        modifier =
                            Modifier.scale(
                                heartScale
                            )
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    IconButton(

                        onClick = {

                            if (
                                !hintActive &&
                                !isLevelComplete &&
                                !gameOver &&
                                lives >= 0.5f
                            ) {

                                /* DICA = -0.5 VIDA */

                                lives =
                                    (
                                            lives - 0.5f
                                            ).coerceAtLeast(
                                            0f
                                        )

                                lifeLostPulse++

                                hintActive =
                                    true

                                if (soundEnabled) {
                                    SoundGenerator
                                        .playErrorSound()
                                }

                                val unmatchedIndices =
                                    cards.indices.filter {
                                        !cards[it].isMatched &&
                                                !cards[it].isFlipped
                                    }

                                if (
                                    unmatchedIndices.size >= 2
                                ) {

                                    val emoji =
                                        cards[
                                            unmatchedIndices.random()
                                        ].emoji

                                    val pairIndices =
                                        unmatchedIndices.filter {
                                            cards[it].emoji ==
                                                    emoji
                                        }

                                    if (
                                        pairIndices.size >= 2
                                    ) {

                                        val selected =
                                            pairIndices.take(
                                                2
                                            )

                                        cards =
                                            cards.mapIndexed {
                                                    i,
                                                    card ->

                                                if (
                                                    i in selected
                                                ) {

                                                    card.copy(
                                                        isFlipped = true
                                                    )

                                                } else {
                                                    card
                                                }
                                            }

                                        scope.launch {

                                            delay(1100)

                                            cards =
                                                cards.mapIndexed {
                                                        i,
                                                        card ->

                                                    if (
                                                        i in selected &&
                                                        !card.isMatched
                                                    ) {

                                                        card.copy(
                                                            isFlipped = false
                                                        )

                                                    } else {
                                                        card
                                                    }
                                                }

                                            hintActive =
                                                false
                                        }

                                    } else {

                                        hintActive =
                                            false
                                    }

                                } else {

                                    hintActive =
                                        false
                                }

                                if (lives <= 0f) {

                                    gameOver =
                                        true

                                    hintActive =
                                        false

                                    saveLevel(
                                        context,
                                        1
                                    )
                                }
                            }
                        },

                        enabled =
                            !hintActive &&
                                    !isLevelComplete &&
                                    !gameOver &&
                                    lives >= 0.5f
                    ) {

                        Text(
                            "💡",
                            fontSize = 21.sp
                        )
                    }

                    if (combo >= 2) {

                        Surface(
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),

                            color =
                                Color(0xFFFFC107)
                                    .copy(
                                        alpha = 0.16f
                                    )
                        ) {

                            Text(
                                text =
                                    "${AppStrings.combo(language)} x$combo",

                                color =
                                    Color(0xFFFFD54F),

                                fontSize = 13.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                modifier =
                                    Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                            )
                        }
                    }

                    Spacer(
                        Modifier.width(7.dp)
                    )

                    Text(
                        text = "⭐ $stars",

                        color =
                            Color.White,

                        fontSize = 16.sp
                    )
                }
            }

            /* =================================================
               TIMER HARD
               ================================================= */

            AnimatedVisibility(
                visible =
                    gameMode == GameMode.HARD,

                enter =
                    fadeIn() +
                            scaleIn(),

                exit =
                    fadeOut() +
                            scaleOut()
            ) {

                val timerColor by animateColorAsState(

                    targetValue =
                        when {
                            timeLeft <= 10 ->
                                Color(0xFFFF5252)

                            timeLeft <= 20 ->
                                Color(0xFFFFB300)

                            else ->
                                Color(0xFF81C784)
                        },

                    animationSpec =
                        tween(300),

                    label = "timerColor"
                )

                Surface(
                    modifier =
                        Modifier
                            .padding(
                                top = 8.dp
                            )
                            .fillMaxWidth(
                                0.55f
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        timerColor.copy(
                            alpha = 0.14f
                        ),

                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            timerColor.copy(
                                alpha = 0.28f
                            )
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            ),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            "⏱️",
                            fontSize = 18.sp
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text(
                            "${AppStrings.time(language)}  $timeLeft s",

                            color =
                                timerColor,

                            fontSize = 16.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(8.dp)
            )

            /* =================================================
               TABULEIRO
               ================================================= */

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth(
                            0.96f
                        )
                        .weight(1f),

                shape =
                    RoundedCornerShape(
                        26.dp
                    ),

                color =
                    panoColor.copy(
                        alpha = 0.84f
                    ),

                border =
                    androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Color(0xFFFFD54F)
                            .copy(
                                alpha = 0.72f
                            )
                    ),

                shadowElevation = 12.dp
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                7.dp
                            )
                ) {

                    val columns =
                        getGridColumns(
                            level
                        )

                    val rows =
                        getGridRows(
                            level
                        )

                    val cardWidth =
                        getCardWidth(
                            level
                        )

                    val cardHeight =
                        getCardHeight(
                            level
                        )

                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(
                                    rememberScrollState()
                                ),

                        verticalArrangement =
                            Arrangement.Center,

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        for (row in 0 until rows) {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 2.dp
                                        ),

                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                for (
                                col in 0 until columns
                                ) {

                                    val index =
                                        row *
                                                columns +
                                                col

                                    if (
                                        index < cards.size
                                    ) {

                                        Box(
                                            modifier =
                                                Modifier.padding(
                                                    horizontal = 2.dp,
                                                    vertical = 2.dp
                                                )
                                        ) {

                                            PlayingCardView(
                                                card =
                                                    cards[index],

                                                cardWidth =
                                                    cardWidth,

                                                cardHeight =
                                                    cardHeight,

                                                onClick = {

                                                    if (
                                                        !isLocked &&
                                                        !cards[index].isFlipped &&
                                                        !cards[index].isMatched &&
                                                        !isLevelComplete &&
                                                        !gameOver &&
                                                        !hintActive
                                                    ) {

                                                        cards =
                                                            cards.mapIndexed {
                                                                    i,
                                                                    card ->

                                                                if (
                                                                    i == index
                                                                ) {

                                                                    card.copy(
                                                                        isFlipped =
                                                                            true
                                                                    )

                                                                } else {
                                                                    card
                                                                }
                                                            }

                                                        if (
                                                            soundEnabled
                                                        ) {

                                                            SoundGenerator
                                                                .playFlipSound()
                                                        }

                                                        if (
                                                            firstSelectedIndex ==
                                                            null
                                                        ) {

                                                            firstSelectedIndex =
                                                                index

                                                        } else {

                                                            secondSelectedIndex =
                                                                index

                                                            moves++

                                                            val firstIndex =
                                                                firstSelectedIndex!!

                                                            val isMatch =
                                                                cards[firstIndex].emoji ==
                                                                        cards[index].emoji

                                                            if (isMatch) {

                                                                cards =
                                                                    cards.mapIndexed {
                                                                            i,
                                                                            card ->

                                                                        if (
                                                                            i ==
                                                                            firstIndex ||
                                                                            i ==
                                                                            index
                                                                        ) {

                                                                            card.copy(
                                                                                isMatched =
                                                                                    true
                                                                            )

                                                                        } else {
                                                                            card
                                                                        }
                                                                    }

                                                                matches++

                                                                combo++

                                                                firstSelectedIndex =
                                                                    null

                                                                secondSelectedIndex =
                                                                    null

                                                                if (
                                                                    soundEnabled
                                                                ) {

                                                                    SoundGenerator
                                                                        .playMatchSound()

                                                                    if (
                                                                        combo >= 2
                                                                    ) {

                                                                        SoundGenerator
                                                                            .playComboSound(
                                                                                combo
                                                                            )
                                                                    }
                                                                }

                                                                val cardPos =
                                                                    getCardCenter(
                                                                        row,
                                                                        col,
                                                                        cardWidth,
                                                                        cardHeight
                                                                    )

                                                                particles =
                                                                    List(
                                                                        10
                                                                    ) { particleIndex ->

                                                                        Particle(
                                                                            id =
                                                                                particleIndex,

                                                                            x =
                                                                                cardPos.first,

                                                                            y =
                                                                                cardPos.second,

                                                                            velocityX =
                                                                                Random.nextFloat() *
                                                                                        5f -
                                                                                        2.5f,

                                                                            velocityY =
                                                                                Random.nextFloat() *
                                                                                        5f -
                                                                                        2.5f,

                                                                            alpha = 1f
                                                                        )
                                                                    }

                                                            } else {

                                                                /* ============================================
                                                                   ERRO = -0.25 VIDA
                                                                   ============================================ */

                                                                lives =
                                                                    (
                                                                            lives -
                                                                                    0.25f
                                                                            )
                                                                        .coerceAtLeast(
                                                                            0f
                                                                        )

                                                                combo = 0

                                                                lifeLostPulse++

                                                                isLocked =
                                                                    true

                                                                val secondIndex =
                                                                    index

                                                                scope.launch {

                                                                    delay(
                                                                        850
                                                                    )

                                                                    cards =
                                                                        cards.mapIndexed {
                                                                                i,
                                                                                card ->

                                                                            if (
                                                                                i ==
                                                                                firstIndex ||
                                                                                i ==
                                                                                secondIndex
                                                                            ) {

                                                                                card.copy(
                                                                                    isFlipped =
                                                                                        false
                                                                                )

                                                                            } else {
                                                                                card
                                                                            }
                                                                        }

                                                                    firstSelectedIndex =
                                                                        null

                                                                    secondSelectedIndex =
                                                                        null

                                                                    isLocked =
                                                                        false

                                                                    if (
                                                                        soundEnabled
                                                                    ) {

                                                                        SoundGenerator
                                                                            .playErrorSound()

                                                                        SoundGenerator
                                                                            .playLifeLostSound()
                                                                    }

                                                                    if (
                                                                        lives <= 0f
                                                                    ) {

                                                                        gameOver =
                                                                            true

                                                                        saveLevel(
                                                                            context,
                                                                            1
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        AnimatedVisibility(
                            visible =
                                isLevelComplete,

                            enter =
                                fadeIn() +
                                        scaleIn(
                                            initialScale =
                                                0.85f
                                        )
                        ) {

                            LevelCompleteCard(
                                stars = stars,

                                language =
                                    language,

                                nextLevel = {

                                    level++

                                    saveLevel(
                                        context,
                                        level
                                    )

                                    cards =
                                        createCardsForLevel(
                                            level
                                        )

                                    firstSelectedIndex =
                                        null

                                    secondSelectedIndex =
                                        null

                                    moves = 0
                                    matches = 0

                                    isLevelComplete =
                                        false

                                    isLocked =
                                        false

                                    gameOver =
                                        false

                                    timeLeft =
                                        if (
                                            gameMode ==
                                            GameMode.HARD
                                        ) {
                                            60
                                        } else {
                                            0
                                        }

                                    lives =
                                        5f
                                }
                            )
                        }

                        Spacer(
                            Modifier.height(12.dp)
                        )
                    }
                }
            }
        }

        /* =================================================
           PARTÍCULAS
           ================================================= */

        particles.forEach { particle ->

            var x by remember(
                particle.id
            ) {
                mutableStateOf(
                    particle.x
                )
            }

            var y by remember(
                particle.id
            ) {
                mutableStateOf(
                    particle.y
                )
            }

            var alpha by remember(
                particle.id
            ) {
                mutableStateOf(
                    particle.alpha
                )
            }

            LaunchedEffect(
                particle.id
            ) {

                repeat(25) {

                    delay(35)

                    x += particle.velocityX
                    y += particle.velocityY
                    alpha -= 0.04f
                }
            }

            Text(
                text = "✨",

                fontSize = 16.sp,

                color =
                    Color.White.copy(
                        alpha =
                            alpha.coerceAtLeast(
                                0f
                            )
                    ),

                modifier =
                    Modifier.offset(
                        x = x.dp,
                        y = y.dp
                    )
            )
        }

        /* =================================================
           TUTORIAL
           ================================================= */

        if (showTutorial) {

            OverlayDialog(
                onDismiss = {},

                content = {

                    Text(
                        "🃏",

                        fontSize = 52.sp
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        AppStrings.tutorial(
                            language
                        ),

                        fontSize = 23.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.White,

                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Erros tiram 0,25 ❤️\n" +
                                "A dica tira 0,5 ❤️\n" +
                                "Encontre todos os pares!",

                        color =
                            Color.White.copy(
                                alpha = 0.75f
                            ),

                        textAlign =
                            TextAlign.Center,

                        fontSize = 15.sp
                    )

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    Button(

                        onClick = {

                            showTutorial =
                                false

                            saveTutorialSeen(
                                context,
                                true
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            AppStrings.gotIt(
                                language
                            )
                        )
                    }
                }
            )
        }

        /* =================================================
           GAME OVER
           ================================================= */

        if (gameOver) {

            OverlayDialog(
                onDismiss = {},

                content = {

                    Text(
                        "💀",
                        fontSize = 58.sp
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        AppStrings.gameOver(
                            language
                        ),

                        color =
                            Color(0xFFFF6B6B),

                        fontSize = 30.sp,

                        fontWeight =
                            FontWeight.Black
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        AppStrings.tryAgain(
                            language
                        ),

                        color =
                            Color.White.copy(
                                alpha = 0.8f
                            ),

                        fontSize = 17.sp,

                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        Modifier.height(24.dp)
                    )

                    Button(

                        onClick = {

                            saveLevel(
                                context,
                                1
                            )

                            onBack()
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFFD84315)
                            )
                    ) {

                        Text(
                            "🏠 ${AppStrings.menu(language)}"
                        )
                    }
                }
            )
        }
    }
}

/* =========================================================
   CARD DE NÍVEL COMPLETO
   ========================================================= */

@Composable
fun LevelCompleteCard(
    stars: Int,
    language: Language,
    nextLevel: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(
                    0.90f
                )
                .padding(
                    8.dp
                ),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        color =
            Color(0xFF1F130D)
                .copy(
                    alpha = 0.97f
                ),

        border =
            androidx.compose.foundation.BorderStroke(
                2.dp,
                Color(0xFFFFD54F)
            ),

        shadowElevation = 14.dp
    ) {

        Column(
            modifier =
                Modifier.padding(
                    22.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                "🎉",

                fontSize = 48.sp
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                "NÍVEL COMPLETO!",

                color =
                    Color.White,

                fontSize = 23.sp,

                fontWeight =
                    FontWeight.Black
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                "⭐".repeat(
                    stars
                ),

                fontSize = 32.sp,

                color =
                    Color(0xFFFFD54F)
            )

            Spacer(
                Modifier.height(14.dp)
            )

            Button(
                onClick = nextLevel,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF2E7D32)
                    )
            ) {

                Text(
                    "🚀 ${AppStrings.nextLevel(language)}"
                )
            }
        }
    }
}

/* =========================================================
   OVERLAY
   ========================================================= */

@Composable
fun OverlayDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.72f
                    )
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(
                        0.84f
                    )
                    .padding(
                        20.dp
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            color =
                Color(0xFF1A1010),

            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(
                        alpha = 0.1f
                    )
                ),

            shadowElevation = 18.dp
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        24.dp
                    ),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                content = content
            )
        }
    }
}

/* =========================================================
   CORAÇÕES
   ========================================================= */

@Composable
fun HeartDisplay(
    lives: Float,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,

        horizontalArrangement =
            Arrangement.spacedBy(
                2.dp
            )
    ) {

        for (i in 0 until 5) {

            val heartValue =
                (
                        lives - i
                        ).coerceIn(
                        0f,
                        1f
                    )

            HeartIcon(
                fill = heartValue
            )
        }
    }
}

@Composable
fun HeartIcon(
    fill: Float
) {

    Canvas(
        modifier =
            Modifier.size(
                21.dp
            )
    ) {

        val w = size.width
        val h = size.height

        val heartPath =
            Path().apply {

                moveTo(
                    w * 0.5f,
                    h * 0.88f
                )

                cubicTo(
                    w * 0.10f,
                    h * 0.55f,
                    w * 0.10f,
                    h * 0.10f,
                    w * 0.35f,
                    h * 0.16f
                )

                cubicTo(
                    w * 0.43f,
                    h * 0.18f,
                    w * 0.48f,
                    h * 0.23f,
                    w * 0.5f,
                    h * 0.30f
                )

                cubicTo(
                    w * 0.52f,
                    h * 0.23f,
                    w * 0.57f,
                    h * 0.18f,
                    w * 0.65f,
                    h * 0.16f
                )

                cubicTo(
                    w * 0.90f,
                    h * 0.10f,
                    w * 0.90f,
                    h * 0.55f,
                    w * 0.5f,
                    h * 0.88f
                )

                close()
            }

        drawPath(
            path = heartPath,
            color =
                Color(0xFF6D1B1B),
            style =
                Stroke(
                    width = 1.7f,
                    cap =
                        StrokeCap.Round
                )
        )

        if (fill > 0f) {

            clipPath(
                heartPath
            ) {

                drawRect(
                    color =
                        Color(0xFFE53935),

                    topLeft =
                        Offset(
                            0f,
                            h * (1f - fill)
                        ),

                    size =
                        Size(
                            w,
                            h * fill
                        )
                )
            }
        }
    }
}

/* =========================================================
   CARTA
   ========================================================= */

@Composable
fun PlayingCardView(
    card: MemoryCard,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {

    val rotation by animateFloatAsState(
        targetValue =
            if (card.isFlipped) {
                180f
            } else {
                0f
            },

        animationSpec =
            tween(
                420,
                easing =
                    FastOutSlowInEasing
            ),

        label =
            "flip"
    )

    val cardScale by animateFloatAsState(
        targetValue =
            if (card.isMatched) {
                0.93f
            } else {
                1f
            },

        animationSpec =
            spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy
            ),

        label =
            "cardScale"
    )

    val borderColor by animateColorAsState(

        targetValue =
            when {
                card.isMatched ->
                    Color(0xFF7CFC8A)

                card.isFlipped ->
                    Color(0xFFFFD54F)

                else ->
                    Color(0xFFB8860B)
            },

        animationSpec =
            tween(250),

        label =
            "border"
    )

    val emojiSize =
        when {

            cardWidth <= 42.dp ->
                22.sp

            cardWidth <= 50.dp ->
                26.sp

            cardWidth <= 60.dp ->
                30.sp

            cardWidth <= 70.dp ->
                35.sp

            else ->
                39.sp
        }

    Box(

        modifier =
            Modifier
                .size(
                    width = cardWidth,
                    height = cardHeight
                )

                .graphicsLayer {
                    rotationY =
                        rotation

                    scaleX =
                        cardScale

                    scaleY =
                        cardScale

                    cameraDistance =
                        18f * density
                }

                .shadow(
                    elevation =
                        if (card.isFlipped) {
                            8.dp
                        } else {
                            4.dp
                        },

                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )

                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )

                .background(

                    when {
                        card.isMatched ->
                            Color(0xFF163D21)

                        card.isFlipped ->
                            Color.White

                        else ->
                            Color(0xFF172554)
                    }
                )

                .border(
                    width =
                        if (card.isMatched) {
                            2.dp
                        } else {
                            1.dp
                        },

                    color =
                        borderColor,

                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )

                .clickable(
                    onClick = onClick
                ),

        contentAlignment =
            Alignment.Center
    ) {

        if (
            card.isFlipped ||
            card.isMatched
        ) {

            Column(
                modifier =
                    Modifier.graphicsLayer {
                        rotationY = 180f
                    },

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = card.emoji,
                    fontSize = emojiSize
                )

                if (card.isMatched) {

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        text = "✓",

                        fontSize = 12.sp,

                        color =
                            Color(0xFF7CFC8A),

                        fontWeight =
                            FontWeight.Bold
                    )
                }

            }

        } else {

            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(5.dp)
            ) {

                val center =
                    Offset(
                        size.width / 2f,
                        size.height / 2f
                    )

                val radius =
                    minOf(
                        size.width,
                        size.height
                    ) * 0.28f

                drawRoundRect(
                    color =
                        Color.White.copy(
                            alpha = 0.05f
                        ),

                    topLeft =
                        Offset(
                            5f,
                            5f
                        ),

                    size =
                        Size(
                            size.width - 10f,
                            size.height - 10f
                        ),

                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(
                                8f,
                                8f
                            ),

                    style =
                        Stroke(1.3f)
                )

                drawCircle(
                    color =
                        Color(0xFFFFC107)
                            .copy(
                                alpha = 0.65f
                            ),

                    radius =
                        radius,

                    center =
                        center,

                    style =
                        Stroke(
                            width = 1.4f
                        )
                )

                drawLine(
                    color =
                        Color(0xFFFFC107)
                            .copy(
                                alpha = 0.5f
                            ),

                    start =
                        Offset(
                            center.x - radius,
                            center.y
                        ),

                    end =
                        Offset(
                            center.x + radius,
                            center.y
                        ),

                    strokeWidth =
                        1.2f
                )

                drawLine(
                    color =
                        Color(0xFFFFC107)
                            .copy(
                                alpha = 0.5f
                            ),

                    start =
                        Offset(
                            center.x,
                            center.y - radius
                        ),

                    end =
                        Offset(
                            center.x,
                            center.y + radius
                        ),

                    strokeWidth =
                        1.2f
                )
            }
        }
    }
}

/* =========================================================
   MESA DE MADEIRA
   ========================================================= */

@Composable
fun WoodenTable() {

    Canvas(
        modifier =
            Modifier.fillMaxSize()
    ) {

        drawRect(
            brush =
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF29160D),
                        Color(0xFF4A2C1B),
                        Color(0xFF2A160D)
                    )
                )
        )

        for (i in 0..36) {

            val y =
                i * 42f

            drawLine(

                color =
                    if (i % 2 == 0) {
                        Color(0xFF5B3A24)
                            .copy(
                                alpha = 0.18f
                            )
                    } else {
                        Color(0xFF1D0F08)
                            .copy(
                                alpha = 0.14f
                            )
                    },

                start =
                    Offset(
                        0f,
                        y
                    ),

                end =
                    Offset(
                        size.width,
                        y + 12f
                    ),

                strokeWidth =
                    5f
            )
        }

        repeat(12) {

            val center =
                Offset(
                    Random.nextFloat() *
                            size.width,

                    Random.nextFloat() *
                            size.height
                )

            drawCircle(

                color =
                    Color(0xFF130A05)
                        .copy(
                            alpha = 0.10f
                        ),

                radius =
                    Random.nextFloat() *
                            28f +
                            7f,

                center =
                    center,

                style =
                    Stroke(
                        width = 2f
                    )
            )
        }
    }
}

/* =========================================================
   FUNÇÕES DE GRID
   ========================================================= */

fun getGridColumns(
    level: Int
): Int {

    return when {

        level <= 4 ->
            4

        level <= 6 ->
            5

        else ->
            6
    }
}

fun getGridRows(
    level: Int
): Int {

    return when {

        level <= 2 ->
            3

        level <= 6 ->
            4

        else ->
            5
    }
}

fun getCardWidth(
    level: Int
): androidx.compose.ui.unit.Dp {

    return when {

        level <= 2 ->
            78.dp

        level <= 4 ->
            68.dp

        level <= 6 ->
            58.dp

        level <= 8 ->
            49.dp

        else ->
            43.dp
    }
}

fun getCardHeight(
    level: Int
): androidx.compose.ui.unit.Dp {

    return when {

        level <= 2 ->
            98.dp

        level <= 4 ->
            86.dp

        level <= 6 ->
            75.dp

        level <= 8 ->
            65.dp

        else ->
            56.dp
    }
}

/* =========================================================
   CRIAÇÃO DAS CARTAS
   ========================================================= */

fun createCardsForLevel(
    level: Int
): List<MemoryCard> {

    val emojis =
        listOf(
            "❤️",
            "🌟",
            "🎈",
            "🍀",
            "🌈",
            "🎯",
            "🎨",
            "🎵",
            "🎭",
            "🎪",
            "🎢",
            "🎡",
            "🎠",
            "🎮",
            "🎲",
            "🎳",
            "🎱",
            "🎸",
            "🎺",
            "🎻",
            "🎹",
            "🎧",
            "🎤",
            "🎬",
            "🍕",
            "🍔",
            "🍩",
            "🍉",
            "🍓",
            "⚽"
        )

    val numPairs =
        when {

            level <= 2 ->
                6

            level <= 4 ->
                8

            level <= 6 ->
                10

            level <= 8 ->
                12

            else ->
                15
        }

    val selected =
        emojis
            .shuffled()
            .take(
                numPairs
            )

    val cards =
        mutableListOf<MemoryCard>()

    selected.forEachIndexed { index, emoji ->

        cards += MemoryCard(
            id =
                index * 2,

            emoji =
                emoji
        )

        cards += MemoryCard(
            id =
                index * 2 + 1,

            emoji =
                emoji
        )
    }

    return cards.shuffled()
}

fun getCardCenter(
    row: Int,
    col: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp
): Pair<Float, Float> {

    val x =
        col *
                (cardWidth.value + 5f) +
                cardWidth.value / 2f

    val y =
        row *
                (cardHeight.value + 5f) +
                cardHeight.value / 2f

    return x to y
}

/* =========================================================
   TEMA
   ========================================================= */

fun getThemeColor(
    theme: String
): Color {

    return when (theme) {

        "green" ->
            Color(0xFF2E7D32)

        "blue" ->
            Color(0xFF1565C0)

        "purple" ->
            Color(0xFF6A1B9A)

        else ->
            Color(0xFFB22222)
    }
}

/* =========================================================
   PERSISTÊNCIA
   ========================================================= */

fun saveLevel(
    context: Context,
    level: Int
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putInt(
            "level",
            level
        )
        .apply()
}

fun loadLevel(
    context: Context
): Int {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getInt(
            "level",
            1
        )
}

fun saveStarsAndRecord(
    context: Context,
    level: Int,
    stars: Int,
    moves: Int
) {

    val prefs =
        context.getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )

    val editor =
        prefs.edit()

    val currentStars =
        prefs.getInt(
            "total_stars",
            0
        )

    editor.putInt(
        "total_stars",
        currentStars + stars
    )

    val key =
        "best_moves_level_$level"

    val best =
        prefs.getInt(
            key,
            Int.MAX_VALUE
        )

    if (moves < best) {

        editor.putInt(
            key,
            moves
        )
    }

    val completed =
        prefs.getInt(
            "levels_completed",
            0
        )

    editor.putInt(
        "levels_completed",
        completed + 1
    )

    editor.apply()
}

fun loadBestMoves(
    context: Context
): Map<Int, Int> {

    val prefs =
        context.getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )

    val result =
        mutableMapOf<Int, Int>()

    for (level in 1..50) {

        val moves =
            prefs.getInt(
                "best_moves_level_$level",
                -1
            )

        if (moves > 0) {
            result[level] = moves
        }
    }

    return result
}

fun loadTotalStars(
    context: Context
): Int {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getInt(
            "total_stars",
            0
        )
}

fun loadLevelsCompleted(
    context: Context
): Int {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getInt(
            "levels_completed",
            0
        )
}

fun saveTheme(
    context: Context,
    theme: String
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            "theme",
            theme
        )
        .apply()
}

fun loadTheme(
    context: Context
): String {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getString(
            "theme",
            "red"
        )
        ?: "red"
}

fun saveMusicEnabled(
    context: Context,
    enabled: Boolean
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putBoolean(
            "music_enabled",
            enabled
        )
        .apply()
}

fun loadMusicEnabled(
    context: Context
): Boolean {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getBoolean(
            "music_enabled",
            true
        )
}

fun saveSoundEnabled(
    context: Context,
    enabled: Boolean
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putBoolean(
            "sound_enabled",
            enabled
        )
        .apply()
}

fun loadSoundEnabled(
    context: Context
): Boolean {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getBoolean(
            "sound_enabled",
            true
        )
}

fun loadAchievements(
    context: Context
): Set<String> {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getStringSet(
            "achievements",
            emptySet()
        )
        ?: emptySet()
}

fun unlockAchievement(
    context: Context,
    key: String
) {

    val prefs =
        context.getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )

    val current =
        prefs
            .getStringSet(
                "achievements",
                emptySet()
            )
            ?.toMutableSet()
            ?: mutableSetOf()

    if (current.add(key)) {

        prefs.edit()
            .putStringSet(
                "achievements",
                current
            )
            .apply()
    }
}

fun saveLanguage(
    context: Context,
    language: Language
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            "language",
            language.code
        )
        .apply()
}

fun loadLanguage(
    context: Context
): Language {

    val code =
        context
            .getSharedPreferences(
                "game_prefs",
                Context.MODE_PRIVATE
            )
            .getString(
                "language",
                "pt"
            )
            ?: "pt"

    return when (code) {

        "en" ->
            Language.EN

        "es" ->
            Language.ES

        else ->
            Language.PT
    }
}

fun saveTutorialSeen(
    context: Context,
    seen: Boolean
) {

    context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .edit()
        .putBoolean(
            "tutorial_seen",
            seen
        )
        .apply()
}

fun loadTutorialSeen(
    context: Context
): Boolean {

    return context
        .getSharedPreferences(
            "game_prefs",
            Context.MODE_PRIVATE
        )
        .getBoolean(
            "tutorial_seen",
            false
        )
}

/* =========================================================
   PREVIEW
   ========================================================= */

@Preview(
    showBackground = true
)
@Composable
fun MemoryGamePreview() {

    JogoDaMemoriaTheme {
        MemoryGameApp()
    }
}