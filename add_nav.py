import sys

path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

warnings = []

# 1) imports (added only if missing)
needed_imports = [
    "androidx.compose.foundation.layout.WindowInsets",
    "androidx.compose.foundation.layout.consumeWindowInsets",
    "androidx.compose.material.icons.filled.History",
    "androidx.compose.material.icons.filled.Home",
    "androidx.compose.material.icons.filled.Settings",
    "androidx.compose.material3.NavigationBar",
    "androidx.compose.material3.NavigationBarItem",
    "androidx.compose.runtime.CompositionLocalProvider",
    "androidx.compose.ui.Alignment",
    "androidx.compose.ui.platform.LocalLayoutDirection",
    "androidx.compose.ui.unit.LayoutDirection",
]
missing_imports = [i for i in needed_imports if ("import " + i + "\n") not in content]
anchor_import = "import kotlin.math.roundToInt\n"
if anchor_import in content:
    block = "".join("import " + i + "\n" for i in missing_imports)
    content = content.replace(anchor_import, block + anchor_import, 1)
else:
    warnings.append("import anchor not found")

# 2) setContent now starts from AppRoot (navigation) instead of the single screen
old_set = """            ArabicDiacritizerTheme {
                ArabicDiacritizerScreen()
            }"""
if old_set in content:
    content = content.replace(old_set, "            AppRoot()", 1)
else:
    warnings.append("setContent block not found")

# 3) new navigation code appended at the end of the file
nav_code = '''

// ---------- Bottom navigation: الرئيسية / السجل / الإعدادات ----------
enum class AppTab { HOME, HISTORY, SETTINGS }

@Composable
fun AppRoot() {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    ArabicDiacritizerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            bottomBar = {
                // Force RTL so the order is: الرئيسية (right) - السجل - الإعدادات (left)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == AppTab.HOME,
                            onClick = { selectedTab = AppTab.HOME },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("الرئيسية") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.HISTORY,
                            onClick = { selectedTab = AppTab.HISTORY },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("السجل") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.SETTINGS,
                            onClick = { selectedTab = AppTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("الإعدادات") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                // Home stays composed the whole time, so the picked image and the
                // diacritized text are NOT lost when switching tabs.
                ArabicDiacritizerScreen()

                when (selectedTab) {
                    AppTab.HOME -> Unit
                    AppTab.HISTORY -> Surface(modifier = Modifier.fillMaxSize()) { PlaceholderScreen("السجل") }
                    AppTab.SETTINGS -> Surface(modifier = Modifier.fillMaxSize()) { PlaceholderScreen("الإعدادات") }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title - قريبًا", style = MaterialTheme.typography.titleMedium)
    }
}
'''
content = content.rstrip("\n") + "\n" + nav_code

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

if warnings:
    print("WARNING:")
    for w in warnings:
        print(" -", w)
else:
    print("Done: bottom navigation added")
