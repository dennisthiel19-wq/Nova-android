# NOVA 0.6

Erster Android-Prototyp für Samsung Galaxy mit Android 15.

## Testziel

1. App öffnen und Mikrofon erlauben.
2. **NOVA STARTEN** antippen.
3. Das Display darf danach gesperrt werden.
4. **„Hey NOVA“** sagen.
5. NOVA bleibt still, öffnet die KI zum gesprochenen Namen und startet dort den Sprachmodus.

Die Benachrichtigung zeigt an, dass der Mikrofon-Dienst aktiv ist. Diese Version nutzt bewusst keine API und kein Spotify. Du kannst drei Namen für ChatGPT, Gemini und Claude festlegen und dann zum Beispiel „Hey Lina“, „Hey Atlas“ oder „Hey Milo“ sagen. Unter **Automatischen Sprachstart aktivieren** muss einmalig NOVA – Sprachstart als Bedienungshilfe erlaubt werden. Der automatische Klick funktioniert nur, wenn die jeweilige KI-App ihren Sprachbutton für Android-Bedienungshilfen zugänglich macht.

## APK automatisch bauen

Das Projekt in ein privates GitHub-Repository hochladen. Unter **Actions → Build NOVA APK → Run workflow** starten. Nach Abschluss das Artefakt **NOVA-0.1-APK** herunterladen, ZIP öffnen und `app-debug.apk` auf dem Galaxy installieren.

Beim Installieren aus Chrome/Dateien muss Android einmalig **„Unbekannte Apps installieren“** erlauben.
