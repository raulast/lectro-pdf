with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.foundation.clickable" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.clickable")

with open("app/src/main/java/com/example/ui/ReaderScreen.kt", "w") as f:
    f.write(content)
