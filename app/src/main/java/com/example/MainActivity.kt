package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// gemini-3.8-flash does not exist as a published model; gemini-3.5-flash is the
// current GA flash model as of mid-2026. Centralized so it's a one-line change
// if/when the Gemini lineup moves again.
private const val GEMINI_MODEL_NAME = "gemini-3.5-flash"

enum class AppTab { HOME, HISTORY, SETTINGS }

enum class AccentColor(
    val label: String,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
) {
    EMERALD(
        "أخضر",
        Color(0xFF0F5132), Color.White, Color(0xFFD1E7DD), Color(0xFF073822),
        Color(0xFF7FD8AE), Color(0xFF0A3B26), Color(0xFF0F5132), Color(0xFFD1E7DD)
    ),
    MAROON(
        "عنابي",
        Color(0xFF7A1F3D), Color.White, Color(0xFFF3D9E1), Color(0xFF4A1226),
        Color(0xFFE8A0B8), Color(0xFF4A1226), Color(0xFF7A1F3D), Color(0xFFF3D9E1)
    ),
    BLUE(
        "أزرق",
        Color(0xFF0D5C8C), Color.White, Color(0xFFD3E7F5), Color(0xFF083A57),
        Color(0xFF8FCBEF), Color(0xFF083A57), Color(0xFF0D5C8C), Color(0xFFD3E7F5)
    ),
    PURPLE(
        "بنفسجي",
        Color(0xFF5B2A86), Color.White, Color(0xFFE5D6F5), Color(0xFF371756),
        Color(0xFFC9A6EE), Color(0xFF371756), Color(0xFF5B2A86), Color(0xFFE5D6F5)
    );

    companion object {
        fun fromName(name: String?): AccentColor = entries.find { it.name == name } ?: EMERALD
    }
}

private const val PREFS_NAME = "app_settings"
private const val KEY_DARK_MODE = "dark_mode_enabled"
private const val KEY_ACCENT = "accent_color"
private const val KEY_HISTORY = "history_entries"

private fun appPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun loadDarkModeEnabled(context: Context, systemDefault: Boolean): Boolean =
    appPrefs(context).getBoolean(KEY_DARK_MODE, systemDefault)

private fun saveDarkModeEnabled(context: Context, enabled: Boolean) {
    appPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
}

private fun loadAccentColor(context: Context): AccentColor =
    AccentColor.fromName(appPrefs(context).getString(KEY_ACCENT, AccentColor.EMERALD.name))

private fun saveAccentColor(context: Context, color: AccentColor) {
    appPrefs(context).edit().putString(KEY_ACCENT, color.name).apply()
}

private fun loadHistory(context: Context): List<Pair<Long, String>> {
    val raw = appPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
    return try {
        val array = org.json.JSONArray(raw)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            obj.getLong("ts") to obj.getString("text")
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun writeHistory(context: Context, entries: List<Pair<Long, String>>) {
    val array = org.json.JSONArray()
    entries.forEach { (ts, t) ->
        val obj = org.json.JSONObject()
        obj.put("ts", ts)
        obj.put("text", t)
        array.put(obj)
    }
    appPrefs(context).edit().putString(KEY_HISTORY, array.toString()).apply()
}

private fun saveHistoryEntry(context: Context, text: String) {
    val existing = loadHistory(context).toMutableList()
    existing.add(0, System.currentTimeMillis() to text)
    writeHistory(context, existing.take(50))
}

private fun deleteHistoryEntry(context: Context, timestamp: Long) {
    writeHistory(context, loadHistory(context).filterNot { it.first == timestamp })
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var darkModeEnabled by remember { mutableStateOf(loadDarkModeEnabled(context, systemDark)) }
    var accentColor by remember { mutableStateOf(loadAccentColor(context)) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    ArabicDiacritizerTheme(darkModeEnabled = darkModeEnabled, accentColor = accentColor) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.SETTINGS,
                        onClick = { selectedTab = AppTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("الإعدادات") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.HISTORY,
                        onClick = { selectedTab = AppTab.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("السجل") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.HOME,
                        onClick = { selectedTab = AppTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("الرئيسية") }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    AppTab.HOME -> ArabicDiacritizerScreen()
                    AppTab.HISTORY -> HistoryScreen()
                    AppTab.SETTINGS -> SettingsScreen(
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChange = {
                            darkModeEnabled = it
                            saveDarkModeEnabled(context, it)
                        },
                        accentColor = accentColor,
                        onAccentColorChange = {
                            accentColor = it
                            saveAccentColor(context, it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var historyEntries by remember { mutableStateOf(loadHistory(context)) }
    val dateFormat = remember {
        java.text.SimpleDateFormat("yyyy/MM/dd - HH:mm", java.util.Locale("ar"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("السجل", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        if (historyEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد نصوص محفوظة بعد",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyEntries, key = { it.first }) { entry ->
                    val timestamp = entry.first
                    val text = entry.second
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = dateFormat.format(java.util.Date(timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (text.length > 150) text.take(150) + "…" else text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { copyToClipboard(context, text) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                                }
                                IconButton(onClick = { shareText(context, text) }) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة")
                                }
                                IconButton(onClick = {
                                    deleteHistoryEntry(context, timestamp)
                                    historyEntries = loadHistory(context)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    accentColor: AccentColor,
    onAccentColorChange: (AccentColor) -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("الإعدادات", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "تخصيص تجربة التشكيل والقراءة الخاصة بك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            OutlinedCard(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "المظهر والعرض",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الوضع الليلي")
                        Switch(checked = darkModeEnabled, onCheckedChange = onDarkModeChange)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("لون السمة")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentColor.entries.forEach { color ->
                                val swatch = if (darkModeEnabled) color.darkPrimary else color.lightPrimary
                                val onSwatch = if (darkModeEnabled) color.darkOnPrimary else color.lightOnPrimary
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(swatch)
                                        .border(
                                            width = if (accentColor == color) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(50)
                                        )
                                        .clickable { onAccentColorChange(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (accentColor == color) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = color.label,
                                            tint = onSwatch,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "النظام والتطبيق",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عن التطبيق")
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "التطبيق لم يُنشر على المتجر بعد", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تقييم التطبيق")
                        Icon(Icons.Default.Star, contentDescription = null)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("حسنًا") }
            },
            title = { Text("عن التطبيق") },
            text = {
                Text("تَشْكِيل الذَّكِي - تطبيق لاستخراج النص العربي من الصور أو النص المباشر وضبطه بالشكل التام باستخدام الذكاء الاصطناعي.")
            }
        )
    }
}

@Composable
fun ArabicDiacritizerTheme(
    darkModeEnabled: Boolean,
    accentColor: AccentColor,
    content: @Composable () -> Unit
) {
    val lightScheme = lightColorScheme(
        primary = accentColor.lightPrimary,
        onPrimary = accentColor.lightOnPrimary,
        primaryContainer = accentColor.lightContainer,
        onPrimaryContainer = accentColor.lightOnContainer,
        secondary = Color(0xFF856404),
        secondaryContainer = Color(0xFFFFF3CD),
        surface = Color(0xFFFBFBF9),
        background = Color(0xFFF7F7F4),
        onSurface = Color(0xFF1F2421),
        onBackground = Color(0xFF1F2421)
    )

    val darkScheme = darkColorScheme(
        primary = accentColor.darkPrimary,
        onPrimary = accentColor.darkOnPrimary,
        primaryContainer = accentColor.darkContainer,
        onPrimaryContainer = accentColor.darkOnContainer,
        secondary = Color(0xFFE0C46B),
        secondaryContainer = Color(0xFF4D3F0A),
        surface = Color(0xFF17201C),
        background = Color(0xFF101512),
        onSurface = Color(0xFFE3E6E3),
        onBackground = Color(0xFFE3E6E3)
    )

    MaterialTheme(
        colorScheme = if (darkModeEnabled) darkScheme else lightScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArabicDiacritizerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fontSizeSp by remember { mutableFloatStateOf(26f) } // Slider 20sp to 42sp
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isTextInputMode by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // A new pick supersedes anything in flight or already shown.
            activeJob?.cancel()
            selectedBitmap?.recycle()
            selectedImageUri = uri
            selectedBitmap = null
            extractedText = ""
            errorMessage = null
            activeJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = loadScaledBitmap(context, uri, maxDimension = 1024)
                    withContext(Dispatchers.Main) {
                        selectedBitmap = bitmap
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "فشل تحميل الصورة: ${e.localizedMessage ?: e.message}"
                    }
                }
            }
        }
    }

    // Turns raw exceptions into a clear Arabic message.
    // Note: the older Gemini SDK has a bug where a 503 "server busy" response
    // (missing an optional "details" field) makes it throw a confusing
    // MissingFieldException instead of a clean error - we detect that case here.
    fun friendlyErrorMessage(e: Throwable): String {
        val raw = "${e.message} ${e.localizedMessage}"
        return when {
            e is OutOfMemoryError ->
                "الملف كبير جدًا على ذاكرة الجهاز. جرّب صورة أو نصًا أصغر."
            raw.contains("UNAVAILABLE") || raw.contains("503") || raw.contains("high demand") ->
                "خوادم Gemini مزدحمة حاليًا. حاول مرة أخرى خلال دقيقة."
            raw.contains("MissingFieldException") ->
                "حدث خطأ غير متوقع من الخادم. حاول مرة أخرى."
            else ->
                "خطأ: ${e.localizedMessage ?: e.message}"
        }
    }

    // Function to diacritize plain text typed or pasted by the user (no image/OCR needed)
    fun processTextWithGemini(inputText: String) {
        if (inputText.isBlank()) {
            errorMessage = "يرجى إدخال نص عربي أولاً"
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            errorMessage = "مفتاح Gemini API غير معين. يرجى تكوين GEMINI_API_KEY في إعدادات التطبيق أو Secrets."
            return
        }

        activeJob?.cancel()
        isLoading = true
        errorMessage = null
        extractedText = ""

        activeJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = GEMINI_MODEL_NAME,
                    apiKey = apiKey
                )

                val prompt = """
                    أنت خبير في علم اللغة العربية والنحو والصرف والرسم الإملائي.
                    مهمتك ضبط النص العربي التالي بالشكل التام الكامل (تشكيل كامل: الفتحة، الضمة، الكسرة، السكون، الشدة، والتنوين، وضبط أواخر الكلمات إعرابياً)،
                    مع الحفاظ على الكلمات وعلامات الترقيم وتنسيق الفقرات والأسطر كما هي تمامًا دون تغيير أو حذف أو إضافة أي كلمة.
                    أعد النص المشكول فقط دون أي مقدمات أو تحيات أو تعليقات خارجية.

                    النص:
                    $inputText
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val resultText = response.text?.trim().orEmpty()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (resultText.isNotBlank()) {
                        extractedText = resultText
                        saveHistoryEntry(context, resultText)
                    } else {
                        errorMessage = "لم يُرجع النموذج استجابة."
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = friendlyErrorMessage(e)
                }
            }
        }
    }

    // Function to perform extraction and diacritization with GenerativeModel
    fun processImageWithGemini() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            errorMessage = "يرجى اختيار صورة أولاً"
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            errorMessage = "مفتاح Gemini API غير معين. يرجى تكوين GEMINI_API_KEY في إعدادات التطبيق أو Secrets."
            return
        }

        // Re-running diacritization supersedes any previous request.
        activeJob?.cancel()
        isLoading = true
        errorMessage = null
        extractedText = ""

        activeJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = GEMINI_MODEL_NAME,
                    apiKey = apiKey
                )

                // Prompt engineered specifically for Arabic OCR and full diacritization (Tashkeel)
                val prompt = """
                    أنت خبير في علم اللغة العربية والنحو والصرف والرسم الإملائي.
                    مهمتك هي:
                    1. استخراج كل النص العربي الموجود في هذه الصورة بدقة متناهية وترتيبه كما ورد.
                    2. ضبط النص بالشكل التام الكامل (تشكيل كامل: الفتحة، الضمة، الكسرة، السكون، الشدة، والتنوين، وضبط أواخر الكلمات إعرابياً).
                    3. الحفاظ على علامات الترقيم وتنسيق الفقرات والأسطر كما هي.
                    4. أعد النص العربي المشكول فقط دون أي مقدمات أو تحيات أو تعليقات خارجية.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val resultText = response.text?.trim().orEmpty()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (resultText.isNotBlank()) {
                        extractedText = resultText
                        saveHistoryEntry(context, resultText)
                    } else {
                        errorMessage = "لم يتم العثور على نص عربي في الصورة أو لم يُرجع النموذج استجابة."
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = friendlyErrorMessage(e)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "تَشْكِيل الذَّكِي",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = GEMINI_MODEL_NAME,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "استخراج النص العربي وضبطه بالشكل التام",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // API Key notice if missing
            if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "تحذير المفتاح",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ملاحظة: تأكد من إضافة مفتاح GEMINI_API_KEY في ملف .env أو في لوحة Secrets في AI Studio لتشغيل المعالجة بنجاح.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Top controls: input mode toggle only (image OCR vs. typed text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isTextInputMode,
                    onClick = { isTextInputMode = false },
                    label = { Text("من صورة") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isTextInputMode,
                    onClick = { isTextInputMode = true },
                    label = { Text("نص مباشر") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Middle content area: large, mostly-empty space when idle;
            // fills naturally with the picked image / typed text / result once there is content.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                contentAlignment = if (selectedBitmap == null && manualInputText.isBlank() && extractedText.isBlank()) {
                    Alignment.Center
                } else {
                    Alignment.TopCenter
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isTextInputMode) {
                        // Direct text input: paste or type Arabic text to diacritize, no image needed
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 320.dp)
                                .testTag("manual_text_field"),
                            placeholder = { Text("الصق أو اكتب النص العربي هنا للتشكيل") },
                            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                        )
                    } else {
                        if (selectedBitmap == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "لم يتم اختيار صورة بعد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            selectedBitmap?.let { bitmap ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 320.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.05f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "الصورة المختارة",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 320.dp),
                                        contentScale = ContentScale.Fit
                                    )

                                    IconButton(
                                        onClick = {
                                            selectedBitmap = null
                                            selectedImageUri = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "حذف الصورة",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error Display
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "خطأ",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Loading Indicator
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "جارٍ استخراج النص وضبطه بالشكل التام عبر الذكاء الاصطناعي...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "قد يستغرق ذلك بضع ثوانٍ لحساب الحركات والإعراب بدقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Result: Scrollable Text Display with 2.0x Line Height
                    if (extractedText.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("result_text_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "النص المشكول (تَشْكِيلٌ كَامِلٌ):",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(context, extractedText)
                                                Toast.makeText(context, "تم نسخ النص المشكول بنجاح", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("copy_text_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ النص",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                shareText(context, extractedText)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "مشاركة النص",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isTextInputMode) processTextWithGemini(manualInputText) else processImageWithGemini()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "إعادة التشكيل",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 500.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFE9DFC0), Color(0xFFC7B47D))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF8A7748).copy(alpha = 0.5f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        SelectionContainer {
                                            Text(
                                                text = extractedText,
                                                fontSize = fontSizeSp.sp,
                                                // High line-height (2.0x) specifically for Arabic diacritics clarity
                                                lineHeight = (fontSizeSp * 2.0f).sp,
                                                textAlign = TextAlign.Start,
                                                fontFamily = FontFamily.Default,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF2B1D0E),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("diacritized_text_display")
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "عدد الأحرف: ${extractedText.length}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    OutlinedButton(
                                        onClick = { extractedText = "" },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("مسح النتيجة")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom controls: font size + primary action (تشكيل) - always reachable, never scroll away
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "حجم الخط",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "حجم الخط للتشكيل:",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${fontSizeSp.roundToInt()} sp",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = 20f..42f,
                        steps = 21,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_size_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (!isTextInputMode) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_image_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "اختيار صورة"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedBitmap == null) "اختيار صورة للنص" else "تغيير الصورة",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isTextInputMode) processTextWithGemini(manualInputText) else processImageWithGemini()
                        },
                        enabled = !isLoading && if (isTextInputMode) manualInputText.isNotBlank() else selectedBitmap != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diacritize_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "بدء التشكيل"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تشكيل", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loads a bitmap from URI and scales it down to prevent memory issues with high-res photos.
 */
private fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val max = maxOf(width, height)
            if (max > maxDimension) {
                val scale = maxDimension.toFloat() / max
                decoder.setTargetSize((width * scale).roundToInt(), (height * scale).roundToInt())
            }
            decoder.isMutableRequired = true
        }
    } else {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            context.contentResolver.openInputStream(uri)?.use { secondStream ->
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                    ?: throw IllegalStateException("تعذر فك ترميز الصورة")
            } ?: throw IllegalStateException("تعذر فتح ملف الصورة")
        } ?: throw IllegalStateException("تعذر فتح مسار الصورة")
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Arabic Diacritized Text", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة النص المشكول")
    context.startActivity(shareIntent)
}
