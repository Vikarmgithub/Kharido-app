package com.khridoapp.mithai;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    static class Product {
        String id, name, nameEn;
        double price, stock;
        int imageResId; // मिठाई की फोटो का आईडी
        Product(String id, String name, String nameEn, double price, double stock, int imageResId) {
            this.id = id; this.name = name; this.nameEn = nameEn;
            this.price = price; this.stock = stock; this.imageResId = imageResId;
        }
    }

    static class OrderItem {
        String id, name;
        double qty, price;
        OrderItem(String id, String name, double qty, double price) {
            this.id = id; this.name = name; this.qty = qty; this.price = price;
        }
    }

    static class Order {
        String id, customerName, time;
        ArrayList<OrderItem> items;
        double total;
        Order(String id, String customerName, ArrayList<OrderItem> items, double total) {
            this.id = id; this.customerName = customerName; this.items = items;
            this.total = total;
            this.time = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        }
    }

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private HashMap<String, Double> cart = new HashMap<>();

    private ScrollView shopContainer, dashboardContainer;
    private LinearLayout shopProductList, dashDynamicContent;
    private Button btnShopView, btnDashView, btnFloatingCart;
    private Button tabInventory, tabOrders, tabAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shopContainer = findViewById(R.id.shopContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        shopProductList = findViewById(R.id.shopProductList);
        dashDynamicContent = findViewById(R.id.dashDynamicContent);
        btnShopView = findViewById(R.id.btnShopView);
        btnDashView = findViewById(R.id.btnDashView);
        btnFloatingCart = findViewById(R.id.btnFloatingCart);
        
        tabInventory = findViewById(R.id.tabInventory);
        tabOrders = findViewById(R.id.tabOrders);
        tabAnalytics = findViewById(R.id.tabAnalytics);

        initSeedProducts();
        renderShop();
        updateCartButton();

        btnShopView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.VISIBLE);
            dashboardContainer.setVisibility(View.GONE);
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
        
        btnFloatingCart.setOnClickListener(v -> showCartDialog());
    }

    private void switchSubTab(Button activeTab) {
        tabInventory.setBackgroundColor(0xFFE0E0E0);
        tabOrders.setBackgroundColor(0xFFE0E0E0);
        tabAnalytics.setBackgroundColor(0xFFE0E0E0);
        activeTab.setBackgroundColor(0xFF03DAC5);
    }

    private void initSeedProducts() {
        if (products.isEmpty()) {
            // यहाँ हमने एंड्रॉइड का डिफ़ॉल्ट आइकॉन सिलेक्ट किया है, आप अपनी इमेजेस भी डाल सकते हैं
            int defaultImg = android.R.drawable.ic_menu_gallery; 
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, 40, defaultImg));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, 25, defaultImg));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, 35, defaultImg));
            products.add(new Product("p4", "बेसन लड्डू", "Besan Ladoo", 400, 30, defaultImg));
        }
    }

    // --- इमेज और लाइव ऑटो-कनवर्टर के साथ नया ऑर्डर लेआउट ---
    private void renderShop() {
        shopProductList.removeAllViews();
        for (final Product p : products) {
            
            // मुख्य कार्ड लेआउट
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 6, 0, 16);
            card.setLayoutParams(cardParams);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(20, 20, 20, 20);
            card.setElevation(3f);

            // नाम और फोटो को साथ दिखाने के लिए होरिज़ॉन्टल रो
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            headerRow.setPadding(0, 0, 0, 12);

            // 1. मिठाई की फोटो (ImageView)
            ImageView ivProduct = new ImageView(this);
            ivProduct.setImageResource(p.imageResId);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(110, 110);
            imgParams.setMargins(0, 0, 16, 0);
            ivProduct.setLayoutParams(imgParams);
            ivProduct.setScaleType(ImageView.ScaleType.CENTER_CROP);
            headerRow.addView(ivProduct);

            // 2. मिठाई का नाम और भाव
            LinearLayout nameContainer = new LinearLayout(this);
            nameContainer.setOrientation(LinearLayout.VERTICAL);
            
            TextView tvName = new TextView(this);
            tvName.setText(p.name);
            tvName.setTextSize(18);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF212121);
            nameContainer.addView(tvName);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("भाव: ₹" + p.price + "/kg  | स्टॉक: " + String.format("%.2f", p.stock) + " kg");
            tvPrice.setTextSize(13);
            tvPrice.setTextColor(0xFF757575);
            nameContainer.addView(tvPrice);

            headerRow.addView(nameContainer);
            card.addView(headerRow);

            // इनपुट फ़ील्ड्स (Kg और Cash का लाइव कॉम्बो)
            LinearLayout inputRow = new LinearLayout(this);
            inputRow.setOrientation(LinearLayout.HORIZONTAL);

            final EditText etKg = new EditText(this);
            etKg.setHint("मात्रा (किलो / kg)");
            etKg.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp1.setMargins(0, 0, 12, 0);
            etKg.setLayoutParams(lp1);
            inputRow.addView(etKg);

            final EditText etRs = new EditText(this);
            etRs.setHint("कैश (रुपये / ₹)");
            etRs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etRs.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            inputRow.addView(etRs);

            // 🔥 लाइव ऑटोमैटिक कैलकुलेटर लॉजिक (TextWatchers)
            etKg.addTextChangedListener(new TextWatcher() {
                private boolean isChanging = false;
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isChanging) return;
                    if (etKg.hasFocus()) {
                        isChanging = true;
                        try {
                            double kg = Double.parseDouble(s.toString());
                            etRs.setText(String.format("%.2f", kg * p.price));
                        } catch (Exception e) { etRs.setText(""); }
                        isChanging = false;
                    }
                }
            });

            etRs.addTextChangedListener(new TextWatcher() {
                private boolean isChanging = false;
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isChanging) return;
                    if (etRs.hasFocus()) {
                        isChanging = true;
                        try {
                            double rs = Double.parseDouble(s.toString());
                            // जब कोई 109 रुपये डालेगा तो यहाँ सटीक किलो शो होगा (उदा. 0.303 kg)
                            etKg.setText(String.format("%.3f", rs / p.price));
                        } catch (Exception e) { etKg.setText(""); }
                        isChanging = false;
                    }
                }
            });

            card.addView(inputRow);

            // मिठाई का अपना अलग कार्ट बटन
            Button btnAdd = new Button(this);
            btnAdd.setText("🛒 कार्ट में जोड़ें");
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
            btnAdd.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 10, 0, 0);
            btnAdd.setLayoutParams(btnParams);
            
            btnAdd.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double finalQty = Double.parseDouble(kgStr);
                    if (finalQty <= p.stock) {
                        double currentInCart = cart.containsKey(p.id) ? cart.get(p.id) : 0;
                        cart.put(p.id, currentInCart + finalQty);
                        Toast.makeText(MainActivity.this, p.name + " कार्ट में जोड़ा गया!", Toast.LENGTH_SHORT).show();
                        etKg.setText("");
                        etRs.setText("");
                        updateCartButton();
                    } else {
                        Toast.makeText(MainActivity.this, "इतना स्टॉक काउंटर पर नहीं है!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "कृपया मात्रा या नकद रुपये लिखें!", Toast.LENGTH_SHORT).show();
                }
            });

            card.addView(btnAdd);
            shopProductList.addView(card);
        }
    }

    private void updateCartButton() {
        btnFloatingCart.setText("🛒 कार्ट (" + cart.size() + ")");
    }

    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("फाइनल बिल रसीद");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        double total = 0;
        final ArrayList<OrderItem> tempItems = new ArrayList<>();

        for (Map.Entry<String, Double> entry : cart.entrySet()) {
            for (Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    double amt = entry.getValue() * p.price;
                    total += amt;
                    tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price));

                    TextView itemTv = new TextView(this);
                    itemTv.setText("• " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                    itemTv.setTextSize(15);
                    itemTv.setPadding(0, 4, 0, 4);
                    layout.addView(itemTv);
                }
            }
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("\nकुल राशि: ₹" + String.format("%.2f", total));
        totalTv.setTextSize(18);
        totalTv.setTypeface(null, Typeface.BOLD);
        totalTv.setTextColor(Color.BLACK);
        layout.addView(totalTv);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("ग्राहक का नाम दर्ज करें");
        layout.addView(nameInput);

        builder.setView(layout);
        final double finalTotal = total;
        builder.setPositiveButton("प्रिंट / सेव करें", (dialog, which) -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty() && !cart.isEmpty()) {
                for (OrderItem item : tempItems) {
                    for (Product p : products) {
                        if (p.id.equals(item.id)) p.stock -= item.qty;
                    }
                }
                orders.add(new Order("ORD" + System.currentTimeMillis(), name, tempItems, finalTotal));
                cart.clear();
                updateCartButton();
                Toast.makeText(MainActivity.this, "बिल सुरक्षित कर लिया गया है! 👍", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "नाम लिखना जरूरी है!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("हटाएं", null);
        builder.show();
    }

    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();
        for (Product p : products) {
            TextView tv = new TextView(this);
            tv.setText("📦 " + p.name + "\nउपलब्ध स्टॉक मात्रा: " + String.format("%.3f", p.stock) + " kg\n");
            tv.setTextSize(16);
            tv.setPadding(16, 16, 16, 16);
            dashDynamicContent.addView(tv);
        }
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        if (orders.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("आज कोई बिल नहीं कटा।");
            empty.setGravity(Gravity.CENTER);
            dashDynamicContent.addView(empty);
            return;
        }
        for (Order o : orders) {
            TextView tv = new TextView(this);
            tv.setText("🧾 रसीद ग्राहक: " + o.customerName + "\nकुल नकद: ₹" + String.format("%.2f", o.total) + "\nसमय: " + o.time + "\n--------------------");
            tv.setTextSize(15);
            tv.setPadding(16, 16, 16, 16);
            dashDynamicContent.addView(tv);
        }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews();
        double totalSales = 0;
        for (Order o : orders) totalSales += o.total;

        TextView tv = new TextView(this);
        tv.setText("📊 आज की कुल काउंटर रिपोर्ट\n\n💰 नेट गल्ला कैश: ₹" + String.format("%.2f", totalSales) + "\n🧾 कुल कटे बिल: " + orders.size());
        tv.setTextSize(18);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(24, 24, 24, 24);
        dashDynamicContent.addView(tv);
    }
}
