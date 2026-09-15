package de.thiel.nova;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.speech.*;
import java.util.*;

public class WakeWordService extends Service implements RecognitionListener {
    private static final String CHANNEL = "nova_listening";
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean stopping;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(101, notification("NOVA hört auf „Hey NOVA“"));
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
            int assistantSlot = AssistantProfiles.match(this, heard);
            if (assistantSlot >= 0) {
                recognizer.cancel();
                AssistantProfiles.setLastPackage(this, AssistantProfiles.packageName(assistantSlot));
                wakeScreenAndOpenAssistant();
                // NOVA stays silent. The Accessibility Service starts the
                // assistant's own voice control once its screen is visible.
                // Do not immediately take the microphone back while the
                // assistant is bringing up its live conversation.
                retry(10000);
                return;
            }
        }
    }

    /**
     * NOVA only handles the wake word. The user's installed ChatGPT app handles
     * the conversation, without an API key and without a NOVA greeting.
     */
    private void wakeScreenAndOpenAssistant() {
        Intent wake = new Intent(this, WakeActivity.class);
        wake.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(wake); }
        catch (Exception ignored) { /* The notification action remains available. */ }
    }

    private void retry(long delayMs) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::listen, delayMs);
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent openAssistant = new Intent(this, WakeActivity.class);
        PendingIntent assistantPending = PendingIntent.getActivity(this, 1, openAssistant,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("NOVA 0.3")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pending)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_play, "KI öffnen", assistantPending).build())
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
