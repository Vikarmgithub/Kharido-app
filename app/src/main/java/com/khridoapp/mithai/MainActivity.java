package com.khridoapp.mithai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static String selectedImageUriStr = ""; 
    private static ImageView previewImageView; 

    static class Product {
        String id, name, imageUriStr; 
        double price;
        Product(String id, String name, double price, String imageUriStr) {
            this.id = id; this.name = name; this.price = price; this.imageUriStr = imageUriStr;
        }
    }

    static class OrderItem {
        String id, name;
        double qty, price; 
        boolean isAdvance;
        OrderItem(String id, String name, double qty, double price, boolean isAdvance) {
            this.id = id; this.name = name; this.qty = qty; this.price = price;
            this.isAdvance = isAdvance;
        }
    }

    static class Order {
        String id, customerName, time, status; 
        String dateKey, monthKey; 
        ArrayList<OrderItem> items;
        double total, received, due;
        Order(String id, String customerName, ArrayList<OrderItem> items, double total, double received, double due) {
            this.id = id; this.customerName = customerName; this.items = items;
            this.total = total; this.received = received; this.due = due;
            this.status = "Pending"; 
            
            Date currentDate = new Date();
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
    private LinearLayout shopProductList, dashDynamicContent, filterBarContainer;
    private Button btnShopView, btnDashView, btnFloatingCart;
    private Button tabInventory, tabOrders, tabAnalytics;
    
    private EditText etSearchName;
    private Spinner spDateFilter;
    private String currentSubTab = "Inventory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shopContainer = findViewById(R.id.shopContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        shopProductList = findViewById(R.id.shopProductList);
        dashDynamicContent = findViewById(R.id.dashDynamicContent);
        filterBarContainer = findViewById(R.id.filterBarContainer);
        
        btnShopView = findViewById(R.id.btnShopView);
        btnDashView = findViewById(R.id.btnDashView);
        btnFloatingCart = findViewById(R.id.btnFloatingCart);
        
        tabInventory = findViewById(R.id.tabInventory);
        tabOrders = findViewById(R.id.tabOrders);
        tabAnalytics = findViewById(R.id.tabAnalytics);
        
        etSearchName = findViewById(R.id.etSearchName);
        spDateFilter = findViewById(R.id.spDateFilter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"All Orders", "Today", "This Month"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDateFilter.setAdapter(filterAdapter);

        initSeedProducts();
        renderShop();
        updateCartButton();

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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { refreshCurrentReportTab(); }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnFloatingCart.setOnClickListener(v -> showCartDialog());
    }

    // 🔥 गायब हुआ मेथड वापस जोड़ दिया गया है ताकि कंपाइलर एरर न दे
    private void updateCartButton() {
        int totalItems = regularCart.size() + advanceCart.size();
        btnFloatingCart.setText("🛒 कार्ट (" + totalItems + ")");
    }

    private void switchSubTab(Button activeTab) {
        tabInventory.setBackgroundColor(0xFFE0E0E0);
        tabOrders.setBackgroundColor(0xFFE0E0E0);
        tabAnalytics.setBackgroundColor(0xFFE0E0E0);
        activeTab.setBackgroundColor(0xFF03DAC5);
        
        if (activeTab == tabInventory) {
            currentSubTab = "Inventory";
            filterBarContainer.setVisibility(View.GONE);
        } else if (activeTab == tabOrders) {
            currentSubTab = "Orders";
            filterBarContainer.setVisibility(View.VISIBLE);
        } else {
            currentSubTab = "Analytics";
            filterBarContainer.setVisibility(View.VISIBLE);
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

        String selectedFilter = spDateFilter.getSelectedItem().toString();
        String todayKey = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
        String thisMonthKey = new SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(new Date());

        if (selectedFilter.equals("Today") && !o.dateKey.equals(todayKey)) {
            return false;
        }
        if (selectedFilter.equals("This Month") && !o.monthKey.equals(thisMonthKey)) {
            return false;
        }

        return true;
    }

    private void initSeedProducts() {
        if (products.isEmpty()) {
            products.add(new Product("p1", "गुलाब जामुन", 360, ""));
            products.add(new Product("p2", "काजू कतली", 820, ""));
            products.add(new Product("p3", "रसगुल्ला", 320, ""));
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
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 6, 0, 16);
            card.setLayoutParams(cardParams);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(24, 24, 24, 24);
            card.setElevation(3f);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView ivMithai = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(120, 120);
            imgLp.setMargins(0, 0, 16, 0);
            ivMithai.setLayoutParams(imgLp);
            ivMithai.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) {
                ivMithai.setImageURI(Uri.parse(p.imageUriStr));
            } else {
                ivMithai.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            headerRow.addView(ivMithai);

            LinearLayout nameContainer = new LinearLayout(this);
            nameContainer.setOrientation(LinearLayout.VERTICAL);
            
            TextView tvName = new TextView(this);
            tvName.setText(p.name);
            tvName.setTextSize(18);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF212121);
            nameContainer.addView(tvName);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("भाव: ₹" + p.price + "/kg");
            tvPrice.setTextColor(0xFF757575);
            tvPrice.setTextSize(13);
            nameContainer.addView(tvPrice);

            headerRow.addView(nameContainer);
            card.addView(headerRow);

            LinearLayout inputContainer = new LinearLayout(this);
            inputContainer.setOrientation(LinearLayout.HORIZONTAL);
            inputContainer.setPadding(0, 12, 0, 12);

            LinearLayout qtyBox = new LinearLayout(this);
            qtyBox.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpQty = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
            lpQty.setMargins(0, 0, 12, 0);
            qtyBox.setLayoutParams(lpQty);

            LinearLayout spinnerRow = new LinearLayout(this);
            spinnerRow.setOrientation(LinearLayout.HORIZONTAL);

            final EditText etKg = new EditText(this);
            etKg.setHint("0.00");
            etKg.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etKg.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            spinnerRow.addView(etKg);

            final Spinner spUnit = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"kg", "gm"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spUnit.setAdapter(adapter);
            spinnerRow.addView(spUnit);
            qtyBox.addView(spinnerRow);

            TextView lblKg = new TextView(this);
            lblKg.setText("⚖️ वजन मात्रा");
            lblKg.setTextSize(11);
            lblKg.setTextColor(0xFF757575);
            qtyBox.addView(lblKg);
            inputContainer.addView(qtyBox);

            LinearLayout rsBox = new LinearLayout(this);
            rsBox.setOrientation(LinearLayout.VERTICAL);
            rsBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            final EditText etRs = new EditText(this);
            etRs.setHint("₹ रुपये");
            etRs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rsBox.addView(etRs);

            TextView lblRs = new TextView(this);
            lblRs.setText("💰 कुल कैश");
            lblRs.setTextSize(11);
            lblRs.setTextColor(0xFF757575);
            rsBox.addView(lblRs);
            inputContainer.addView(rsBox);

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

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            Button btnAddRegular = new Button(this);
            btnAddRegular.setText("🛒 काउंटर कार्ट");
            btnAddRegular.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
            btnAddRegular.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lpBtn1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpBtn1.setMargins(0, 0, 8, 0);
            btnAddRegular.setLayoutParams(lpBtn1);
            
            btnAddRegular.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double qtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    double current = regularCart.containsKey(p.id) ? regularCart.get(p.id) : 0;
                    regularCart.put(p.id, current + qtyInKg);
                    Toast.makeText(MainActivity.this, p.name + " कार्ट में जमा!", Toast.LENGTH_SHORT).show();
                    etKg.setText(""); etRs.setText("");
                    updateCartButton();
                }
            });
            actionRow.addView(btnAddRegular);

            Button btnAddAdvance = new Button(this);
            btnAddAdvance.setText("📋 एडवांस Book");
            btnAddAdvance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
            btnAddAdvance.setTextColor(Color.WHITE);
            btnAddAdvance.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            
            btnAddAdvance.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double qtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    double current = advanceCart.containsKey(p.id) ? advanceCart.get(p.id) : 0;
                    advanceCart.put(p.id, current + qtyInKg);
                    Toast.makeText(MainActivity.this, p.name + " एडवांस बुक!", Toast.LENGTH_SHORT).show();
                    etKg.setText(""); etRs.setText("");
                    updateCartButton();
                }
            });
            actionRow.addView(btnAddAdvance);

            card.addView(actionRow);
            shopProductList.addView(card);
        }
    }

    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();

        Button btnAddNewProduct = new Button(this);
        btnAddNewProduct.setText("➕ नई मिठाई दुकान में जोड़ें");
        btnAddNewProduct.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
        btnAddNewProduct.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 0, 0, 16);
        btnAddNewProduct.setLayoutParams(btnLp);
        btnAddNewProduct.setOnClickListener(v -> showAddProductDialog());
        dashDynamicContent.addView(btnAddNewProduct);

        for (final Product p : products) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 16, 16, 16);
            row.setBackgroundColor(Color.WHITE);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 8);
            row.setLayoutParams(lp);

            ImageView ivThumb = new ImageView(this);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(80, 80);
            tLp.setMargins(0,0,12,0);
            ivThumb.setLayoutParams(tLp);
            if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) {
                ivThumb.setImageURI(Uri.parse(p.imageUriStr));
            } else {
                ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            row.addView(ivThumb);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(p.name + "\nभाव: ₹" + p.price + "/kg");
            tvInfo.setTextSize(16);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvInfo);

            Button btnEditProduct = new Button(this);
            btnEditProduct.setText("✏️ सुधारें");
            btnEditProduct.setTextSize(12);
            btnEditProduct.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
            btnEditProduct.setTextColor(Color.BLACK);
            btnEditProduct.setOnClickListener(v -> showEditProductDialog(p));
            row.addView(btnEditProduct);
            dashDynamicContent.addView(row);
        }
    }

    private void showAddProductDialog() {
        selectedImageUriStr = ""; 
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ नई मिठाई का विवरण");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        final EditText etName = new EditText(this); etName.setHint("मिठाई का नाम लिखें"); layout.addView(etName);
        final EditText etPrice = new EditText(this); etPrice.setHint("भाव प्रति किलो (₹)"); etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etPrice);
        Button btnSelectImg = new Button(this); btnSelectImg.setText("🖼️ गैलरी से फोटो चुनें"); layout.addView(btnSelectImg);
        previewImageView = new ImageView(this); previewImageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150)); previewImageView.setVisibility(View.GONE); layout.addView(previewImageView);
        btnSelectImg.setOnClickListener(v -> openGallery());
        builder.setView(layout);
        builder.setPositiveButton("सुरक्षित करें", (dialog, which) -> {
            String name = etName.getText().toString(); String priceStr = etPrice.getText().toString();
            if (!name.isEmpty() && !priceStr.isEmpty()) {
                products.add(new Product("p" + (products.size() + 1), name, Double.parseDouble(priceStr), selectedImageUriStr));
                renderInventoryTab();
            }
        });
        builder.show();
    }

    private void showEditProductDialog(final Product p) {
        selectedImageUriStr = p.imageUriStr; 
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏️ मिठाई का विवरण बदलें");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        final EditText etName = new EditText(this); etName.setText(p.name); layout.addView(etName);
        final EditText etPrice = new EditText(this); etPrice.setText(String.valueOf(p.price)); etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etPrice);
        Button btnSelectImg = new Button(this); btnSelectImg.setText("🖼️ नई फोटो बदलें"); layout.addView(btnSelectImg);
        previewImageView = new ImageView(this); previewImageView.setLayoutParams(new LinearLayout.LayoutParams(150, 150));
        if (p.imageUriStr != null && !p.imageUriStr.isEmpty()) { previewImageView.setImageURI(Uri.parse(p.imageUriStr)); previewImageView.setVisibility(View.VISIBLE); }
        btnSelectImg.setOnClickListener(v -> openGallery());
        builder.setView(layout);
        builder.setPositiveButton("बदलाव सेव करें", (dialog, which) -> {
            if (!etName.getText().toString().isEmpty() && !etPrice.getText().toString().isEmpty()) {
                p.name = etName.getText().toString(); p.price = Double.parseDouble(etPrice.getText().toString()); p.imageUriStr = selectedImageUriStr; renderInventoryTab();
            }
        });
        builder.show();
    }

    private void showCartDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("FINAL BILL CALCULATOR");
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        double total = 0;
        final ArrayList<OrderItem> tempItems = new ArrayList<>();

        for (Map.Entry<String, Double> entry : regularCart.entrySet()) {
            for (final Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    final double amt = entry.getValue() * p.price; total += amt;
                    tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, false));
                    LinearLayout row = new LinearLayout(this);
                    TextView tv = new TextView(this); tv.setText("• " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); row.addView(tv);
                    Button btnEditItem = new Button(this); btnEditItem.setText("⚙️ बदलें"); btnEditItem.setTextSize(10);
                    btnEditItem.setOnClickListener(v -> {
                        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this); b.setTitle(p.name + " मात्रा");
                        final EditText in = new EditText(MainActivity.this); in.setText(String.valueOf(entry.getValue())); b.setView(in);
                        b.setPositiveButton("ओके", (d, w) -> { if(!in.getText().toString().isEmpty()) { regularCart.put(p.id, Double.parseDouble(in.getText().toString())); showCartDialog(); } }); b.show();
                    });
                    row.addView(btnEditItem); layout.addView(row);
                }
            }
        }

        for (Map.Entry<String, Double> entry : advanceCart.entrySet()) {
            for (final Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    final double amt = entry.getValue() * p.price; total += amt;
                    tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, true));
                    LinearLayout row = new LinearLayout(this);
                    TextView tv = new TextView(this); tv.setText("• [Adv] " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); row.addView(tv);
                    Button btnEditItem = new Button(this); btnEditItem.setText("⚙️ बदलें"); btnEditItem.setTextSize(10);
                    btnEditItem.setOnClickListener(v -> {
                        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this); b.setTitle(p.name + " (Adv) मात्रा");
                        final EditText in = new EditText(MainActivity.this); in.setText(String.valueOf(entry.getValue())); b.setView(in);
                        b.setPositiveButton("ओके", (d, w) -> { if(!in.getText().toString().isEmpty()) { advanceCart.put(p.id, Double.parseDouble(in.getText().toString())); showCartDialog(); } }); b.show();
                    });
                    row.addView(btnEditItem); layout.addView(row);
                }
            }
        }

        final TextView tvTotal = new TextView(this); tvTotal.setText("\nकुल बिल राशि: ₹" + String.format("%.2f", total)); tvTotal.setTextSize(16); tvTotal.setTypeface(null, Typeface.BOLD); layout.addView(tvTotal);
        final EditText etReceived = new EditText(this); etReceived.setHint("प्राप्त रुपये (Cash Received)"); etReceived.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etReceived);
        final TextView tvDue = new TextView(this); tvDue.setText("बाकी उधारी राशि: ₹0.00"); tvDue.setTextColor(Color.RED); layout.addView(tvDue);

        final double finalTotal = total;
        etReceived.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                try { double rec = Double.parseDouble(s.toString()); tvDue.setText("बाकी उधारी राशि: ₹" + String.format("%.2f", Math.max(0, finalTotal - rec))); } catch (Exception e) { tvDue.setText("बाकी उधारी राशि: ₹" + String.format("%.2f", finalTotal)); }
            }
        });

        final EditText nameInput = new EditText(this); nameInput.setHint("ग्राहक का नाम"); layout.addView(nameInput);
        builder.setView(layout);
        builder.setPositiveButton("ऑर्डर डन (सेव)", (dialog, which) -> {
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
        b.setTitle("पक्की रसीद विवरण: " + o.customerName);
        LinearLayout mainLp = new LinearLayout(this); mainLp.setOrientation(LinearLayout.VERTICAL); mainLp.setPadding(30, 24, 30, 24);
        TextView tvMeta = new TextView(this); tvMeta.setText("तारीख: " + o.time + "\nस्टेटस: " + o.status + "\n--------------------"); mainLp.addView(tvMeta);
        for (OrderItem item : o.items) {
            TextView tvItem = new TextView(this); String prefix = item.isAdvance ? "[Advance] " : "";
            tvItem.setText("• " + prefix + item.name + "\n  " + String.format("%.3f", item.qty) + " kg @ ₹" + String.format("%.2f", item.price) + "/kg = ₹" + String.format("%.2f", item.qty * item.price));
            mainLp.addView(tvItem);
        }
        TextView tvSummary = new TextView(this); tvSummary.setText("--------------------\nकुल योग: ₹" + String.format("%.2f", o.total) + "\nजमा कैश: ₹" + String.format("%.2f", o.received) + "\n🔴 बाकी उधारी: ₹" + String.format("%.2f", o.due));
        tvSummary.setTypeface(null, Typeface.BOLD); mainLp.addView(tvSummary);
        b.setView(mainLp); b.setPositiveButton("ठीक है", null); b.show();
    }

    private void showPayUdharDialog(final Order o) {
        AlertDialog.Builder b = new AlertDialog.Builder(this); b.setTitle("खाता उधारी जमा: " + o.customerName);
        LinearLayout lp = new LinearLayout(this); lp.setOrientation(LinearLayout.VERTICAL); lp.setPadding(32, 24, 32, 24);
        TextView tvInfo = new TextView(this); tvInfo.setText("कुल बाकी उधारी राशि: ₹" + String.format("%.2f", o.due)); tvInfo.setTextColor(Color.RED); lp.addView(tvInfo);
        final EditText etPay = new EditText(this); etPay.setHint("जमा करने वाले रुपये लिखें"); lp.addView(etPay);
        b.setView(lp);
        b.setPositiveButton("उधारी जमा करें", (dialog, which) -> {
            if (!etPay.getText().toString().isEmpty()) {
                double pAmt = Double.parseDouble(etPay.getText().toString());
                if (pAmt <= o.due) { o.received += pAmt; o.due -= pAmt; renderOrdersTab(); }
            }
        });
        b.show();
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        int visibleCount = 0;

        for (final Order o : orders) {
            if (!shouldShowOrder(o)) continue; 
            visibleCount++;

            LinearLayout orderCard = new LinearLayout(this); orderCard.setOrientation(LinearLayout.VERTICAL); orderCard.setPadding(24, 24, 24, 24); orderCard.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 4, 0, 16); orderCard.setLayoutParams(lp);
            
            TextView tvTime = new TextView(this);
            String statusColor = o.status.equals("Complete") ? "🟢 Complete" : (o.status.equals("Rejected") ? "❌ Rejected" : "⏳ Pending");
            tvTime.setText("📅 " + o.time + " | " + statusColor); tvTime.setTextSize(12); tvTime.setTextColor(Color.GRAY); orderCard.addView(tvTime);

            TextView tvHeader = new TextView(this); tvHeader.setText("🧾 ग्राहक: " + o.customerName + "\n💰 कुल बिल: ₹" + String.format("%.2f", o.total) + " | प्राप्त: ₹" + String.format("%.2f", o.received) + "\n🔴 बाकी (Due): ₹" + String.format("%.2f", o.due));
            tvHeader.setTypeface(null, Typeface.BOLD); tvHeader.setTextColor(Color.BLACK); orderCard.addView(tvHeader);

            LinearLayout rowActions = new LinearLayout(this); rowActions.setOrientation(LinearLayout.HORIZONTAL); rowActions.setPadding(0, 12, 0, 0);

            Button btnViewDetails = new Button(this); btnViewDetails.setText("👁️ बिल विवरण"); btnViewDetails.setTextSize(11); btnViewDetails.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
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
                Button btnPayUdhar = new Button(this); btnPayUdhar.setText("💵 उधारी जमा करें"); btnPayUdhar.setTextSize(11); btnPayUdhar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800)); btnPayUdhar.setTextColor(Color.WHITE);
                btnPayUdhar.setOnClickListener(v -> showPayUdharDialog(o)); rowActions.addView(btnPayUdhar);
            }
            orderCard.addView(rowActions); dashDynamicContent.addView(orderCard);
        }

        if (visibleCount == 0) {
            TextView empty = new TextView(this); empty.setText("इस फ़िल्टर में कोई ऑर्डर नहीं मिला।"); empty.setPadding(20, 20, 20, 20); dashDynamicContent.addView(empty);
        }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews();
        double totalSales = 0, totalReceived = 0;
        int filteredCount = 0;

        for (Order o : orders) { 
            if (!shouldShowOrder(o)) continue; 
            
            if (!o.status.equals("Rejected")) { 
                totalSales += o.total; 
                totalReceived += o.received; 
                filteredCount++;
            }
        }
        
        TextView tv = new TextView(this);
        String currentFilterName = spDateFilter.getSelectedItem().toString();
        tv.setText("📊 क्लोजिंग रिपोर्ट (" + currentFilterName + ")\n\n" +
                   "📦 फ़िल्टर किए गए कुल ऑर्डर्स: " + filteredCount + "\n" +
                   "💰 कुल बिक्री (Net Sales): ₹" + String.format("%.2f", totalSales) + "\n" +
                   "💵 कुल नकद प्राप्त (Cash In Hand): ₹" + String.format("%.2f", totalReceived) + "\n" +
                   "🔴 मार्केट में बाकी उधारी (Total Due): ₹" + String.format("%.2f", (totalSales - totalReceived)));
        tv.setTextSize(16); tv.setPadding(24, 24, 24, 24); tv.setTypeface(null, Typeface.BOLD);
        dashDynamicContent.addView(tv);
    }
}
