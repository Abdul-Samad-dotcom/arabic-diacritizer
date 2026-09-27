path = "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

replacements = [
    # 1) Add shared friendly-error helper before processTextWithGemini
    (
        '''    // Function to diacritize plain text typed or pasted by the user (no image/OCR needed)''',
        '''    // Turns raw exceptions into a clear Arabic message.
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

    // Function to diacritize plain text typed or pasted by the user (no image/OCR needed)'''
    ),
    # 2) Use helper in text-mode catch block
    (
        '''            } catch (e: Throwable) {
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

    // Function to perform extraction and diacritization with GenerativeModel''',
        '''            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = friendlyErrorMessage(e)
                }
            }
        }
    }

    // Function to perform extraction and diacritization with GenerativeModel'''
    ),
    # 3) Use helper in image-mode catch block
    (
        '''            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = if (e is OutOfMemoryError) {
                        "الصورة كبيرة جدًا على ذاكرة الجهاز. جرّب صورة أصغر أو أقل دقة."
                    } else {
                        "خطأ أثناء معالجة الصورة: ${e.localizedMessage ?: e.message}"
                    }
                }
            }''',
        '''            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = friendlyErrorMessage(e)
                }
            }'''
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
