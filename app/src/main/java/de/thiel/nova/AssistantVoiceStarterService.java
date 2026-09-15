package de.thiel.nova;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
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
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null ||
                !selectedPackage().contentEquals(event.getPackageName()) ||
                System.currentTimeMillis() - lastClick < 2500) return;
        pressVoiceButton();
        // Check several times while ChatGPT draws its composer. The first
        // successful click wins, so this adds no waiting after it is ready.
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::pressVoiceButton, 120);
        handler.postDelayed(this::pressVoiceButton, 330);
        handler.postDelayed(this::pressVoiceButton, 680);
        handler.postDelayed(this::tapVoicePositionFallback, 950);
    }

    private void pressVoiceButton() {
        if (System.currentTimeMillis() - lastClick < 2500) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo voice = findVoiceButton(root);
        if (voice != null) {
            if (voice.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                lastClick = System.currentTimeMillis();
            voice.recycle();
        }
        if (root != null) root.recycle();
    }

    private String selectedPackage() {
        return AssistantProfiles.lastPackage(this);
    }

    /**
     * ChatGPT's voice icon is sometimes not exposed with a readable label.
     * On the Samsung prototype it is consistently the lower-right action.
     * This fallback is strictly limited to the selected assistant app.
     */
    private void tapVoicePositionFallback() {
        if (System.currentTimeMillis() - lastClick < 2500) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        boolean selectedAppVisible = root != null && root.getPackageName() != null &&
                selectedPackage().contentEquals(root.getPackageName());
        if (root != null) root.recycle();
        if (!selectedAppVisible) return;

        float x = getResources().getDisplayMetrics().widthPixels * 0.885f;
        float y = getResources().getDisplayMetrics().heightPixels * 0.850f;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription tap =
                new GestureDescription.StrokeDescription(path, 0, 60);
        if (dispatchGesture(new GestureDescription.Builder().addStroke(tap).build(), null, null))
            lastClick = System.currentTimeMillis();
    }

    private AccessibilityNodeInfo findVoiceButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence label = node.getContentDescription();
        if (label == null) label = node.getText();
        String text = label == null ? "" : label.toString().toLowerCase(Locale.GERMAN);
        String id = node.getViewIdResourceName();
        id = id == null ? "" : id.toLowerCase(Locale.ROOT);
        boolean looksLikeVoice = text.contains("voice") || text.contains("sprach") ||
                text.contains("unterhaltung") || text.contains("audio") ||
                id.contains("voice") || id.contains("audio") || id.contains("microphone");
        if (looksLikeVoice) {
            AccessibilityNodeInfo clickable = clickableParent(node);
            if (clickable != null) return clickable;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findVoiceButton(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo clickableParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        while (current != null) {
            if (current.isClickable()) return current;
            AccessibilityNodeInfo parent = current.getParent();
            current.recycle();
            current = parent;
        }
        return null;
    }

    @Override public void onInterrupt() { }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
