path = "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

replacements = [
    # 1) imports: icons + material3 widgets
    (
        '''import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.lightColorScheme''',
        '''import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme'''
    ),
    # 2) TextDirection import
    (
        '''import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign''',
        '''import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection'''
    ),
    # 3) new state vars
    (
        '''    var fontSizeSp by remember { mutableFloatStateOf(26f) } // Slider 20sp to 42sp
    var activeJob by remember { mutableStateOf<Job?>(null) }''',
        '''    var fontSizeSp by remember { mutableFloatStateOf(26f) } // Slider 20sp to 42sp
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var isTextInputMode by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }'''
    ),
    # 4) new processTextWithGemini function, inserted before processImageWithGemini
    (
        '''    // Function to perform extraction and diacritization with GenerativeModel''',
        '''    // Function to diacritize plain text typed or pasted by the user (no image/OCR needed)
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
                    } else {
                        errorMessage = "لم يُرجع النموذج استجابة."
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = if (e is OutOfMemoryError) {
                        "النص طويل جدًا على ذاكرة الجهاز. جرّب تقسيمه لأجزاء أصغر."
                    } else {
                        "خطأ أثناء معالجة النص: ${e.localizedMessage ?: e.message}"
                    }
                }
            }
        }
    }

    // Function to perform extraction and diacritization with GenerativeModel'''
    ),
    # 5) UI: mode toggle + text input card, opening an else-branch around the image card
    (
        '''            // Controls Card: Image Picker & Actions''',
        '''            // Input mode toggle: image (OCR) vs. typing/pasting text directly
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            if (isTextInputMode) {
                // Direct text input card: paste or type Arabic text to diacritize, no image needed
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
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp)
                                .testTag("manual_text_field"),
                            placeholder = { Text("الصق أو اكتب النص العربي هنا للتشكيل") },
                            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                        )
                        FilledTonalButton(
                            onClick = { processTextWithGemini(manualInputText) },
                            enabled = !isLoading && manualInputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diacritize_text_button"),
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
            } else {

            // Controls Card: Image Picker & Actions'''
    ),
    # 6) close the else-branch right before the font size slider card
    (
        '''                        }
                    }
                }
            }

            // Typography Controls: Font Size Slider (20sp to 42sp)''',
        '''                        }
                    }
                }
            }
            }

            // Typography Controls: Font Size Slider (20sp to 42sp)'''
    ),
]

missing = []
for old, new in replacements:
    if old not in content:
        missing.append(old[:60])
    else:
        content = content.replace(old, new, 1)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

if missing:
    print("WARNING - some patterns were not found:")
    for m in missing:
        print(" -", m)
else:
    print("All replacements applied successfully")
