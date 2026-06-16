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

    // ⚠️ यहाँ अपनी वह Gmail ID डालें जिसे आप एडमिन बनाना चाहते हैं
    private static final String ADMIN_EMAIL = "vikarmsrkian6514@gmail.com"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔒 सुरक्षा: चेक करें कि क्या यूजर फायरबेस पर लॉग इन है?
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        // यह चेक करेगा कि यूजर लॉग इन है और क्या वही ईमेल है जो एडमिन की है
        if (user == null || !user.getEmail().equalsIgnoreCase(ADMIN_EMAIL)) {
            Toast.makeText(this, "🔴 अनधिकृत पहुंच! आप एडमिन नहीं हैं।", Toast.LENGTH_LONG).show();
            finish(); 
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 60, 50, 50);
        layout.setBackgroundColor(0xFFF4F6F9);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("👑 क्लाउड एडमिन पैनल\nलाइसेंस की जनरेटर");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFF6200EE);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 40);
        layout.addView(tvTitle);

        TextView tvAdminInfo = new TextView(this);
        tvAdminInfo.setText("🟢 एडमिन: " + user.getEmail());
        tvAdminInfo.setTextSize(12);
        tvAdminInfo.setTextColor(Color.GRAY);
        tvAdminInfo.setGravity(Gravity.CENTER);
        tvAdminInfo.setPadding(0, 0, 0, 20);
        layout.addView(tvAdminInfo);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("📱 ग्राहक की Device ID:");
        layout.addView(tvLabel);

        final EditText etDeviceIdInput = new EditText(this);
        etDeviceIdInput.setHint("उदा: ea3df55f0a5378ae");
        layout.addView(etDeviceIdInput);

        final TextView tvResultKey = new TextView(this);
        tvResultKey.setTextSize(15);
        tvResultKey.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvResultKey.setTextColor(Color.parseColor("#00C853"));
        tvResultKey.setGravity(Gravity.CENTER);
        tvResultKey.setPadding(0, 30, 0, 30);
        
        Button btnGenerate = new Button(this);
        btnGenerate.setText("⚡ लाइसेंस की जनरेट करें");
        btnGenerate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
        btnGenerate.setTextColor(Color.WHITE);
        layout.addView(btnGenerate);
        layout.addView(tvResultKey);

        final Button btnCopyKey = new Button(this);
        btnCopyKey.setText("📋 की (Key) कॉपी करें");
        btnCopyKey.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
        btnCopyKey.setVisibility(View.GONE);
        layout.addView(btnCopyKey);

        setContentView(layout);

        btnGenerate.setOnClickListener(v -> {
            String dId = etDeviceIdInput.getText().toString().trim();
            if (dId.isEmpty()) {
                Toast.makeText(this, "Device ID तो डालो भाई!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔐 क्रिप्टोग्राफिक शिफ्टिंग फार्मूला (जो आपके ऐप में है)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dId.length(); i++) {
                sb.append((char) (dId.charAt(i) + 3));
            }
            String shiftedStr = sb.toString().toUpperCase();

            if (shiftedStr.length() > 2) {
                char first = shiftedStr.charAt(0);
                char last = shiftedStr.charAt(shiftedStr.length() - 1);
                shiftedStr = last + shiftedStr.substring(1, shiftedStr.length() - 2) + first;
            }

            final String finalGeneratedKey = "MITHAI-" + shiftedStr + "-" + (dId.length() * 7) + "-893";
            tvResultKey.setText("🔑 जनरेटेड लाइसेंस की:\n\n" + finalGeneratedKey);
            btnCopyKey.setVisibility(View.VISIBLE);

            btnCopyKey.setOnClickListener(v1 -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MithaiKey", finalGeneratedKey);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(KeyGeneratorActivity.this, "की कॉपी हो गई! ✅", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
