# NOVA 0.1

Erster Android-Prototyp für Samsung Galaxy mit Android 15.

## Testziel

1. App öffnen und Mikrofon erlauben.
2. **NOVA STARTEN** antippen.
3. Das Display darf danach gesperrt werden.
4. **„Hey NOVA“** sagen.
5. NOVA antwortet: **„Ja, Dennis?“**

Die Benachrichtigung zeigt an, dass der Mikrofon-Dienst aktiv ist. Diese Version nutzt bewusst noch keine ChatGPT-API und kein Spotify. Sie testet nur Wakeword, Hintergrundbetrieb und Sprachausgabe.

## APK automatisch bauen

Das Projekt in ein privates GitHub-Repository hochladen. Unter **Actions → Build NOVA APK → Run workflow** starten. Nach Abschluss das Artefakt **NOVA-0.1-APK** herunterladen, ZIP öffnen und `app-debug.apk` auf dem Galaxy installieren.

Beim Installieren aus Chrome/Dateien muss Android einmalig **„Unbekannte Apps installieren“** erlauben.
