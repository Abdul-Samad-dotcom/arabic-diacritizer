path = "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

warnings = []

if "import androidx.compose.ui.graphics.Brush\n" not in content:
    content = content.replace(
        "import androidx.compose.ui.graphics.Color\n",
        "import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\n",
        1
    )

old_box = '''                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )'''
new_box = '''                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE9DFC0), Color(0xFFC7B47D))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFF8A7748).copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )'''
if old_box in content:
    content = content.replace(old_box, new_box, 1)
else:
    warnings.append("text-box background block not found")

old_color1 = "color = MaterialTheme.colorScheme.onBackground,"
new_color1 = "color = Color(0xFF2B1D0E),"
if old_color1 in content:
    content = content.replace(old_color1, new_color1, 1)
else:
    warnings.append("output text color not found")

old_color2 = "color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),"
new_color2 = "color = Color(0xFF2B1D0E).copy(alpha = 0.55f),"
if old_color2 in content:
    content = content.replace(old_color2, new_color2, 1)
else:
    warnings.append("placeholder text color not found")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

if warnings:
    print("WARNING:")
    for w in warnings:
        print(" -", w)
else:
    print("Done: parchment background applied")
