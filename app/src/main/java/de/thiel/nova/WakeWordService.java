package de.thiel.nova;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class WakeWordService extends Service implements RecognitionListener {
    private static final String CHANNEL = "nova_listening";
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean stopping;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(101, notification("NOVA hört auf „Hey NOVA“"));
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.GERMAN);
        });
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        listen();
    }

    private void listen() {
        if (!stopping) {
            try { recognizer.startListening(speechIntent); }
            catch (Exception ignored) { retry(900); }
        }
    }

    private void inspect(Bundle bundle) {
        if (bundle == null) return;
        ArrayList<String> lines = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (lines == null) return;
        for (String line : lines) {
            String heard = line.toLowerCase(Locale.GERMAN).trim();
            if (heard.contains("hey nova") || heard.contains("hallo nova")) {
                recognizer.cancel();
                wakeScreenAndOpenChatGpt();
                retry(1800);
                return;
            }
        }
    }

    /**
     * NOVA is only the silent wake layer. The actual conversation continues
     * in the user's installed ChatGPT app, with no API key required here.
     */
    private void wakeScreenAndOpenChatGpt() {
        try {
            PowerManager power = getSystemService(PowerManager.class);
            PowerManager.WakeLock wakeLock = power.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "NOVA:WakeScreen");
            wakeLock.acquire(2500);
        } catch (Exception ignored) {
            // NOVA still opens ChatGPT if a phone blocks programmatic wake-up.
        }

        handler.postDelayed(() -> {
            try {
                Intent chatGpt = getPackageManager().getLaunchIntentForPackage("com.openai.chatgpt");
                if (chatGpt != null) {
                    chatGpt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(chatGpt);
                } else {
                    Intent browser = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://chatgpt.com"));
                    browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(browser);
                }
            } catch (Exception ignored) {
                // The foreground notification remains available as a fallback.
            }
        }, 650);
    }

    private void retry(long delayMs) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::listen, delayMs);
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("NOVA 0.1")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL, "NOVA Spracherkennung", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        stopping = true;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    @Override public void onPartialResults(Bundle partialResults) { inspect(partialResults); }
    @Override public void onResults(Bundle results) { inspect(results); retry(400); }
    @Override public void onError(int error) { retry(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1200 : 450); }
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onEvent(int eventType, Bundle params) {}
}
