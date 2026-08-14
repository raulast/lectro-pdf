import os

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

imports = [
    "import androidx.compose.foundation.background",
    "import androidx.compose.foundation.border",
    "import androidx.compose.foundation.shape.CircleShape",
    "import androidx.compose.ui.graphics.toArgb",
    "import androidx.compose.foundation.text.KeyboardOptions",
    "import androidx.compose.ui.text.input.KeyboardType"
]

for imp in imports:
    if imp not in content:
        content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\n" + imp)

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
