with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "r") as f:
    content = f.read()

# Replace "    }\n    }\n    private fun saveProgress() {" with "    }\n    private fun saveProgress() {"
content = content.replace("    }\n    }\n    private fun saveProgress() {", "    }\n    private fun saveProgress() {")

with open("app/src/main/java/com/example/ui/ReaderViewModel.kt", "w") as f:
    f.write(content)
