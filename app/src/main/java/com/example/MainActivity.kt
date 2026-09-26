package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArabicDiacritizerTheme {
                ArabicDiacritizerScreen()
            }
        }
    }
}

@Composable
fun ArabicDiacritizerTheme(content: @Composable () -> Unit) {
    val emeraldScheme = lightColorScheme(
        primary = Color(0xFF0F5132),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E7DD),
        onPrimaryContainer = Color(0xFF073822),
        secondary = Color(0xFF856404),
        secondaryContainer = Color(0xFFFFF3CD),
        surface = Color(0xFFFBFBF9),
        background = Color(0xFFF7F7F4),
        onSurface = Color(0xFF1F2421),
        onBackground = Color(0xFF1F2421)
    )

    MaterialTheme(
        colorScheme = emeraldScheme,
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
                    val bitmap = loadScaledBitmap(context, uri, maxDimension = 1536)
                    withContext(Dispatchers.Main) {
                        selectedBitmap = bitmap
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "فشل تحميل الصورة: ${e.localizedMessage ?: e.message}"
                    }
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
                    } else {
                        errorMessage = "لم يتم العثور على نص عربي في الصورة أو لم يُرجع النموذج استجابة."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "خطأ أثناء معالجة الصورة: ${e.localizedMessage ?: e.message}"
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key notice if missing
            if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
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

            // Controls Card: Image Picker & Actions
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_image_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
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

                        if (selectedBitmap != null) {
                            FilledTonalButton(
                                onClick = { processImageWithGemini() },
                                enabled = !isLoading,
                                modifier = Modifier.testTag("diacritize_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "بدء التشكيل"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تشكيل")
                                }
                            }
                        }
                    }

                    // Image Preview Area
                    selectedBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
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
                                    .heightIn(max = 220.dp),
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

            // Typography Controls: Font Size Slider (20sp to 42sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            text = "${fontSizeSp.roundToInt()} sp (تباعد الأسطر 2.0x)",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20sp (صغير)", style = MaterialTheme.typography.labelSmall)
                        Text("31sp (متوسط)", style = MaterialTheme.typography.labelSmall)
                        Text("42sp (كبير وواضح)", style = MaterialTheme.typography.labelSmall)
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

            // Main Output Area: Scrollable Text Display with 2.0x Line Height
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
                    // Header of result card with action buttons
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

                        if (extractedText.isNotBlank()) {
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
                                    onClick = { processImageWithGemini() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "إعادة التشكيل",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    // Arabic Text Display with RTL and 2.0x Line Height
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 500.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            SelectionContainer {
                                if (extractedText.isNotBlank()) {
                                    Text(
                                        text = extractedText,
                                        fontSize = fontSizeSp.sp,
                                        // High line-height (2.0x) specifically for Arabic diacritics clarity
                                        lineHeight = (fontSizeSp * 2.0f).sp,
                                        textAlign = TextAlign.Start,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("diacritized_text_display")
                                    )
                                } else {
                                    Text(
                                        text = if (isLoading) {
                                            "يتم الآن تحليل الصورة وإضافة الحركات التشكيلية..."
                                        } else {
                                            "اختر صورة تحتوي على نص عربي واضغط على زر «تشكيل» لاستخراج النص وضبطه بالشكل التام (الفَتْحَة، الضَّمَّة، الكَسْرَة، السُّكُون، الشَّدَّة، التَّنْوِين).\n\nسيعرض النص هنا مع تباعد أسطر مضاعف (2.0x) لتوضيح علامات التشكيل بدقة دون تداخل."
                                        },
                                        fontSize = 18.sp,
                                        lineHeight = 36.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (extractedText.isNotBlank()) {
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

            Spacer(modifier = Modifier.height(16.dp))
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
