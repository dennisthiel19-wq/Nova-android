package de.thiel.nova;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 10;
    private TextView status;
    private Button toggle;
    private boolean running;
    private EditText[] assistantNames = new EditText[3];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        toggle = findViewById(R.id.toggle);
        assistantNames[0] = findViewById(R.id.chatgpt_name);
        assistantNames[1] = findViewById(R.id.gemini_name);
        assistantNames[2] = findViewById(R.id.claude_name);
        for (int i = 0; i < assistantNames.length; i++) assistantNames[i].setText(AssistantProfiles.name(this, i));
        findViewById(R.id.save_assistant).setOnClickListener(v -> {
            for (int i = 0; i < assistantNames.length; i++) {
                String name = assistantNames[i].getText().toString().trim();
                AssistantProfiles.saveName(this, i, name.isEmpty() ? AssistantProfiles.appName(i) : name);
            }
            status.setText("KI-Namen gespeichert – NOVA hört still zu.");
        });
        toggle.setOnClickListener(v -> {
            if (running) stopNova(); else requestAndStart();
        });
        findViewById(R.id.accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private void requestAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSIONS);
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
            }
            return;
        }
        startNova();
    }

    private void startNova() {
        startForegroundService(new Intent(this, WakeWordService.class));
        running = true;
        status.setText("NOVA hört still auf deine KI-Namen …");
        toggle.setText("NOVA STOPPEN");
    }

    private void stopNova() {
        stopService(new Intent(this, WakeWordService.class));
        running = false;
        status.setText("NOVA ist pausiert");
        toggle.setText("NOVA STARTEN");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startNova();
        else status.setText("Mikrofonfreigabe wird benötigt");
    }
}
