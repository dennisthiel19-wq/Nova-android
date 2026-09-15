package de.thiel.nova;

import android.content.Context;
import java.util.Locale;

/** Stores the user's spoken names for the installed AI apps. */
public final class AssistantProfiles {
    private static final String[] APPS = {"ChatGPT", "Gemini", "Claude"};
    private static final String[] PACKAGES = {"com.openai.chatgpt", "com.google.android.apps.bard", "com.anthropic.claude"};
    private AssistantProfiles() { }
    public static String appName(int slot) { return APPS[slot]; }
    public static String packageName(int slot) { return PACKAGES[slot]; }
    public static String name(Context context, int slot) {
        return context.getSharedPreferences("nova", Context.MODE_PRIVATE).getString("assistant_name_" + slot, APPS[slot]);
    }
    public static void saveName(Context context, int slot, String name) {
        context.getSharedPreferences("nova", Context.MODE_PRIVATE).edit().putString("assistant_name_" + slot, name.trim()).apply();
    }
    public static int match(Context context, String spoken) {
        // Android may transcribe "ChatGPT" as "Chat GPT" or "Chat G P T".
        // Remove spaces and punctuation before comparing wake phrases.
        String text = normalise(spoken);
        for (int slot = 0; slot < APPS.length; slot++) {
            String name = normalise(name(context, slot));
            if (!name.isEmpty() && (text.contains("hey" + name) || text.contains("hallo" + name))) return slot;
        }
        return -1;
    }

    private static String normalise(String value) {
        return value.toLowerCase(Locale.GERMAN).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    public static void setLastPackage(Context context, String packageName) {
        context.getSharedPreferences("nova", Context.MODE_PRIVATE).edit().putString("last_assistant_package", packageName).apply();
    }
    public static String lastPackage(Context context) {
        return context.getSharedPreferences("nova", Context.MODE_PRIVATE).getString("last_assistant_package", PACKAGES[0]);
    }
}
