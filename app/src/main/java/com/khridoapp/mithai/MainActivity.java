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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {
    // Existing variables ke neeche ye add karo
private static final int PICK_IMAGE_PRODUCT = 201;
private ImageView tempDialogImageView = null;
private String tempSelectedImageUri = "";

    // ==================== DATA CLASSES ====================
    static class Product {
        String id, name, nameEn, imageUriStr;
        double price;
        Product(String id, String name, String nameEn, double price, String imageUriStr) {
            this.id = id;
            this.name = name;
            this.nameEn = nameEn;
            this.price = price;
            this.imageUriStr = imageUriStr;
        }
    }

    static class OrderItem {
        String id, name, nameEn;
        double qty, price;
        boolean isAdvance;
        OrderItem(String id, String name, String nameEn, double qty, double price, boolean isAdvance) {
            this.id = id;
            this.name = name;
            this.nameEn = nameEn;
            this.qty = qty;
            this.price = price;
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
            this.id = id;
            this.customerName = customerName;
            this.items = items;
            this.total = total;
            this.received = received;
            this.due = due;
            this.status = "Pending";
            Date currentDate = new Date();
            this.timestamp = currentDate.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault());
            this.time = sdf.format(currentDate);
            this.dateKey = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(currentDate);
            this.monthKey = new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(currentDate);
        }
    }

    // ==================== UI ELEMENTS ====================
    private ScrollView shopContainer, dashboardContainer;
    private LinearLayout shopProductList, dashDynamicContent, filterBarContainer, customDateRow;
    private Button btnShopView, btnDashView, btnFloatingCart, btnFromDate, btnToDate;
    private Button tabInventory, tabOrders, tabAnalytics;
    private ImageView btnSettings;
    private EditText etSearchName;
    private Spinner spDateFilter;

    // ==================== DATA & STATE ====================
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private HashMap<String, Double> regularCart = new HashMap<>();
    private HashMap<String, Double> advanceCart = new HashMap<>();

    private String currentSubTab = "Inventory";
    private long fromTimestamp = 0;
    private long toTimestamp = Long.MAX_VALUE;
    private boolean isHindi = true;

    // Firebase & Activation
    private FirebaseAuth mAuth;
    private boolean isAppActivated = false;
    private String deviceId = "";
    private SharedPreferences activationPrefs;
    private static final String PREFS_NAME = "MithaiDeviceLockPrefs";
    private static final String KEY_IS_ACTIVATED = "IsAppFullyActivated";
    private static final String ADMIN_EMAIL = "vikarmsrkian6514@gmail.com";

    // ==================== LIFECYCLE ====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Get Device ID
        try {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) deviceId = "MITHAIPOS404";
        } catch (Exception e) {
            deviceId = "MITHAIPOS999";
        }

        // Check Activation Status
        activationPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isAppActivated = activationPrefs.getBoolean(KEY_IS_ACTIVATED, false);

        // Initialize UI
        initializeViews();
        setupListeners();
        initSeedProducts();
        updateUI();

        // Show Activation if needed
        if (!isAppActivated) {
            showActivationSystemDialog();
        }
    }

    // ==================== INITIALIZATION ====================
    private void initializeViews() {
        // Containers
        shopContainer = findViewById(R.id.shopContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        shopProductList = findViewById(R.id.shopProductList);
        dashDynamicContent = findViewById(R.id.dashDynamicContent);
        filterBarContainer = findViewById(R.id.filterBarContainer);
        customDateRow = findViewById(R.id.customDateRow);

        // Main Navigation Buttons
        btnShopView = findViewById(R.id.btnShopView);
        btnDashView = findViewById(R.id.btnDashView);
        btnFloatingCart = findViewById(R.id.btnFloatingCart);

        // Dashboard Tabs
        tabInventory = findViewById(R.id.tabInventory);
        tabOrders = findViewById(R.id.tabOrders);
        tabAnalytics = findViewById(R.id.tabAnalytics);

        // Filter Elements
        etSearchName = findViewById(R.id.etSearchName);
        spDateFilter = findViewById(R.id.spDateFilter);
        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);

        // Settings
        btnSettings = findViewById(R.id.btnSettings);

        // Setup Date Filter Spinner
        setupDateFilterSpinner();
    }

    private void setupListeners() {
        // Settings Button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> showLanguageSettingsDialog());
        }

        // ==================== MAIN NAVIGATION ====================
        if (btnShopView != null) {
            btnShopView.setOnClickListener(v -> {
                // Show Shop, Hide Dashboard
                shopContainer.setVisibility(View.VISIBLE);
                dashboardContainer.setVisibility(View.GONE);

                // Update Button Colors
                btnShopView.setBackgroundColor(Color.parseColor("#6200EE"));
                btnShopView.setTextColor(Color.WHITE);
                btnDashView.setBackgroundColor(Color.WHITE);
                btnDashView.setTextColor(Color.parseColor("#6200EE"));

                // Refresh Shop
                renderShop();
            });
        }

        if (btnDashView != null) {
            btnDashView.setOnClickListener(v -> {
                // Show Dashboard, Hide Shop
                shopContainer.setVisibility(View.GONE);
                dashboardContainer.setVisibility(View.VISIBLE);

                // Update Button Colors
                btnDashView.setBackgroundColor(Color.parseColor("#6200EE"));
                btnDashView.setTextColor(Color.WHITE);
                btnShopView.setBackgroundColor(Color.WHITE);
                btnShopView.setTextColor(Color.parseColor("#6200EE"));

                // Show Inventory by default
                if (currentSubTab.equals("Inventory")) {
                    renderInventoryTab();
                }
            });
        }

        // ==================== CART BUTTON ====================
        if (btnFloatingCart != null) {
            btnFloatingCart.setOnClickListener(v -> {
                if (!isAppActivated) {
                    showActivationSystemDialog();
                    return;
                }
                showCartDialog();
            });
        }

        // ==================== DASHBOARD TABS ====================
        if (tabInventory != null) {
            tabInventory.setOnClickListener(v -> {
                currentSubTab = "Inventory";
                switchSubTab(tabInventory);
                renderInventoryTab();
            });
        }

        if (tabOrders != null) {
            tabOrders.setOnClickListener(v -> {
                currentSubTab = "Orders";
                switchSubTab(tabOrders);
                renderOrdersTab();
            });
        }

        if (tabAnalytics != null) {
            tabAnalytics.setOnClickListener(v -> {
                currentSubTab = "Analytics";
                switchSubTab(tabAnalytics);
                renderAnalyticsTab();
            });
        }

        // ==================== DATE BUTTONS ====================
        if (btnFromDate != null) {
            btnFromDate.setOnClickListener(v -> showDatePicker(true));
        }

        if (btnToDate != null) {
            btnToDate.setOnClickListener(v -> showDatePicker(false));
        }

        // ==================== SEARCH FILTER ====================
        if (etSearchName != null) {
            etSearchName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    refreshCurrentReportTab();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // ==================== DATE FILTER SPINNER ====================
        if (spDateFilter != null) {
            spDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Show custom date range for position 3 (Custom)
                    if (customDateRow != null) {
                        customDateRow.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                    }
                    refreshCurrentReportTab();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    // ==================== SETUP FUNCTIONS ====================
    private void setupDateFilterSpinner() {
        if (spDateFilter == null) return;
        String[] filterOptions = isHindi
                ? new String[]{"सभी ऑर्डर्स", "आज के", "इस महीने के", "📅 कस्टम तारीख"}
                : new String[]{"All Orders", "Today", "This Month", "📅 Custom Range"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterOptions);
        spDateFilter.setAdapter(filterAdapter);
    }

    private void initSeedProducts() {
        if (products.isEmpty()) {
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, ""));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, ""));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, ""));
        }
    }

    // ==================== RENDERING FUNCTIONS ====================
    private void renderShop() {
        if (shopProductList == null) return;
        shopProductList.removeAllViews();

        for (final Product p : products) {
            // Product Card Container
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, 8, 0, 12);
            card.setLayoutParams(cardParams);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(16, 16, 16, 16);
            card.setElevation(4f);

            // Header Row: Image + Name
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
            headerParams.setMargins(0, 0, 0, 12);
            headerRow.setLayoutParams(headerParams);

            // Product Image
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(100, 100);
            imgParams.setMargins(0, 0, 12, 0);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (!p.imageUriStr.isEmpty()) {
                imageView.setImageURI(Uri.parse(p.imageUriStr));
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            imageView.setBackgroundColor(Color.parseColor("#E0E0E0"));
            headerRow.addView(imageView);

            // Product Info
            LinearLayout infoContainer = new LinearLayout(this);
            infoContainer.setOrientation(LinearLayout.VERTICAL);
            infoContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView productName = new TextView(this);
            productName.setText(isHindi ? p.name : p.nameEn);
            productName.setTextSize(16);
            productName.setTypeface(null, Typeface.BOLD);
            productName.setTextColor(Color.BLACK);
            infoContainer.addView(productName);

            TextView priceText = new TextView(this);
            priceText.setText("₹" + p.price + " प्रति यूनिट");
            priceText.setTextSize(13);
            priceText.setTextColor(Color.parseColor("#666666"));
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(-2, -2);
            priceParams.topMargin = 4;
            priceText.setLayoutParams(priceParams);
            infoContainer.addView(priceText);

            headerRow.addView(infoContainer);
            card.addView(headerRow);

            // Input Row: Quantity + Button
            LinearLayout inputRow = new LinearLayout(this);
            inputRow.setOrientation(LinearLayout.HORIZONTAL);
            inputRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

            // Quantity Input
            final EditText qtyInput = new EditText(this);
            qtyInput.setHint("मात्रा (kg/pcs)");
            qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(0, 44, 1.2f);
            qtyParams.setMargins(0, 0, 8, 0);
            qtyInput.setLayoutParams(qtyParams);
            qtyInput.setPadding(8, 8, 8, 8);
            qtyInput.setBackgroundColor(Color.parseColor("#F5F5F5"));
            inputRow.addView(qtyInput);

            // Add to Cart Button
            Button addBtn = new Button(this);
            addBtn.setText("🛒 जोड़ें");
            addBtn.setTextSize(12);
            addBtn.setTextColor(Color.WHITE);
            addBtn.setBackgroundColor(Color.parseColor("#6200EE"));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, 44, 1f);
            addBtn.setLayoutParams(btnParams);
            addBtn.setOnClickListener(v -> {
                String qtyStr = qtyInput.getText().toString().trim();
                if (qtyStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "मात्रा दर्ज करें!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    double qty = Double.parseDouble(qtyStr);
                    if (qty <= 0) {
                        Toast.makeText(MainActivity.this, "मात्रा 0 से अधिक होनी चाहिए!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    regularCart.put(p.id, qty);
                    updateCartButton();
                    qtyInput.setText("");
                    Toast.makeText(MainActivity.this, p.name + " कार्ट में जोड़ दी गई!", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "सही मात्रा दर्ज करें!", Toast.LENGTH_SHORT).show();
                }
            });
            inputRow.addView(addBtn);
            card.addView(inputRow);

            shopProductList.addView(card);
        }
    }

    private void renderInventoryTab() {
    if (dashDynamicContent == null) return;
    dashDynamicContent.removeAllViews();

    Button addBtn = new Button(this);
    addBtn.setText("➕ नई मिठाई जोड़ें");
    addBtn.setTextColor(Color.WHITE);
    addBtn.setBackgroundColor(Color.parseColor("#6200EE"));
    LinearLayout.LayoutParams addP = new LinearLayout.LayoutParams(-1, -2);
    addP.setMargins(0, 0, 0, 16);
    addBtn.setLayoutParams(addP);
    addBtn.setOnClickListener(v -> showAddEditProductDialog(null));
    dashDynamicContent.addView(addBtn);

    for (Product p : products) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(16, 16, 16, 16);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setElevation(3f);
        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1, -2);
        rowP.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rowP);

        // Mini Photo
        ImageView img = new ImageView(this);
        LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(90, 90);
        imgP.setMargins(0, 0, 16, 0);
        img.setLayoutParams(imgP);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(Color.parseColor("#E8E0F0"));
        if (p.imageUriStr != null && !p.imageUriStr.isEmpty())
            img.setImageURI(Uri.parse(p.imageUriStr));
        else
            img.setImageResource(android.R.drawable.ic_menu_gallery);
        row.addView(img);

        // Name + Rate
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(isHindi ? p.name : p.nameEn);
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.BLACK);
        info.addView(tvName);

        TextView tvRate = new TextView(this);
        tvRate.setText("₹" + (int) p.price + " / kg");
        tvRate.setTextSize(13);
        tvRate.setTextColor(Color.parseColor("#6200EE"));
        info.addView(tvRate);
        row.addView(info);

        // Edit Button
        Button editBtn = new Button(this);
        editBtn.setText("✏️ Edit");
        editBtn.setTextSize(12);
        editBtn.setTextColor(Color.WHITE);
        editBtn.setBackgroundColor(Color.parseColor("#03DAC5"));
        final Product fp = p;
        editBtn.setOnClickListener(v -> showAddEditProductDialog(fp));
        row.addView(editBtn);

        dashDynamicContent.addView(row);
    }
}

private void showAddEditProductDialog(Product product) {
    boolean isEditing = (product != null);
    tempSelectedImageUri = isEditing ? product.imageUriStr : "";

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(isEditing ? "✏️ मिठाई Edit करें" : "➕ नई मिठाई जोड़ें");

    ScrollView sv = new ScrollView(this);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 20, 40, 20);
    sv.addView(layout);

    // Photo preview
    tempDialogImageView = new ImageView(this);
    LinearLayout.LayoutParams photoP = new LinearLayout.LayoutParams(-1, 220);
    photoP.setMargins(0, 0, 0, 10);
    tempDialogImageView.setLayoutParams(photoP);
    tempDialogImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    tempDialogImageView.setBackgroundColor(Color.parseColor("#E8E0F0"));
    if (isEditing && product.imageUriStr != null && !product.imageUriStr.isEmpty())
        tempDialogImageView.setImageURI(Uri.parse(product.imageUriStr));
    else
        tempDialogImageView.setImageResource(android.R.drawable.ic_menu_gallery);
    layout.addView(tempDialogImageView);

    // Photo pick button
    Button btnPhoto = new Button(this);
    btnPhoto.setText("📷 फोटो चुनें / बदलें");
    btnPhoto.setTextColor(Color.WHITE);
    btnPhoto.setBackgroundColor(Color.parseColor("#6200EE"));
    LinearLayout.LayoutParams bpP = new LinearLayout.LayoutParams(-1, -2);
    bpP.setMargins(0, 0, 0, 20);
    btnPhoto.setLayoutParams(bpP);
    btnPhoto.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE_PRODUCT);
    });
    layout.addView(btnPhoto);

    // Hindi Name
    addDialogLabel(layout, "मिठाई का नाम (हिंदी) *");
    EditText etHindi = new EditText(this);
    etHindi.setHint("जैसे: गुलाब जामुन");
    if (isEditing) etHindi.setText(product.name);
    layout.addView(etHindi);

    // English Name
    addDialogLabel(layout, "मिठाई का नाम (English)");
    EditText etEnglish = new EditText(this);
    etEnglish.setHint("e.g. Gulab Jamun");
    if (isEditing) etEnglish.setText(product.nameEn);
    layout.addView(etEnglish);

    // Rate
    addDialogLabel(layout, "रेट (₹ per kg) *");
    EditText etRate = new EditText(this);
    etRate.setHint("जैसे: 360");
    etRate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    if (isEditing) etRate.setText(String.valueOf((int) product.price));
    layout.addView(etRate);

    builder.setView(sv);
    builder.setPositiveButton(isEditing ? "💾 Save" : "➕ जोड़ें", (dialog, which) -> {
        String nameH = etHindi.getText().toString().trim();
        String nameE = etEnglish.getText().toString().trim();
        String rateStr = etRate.getText().toString().trim();

        if (nameH.isEmpty() || rateStr.isEmpty()) {
            Toast.makeText(this, "नाम और रेट जरूरी है!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nameE.isEmpty()) nameE = nameH;
        double rate = Double.parseDouble(rateStr);

        if (isEditing) {
            product.name = nameH;
            product.nameEn = nameE;
            product.price = rate;
            product.imageUriStr = tempSelectedImageUri;
            Toast.makeText(this, "✅ " + nameH + " Update हो गई!", Toast.LENGTH_SHORT).show();
        } else {
            products.add(new Product("p" + System.currentTimeMillis(),
                nameH, nameE, rate, tempSelectedImageUri));
            Toast.makeText(this, "✅ " + nameH + " जोड़ी गई!", Toast.LENGTH_SHORT).show();
        }
        renderInventoryTab();
        renderShop();
    });
    builder.setNegativeButton("Cancel", null);
    builder.show();
}

private void addDialogLabel(LinearLayout layout, String text) {
    TextView tv = new TextView(this);
    tv.setText(text);
    tv.setTextSize(13);
    tv.setTextColor(Color.parseColor("#444444"));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, 14, 0, 2);
    tv.setLayoutParams(p);
    layout.addView(tv);
}

// Image picker result
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_IMAGE_PRODUCT && resultCode == RESULT_OK && data != null) {
        Uri uri = data.getData();
        if (uri != null) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            tempSelectedImageUri = uri.toString();
            if (tempDialogImageView != null)
                tempDialogImageView.setImageURI(uri);
        }
    }
}

    private void renderOrdersTab() {
    if (dashDynamicContent == null) return;
    dashDynamicContent.removeAllViews();

    ArrayList<Order> filtered = new ArrayList<>();
    for (Order o : orders) if (shouldShowOrder(o)) filtered.add(o);

    if (filtered.isEmpty()) {
        TextView noData = new TextView(this);
        noData.setText("कोई ऑर्डर नहीं मिला");
        noData.setGravity(Gravity.CENTER);
        noData.setTextColor(Color.parseColor("#999999"));
        noData.setTextSize(14);
        noData.setLayoutParams(new LinearLayout.LayoutParams(-1, 200));
        dashDynamicContent.addView(noData);
        return;
    }

    for (Order o : filtered) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(16, 16, 16, 16);
        card.setElevation(4f);
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(-1, -2);
        cP.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cP);

        // ── Header: Name + Status ──
        LinearLayout hRow = new LinearLayout(this);
        hRow.setOrientation(LinearLayout.HORIZONTAL);
        hRow.setGravity(Gravity.CENTER_VERTICAL);
        hRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView tvName = new TextView(this);
        tvName.setText("👤 " + o.customerName);
        tvName.setTextSize(16);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        hRow.addView(tvName);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(o.status);
        tvStatus.setTextSize(11);
        tvStatus.setPadding(14, 6, 14, 6);
        tvStatus.setTextColor(Color.WHITE);
        int sColor = o.status.equals("Completed") ? Color.parseColor("#4CAF50")
                   : o.status.equals("Cancelled") ? Color.parseColor("#F44336")
                   : Color.parseColor("#FF9800");
        tvStatus.setBackgroundColor(sColor);
        hRow.addView(tvStatus);
        card.addView(hRow);

        // ── Date/Time ──
        TextView tvTime = new TextView(this);
        tvTime.setText("📅 " + o.time);
        tvTime.setTextSize(12);
        tvTime.setTextColor(Color.parseColor("#777777"));
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(-1, -2);
        tP.topMargin = 6;
        tvTime.setLayoutParams(tP);
        card.addView(tvTime);

        // ── Items summary ──
        StringBuilder sb = new StringBuilder("🛒 ");
        for (int i = 0; i < o.items.size(); i++) {
            sb.append(o.items.get(i).name)
              .append(" ×").append(o.items.get(i).qty).append("kg");
            if (i < o.items.size() - 1) sb.append(", ");
        }
        TextView tvItems = new TextView(this);
        tvItems.setText(sb.toString());
        tvItems.setTextSize(12);
        tvItems.setTextColor(Color.parseColor("#444444"));
        LinearLayout.LayoutParams itP = new LinearLayout.LayoutParams(-1, -2);
        itP.topMargin = 4;
        tvItems.setLayoutParams(itP);
        card.addView(tvItems);

        // ── Financial cells ──
        LinearLayout finRow = new LinearLayout(this);
        finRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams frP = new LinearLayout.LayoutParams(-1, -2);
        frP.setMargins(0, 10, 0, 0);
        finRow.setLayoutParams(frP);
        addFinCell(finRow, "कुल", "₹" + (int) o.total, Color.BLACK);
        addFinCell(finRow, "मिला", "₹" + (int) o.received, Color.parseColor("#4CAF50"));
        addFinCell(finRow, "बकाया", "₹" + (int) o.due,
            o.due > 0 ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));
        card.addView(finRow);

        // ── Action Buttons ──
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brP = new LinearLayout.LayoutParams(-1, -2);
        brP.topMargin = 12;
        btnRow.setLayoutParams(brP);

        // View
        Button btnView = makeActionBtn("👁 View", "#6200EE");
        final Order fo = o;
        btnView.setOnClickListener(v -> showOrderDetailDialog(fo));
        LinearLayout.LayoutParams vP = new LinearLayout.LayoutParams(0, -2, 1f);
        vP.setMargins(0, 0, 4, 0);
        btnView.setLayoutParams(vP);
        btnRow.addView(btnView);

        // Udhar update
        if (o.due > 0 && !o.status.equals("Cancelled")) {
            Button btnPay = makeActionBtn("💰 उधार", "#FF9800");
            btnPay.setOnClickListener(v -> showUpdateDueDialog(fo));
            LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, -2, 1f);
            pP.setMargins(4, 0, 4, 0);
            btnPay.setLayoutParams(pP);
            btnRow.addView(btnPay);
        }

        // Complete / Cancel — sirf Pending orders pe
        if (o.status.equals("Pending")) {
            Button btnDone = makeActionBtn("✅ Done", "#4CAF50");
            btnDone.setOnClickListener(v -> {
                fo.status = "Completed";
                renderOrdersTab();
                Toast.makeText(this, "✅ ऑर्डर Complete!", Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(0, -2, 1f);
            dP.setMargins(4, 0, 4, 0);
            btnDone.setLayoutParams(dP);
            btnRow.addView(btnDone);

            Button btnCancel = makeActionBtn("❌ Cancel", "#F44336");
            btnCancel.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Cancel करें?")
                    .setMessage(fo.customerName + " का ऑर्डर cancel करना है?")
                    .setPositiveButton("हाँ", (d, w) -> {
                        fo.status = "Cancelled";
                        renderOrdersTab();
                        Toast.makeText(this, "❌ Cancelled", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("नहीं", null).show()
            );
            LinearLayout.LayoutParams xP = new LinearLayout.LayoutParams(0, -2, 1f);
            xP.setMargins(4, 0, 0, 0);
            btnCancel.setLayoutParams(xP);
            btnRow.addView(btnCancel);
        }

        card.addView(btnRow);
        dashDynamicContent.addView(card);
    }
}

// Helper — colored button
private Button makeActionBtn(String text, String hexColor) {
    Button btn = new Button(this);
    btn.setText(text);
    btn.setTextSize(11);
    btn.setTextColor(Color.WHITE);
    btn.setBackgroundColor(Color.parseColor(hexColor));
    return btn;
}

// Financial cell helper
private void addFinCell(LinearLayout parent, String label, String value, int color) {
    LinearLayout cell = new LinearLayout(this);
    cell.setOrientation(LinearLayout.VERTICAL);
    cell.setGravity(Gravity.CENTER);
    cell.setBackgroundColor(Color.parseColor("#F5F5F5"));
    cell.setPadding(8, 10, 8, 10);
    LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, -2, 1f);
    cP.setMargins(2, 0, 2, 0);
    cell.setLayoutParams(cP);

    TextView lbl = new TextView(this);
    lbl.setText(label);
    lbl.setTextSize(11);
    lbl.setTextColor(Color.parseColor("#888888"));
    lbl.setGravity(Gravity.CENTER);
    cell.addView(lbl);

    TextView val = new TextView(this);
    val.setText(value);
    val.setTextSize(14);
    val.setTypeface(null, Typeface.BOLD);
    val.setTextColor(color);
    val.setGravity(Gravity.CENTER);
    cell.addView(val);

    parent.addView(cell);
}

// Order detail dialog
private void showOrderDetailDialog(Order o) {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 20, 40, 20);

    addDetailRow(layout, "👤 ग्राहक", o.customerName);
    addDetailRow(layout, "📅 समय", o.time);
    addDetailRow(layout, "📊 Status", o.status);

    addDivider(layout);

    TextView tvHead = new TextView(this);
    tvHead.setText("🛒 Items:");
    tvHead.setTextSize(14);
    tvHead.setTypeface(null, Typeface.BOLD);
    layout.addView(tvHead);

    for (OrderItem item : o.items) {
        TextView tv = new TextView(this);
        tv.setText("  • " + item.name + " × " + item.qty + "kg = ₹" + (int) item.price);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams iP = new LinearLayout.LayoutParams(-1, -2);
        iP.topMargin = 4;
        tv.setLayoutParams(iP);
        layout.addView(tv);
    }

    addDivider(layout);

    addDetailRow(layout, "💰 कुल", "₹" + (int) o.total);
    addDetailRow(layout, "✅ मिला", "₹" + (int) o.received);
    addDetailRow(layout, "⚠️ बकाया", "₹" + (int) o.due);

    ScrollView sv = new ScrollView(this);
    sv.addView(layout);

    new AlertDialog.Builder(this)
        .setTitle("📋 Order — " + o.customerName)
        .setView(sv)
        .setPositiveButton("Close", null)
        .show();
}

private void addDetailRow(LinearLayout layout, String label, String value) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams rP = new LinearLayout.LayoutParams(-1, -2);
    rP.topMargin = 10;
    row.setLayoutParams(rP);

    TextView lbl = new TextView(this);
    lbl.setText(label);
    lbl.setTextSize(13);
    lbl.setTextColor(Color.parseColor("#888888"));
    lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    row.addView(lbl);

    TextView val = new TextView(this);
    val.setText(value);
    val.setTextSize(13);
    val.setTypeface(null, Typeface.BOLD);
    val.setTextColor(Color.BLACK);
    val.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    row.addView(val);

    layout.addView(row);
}

private void addDivider(LinearLayout layout) {
    View div = new View(this);
    div.setBackgroundColor(Color.parseColor("#E0E0E0"));
    LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(-1, 2);
    dP.setMargins(0, 14, 0, 14);
    div.setLayoutParams(dP);
    layout.addView(div);
}

// Udhar update dialog
private void showUpdateDueDialog(Order o) {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 20, 40, 20);

    EditText etPay = new EditText(this);
    etPay.setHint("कितने ₹ मिले?");
    etPay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    layout.addView(etPay);

    new AlertDialog.Builder(this)
        .setTitle("💰 उधार Update — " + o.customerName)
        .setMessage("बाकी बकाया: ₹" + (int) o.due)
        .setView(layout)
        .setPositiveButton("💾 Update", (d, w) -> {
            String s = etPay.getText().toString().trim();
            if (s.isEmpty()) return;
            double payment = Math.min(Double.parseDouble(s), o.due);
            o.received += payment;
            o.due -= payment;
            if (o.due <= 0) {
                o.due = 0;
                o.status = "Completed";
                Toast.makeText(this, "🎉 पूरा उधार Clear!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "✅ ₹" + (int) payment + " मिले, बकाया: ₹" + (int) o.due, Toast.LENGTH_SHORT).show();
            }
            renderOrdersTab();
        })
        .setNegativeButton("Cancel", null)
        .show();
                }
    private void renderAnalyticsTab() {
        if (dashDynamicContent == null) return;
        dashDynamicContent.removeAllViews();

        double totalSales = 0;
        double totalReceived = 0;
        double totalDue = 0;

        for (Order o : orders) {
            if (!shouldShowOrder(o)) continue;
            totalSales += o.total;
            totalReceived += o.received;
            totalDue += o.due;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(16, 16, 16, 16);
        card.setElevation(2f);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        // Title
        TextView titleText = new TextView(this);
        titleText.setText("📊 बिक्री सारांश");
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        card.addView(titleText);

        // Total Sales
        addAnalyticsRow(card, "कुल बिक्री", "₹" + (int) totalSales, Color.parseColor("#4CAF50"));

        // Received
        addAnalyticsRow(card, "प्राप्त", "₹" + (int) totalReceived, Color.parseColor("#2196F3"));

        // Due
        addAnalyticsRow(card, "बकाया", "₹" + (int) totalDue, Color.parseColor("#F44336"));

        dashDynamicContent.addView(card);
    }

    private void addAnalyticsRow(LinearLayout container, String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = 12;
        row.setLayoutParams(rowParams);

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(labelText);

        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextSize(16);
        valueText.setTypeface(null, Typeface.BOLD);
        valueText.setTextColor(color);
        row.addView(valueText);

        container.addView(row);
    }

    // ==================== HELPER FUNCTIONS ====================
    private void switchSubTab(Button activeTab) {
        // Reset all tabs
        if (tabInventory != null) {
            tabInventory.setBackgroundColor(Color.parseColor("#E0E0E0"));
            tabInventory.setTextColor(Color.BLACK);
        }
        if (tabOrders != null) {
            tabOrders.setBackgroundColor(Color.parseColor("#E0E0E0"));
            tabOrders.setTextColor(Color.BLACK);
        }
        if (tabAnalytics != null) {
            tabAnalytics.setBackgroundColor(Color.parseColor("#E0E0E0"));
            tabAnalytics.setTextColor(Color.BLACK);
        }

        // Activate current tab
        if (activeTab != null) {
            activeTab.setBackgroundColor(Color.parseColor("#03DAC5"));
            activeTab.setTextColor(Color.BLACK);
        }

        // Show/hide filter bar
        if (filterBarContainer != null) {
            filterBarContainer.setVisibility(activeTab == tabInventory ? View.GONE : View.VISIBLE);
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
        // Search Filter
        if (etSearchName != null) {
            String searchTerm = etSearchName.getText().toString().trim().toLowerCase();
            if (!searchTerm.isEmpty() && !o.customerName.toLowerCase().contains(searchTerm)) {
                return false;
            }
        }

        // Date Filter
        if (spDateFilter != null) {
            int selectedPos = spDateFilter.getSelectedItemPosition();
            String todayKey = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
            String monthKey = new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(new Date());

            switch (selectedPos) {
                case 1: // Today
                    if (!o.dateKey.equals(todayKey)) return false;
                    break;
                case 2: // This Month
                    if (!o.monthKey.equals(monthKey)) return false;
                    break;
                case 3: // Custom Date Range
                    if (o.timestamp < fromTimestamp || o.timestamp > toTimestamp) return false;
                    break;
            }
        }

        return true;
    }

    private void updateCartButton() {
        if (btnFloatingCart != null) {
            int count = regularCart.size() + advanceCart.size();
            btnFloatingCart.setText("🛒 कार्ट (" + count + ")");
        }
    }

    private void showDatePicker(final boolean isFrom) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            if (isFrom) {
                selected.set(Calendar.HOUR_OF_DAY, 0);
                selected.set(Calendar.MINUTE, 0);
                selected.set(Calendar.SECOND, 0);
                fromTimestamp = selected.getTimeInMillis();
                if (btnFromDate != null) btnFromDate.setText("शुरु: " + dayOfMonth);
            } else {
                selected.set(Calendar.HOUR_OF_DAY, 23);
                selected.set(Calendar.MINUTE, 59);
                selected.set(Calendar.SECOND, 59);
                toTimestamp = selected.getTimeInMillis();
                if (btnToDate != null) btnToDate.setText("अंत: " + dayOfMonth);
            }
            refreshCurrentReportTab();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateUI() {
        if (isHindi) {
            if (btnShopView != null) btnShopView.setText("🛍️ दुकान");
            if (btnDashView != null) btnDashView.setText("📊 डैशबोर्ड");
            if (tabInventory != null) tabInventory.setText(isHindi ? "💰 रेट" : "💰 Rate");
            if (tabOrders != null) tabOrders.setText(isHindi ? "🧾 ऑर्डर्स" : "🧾 Orders");
            if (tabAnalytics != null) tabAnalytics.setText("📈 रिपोर्ट");
            if (etSearchName != null) etSearchName.setHint("🔍 नाम खोजें...");
        } else {
            if (btnShopView != null) btnShopView.setText("🛍️ Shop");
            if (btnDashView != null) btnDashView.setText("📊 Dashboard");
            if (tabInventory != null) tabInventory.setText("📦 Inventory");
            if (tabOrders != null) tabOrders.setText("🧾 Orders");
            if (tabAnalytics != null) tabAnalytics.setText("📈 Reports");
            if (etSearchName != null) etSearchName.setHint("🔍 Search...");
        }
        setupDateFilterSpinner();
        updateCartButton();
        renderShop();
    }

    // ==================== DIALOG FUNCTIONS ====================
    private void showLanguageSettingsDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(isHindi ? "⚙️ सेटिंग्स" : "⚙️ Settings");

    if (!isAppActivated) {
        // Demo mode — License Panel dikhao
        String[] options = isHindi
            ? new String[]{"🌐 English", "🔑 लाइसेंस पैनल", "🛠️ Admin Panel"}
            : new String[]{"🌐 Hindi", "🔑 License Panel", "🛠️ Admin Panel"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { isHindi = !isHindi; updateUI(); }
            else if (which == 1) { showActivationSystemDialog(); }
            else { verifyAdminAndOpenGenerator(); }
        });
    } else {
        // Activated — License Panel hide
        String[] options = isHindi
            ? new String[]{"🌐 English", "🛠️ Admin Panel"}
            : new String[]{"🌐 Hindi", "🛠️ Admin Panel"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { isHindi = !isHindi; updateUI(); }
            else { verifyAdminAndOpenGenerator(); }
        });
    }
    builder.show();
    }

    private void showAddProductDialog() {
        if (!isAppActivated) {
            showActivationSystemDialog();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ नई मिठाई जोड़ें");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                products.add(new Product("p" + (products.size() + 1), name, name, 300, ""));
                renderInventoryTab();
                Toast.makeText(MainActivity.this, "मिठाई जोड़ दी गई!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCartDialog() {
        if (regularCart.isEmpty()) {
            Toast.makeText(this, "कार्ट खाली है!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🛒 ऑर्डर पूरा करें");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText customerName = new EditText(this);
        customerName.setHint("ग्राहक का नाम");
        layout.addView(customerName);

        EditText amountReceived = new EditText(this);
        amountReceived.setHint("राशि प्राप्त");
        amountReceived.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(amountReceived);

        builder.setView(layout);
        builder.setPositiveButton("Complete", (dialog, which) -> {
            String name = customerName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(MainActivity.this, "नाम दर्ज करें!", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = 0;
            ArrayList<OrderItem> items = new ArrayList<>();
            for (String productId : regularCart.keySet()) {
                Product p = products.stream().filter(prod -> prod.id.equals(productId)).findFirst().orElse(null);
                if (p != null) {
                    double qty = regularCart.get(productId);
                    double price = p.price * qty;
                    total += price;
                    items.add(new OrderItem(productId, p.name, p.nameEn, qty, price, false));
                }
            }

            double received = 0;
            try {
                received = Double.parseDouble(amountReceived.getText().toString());
            } catch (NumberFormatException e) {
                received = total;
            }

            double due = total - received;
            orders.add(new Order("ORD" + System.currentTimeMillis(), name, items, total, received, due));
            regularCart.clear();
            advanceCart.clear();
            updateCartButton();
            Toast.makeText(MainActivity.this, "ऑर्डर पूरा हो गया!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showActivationSystemDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("🔐 App Activation");
    builder.setCancelable(false); // Back press se band na ho

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 20, 40, 20);

    // Device ID dikhao
    TextView tvDeviceId = new TextView(this);
    tvDeviceId.setText("📱 आपकी Device ID:\n" + deviceId);
    tvDeviceId.setTextSize(13);
    tvDeviceId.setTextColor(Color.parseColor("#6200EE"));
    tvDeviceId.setPadding(0, 0, 0, 16);
    layout.addView(tvDeviceId);

    // Copy Device ID button
    Button btnCopyId = new Button(this);
    btnCopyId.setText("📋 Device ID Copy करें");
    btnCopyId.setTextColor(Color.WHITE);
    btnCopyId.setBackgroundColor(Color.parseColor("#6200EE"));
    btnCopyId.setOnClickListener(v -> {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip =
            android.content.ClipData.newPlainText("DeviceID", deviceId);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Device ID Copy हो गई! ✅", Toast.LENGTH_SHORT).show();
        }
    });
    layout.addView(btnCopyId);

    // License Key input
    TextView tvLabel = new TextView(this);
    tvLabel.setText("\n🔑 License Key यहाँ डालें:");
    tvLabel.setTextSize(14);
    tvLabel.setTextColor(Color.BLACK);
    layout.addView(tvLabel);

    EditText etKey = new EditText(this);
    etKey.setHint("XXXXXX-XXXXXX-XX-XXX");
    etKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                       android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    layout.addView(etKey);

    builder.setView(layout);

    builder.setPositiveButton("✅ Activate", (dialog, which) -> {
        String enteredKey = etKey.getText().toString().trim();
        if (validateLicenseKey(enteredKey, deviceId)) {
            // Key sahi hai — activate karo
            activationPrefs.edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .apply();
            isAppActivated = true;
            Toast.makeText(this, "🎉 App Activate हो गई!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "❌ गलत License Key!", Toast.LENGTH_SHORT).show();
            // Dialog fir se dikhao
            showActivationSystemDialog();
        }
    });

    builder.setNegativeButton("Demo Mode", (dialog, which) -> {
        Toast.makeText(this, "⚠️ Demo Mode — सीमित सुविधाएं", Toast.LENGTH_SHORT).show();
    });

    builder.show();
}

// Key validation — KeyGeneratorActivity ke algorithm ka reverse
private boolean validateLicenseKey(String enteredKey, String dId) {
    try {
        // Expected key banao device ID se
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

        String expectedKey = "MITHAI-" + shiftedStr + "-" + (dId.length() * 7) + "-893";
        return enteredKey.equalsIgnoreCase(expectedKey);
    } catch (Exception e) {
        return false;
    }
}

    private void verifyAdminAndOpenGenerator() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText emailInput = new EditText(this);
        emailInput.setHint("Email");
        layout.addView(emailInput);

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);
        builder.setPositiveButton("Login", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "सभी fields भरें!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "✅ Admin Verified!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, KeyGeneratorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "❌ Login Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
