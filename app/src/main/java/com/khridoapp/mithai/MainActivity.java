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

        // Add Product Button
        Button addProductBtn = new Button(this);
        addProductBtn.setText("➕ नई मिठाई जोड़ें");
        addProductBtn.setOnClickListener(v -> showAddProductDialog());
        dashDynamicContent.addView(addProductBtn);

        // List Products
        for (Product p : products) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(Color.WHITE);
            row.setPadding(12, 12, 12, 12);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 4, 0, 4);
            row.setLayoutParams(rowParams);
            row.setElevation(1f);

            TextView itemText = new TextView(this);
            itemText.setText(p.name + " - ₹" + p.price);
            itemText.setTextSize(14);
            itemText.setTypeface(null, Typeface.BOLD);
            row.addView(itemText);

            dashDynamicContent.addView(row);
        }
    }

    private void renderOrdersTab() {
        if (dashDynamicContent == null) return;
        dashDynamicContent.removeAllViews();

        boolean found = false;
        for (Order o : orders) {
            if (!shouldShowOrder(o)) continue;
            found = true;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(Color.WHITE);
            row.setPadding(12, 12, 12, 12);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 4, 0, 4);
            row.setLayoutParams(rowParams);
            row.setElevation(1f);

            TextView custText = new TextView(this);
            custText.setText(o.customerName + " - ₹" + o.total);
            custText.setTextSize(14);
            custText.setTypeface(null, Typeface.BOLD);
            row.addView(custText);

            TextView timeText = new TextView(this);
            timeText.setText("समय: " + o.time);
            timeText.setTextSize(12);
            timeText.setTextColor(Color.parseColor("#666666"));
            row.addView(timeText);

            dashDynamicContent.addView(row);
        }

        if (!found) {
            TextView noData = new TextView(this);
            noData.setText("कोई ऑर्डर नहीं मिला");
            noData.setTextSize(14);
            noData.setGravity(Gravity.CENTER);
            noData.setTextColor(Color.parseColor("#999999"));
            LinearLayout.LayoutParams noDataParams = new LinearLayout.LayoutParams(-1, 100);
            noData.setLayoutParams(noDataParams);
            dashDynamicContent.addView(noData);
        }
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
            if (tabInventory != null) tabInventory.setText("📦 स्टॉक");
            if (tabOrders != null) tabOrders.setText("🧾 उधारी");
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
        String[] options = isHindi
                ? new String[]{"🌐 English", "🔑 लाइसेंस पैनल", "🛠️ Admin Panel"}
                : new String[]{"🌐 Hindi", "🔑 License Panel", "🛠️ Admin Panel"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                isHindi = !isHindi;
                updateUI();
            } else if (which == 1) {
                showActivationSystemDialog();
            } else {
                verifyAdminAndOpenGenerator();
            }
        });
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
    etKey.setHint("MITHAI-XXXXXX-XX-893");
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
