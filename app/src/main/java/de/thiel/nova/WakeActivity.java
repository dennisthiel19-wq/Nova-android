package de.thiel.nova;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** A short visible bridge: wake the display first, then hand over to ChatGPT. */
public class WakeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        new Handler(Looper.getMainLooper()).postDelayed(this::openChatGpt, 300);
    }

    private void openChatGpt() {
        try {
            Intent chatGpt = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
            if (chatGpt != null) {
                chatGpt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(chatGpt);
            } else {
                Intent browser = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://chatgpt.com"));
                startActivity(browser);
            }
        } catch (Exception ignored) {
            // The foreground notification exposes the same handover as a fallback.
        }
        finish();
    }
}
