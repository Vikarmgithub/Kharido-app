package com.khridoapp.mithai;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ClipboardManager; // क्लिपबोर्ड के लिए इम्पोर्ट
import android.content.ClipData;         // क्लिपबोर्ड डेटा के लिए इम्पोर्ट
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static String selectedImageUriStr = ""; 
    private static ImageView previewImageView; 

    private boolean isHindi = true; 
    
    // एक्टिवेशन और डिवाइस बाइंडिंग वेरिएबल्स
    private boolean isAppActivated = false;
    private String deviceId = "";
    private SharedPreferences activationPrefs;
    private static final String PREFS_NAME = "MithaiDeviceLockPrefs";
    private static final String KEY_IS_ACTIVATED = "IsAppFullyActivated";

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

        try {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "MITHAIPOS404";
            }
        } catch (Exception e) {
            deviceId = "MITHAIPOS999";
        }

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
            shopContainer.setVisibility(View.VISIBLE);
            dashboardContainer.setVisibility(View.GONE);
            filterBarContainer.setVisibility(View.GONE);
            btnShopView.setBackgroundColor(0xFF6200EE);
            btnShopView.setTextColor(Color.WHITE);
            btnDashView.setBackgroundColor(Color.WHITE);
            btnDashView.setTextColor(0xFF6200EE);
            renderShop();
        });

        btnDashView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.GONE);
            dashboardContainer.setVisibility(View.VISIBLE);
            btnDashView.setBackgroundColor(0xFF6200EE);
            btnDashView.setTextColor(Color.WHITE);
            btnShopView.setBackgroundColor(Color.WHITE);
            btnShopView.setTextColor(0xFF6200EE);
            switchSubTab(tabInventory);
            renderInventoryTab();
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
                if (position == 3) {
                    customDateRow.setVisibility(View.VISIBLE);
                } else {
                    customDateRow.setVisibility(View.GONE);
                    fromTimestamp = 0;
                    toTimestamp = Long.MAX_VALUE;
                }
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
                if (Character.isLetterOrDigit(ch)) {
                    sb.append((char) (ch + 3));
                } else {
                    sb.append(ch);
                }
            }
            String shiftedStr = sb.toString().toUpperCase();

            if (shiftedStr.length() > 2) {
                char first = shiftedStr.charAt(0);
                char last = shiftedStr.charAt(shiftedStr.length() - 1);
                shiftedStr = last + shiftedStr.substring(1, shiftedStr.length() - 1) + first;
            }

            String correctCalculatedKey = "MITHAI-" + shiftedStr + "-" + (deviceId.length() * 7) + "-893";
            
            return inputKey.equals(correctCalculatedKey);
        } catch (Exception e) {
            return false;
        }
    }

    // 🔒 कॉपी बटन के साथ नया एक्टिवेशन डायलॉग
    private void showActivationSystemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "🔑 सुरक्षित डिवाइस लाइसेंस एक्टिवेशन" : "🔑 Device Locked Activation");
        builder.setCancelable(false); 

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(isHindi ? "यह ऐप आपके हार्डवेयर से लॉक है। नीचे दी गई 'Device ID' कॉपी करके एडमिन को भेजें और लाइसेंस की प्राप्त करें:" 
                             : "This software is tied to your hardware. Copy the 'Device ID' below and send it to the admin to get your key:");
        tvMsg.setTextSize(13);
        layout.addView(tvMsg);

        TextView tvIdDisplay = new TextView(this);
        tvIdDisplay.setText("\n🆔 Device ID: " + deviceId);
        tvIdDisplay.setTextSize(15);
        tvIdDisplay.setTypeface(null, Typeface.BOLD);
        tvIdDisplay.setTextColor(Color.RED);
        tvIdDisplay.setGravity(Gravity.CENTER);
        layout.addView(tvIdDisplay);

        // 🔥 📋 कॉपी आईडी बटन जोड़ा गया
        Button btnCopyId = new Button(this);
        btnCopyId.setText(isHindi ? "📋 डिवाइस आईडी कॉपी करें" : "📋 Copy Device ID");
        btnCopyId.setTextSize(12);
        btnCopyId.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
        btnCopyId.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams btnCopyLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnCopyLp.gravity = Gravity.CENTER;
        btnCopyLp.setMargins(0, 8, 0, 24);
        btnCopyId.setLayoutParams(btnCopyLp);
        
        btnCopyId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("DeviceID", deviceId);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, isHindi ? "आईडी क्लिपबोर्ड पर कॉपी हो गई! ✅" : "Device ID Copied! ✅", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnCopyId);

        final EditText etKeyInput = new EditText(this);
        etKeyInput.setHint(isHindi ? "कॉम्प्लेक्स लाइसेंस की यहाँ डालें..." : "Enter complex license key...");
        etKeyInput.setGravity(Gravity.CENTER);
        etKeyInput.setTypeface(Typeface.MONOSPACE);
        layout.addView(etKeyInput);

        builder.setView(layout);

        builder.setPositiveButton(isHindi ? "🔓 एक्टिवेट करें" : "🔒 Activate", (dialog, which) -> {
            String enteredKey = etKeyInput.getText().toString().trim().toUpperCase();
            
            if (checkLicenseKey(enteredKey)) {
                SharedPreferences.Editor editor = activationPrefs.edit();
                editor.putBoolean(KEY_IS_ACTIVATED, true);
                editor.apply();
                
                isAppActivated = true;
                Toast.makeText(MainActivity.this, isHindi ? "🎉 बधाई हो! डिवाइस एक्टिवेट हो गया।" : "🎉 License Verified! Activated.", Toast.LENGTH_LONG).show();
                updateUI();
            } else {
                Toast.makeText(MainActivity.this, isHindi ? "❌ गलत लाइसेंस की! ऐप सिर्फ डेमो मोड में रहेगा।" : "❌ Incorrect License Key! Active in demo mode.", Toast.LENGTH_LONG).show();
                isAppActivated = false;
                showActivationSystemDialog(); 
            }
        });

        builder.setNegativeButton(isHindi ? "👀 डेमो देखें" : "👀 View Demo", (dialog, which) -> {
            isAppActivated = false;
            Toast.makeText(MainActivity.this, isHindi ? "⚠️ डेमो मोड चालू: बिल सेविंग लॉक्ड है।" : "⚠️ Demo Mode Enabled: Data saving is locked.", Toast.LENGTH_LONG).show();
        });

        builder.show();
    }

    private void setupDateFilterSpinner() {
        String[] filterOptions = isHindi ? 
                new String[]{"सभी ऑर्डर्स", "आज के", "इस महीने के", "📅 कस्टम तारीख (Range)"} : 
                new String[]{"All Orders", "Today", "This Month", "📅 Custom Range"};
                
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterOptions);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDateFilter.setAdapter(filterAdapter);
    }

    private void showDatePicker(final boolean isFrom) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar pickedCal = Calendar.getInstance();
            pickedCal.set(selectedYear, selectedMonth, selectedDay);
            
            if (isFrom) {
                pickedCal.set(Calendar.HOUR_OF_DAY, 0); pickedCal.set(Calendar.MINUTE, 0); pickedCal.set(Calendar.SECOND, 0);
                fromTimestamp = pickedCal.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
                btnFromDate.setText((isHindi ? "से: " : "From: ") + sdf.format(pickedCal.getTime()));
            } else {
                pickedCal.set(Calendar.HOUR_OF_DAY, 23); pickedCal.set(Calendar.MINUTE, 59); pickedCal.set(Calendar.SECOND, 59);
                toTimestamp = pickedCal.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
                btnToDate.setText((isHindi ? "तक: " : "To: ") + sdf.format(pickedCal.getTime()));
            }
            refreshCurrentReportTab(); 
        }, year, month, day);
        dialog.show();
    }

    private void showLanguageSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "⚙️ सेटिंग (Settings)" : "⚙️ Settings");
        
        String activationLabel = isAppActivated ? (isHindi ? "\n\n🔰 लाइसेंस स्थिति: ✅ डिवाइस एक्टिवेटेड" : "\n\n🔰 License Status: ✅ Device Activated") 
                                                : (isHindi ? "\n\n🔰 स्थिति: ⚠️ डेमो मोड (लाइसेंस पेंडिंग)" : "\n\n🔰 Status: ⚠️ Demo Mode (Unlicensed)");

        String[] options = isHindi ? new String[]{"English भाषा चुनें", "🔑 लाइसेंस एक्टिवेशन पैनल" + activationLabel} 
                                   : new String[]{"Switch to Hindi (हिंदी)", "🔑 License Activation Panel" + activationLabel};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                isHindi = !isHindi;
                updateUI();
            } else {
                showActivationSystemDialog();
            }
        });
        builder.show();
    }

    private void updateUI() {
        if (isHindi) {
            btnShopView.setText("🏪 दुकान काउंटर"); btnDashView.setText("📊 डैशबोर्ड");
            tabInventory.setText("📦 मिठाई लिस्ट &amp; रेट"); tabOrders.setText("🧾 उधारी सूची"); tabAnalytics.setText("📈 क्लोजिंग रिपोर्ट");
            etSearchName.setHint("🔍 ग्राहक के नाम से खोजें..."); btnFromDate.setText("से: चुनें"); btnToDate.setText("तक: चुनें");
        } else {
            btnShopView.setText("🏪 Shop Counter"); btnDashView.setText("📊 Dashboard");
            tabInventory.setText("📦 Inventory"); tabOrders.setText("🧾 Udhar Orders"); tabAnalytics.setText("📈 Reports");
            etSearchName.setHint("🔍 Search by Customer Name..."); btnFromDate.setText("From: Select"); btnToDate.setText("To: Select");
        }
        setupDateFilterSpinner();
        updateCartButton();
        renderShop();
        if (dashboardContainer.getVisibility() == View.VISIBLE) {
            refreshCurrentReportTab();
            if (currentSubTab.equals("Inventory")) renderInventoryTab();
        }
    }

    private void updateCartButton() {
        int totalItems = regularCart.size() + advanceCart.size();
        btnFloatingCart.setText(isHindi ? ("🛒 कार्ट (" + totalItems + ")") : ("🛒 Cart (" + totalItems + ")"));
    }

    private void switchSubTab(Button activeTab) {
        tabInventory.setBackgroundColor(0xFFE0E0E0);
        tabOrders.setBackgroundColor(0xFFE0E0E0);
        tabAnalytics.setBackgroundColor(0xFFE0E0E0);
        activeTab.setBackgroundColor(0xFF03DAC5);
        
        if (activeTab == tabInventory) {
            currentSubTab = "Inventory"; filterBarContainer.setVisibility(View.GONE);
        } else if (activeTab == tabOrders) {
            currentSubTab = "Orders"; filterBarContainer.setVisibility(View.VISIBLE);
        } else {
            currentSubTab = "Analytics"; filterBarContainer.setVisibility(View.VISIBLE);
        }
    }

    private void refreshCurrentReportTab() {
        if (currentSubTab.equals("Orders")) {
            renderOrdersTab();
        } else if (currentSubTab.equals("Analytics")) {
            renderAnalyticsTab();
        }
    }

    private boolean shouldShowOrder(Order o) {
        String searchTxt = etSearchName.getText().toString().trim().toLowerCase();
        if (!searchTxt.isEmpty() && !o.customerName.toLowerCase().contains(searchTxt)) {
            return false; 
        }

        int position = spDateFilter.getSelectedItemPosition();
        String todayKey = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
        String thisMonthKey = new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(new Date());

        if (position == 1 && !o.dateKey.equals(todayKey)) return false;
        if (position == 2 && !o.monthKey.equals(thisMonthKey)) return false;
        if (position == 3 && (o.timestamp < fromTimestamp || o.timestamp > toTimestamp)) return false;

        return true;
    }

    private void initSeedProducts() {
        if (products.isEmpty()) {
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, ""));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, ""));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, ""));
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImageUriStr = imageUri.toString(); 
            if (previewImageView != null) {
                previewImageView.setImageURI(imageUri);
                previewImageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void renderShop() {
        shopProductList.removeAllViews();
        for (final Product p : products) {
            
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 6, 0, 16); card.setLayoutParams(cardParams);
            card.setBackgroundColor(Color.WHITE); card.setPadding(24, 24, 24, 24); card.setElevation(3f);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL); headerRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView ivMithai = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(120, 120); imgLp.setMargins(0, 0, 16, 0);
            ivMithai.setLayoutParams(imgLp); ivMithai.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) {
                ivMithai.setImageURI(Uri.parse(p.imageUriStr));
            } else {
                ivMithai.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            headerRow.addView(ivMithai);

            LinearLayout nameContainer = new LinearLayout(this); nameContainer.setOrientation(LinearLayout.VERTICAL);
            TextView tvName = new TextView(this); tvName.setText(isHindi ? p.name : p.nameEn); tvName.setTextSize(18); tvName.setTypeface(null, Typeface.BOLD); tvName.setTextColor(0xFF212121); nameContainer.addView(tvName);
            TextView tvPrice = new TextView(this); tvPrice.setText(isHindi ? ("भाव: ₹" + p.price + "/kg") : ("Rate: ₹" + p.price + "/kg")); tvPrice.setTextColor(0xFF757575); tvPrice.setTextSize(13); nameContainer.addView(tvPrice);

            headerRow.addView(nameContainer); card.addView(headerRow);

            LinearLayout inputContainer = new LinearLayout(this); inputContainer.setOrientation(LinearLayout.HORIZONTAL); inputContainer.setPadding(0, 12, 0, 12);
            LinearLayout qtyBox = new LinearLayout(this); qtyBox.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpQty = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f); lpQty.setMargins(0, 0, 12, 0); qtyBox.setLayoutParams(lpQty);

            LinearLayout spinnerRow = new LinearLayout(this); spinnerRow.setOrientation(LinearLayout.HORIZONTAL);
            final EditText etKg = new EditText(this); etKg.setHint("0.00"); etKg.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); etKg.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); spinnerRow.addView(etKg);

            final Spinner spUnit = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"kg", "gm"}); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spUnit.setAdapter(adapter); spinnerRow.addView(spUnit); qtyBox.addView(spinnerRow);

            TextView lblKg = new TextView(this); lblKg.setText(isHindi ? "⚖️ वजन मात्रा" : "⚖️ Weight Qty"); lblKg.setTextSize(11); lblKg.setTextColor(0xFF757575); qtyBox.addView(lblKg); inputContainer.addView(qtyBox);

            LinearLayout rsBox = new LinearLayout(this); rsBox.setOrientation(LinearLayout.VERTICAL); rsBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            final EditText etRs = new EditText(this); etRs.setHint(isHindi ? "₹ रुपये" : "₹ Cash"); etRs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); rsBox.addView(etRs);
            TextView lblRs = new TextView(this); lblRs.setText(isHindi ? "💰 कुल कैश" : "💰 Total Price"); lblRs.setTextSize(11); lblRs.setTextColor(0xFF757575); rsBox.addView(lblRs); inputContainer.addView(rsBox);

            card.addView(inputContainer);

            etKg.addTextChangedListener(new TextWatcher() {
                private boolean isChanging = false;
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isChanging || !etKg.hasFocus()) return;
                    isChanging = true;
                    try {
                        double val = Double.parseDouble(s.toString());
                        double finalKg = spUnit.getSelectedItem().toString().equals("gm") ? (val / 1000.0) : val;
                        etRs.setText(String.format("%.2f", finalKg * p.price));
                    } catch (Exception e) { etRs.setText(""); }
                    isChanging = false;
                }
            });

            etRs.addTextChangedListener(new TextWatcher() {
                private boolean isChanging = false;
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isChanging || !etRs.hasFocus()) return;
                    isChanging = true;
                    try {
                        double cash = Double.parseDouble(s.toString());
                        double computedKg = cash / p.price;
                        if (spUnit.getSelectedItem().toString().equals("gm")) {
                            etKg.setText(String.format("%.0f", computedKg * 1000.0));
                        } else {
                            etKg.setText(String.format("%.3f", computedKg));
                        }
                    } catch (Exception e) { etKg.setText(""); }
                    isChanging = false;
                }
            });

            LinearLayout actionRow = new LinearLayout(this); actionRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnAddRegular = new Button(this); btnAddRegular.setText(isHindi ? "🛒 काउंटर कार्ट" : "🛒 Regular Cart"); btnAddRegular.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE)); btnAddRegular.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lpBtn1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lpBtn1.setMargins(0, 0, 8, 0); btnAddRegular.setLayoutParams(lpBtn1);
            btnAddRegular.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double qtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    double current = regularCart.containsKey(p.id) ? regularCart.get(p.id) : 0;
                    regularCart.put(p.id, current + qtyInKg);
                    Toast.makeText(MainActivity.this, (isHindi ? p.name : p.nameEn) + " Cart Done!", Toast.LENGTH_SHORT).show();
                    etKg.setText(""); etRs.setText(""); updateCartButton();
                }
            });
            actionRow.addView(btnAddRegular);

            Button btnAddAdvance = new Button(this); btnAddAdvance.setText(isHindi ? "📋 एडवांस Book" : "📋 Book Advance"); btnAddAdvance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800)); btnAddAdvance.setTextColor(Color.WHITE);
            btnAddAdvance.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            btnAddAdvance.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double qtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    double current = advanceCart.containsKey(p.id) ? advanceCart.get(p.id) : 0;
                    advanceCart.put(p.id, current + qtyInKg);
                    Toast.makeText(MainActivity.this, (isHindi ? p.name : p.nameEn) + " Advance Booked!", Toast.LENGTH_SHORT).show();
                    etKg.setText(""); etRs.setText(""); updateCartButton();
                }
            });
            actionRow.addView(btnAddAdvance); card.addView(actionRow); shopProductList.addView(card);
        }
    }

    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();

        Button btnAddNewProduct = new Button(this); btnAddNewProduct.setText(isHindi ? "➕ नई मिठाई दुकान में जोड़ें" : "➕ Add New Item");
        btnAddNewProduct.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE)); btnAddNewProduct.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); btnLp.setMargins(0, 0, 0, 16); btnAddNewProduct.setLayoutParams(btnLp);
        btnAddNewProduct.setOnClickListener(v -> showAddProductDialog()); dashDynamicContent.addView(btnAddNewProduct);

        for (final Product p : products) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(16, 16, 16, 16); row.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 4, 0, 8); row.setLayoutParams(lp);

            ImageView ivThumb = new ImageView(this); LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(80, 80); tLp.setMargins(0,0,12,0); ivThumb.setLayoutParams(tLp);
            if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) ivThumb.setImageURI(Uri.parse(p.imageUriStr)); else ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            row.addView(ivThumb);

            TextView tvInfo = new TextView(this); tvInfo.setText(isHindi ? (p.name + "\nभाव: ₹" + p.price + "/kg") : (p.nameEn + "\nRate: ₹" + p.price + "/kg")); tvInfo.setTextSize(16); tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); row.addView(tvInfo);

            Button btnEditProduct = new Button(this); btnEditProduct.setText(isHindi ? "✏️ सुधारें" : "✏️ Edit"); btnEditProduct.setTextSize(12); btnEditProduct.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5)); btnEditProduct.setTextColor(Color.BLACK);
            btnEditProduct.setOnClickListener(v -> showEditProductDialog(p)); row.addView(btnEditProduct); dashDynamicContent.addView(row);
        }
    }

    private void showAddProductDialog() {
        if (!isAppActivated) {
            Toast.makeText(this, isHindi ? "🔒 यह फ़ीचर सिर्फ एक्टिवेटेड वर्ज़न में उपलब्ध है!" : "🔒 Locked in Demo Mode.", Toast.LENGTH_LONG).show();
            showActivationSystemDialog();
            return;
        }

        selectedImageUriStr = ""; AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "➕ नई मिठाई का विवरण" : "➕ Add New Product");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(32, 24, 32, 24);
        
        final EditText etNameHi = new EditText(this); etNameHi.setHint(isHindi ? "मिठाई का हिंदी नाम" : "Product Hindi Name"); layout.addView(etNameHi);
        final EditText etNameEn = new EditText(this); etNameEn.setHint(isHindi ? "मिठाई का अंग्रेजी नाम" : "Product English Name"); layout.addView(etNameEn);
        final EditText etPrice = new EditText(this); etPrice.setHint(isHindi ? "भाव प्रति किलो (₹)" : "Rate per KG (₹)"); etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etPrice);
        
        Button btnSelectImg = new Button(this); btnSelectImg.setText(isHindi ? "🖼️ गैलरी से फोटो चुनें" : "🖼️ Choose Photo"); layout.addView(btnSelectImg);
        previewImageView = new ImageView(this); previewImageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150)); previewImageView.setVisibility(View.GONE); layout.addView(previewImageView);
        btnSelectImg.setOnClickListener(v -> openGallery()); builder.setView(layout);
        
        builder.setPositiveButton(isHindi ? "सुरक्षित करें" : "Save", (dialog, which) -> {
            String nameHi = etNameHi.getText().toString(); String nameEn = etNameEn.getText().toString(); String priceStr = etPrice.getText().toString();
            if (!nameHi.isEmpty() && !priceStr.isEmpty()) {
                if(nameEn.isEmpty()) nameEn = nameHi;
                products.add(new Product("p" + (products.size() + 1), nameHi, nameEn, Double.parseDouble(priceStr), selectedImageUriStr));
                renderInventoryTab();
            }
        });
        builder.show();
    }

    private void showEditProductDialog(final Product p) {
        if (!isAppActivated) {
            Toast.makeText(this, isHindi ? "🔒 एडिट लॉक्ड! कृपया सॉफ्टवेयर की डालें।" : "🔒 Locked in Demo Mode.", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedImageUriStr = p.imageUriStr; AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isHindi ? "✏️ मिठाई का विवरण बदलें" : "✏️ Edit Product details");
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(32, 24, 32, 24);
        
        final EditText etNameHi = new EditText(this); etNameHi.setText(p.name); layout.addView(etNameHi);
        final EditText etNameEn = new EditText(this); etNameEn.setText(p.nameEn); layout.addView(etNameEn);
        final EditText etPrice = new EditText(this); etPrice.setText(String.valueOf(p.price)); etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etPrice);
        
        Button btnSelectImg = new Button(this); btnSelectImg.setText(isHindi ? "🖼️ नई फोटो बदलें" : "🖼️ Change Photo"); layout.addView(btnSelectImg);
        previewImageView = new ImageView(this); previewImageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150));
        if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) { previewImageView.setImageURI(Uri.parse(p.imageUriStr)); previewImageView.setVisibility(View.VISIBLE); }
        btnSelectImg.setOnClickListener(v -> openGallery()); builder.setView(layout);
        
        builder.setPositiveButton(isHindi ? "बदलाव सेव करें" : "Update", (dialog, which) -> {
            if (!etNameHi.getText().toString().isEmpty() && !etPrice.getText().toString().isEmpty()) {
                p.name = etNameHi.getText().toString(); p.nameEn = etNameEn.getText().toString().isEmpty() ? etNameHi.getText().toString() : etNameEn.getText().toString();
                p.price = Double.parseDouble(etPrice.getText().toString()); p.imageUriStr = selectedImageUriStr; renderInventoryTab();
            }
        });
        builder.show();
    }

    private void showCartDialog() {
        if (!isAppActivated) {
            Toast.makeText(this, isHindi ? "🔒 ऑर्डर सेव लॉक्ड है! कृपया पहले लाइसेंस की डालें।" : "🔒 Locked! Please activate the software to save orders.", Toast.LENGTH_LONG).show();
            showActivationSystemDialog();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle("FINAL BILL CALCULATOR");
        final LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(32, 24, 32, 24);
        double total = 0; final ArrayList<OrderItem> tempItems = new ArrayList<>();

        for (Map.Entry<String, Double> entry : regularCart.entrySet()) {
            for (final Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    final double amt = entry.getValue() * p.price; total += amt;
                    tempItems.add(new OrderItem(p.id, p.name, p.nameEn, entry.getValue(), p.price, false));
                    LinearLayout row = new LinearLayout(this); TextView tv = new TextView(this); String displayName = isHindi ? p.name : p.nameEn;
                    tv.setText("• " + displayName + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); row.addView(tv);
                    Button btnEditItem = new Button(this); btnEditItem.setText(isHindi ? "⚙️ बदलें" : "⚙️ Edit"); btnEditItem.setTextSize(10);
                    btnEditItem.setOnClickListener(v -> {
                        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this); b.setTitle(displayName);
                        final EditText in = new EditText(MainActivity.this); in.setText(String.valueOf(entry.getValue())); b.setView(in);
                        b.setPositiveButton("OK", (d, w) -> { if(!in.getText().toString().isEmpty()) { regularCart.put(p.id, Double.parseDouble(in.getText().toString())); showCartDialog(); } }); b.show();
                    });
                    row.addView(btnEditItem); layout.addView(row);
                }
            }
        }

        for (Map.Entry<String, Double> entry : advanceCart.entrySet()) {
            for (final Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    final double amt = entry.getValue() * p.price; total += amt;
                    tempItems.add(new OrderItem(p.id, p.name, p.nameEn, entry.getValue(), p.price, true));
                    LinearLayout row = new LinearLayout(this); TextView tv = new TextView(this); String displayName = isHindi ? p.name : p.nameEn;
                    tv.setText("• [Adv] " + displayName + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); row.addView(tv);
                    Button btnEditItem = new Button(this); btnEditItem.setText(isHindi ? "⚙️ बदलें" : "⚙️ Edit"); btnEditItem.setTextSize(10);
                    btnEditItem.setOnClickListener(v -> {
                        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this); b.setTitle(displayName + " (Adv)");
                        final EditText in = new EditText(MainActivity.this); in.setText(String.valueOf(entry.getValue())); b.setView(in);
                        b.setPositiveButton("OK", (d, w) -> { if(!in.getText().toString().isEmpty()) { advanceCart.put(p.id, Double.parseDouble(in.getText().toString())); showCartDialog(); } }); b.show();
                    });
                    row.addView(btnEditItem); layout.addView(row);
                }
            }
        }

        final TextView tvTotal = new TextView(this); tvTotal.setText((isHindi ? "\nकुल बिल राशि: ₹" : "\nGrand Total: ₹") + String.format("%.2f", total)); tvTotal.setTextSize(16); tvTotal.setTypeface(null, Typeface.BOLD); layout.addView(tvTotal);
        final EditText etReceived = new EditText(this); etReceived.setHint(isHindi ? "प्राप्त रुपये (Cash Received)" : "Cash Received (₹)"); etReceived.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etReceived);
        final TextView tvDue = new TextView(this); tvDue.setText(isHindi ? "बाकी उधारी राशि: ₹0.00" : "Remaining Due: ₹0.00"); tvDue.setTextColor(Color.RED); layout.addView(tvDue);

        final double finalTotal = total;
        etReceived.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                try { double rec = Double.parseDouble(s.toString()); String dueLabel = isHindi ? "बाकी उधारी राशि: ₹" : "Remaining Due: ₹"; tvDue.setText(dueLabel + String.format("%.2f", Math.max(0, finalTotal - rec))); } catch (Exception e) { String dueLabel = isHindi ? "बाकी उधारी राशि: ₹" : "Remaining Due: ₹"; tvDue.setText(dueLabel + String.format("%.2f", finalTotal)); }
            }
        });

        final EditText nameInput = new EditText(this); nameInput.setHint(isHindi ? "ग्राहक का नाम" : "Customer Name"); layout.addView(nameInput); builder.setView(layout);
        
        builder.setPositiveButton(isHindi ? "ऑर्डर डन (सेव)" : "Save Order", (dialog, which) -> {
            String name = nameInput.getText().toString(); String recStr = etReceived.getText().toString();
            double recVal = recStr.isEmpty() ? finalTotal : Double.parseDouble(recStr);
            if (!name.isEmpty() && !tempItems.isEmpty()) {
                orders.add(new Order("ORD" + System.currentTimeMillis(), name, tempItems, finalTotal, recVal, finalTotal - recVal));
                regularCart.clear(); advanceCart.clear(); updateCartButton();
            }
        });
        builder.show();
    }

    private void showOrderDetailsDialog(Order o) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle((isHindi ? "पक्की रसीद विवरण: " : "Invoice View: ") + o.customerName);
        LinearLayout mainLp = new LinearLayout(this); mainLp.setOrientation(LinearLayout.VERTICAL); mainLp.setPadding(30, 24, 30, 24);
        TextView tvMeta = new TextView(this); tvMeta.setText((isHindi ? "तारीख: " : "Date: ") + o.time + (isHindi ? "\nस्टेटस: " : "\nStatus: ") + o.status + "\n--------------------"); mainLp.addView(tvMeta);
        for (OrderItem item : o.items) {
            TextView tvItem = new TextView(this); String prefix = item.isAdvance ? "[Advance] " : ""; String itemName = isHindi ? item.name : item.nameEn;
            tvItem.setText("• " + prefix + itemName + "\n  " + String.format("%.3f", item.qty) + " kg @ ₹" + String.format("%.2f", item.price) + "/kg = ₹" + String.format("%.2f", item.qty * item.price));
            mainLp.addView(tvItem);
        }
        TextView tvSummary = new TextView(this); tvSummary.setText(isHindi ? ("--------------------\nकुल योग: ₹" + String.format("%.2f", o.total) + "\nजमा कैश: ₹" + String.format("%.2f", o.received) + "\n🔴 बाकी उधारी: ₹" + String.format("%.2f", o.due)) : ("--------------------\nTotal Bill: ₹" + String.format("%.2f", o.total) + "\nReceived: ₹" + String.format("%.2f", o.received) + "\n🔴 Total Due: ₹" + String.format("%.2f", o.due)));
        tvSummary.setTypeface(null, Typeface.BOLD); mainLp.addView(tvSummary); b.setView(mainLp); b.setPositiveButton(isHindi ? "ठीक है" : "Close", null); b.show();
    }

    private void showPayUdharDialog(final Order o) {
        if (!isAppActivated) {
            Toast.makeText(this, "🔒 उधारी जमा फ़ीचर सिर्फ प्रीमियम एक्टिवेटेड वर्ज़न के लिए है!", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle((isHindi ? "खाता उधारी जमा: " : "Clear Udhar Account: ") + o.customerName);
        LinearLayout lp = new LinearLayout(this); lp.setOrientation(LinearLayout.VERTICAL); lp.setPadding(32, 24, 32, 24);
        TextView tvInfo = new TextView(this); tvInfo.setText((isHindi ? "कुल बाकी उधारी राशि: ₹" : "Current Total Due: ₹") + String.format("%.2f", o.due)); tvInfo.setTextColor(Color.RED); lp.addView(tvInfo);
        final EditText etPay = new EditText(this); etPay.setHint(isHindi ? "जमा करने वाले रुपये लिखें" : "Enter Cash Amount to Pay"); lp.addView(etPay); b.setView(lp);
        b.setPositiveButton(isHindi ? "उधारी जमा करें" : "Pay Money", (dialog, which) -> {
            if (!etPay.getText().toString().isEmpty()) {
                double pAmt = Double.parseDouble(etPay.getText().toString());
                if (pAmt <= o.due) { o.received += pAmt; o.due -= pAmt; renderOrdersTab(); }
            }
        });
        b.show();
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews(); int visibleCount = 0;
        for (final Order o : orders) {
            if (!shouldShowOrder(o)) continue; 
            visibleCount++;

            LinearLayout orderCard = new LinearLayout(this); orderCard.setOrientation(LinearLayout.VERTICAL); orderCard.setPadding(24, 24, 24, 24); orderCard.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 4, 0, 16); orderCard.setLayoutParams(lp);
            
            TextView tvTime = new TextView(this); String statusColor = o.status.equals("Complete") ? (isHindi ? "🟢 पूरा हुआ" : "🟢 Complete") : (o.status.equals("Rejected") ? (isHindi ? "❌ रिजेक्टेड" : "❌ Rejected") : (isHindi ? "⏳ पेंडिंग" : "⏳ Pending"));
            tvTime.setText("📅 " + o.time + " | " + statusColor); tvTime.setTextSize(12); tvTime.setTextColor(Color.GRAY); orderCard.addView(tvTime);

            TextView tvHeader = new TextView(this); tvHeader.setText(isHindi ? ("🧾 ग्राहक: " + o.customerName + "\n💰 कुल bill: ₹" + String.format("%.2f", o.total) + " | प्राप्त: ₹" + String.format("%.2f", o.received) + "\n🔴 बाकी (Due): ₹" + String.format("%.2f", o.due)) : ("🧾 Customer: " + o.customerName + "\n💰 Total: ₹" + String.format("%.2f", o.total) + " | Paid: ₹" + String.format("%.2f", o.received) + "\n🔴 Due: ₹" + String.format("%.2f", o.due)));
            tvHeader.setTypeface(null, Typeface.BOLD); tvHeader.setTextColor(Color.BLACK); orderCard.addView(tvHeader);

            LinearLayout rowActions = new LinearLayout(this); rowActions.setOrientation(LinearLayout.HORIZONTAL); rowActions.setPadding(0, 12, 0, 0);
            Button btnViewDetails = new Button(this); btnViewDetails.setText(isHindi ? "👁️ बिल विवरण" : "👁️ View Bill"); btnViewDetails.setTextSize(11); btnViewDetails.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
            LinearLayout.LayoutParams lpAct1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lpAct1.setMargins(0, 0, 6, 0); btnViewDetails.setLayoutParams(lpAct1);
            btnViewDetails.setOnClickListener(v -> showOrderDetailsDialog(o)); rowActions.addView(btnViewDetails);

            if (o.status.equals("Pending")) {
                Button btnComplete = new Button(this); btnComplete.setText("Complete"); btnComplete.setTextSize(11); btnComplete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
                LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lpC.setMargins(0, 0, 6, 0); btnComplete.setLayoutParams(lpC);
                btnComplete.setOnClickListener(v -> { o.status = "Complete"; renderOrdersTab(); }); rowActions.addView(btnComplete);

                Button btnReject = new Button(this); btnReject.setText("Reject"); btnReject.setTextSize(11); btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252));
                btnReject.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                btnReject.setOnClickListener(v -> { o.status = "Rejected"; renderOrdersTab(); }); rowActions.addView(btnReject);
            } else if (o.status.equals("Complete") && o.due > 0) {
                Button btnPayUdhar = new Button(this); btnPayUdhar.setText(isHindi ? "💵 उधारी जमा करें" : "💵 Clear Due"); btnPayUdhar.setTextSize(11); btnPayUdhar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800)); btnPayUdhar.setTextColor(Color.WHITE);
                btnPayUdhar.setOnClickListener(v -> showPayUdharDialog(o)); rowActions.addView(btnPayUdhar);
            }
            orderCard.addView(rowActions); dashDynamicContent.addView(orderCard);
        }
        if (visibleCount == 0) { TextView empty = new TextView(this); empty.setText(isHindi ? "इस फ़िल्टर में कोई ऑर्डर नहीं मिला।" : "No matching orders found."); empty.setPadding(20, 20, 20, 20); dashDynamicContent.addView(empty); }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews(); double totalSales = 0, totalReceived = 0; int filteredCount = 0;
        for (Order o : orders) { 
            if (!shouldShowOrder(o)) continue; 
            if (!o.status.equals("Rejected")) { totalSales += o.total; totalReceived += o.received; filteredCount++; }
        }
        TextView tv = new TextView(this); String currentFilterName = spDateFilter.getSelectedItem().toString();
        if (isHindi) tv.setText("📊 क्लोजिंग रिपोर्ट (" + currentFilterName + ")\n\n" + "📦 फ़िल्टर किए गए कुल ऑर्डर्स: " + filteredCount + "\n" + "💰 कुल बिक्री (Net Sales): ₹" + String.format("%.2f", totalSales) + "\n" + "💵 कुल नकद प्राप्त (Cash In Hand): ₹" + String.format("%.2f", totalReceived) + "\n" + "🔴  मार्केट में बाकी उधारी (Total Due): ₹" + String.format("%.2f", (totalSales - totalReceived)));
        else tv.setText("📊 Closing Report (" + currentFilterName + ")\n\n" + "📦 Filtered Total Orders: " + filteredCount + "\n" + "💰 Net Sales: ₹" + String.format("%.2f", totalSales) + "\n" + "💵 Cash In Hand: ₹" + String.format("%.2f", totalReceived) + "\n" + "🔴 Market Total Due: ₹" + String.format("%.2f", (totalSales - totalReceived)));
        tv.setTextSize(16); tv.setPadding(24, 24, 24, 24); tv.setTypeface(null, Typeface.BOLD); dashDynamicContent.addView(tv);
    }
}
