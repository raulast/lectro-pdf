with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

queries_block = """
    <queries>
        <intent>
            <action android:name="android.intent.action.TTS_SERVICE" />
        </intent>
    </queries>

    <application"""

content = content.replace("    <application", queries_block)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
