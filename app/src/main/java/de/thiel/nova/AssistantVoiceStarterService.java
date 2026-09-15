package de.thiel.nova;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/**
 * Optional, user-enabled bridge for the installed ChatGPT app. Android does
 * not expose a public "start voice chat" intent, so this only clicks a button
 * when its accessible label clearly identifies it as a voice button.
 */
public class AssistantVoiceStarterService extends AccessibilityService {
    private long lastClick;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null ||
                !selectedPackage().contentEquals(event.getPackageName()) ||
                System.currentTimeMillis() - lastClick < 2500) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo voice = findVoiceButton(root);
        if (voice != null) {
            lastClick = System.currentTimeMillis();
            voice.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            voice.recycle();
        }
        if (root != null) root.recycle();
    }

    private String selectedPackage() {
        return AssistantProfiles.lastPackage(this);
    }

    private AccessibilityNodeInfo findVoiceButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence label = node.getContentDescription();
        if (label == null) label = node.getText();
        String text = label == null ? "" : label.toString().toLowerCase(Locale.GERMAN);
        boolean looksLikeVoice = text.contains("voice") || text.contains("sprach") ||
                text.contains("unterhaltung") || text.contains("audio");
        if (looksLikeVoice && node.isClickable()) return AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findVoiceButton(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    @Override public void onInterrupt() { }
}
