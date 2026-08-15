with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "r") as f:
    content = f.read()

# Replace the exact sequence
import re
content = re.sub(r"    \}\n    \}\n    private fun saveProgress", r"    }\n    private fun saveProgress", content)

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "w") as f:
    f.write(content)
