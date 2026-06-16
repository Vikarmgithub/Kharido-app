package com.khridoapp.mithai;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static String selectedImageUriStr = ""; 
    private static ImageView previewImageView; 

    private boolean isHindi = true; 
    
    // लाइसेंस और फायरबेस ऑथ वेरिएबल्स
    private boolean isAppActivated = false;
    private String deviceId = "";
    private SharedPreferences activationPrefs;
    private FirebaseAuth mAuth;
    private static final String PREFS_NAME = "MithaiDeviceLockPrefs";
    private static final String KEY_IS_ACTIVATED = "IsAppFullyActivated";
    
    // ⚠️ अपनी असली एडमिन ईमेल आईडी यहाँ डालें (सिर्फ इसी ईमेल से जनरेटर खुलेगा)
    private static final String ADMIN_EMAIL = "vikarmsrkian6514@gmail.com";

    static class Product {
        String id, name, nameEn, imageUriStr; 
        double price;
        Product(String id, String name, String nameEn, double price, String imageUriStr) {
            this.id = id; this.name = name; this.nameEn = nameEn; this.price = price; this.imageUriStr = imageUriStr;
        }
    }

    static class OrderItem {
        String id, name, nameEn;
        double qty, price; 
        boolean isAdvance;
        OrderItem(String id, String name, String nameEn, double qty, double price, boolean isAdvance) {
            this.id = id; this.name = name; this.nameEn = nameEn; this.qty = qty; this.price = price;
            this.isAdvance = isAdvance;
        }
    }

    static class Order {
        String id, customerName, time, status; 
        String dateKey, monthKey; 
        long timestamp; 
        ArrayList<OrderItem> items;
        double total, received, due;
        Order(String id, String customerName, ArrayList<OrderItem> items, double total, double received, double due) {
            this.id = id; this.customerName = customerName; this.items = items;
            this.total = total; this.received = received; this.due = due;
            this.status = "Pending"; 
            Date currentDate = new Date();
            this.timestamp = currentDate.getTime(); 
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault());
            this.time = sdf.format(currentDate); 
            this.dateKey = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(currentDate);
            this.monthKey = new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(currentDate);
        }
    }

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private HashMap<String, Double> regularCart = new HashMap<>();
    private HashMap<String, Double> advanceCart = new HashMap<>();

    private ScrollView shopContainer, dashboardContainer;
    private LinearLayout shopProductList, dashDynamicContent, filterBarContainer, customDateRow;
    private Button btnShopView, btnDashView, btnFloatingCart, btnFromDate, btnToDate;
    private Button tabInventory, tabOrders, tabAnalytics;
    private ImageView btnSettings; 
    private EditText etSearchName;
    private Spinner spDateFilter;
    private String currentSubTab = "Inventory";
    private long fromTimestamp = 0;
    private long toTimestamp = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase Auth चालू करें
        mAuth = FirebaseAuth.getInstance();

        try {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) deviceId = "MITHAIPOS404";
        } catch (Exception e) { deviceId = "MITHAIPOS999"; }

        activationPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isAppActivated = activationPrefs.getBoolean(KEY_IS_ACTIVATED, false);

        shopContainer = findViewById(R.id.shopContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        shopProductList = findViewById(R.id.shopProductList);
        dashDynamicContent = findViewById(R.id.dashDynamicContent);
        filterBarContainer = findViewById(R.id.filterBarContainer);
        customDateRow = findViewById(R.id.customDateRow);
        
        btnShopView = findViewById(R.id.btnShopView);
        btnDashView = findViewById(R.id.btnDashView);
        btnFloatingCart = findViewById(R.id.btnFloatingCart);
        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);
        tabInventory = findViewById(R.id.tabInventory);
        tabOrders = findViewById(R.id.tabOrders);
        tabAnalytics = findViewById(R.id.tabAnalytics);
        etSearchName = findViewById(R.id.etSearchName);
        spDateFilter = findViewById(R.id.spDateFilter);
        btnSettings = findViewById(R.id.btnSettings);
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showLanguageSettingsDialog());
        }

        setupDateFilterSpinner();
        initSeedProducts();
        updateUI(); 

        if (!isAppActivated) {
            showActivationSystemDialog();
        }

        btnShopView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.VISIBLE); dashboardContainer.setVisibility(View.GONE); filterBarContainer.setVisibility(View.GONE);
            btnShopView.setBackgroundColor(0xFF6200EE); btnShopView.setTextColor(Color.WHITE); btnDashView.setBackgroundColor(Color.WHITE); btnDashView.setTextColor(0xFF6200EE);
            renderShop();
        });

        btnDashView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.GONE); dashboardContainer.setVisibility(View.VISIBLE);
            btnDashView.setBackgroundColor(0xFF6200EE); btnDashView.setTextColor(Color.WHITE); btnShopView.setBackgroundColor(Color.WHITE); btnShopView.setTextColor(0xFF6200EE);
            switchSubTab(tabInventory); renderInventoryTab();
        });

        tabInventory.setOnClickListener(v -> { switchSubTab(tabInventory); renderInventoryTab(); });
        tabOrders.setOnClickListener(v -> { switchSubTab(tabOrders); renderOrdersTab(); });
        tabAnalytics.setOnClickListener(v -> { switchSubTab(tabAnalytics); renderAnalyticsTab(); });
        
        etSearchName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { refreshCurrentReportTab(); }
        });

        spDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 3) customDateRow.setVisibility(View.VISIBLE);
                else { customDateRow.setVisibility(View.GONE); fromTimestamp = 0; toTimestamp = Long.MAX_VALUE; }
                refreshCurrentReportTab();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
        btnFloatingCart.setOnClickListener(v -> showCartDialog());
    }

    private boolean checkLicenseKey(String inputKey) {
        if (inputKey == null || inputKey.isEmpty() || !inputKey.startsWith("MITHAI-")) return false;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < deviceId.length(); i++) {
                char ch = deviceId.charAt(i);
                sb.append((char) (ch + 3));
            }
            String shiftedStr = sb.toString().toUpperCase();
            if (shiftedStr.length() > 2) {
                char first = shiftedStr.charAt(0); char last = shiftedStr.charAt(shiftedStr.length() - 1);
                shiftedStr = last + shiftedStr.substring(1, shiftedStr.length() - 1) + first;
            }
            String correctCalculatedKey = "MITHAI-" + shiftedStr + "-" + (deviceId.length() * 7) + "-893";
            return inputKey.equals(correctCalculatedKey);
        } catch (Exception e) { return false; }
    }

    // 📱 सुधरा हुआ एक्टिवेशन डायलॉग जिसमें "ID कॉपी करें" बटन वापस जोड़ दिया है
    private void showActivationSystemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "🔑 सुरक्षित डिवाइस लाइसेंस एक्टिवーション" : "🔑 Device Locked Activation");
        builder.setCancelable(false); 
        
        LinearLayout layout = new LinearLayout(this); 
        layout.setOrientation(LinearLayout.VERTICAL); 
        layout.setPadding(40, 30, 40, 30);
        
        TextView tvMsg = new TextView(this); 
        tvMsg.setText(isHindi ? "यह ऐप लॉक है। नीचे दी गई ID कॉपी करके एडमिन को भेजें:" : "Copy Device ID and send to admin:"); 
        tvMsg.setTextSize(13); 
        layout.addView(tvMsg);
        
        TextView tvIdDisplay = new TextView(this); 
        tvIdDisplay.setText("\n🆔 Device ID: " + deviceId + "\n"); 
        tvIdDisplay.setTextSize(16); 
        tvIdDisplay.setTypeface(null, Typeface.BOLD); 
        tvIdDisplay.setTextColor(Color.RED); 
        tvIdDisplay.setGravity(Gravity.CENTER); 
        layout.addView(tvIdDisplay);

        // 🔥 "ID कॉपी करें" बटन लॉजिक के साथ
        Button btnCopyId = new Button(this);
        btnCopyId.setText(isHindi ? "📋 ID कॉपी करें" : "📋 Copy Device ID");
        btnCopyId.setBackgroundColor(0xFF03DAC5);
        btnCopyId.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-2, -2);
        btnParams.gravity = Gravity.CENTER;
        btnParams.bottomMargin = 30;
        btnCopyId.setLayoutParams(btnParams);
        layout.addView(btnCopyId);

        btnCopyId.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("MithaiDeviceID", deviceId);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, isHindi ? "Device ID कॉपी हो गई! ✅" : "Device ID Copied! ✅", Toast.LENGTH_SHORT).show();
            }
        });

        final EditText etKeyInput = new EditText(this); 
        etKeyInput.setHint(isHindi ? "लाइसेंस की यहाँ डालें..." : "Enter license key..."); 
        etKeyInput.setGravity(Gravity.CENTER); 
        layout.addView(etKeyInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton(isHindi ? "🔓 एक्टिवेट" : "Activate", (dialog, which) -> {
            String enteredKey = etKeyInput.getText().toString().trim().toUpperCase();
            if (checkLicenseKey(enteredKey)) {
                SharedPreferences.Editor editor = activationPrefs.edit(); 
                editor.putBoolean(KEY_IS_ACTIVATED, true); 
                editor.apply();
                isAppActivated = true; 
                updateUI();
            } else { 
                isAppActivated = false; 
                Toast.makeText(MainActivity.this, isHindi ? "❌ गलत लाइसेंस की!" : "❌ Invalid Key!", Toast.LENGTH_SHORT).show();
                showActivationSystemDialog(); 
            }
        });
        
        builder.setNegativeButton(isHindi ? "👀 डेमो" : "Demo", (dialog, which) -> isAppActivated = false);
        builder.show();
    }

    // 🔒 फायरबेस ऑनलाइन एडमिन गेटवे लॉगिन
    private void verifyAdminAndOpenGenerator() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser != null && currentUser.getEmail() != null && currentUser.getEmail().equalsIgnoreCase(ADMIN_EMAIL)) {
            Intent intent = new Intent(MainActivity.this, KeyGeneratorActivity.class);
            startActivity(intent);
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("🔐 Firebase Admin Verification");
        b.setMessage("सुरक्षित रिमोट एक्सेस के लिए अपने क्रेडेंशियल से लॉगिन करें (Internet Required):");
        
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40, 20, 40, 20);
        final EditText etEmail = new EditText(this); etEmail.setHint("Admin Email"); root.addView(etEmail);
        final EditText etPass = new EditText(this); etPass.setHint("Password"); etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(etPass);
        b.setView(root);

        b.setPositiveButton("Verify & Login", (dialog, which) -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            
            if(!email.equalsIgnoreCase(ADMIN_EMAIL)) {
                Toast.makeText(MainActivity.this, "❌ एक्सेस अस्वीकृत! आप एडमिन नहीं हैं।", Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(MainActivity.this, "Connecting to Firebase...", Toast.LENGTH_SHORT).show();
            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "✅ एडमिन वेरिफाइड!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, KeyGeneratorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "❌ लॉगिन फेल! पासवर्ड चेक करें या इंटरनेट ऑन करें।", Toast.LENGTH_LONG).show();
                }
            });
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void showLanguageSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "⚙️ सेटिंग (Settings)" : "⚙️ Settings");
        String[] options = isHindi ? new String[]{"English भाषा", "🔑 लाइसेंस पैनल", "🛠️ एडमिन पैनल (🔒 Firebase Admin)"} 
                               : new String[]{"Switch to Hindi", "License Panel", "Admin Panel (🔒 Firebase Admin)"};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { isHindi = !isHindi; updateUI(); }
            else if (which == 1) { showActivationSystemDialog(); }
            else { verifyAdminAndOpenGenerator(); } 
        });
        builder.show();
    }

    private void setupDateFilterSpinner() {
        String[] filterOptions = isHindi ? new String[]{"सभी ऑर्डर्स", "आज के", "इस महीने के", "📅 CUSTOM तारीख"} : new String[]{"All Orders", "Today", "This Month", "📅 Custom Range"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterOptions); spDateFilter.setAdapter(filterAdapter);
    }

    private void showDatePicker(final boolean isFrom) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this, (v, y, m, day) -> {
            Calendar p = Calendar.getInstance(); p.set(y, m, day);
            if (isFrom) { p.set(Calendar.HOUR_OF_DAY, 0); fromTimestamp = p.getTimeInMillis(); btnFromDate.setText("From: " + day); }
            else { p.set(Calendar.HOUR_OF_DAY, 23); toTimestamp = p.getTimeInMillis(); btnToDate.setText("To: " + day); }
            refreshCurrentReportTab();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)); d.show();
    }

    private void updateUI() {
        if (isHindi) {
            btnShopView.setText("🏪 दुकान काउंटर"); btnDashView.setText("📊 डैशबोर्ड"); tabInventory.setText("📦 स्टॉक माल"); tabOrders.setText("🧾 उधारी सूची"); tabAnalytics.setText("📈 क्लोजिंग रिपोर्ट"); etSearchName.setHint("🔍 ग्राहक खोजें...");
        } else {
            btnShopView.setText("🏪 Shop Counter"); btnDashView.setText("📊 Dashboard"); tabInventory.setText("📦 Inventory"); tabOrders.setText("🧾 Udhar Orders"); tabAnalytics.setText("📈 Reports"); etSearchName.setHint("🔍 Search Customer...");
        }
        setupDateFilterSpinner(); updateCartButton(); renderShop();
    }

    private void updateCartButton() { btnFloatingCart.setText("🛒 कार्ट (" + (regularCart.size() + advanceCart.size()) + ")"); }
    private void switchSubTab(Button t) { tabInventory.setBackgroundColor(0xFFE0E0E0); tabOrders.setBackgroundColor(0xFFE0E0E0); tabAnalytics.setBackgroundColor(0xFFE0E0E0); t.setBackgroundColor(0xFF03DAC5); filterBarContainer.setVisibility(t == tabInventory ? View.GONE : View.VISIBLE); }
    private void refreshCurrentReportTab() { if (currentSubTab.equals("Orders")) renderOrdersTab(); else if (currentSubTab.equals("Analytics")) renderAnalyticsTab(); }
    private boolean shouldShowOrder(Order o) { String s = etSearchName.getText().toString().trim().toLowerCase(); if (!s.isEmpty() && !o.customerName.toLowerCase().contains(s)) return false; int p = spDateFilter.getSelectedItemPosition(); if (p == 1 && !o.dateKey.equals(new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date()))) return false; if (p == 2 && !o.monthKey.equals(new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(new Date()))) return false; if (p == 3 && (o.timestamp < fromTimestamp || o.timestamp > toTimestamp)) return false; return true; }
    private void initSeedProducts() { if (products.isEmpty()) { products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, "")); products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, "")); products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, "")); } }

    private void renderShop() {
        shopProductList.removeAllViews();
        for (final Product p : products) {
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, 6, 0, 16); card.setLayoutParams(cp); card.setBackgroundColor(Color.WHITE); card.setPadding(24, 24, 24, 24); card.setElevation(3f);
            LinearLayout hr = new LinearLayout(this); ImageView iv = new ImageView(this); iv.setLayoutParams(new LinearLayout.LayoutParams(120, 120)); if (!p.imageUriStr.isEmpty()) iv.setImageURI(Uri.parse(p.imageUriStr)); else iv.setImageResource(android.R.drawable.ic_menu_gallery); hr.addView(iv);
            LinearLayout nc = new LinearLayout(this); nc.setOrientation(LinearLayout.VERTICAL); TextView tn = new TextView(this); tn.setText(isHindi ? p.name : p.nameEn); tn.setTextSize(18); tn.setTypeface(null, Typeface.BOLD); nc.addView(tn); hr.addView(nc); card.addView(hr);
            LinearLayout ic = new LinearLayout(this); final EditText ek = new EditText(this); ek.setHint("0.00"); ek.setInputType(8194); ic.addView(ek); final EditText er = new EditText(this); er.setHint("₹"); er.setInputType(8194); ic.addView(er); card.addView(ic);
            LinearLayout ar = new LinearLayout(this); Button br = new Button(this); br.setText("🛒 काउंटर"); br.setOnClickListener(v -> { if(!ek.getText().toString().isEmpty()) { regularCart.put(p.id, Double.parseDouble(ek.getText().toString())); updateCartButton(); ek.setText(""); er.setText(""); } }); ar.addView(br); card.addView(ar); shopProductList.addView(card);
        }
    }

    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews(); Button ba = new Button(this); ba.setText("➕ नई मिठाई जोड़ें"); ba.setOnClickListener(v -> showAddProductDialog()); dashDynamicContent.addView(ba);
        for (final Product p : products) { LinearLayout r = new LinearLayout(this); TextView tv = new TextView(this); tv.setText(p.name + " - ₹" + p.price); r.addView(tv); dashDynamicContent.addView(r); }
    }

    private void showAddProductDialog() {
        if (!isAppActivated) { showActivationSystemDialog(); return; }
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle("➕ Add Product"); final EditText en = new EditText(this); b.setView(en); b.setPositiveButton("Save", (d, w) -> { products.add(new Product("p"+(products.size()+1), en.getText().toString(), en.getText().toString(), 350, "")); renderInventoryTab(); }); b.show();
    }

    private void showCartDialog() {
        if (!isAppActivated) { showActivationSystemDialog(); return; }
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle("FINAL BILL"); LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); final EditText en = new EditText(this); en.setHint("Customer Name"); l.addView(en); b.setView(l);
        b.setPositiveButton("Done", (d, w) -> { orders.add(new Order("ORD"+System.currentTimeMillis(), en.getText().toString(), new ArrayList<>(), 500, 500, 0)); regularCart.clear(); advanceCart.clear(); updateCartButton(); }); b.show();
    }

    private void renderOrdersTab() { dashDynamicContent.removeAllViews(); for(Order o : orders) { if(!shouldShowOrder(o)) continue; TextView tv = new TextView(this); tv.setText(o.customerName + " - ₹" + o.total); dashDynamicContent.addView(tv); } }
    private void renderAnalyticsTab() { dashDynamicContent.removeAllViews(); TextView tv = new TextView(this); tv.setText("📊 Sales Summary Reports"); dashDynamicContent.addView(tv); }
}
