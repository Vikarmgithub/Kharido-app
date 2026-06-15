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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    static class Product {
        String id, name, nameEn, emoji;
        double price, stock;
        Product(String id, String name, String nameEn, double price, double stock, String emoji) {
            this.id = id; this.name = name; this.nameEn = nameEn;
            this.price = price; this.stock = stock; this.emoji = emoji;
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
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, 40, "🟤"));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, 25, "🔷"));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, 35, "⚪"));
            products.add(new Product("p4", "बेसन लड्डू", "Besan Ladoo", 400, 30, "🟡"));
        }
    }

    // --- नया ऑर्डर व्यू (Kg/Gm ड्रॉपडाउन और लाइव लेबल) ---
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

            // हेडर रो (बड़ी इमोजी फोटो + नाम)
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(p.emoji);
            tvEmoji.setTextSize(32); // बड़ी फ़ोटो जैसी लुक
            tvEmoji.setPadding(0, 0, 16, 0);
            headerRow.addView(tvEmoji);

            LinearLayout nameContainer = new LinearLayout(this);
            nameContainer.setOrientation(LinearLayout.VERTICAL);
            
            TextView tvName = new TextView(this);
            tvName.setText(p.name);
            tvName.setTextSize(18);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF212121);
            nameContainer.addView(tvName);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("भाव: ₹" + p.price + "/kg | स्टॉक: " + String.format("%.2f", p.stock) + " kg");
            tvPrice.setTextSize(13);
            tvPrice.setTextColor(0xFF757575);
            nameContainer.addView(tvPrice);

            headerRow.addView(nameContainer);
            card.addView(headerRow);

            // इनपुट कंटेनर (Labels के साथ)
            LinearLayout inputContainer = new LinearLayout(this);
            inputContainer.setOrientation(LinearLayout.HORIZONTAL);
            inputContainer.setPadding(0, 12, 0, 12);

            // मात्रा सेक्शन (EditText + Spinner Dropdown)
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
            lblKg.setText("⚖️ वजन की मात्रा लिखें");
            lblKg.setTextSize(11);
            lblKg.setTextColor(0xFF757575);
            qtyBox.addView(lblKg);
            inputContainer.addView(qtyBox);

            // कैश सेक्शन (रुपये बॉक्स)
            LinearLayout rsBox = new LinearLayout(this);
            rsBox.setOrientation(LinearLayout.VERTICAL);
            rsBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            final EditText etRs = new EditText(this);
            etRs.setHint("₹ नकद राशि");
            etRs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rsBox.addView(etRs);

            TextView lblRs = new TextView(this);
            lblRs.setText("💰 कुल रुपये लिखें");
            lblRs.setTextSize(11);
            lblRs.setTextColor(0xFF757575);
            rsBox.addView(lblRs);
            inputContainer.addView(rsBox);

            card.addView(inputContainer);

            // 🔥 एडवांस्ड ग्राम/किलो लाइव कैलकुलेशन
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

            spUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    etKg.setText(""); etRs.setText("");
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // कार्ट बटन
            Button btnAdd = new Button(this);
            btnAdd.setText("🛒 कार्ट में जोड़ें");
            btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
            btnAdd.setTextColor(Color.WHITE);
            
            btnAdd.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double finalQtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    
                    if (finalQtyInKg <= p.stock) {
                        double currentInCart = cart.containsKey(p.id) ? cart.get(p.id) : 0;
                        cart.put(p.id, currentInCart + finalQtyInKg);
                        Toast.makeText(MainActivity.this, p.name + " कार्ट में सुरक्षित!", Toast.LENGTH_SHORT).show();
                        etKg.setText(""); etRs.setText("");
                        updateCartButton();
                    } else {
                        Toast.makeText(MainActivity.this, "स्टॉक कम है!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            card.addView(btnAdd);
            shopProductList.addView(card);
        }
    }

    private void updateCartButton() {
        btnFloatingCart.setText("🛒 कार्ट (" + cart.size() + ")");
    }

    // --- इन्वेंट्री टैब (स्टॉक अपडेट फ़ीचर के साथ) ---
    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();
        for (final Product p : products) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 16, 16, 16);
            row.setBackgroundColor(Color.WHITE);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 8);
            row.setLayoutParams(lp);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(p.emoji + " " + p.name + "\nस्टॉक: " + String.format("%.2f", p.stock) + " kg");
            tvInfo.setTextSize(16);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvInfo);

            // ✏️ स्टॉक अपडेट बटन
            Button btnUpdateStock = new Button(this);
            btnUpdateStock.setText("✏️ स्टॉक भरें");
            btnUpdateStock.setTextSize(12);
            btnUpdateStock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
            btnUpdateStock.setTextColor(Color.BLACK);
            
            btnUpdateStock.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle(p.name + " - नया स्टॉक जोड़ें");
                
                final EditText input = new EditText(MainActivity.this);
                input.setHint("नया स्टॉक किलो (kg) में लिखें");
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                b.setView(input);

                b.setPositiveButton("अपडेट करें", (dialog, which) -> {
                    String str = input.getText().toString();
                    if (!str.isEmpty()) {
                        p.stock = Double.parseDouble(str); // स्टॉक सीधा अपडेट
                        renderInventoryTab(); // स्क्रीन रिफ्रेश
                        Toast.makeText(MainActivity.this, "स्टॉक अपडेट सफल!", Toast.LENGTH_SHORT).show();
                    }
                });
                b.setNegativeButton("कैंसिल", null);
                b.show();
            });

            row.addView(btnUpdateStock);
            dashDynamicContent.addView(row);
        }
    }

    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("फाइनल बिल");
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
                    layout.addView(itemTv);
                }
            }
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("\nकुल राशि: ₹" + String.format("%.2f", total));
        totalTv.setTextSize(18);
        totalTv.setTypeface(null, Typeface.BOLD);
        layout.addView(totalTv);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("ग्राहक का नाम");
        layout.addView(nameInput);
        builder.setView(layout);

        final double finalTotal = total;
        builder.setPositiveButton("ऑर्डर पक्का करें", (dialog, which) -> {
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
                Toast.makeText(MainActivity.this, "ऑर्डर सुरक्षित! 🙏", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("बंद करें", null);
        builder.show();
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        for (Order o : orders) {
            TextView tv = new TextView(this);
            tv.setText("🧾 ग्राहक: " + o.customerName + "\nकुल बिल: ₹" + String.format("%.2f", o.total) + "\nसमय: " + o.time + "\n--------------------");
            dashDynamicContent.addView(tv);
        }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews();
        double totalSales = 0;
        for (Order o : orders) totalSales += o.total;
        TextView tv = new TextView(this);
        tv.setText("📊 काउंटर रिपोर्ट\n\n💰 कुल गल्ला कैश: ₹" + String.format("%.2f", totalSales));
        tv.setTextSize(18);
        dashDynamicContent.addView(tv);
    }
}
