package com.khridoapp.mithai;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class KeyGeneratorActivity extends Activity {

    // ⚠️ यहाँ अपनी असली Gmail ID दोबारा चेक कर लें भाई
    private static final String ADMIN_EMAIL = "your_google_email@gmail.com"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔒 सुरक्षा लॉक
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user == null || !user.getEmail().equalsIgnoreCase(ADMIN_EMAIL)) {
            Toast.makeText(this, "🔴 अनधिकृत पहुंच! आप एडमिन नहीं हैं।", Toast.LENGTH_LONG).show();
            finish(); 
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 60, 50, 50);
        layout.setBackgroundColor(0xFFF4F6F9);

        // 🔥 यहाँ सुंदर सा "क्लाउड एडमिन पैनल" टाइटिल सेट कर दिया है
        TextView tvTitle = new TextView(this);
        tvTitle.setText("👑 श्री मिष्ठान भंडार\n☁️ क्लाउड एडमिन पैनल");
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFF6200EE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 40);
        layout.addView(tvTitle);

        TextView tvAdminInfo = new TextView(this);
        tvAdminInfo.setText("🟢 वेरीफाइड एडमिन: " + user.getEmail());
        tvAdminInfo.setTextSize(13);
        tvAdminInfo.setTextColor(Color.parseColor("#4CAF50"));
        tvAdminInfo.setGravity(Gravity.CENTER);
        tvAdminInfo.setPadding(0, 0, 0, 30);
        layout.addView(tvAdminInfo);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("📱 ग्राहक की Device ID यहाँ पेस्ट करें:");
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(Color.BLACK);
        layout.addView(tvLabel);

        final EditText etDeviceIdInput = new EditText(this);
        etDeviceIdInput.setHint("उदा: ea3df55f0a5378ae");
        layout.addView(etDeviceIdInput);

        final TextView tvResultKey = new TextView(this);
        tvResultKey.setTextSize(16);
        tvResultKey.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvResultKey.setTextColor(Color.parseColor("#00C853"));
        tvResultKey.setGravity(Gravity.CENTER);
        tvResultKey.setPadding(0, 30, 0, 30);
        
        Button btnGenerate = new Button(this);
        btnGenerate.setText("⚡ लाइसेंस की (Key) बनाएं");
        btnGenerate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
        btnGenerate.setTextColor(Color.WHITE);
        layout.addView(btnGenerate);
        layout.addView(tvResultKey);

        final Button btnCopyKey = new Button(this);
        btnCopyKey.setText("📋 बनी हुई Key कॉपी करें");
        btnCopyKey.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
        btnCopyKey.setTextColor(Color.BLACK);
        btnCopyKey.setVisibility(View.GONE);
        layout.addView(btnCopyKey);

        setContentView(layout);

        btnGenerate.setOnClickListener(v -> {
            String dId = etDeviceIdInput.getText().toString().trim();
            if (dId.isEmpty()) {
                Toast.makeText(this, "पहले Device ID तो डालो भाई!", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dId.length(); i++) {
                sb.append((char) (dId.charAt(i) + 3));
            }
            String shiftedStr = sb.toString().toUpperCase();

            if (shiftedStr.length() > 2) {
                char first = shiftedStr.charAt(0);
                char last = shiftedStr.charAt(shiftedStr.length() - 1);
                shiftedStr = last + shiftedStr.substring(1, shiftedStr.length() - 1) + first;
            }

            final String finalGeneratedKey = "MITHAI-" + shiftedStr + "-" + (dId.length() * 7) + "-893";
            tvResultKey.setText("🔑 ग्राहक की लाइसेंस की:\n\n" + finalGeneratedKey);
            btnCopyKey.setVisibility(View.VISIBLE);

            btnCopyKey.setOnClickListener(v1 -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MithaiKey", finalGeneratedKey);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(KeyGeneratorActivity.this, "लाइसेंस की कॉपी हो गई! ✅", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
