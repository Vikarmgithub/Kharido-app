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
        boolean isAdvance; // एडवांस ऑर्डर ट्रैक करने के लिए
        OrderItem(String id, String name, double qty, double price, boolean isAdvance) {
            this.id = id; this.name = name; this.qty = qty; this.price = price;
            this.isAdvance = isAdvance;
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
    
    // दो अलग कार्ट: एक तुरंत बिक्री के लिए, एक एडवांस बुकिंग के लिए
    private HashMap<String, Double> regularCart = new HashMap<>();
    private HashMap<String, Double> advanceCart = new HashMap<>();

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
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, 15, "🟤"));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, 0, "🔷")); // स्टॉक खत्म (टेस्टिंग के लिए 0)
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, 20, "⚪"));
            products.add(new Product("p4", "बेसन लड्डू", "Besan Ladoo", 400, 5, "🟡"));
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

            // मिठाई हेडर
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvEmoji = new TextView(this);
            tvEmoji.setText(p.emoji);
            tvEmoji.setTextSize(32);
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
            // अगर स्टॉक 0 है तो लाल रंग में "स्टॉक खत्म" दिखाएं
            if (p.stock <= 0) {
                tvPrice.setText("भाव: ₹" + p.price + "/kg | ⚠️ स्टॉक खत्म (सिर्फ एडवांस ऑर्डर)");
                tvPrice.setTextColor(Color.RED);
            } else {
                tvPrice.setText("भाव: ₹" + p.price + "/kg | स्टॉक: " + String.format("%.2f", p.stock) + " kg");
                tvPrice.setTextColor(0xFF757575);
            }
            tvPrice.setTextSize(13);
            nameContainer.addView(tvPrice);

            headerRow.addView(nameContainer);
            card.addView(headerRow);

            // इनपुट कंटेनर (लाइव लेबल्स के साथ)
            LinearLayout inputContainer = new LinearLayout(this);
            inputContainer.setOrientation(LinearLayout.HORIZONTAL);
            inputContainer.setPadding(0, 12, 0, 12);

            // वजन फ़ील्ड + ड्रॉपडाउन
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
            lblKg.setText("⚖️ वजन की मात्रा");
            lblKg.setTextSize(11);
            lblKg.setTextColor(0xFF757575);
            qtyBox.addView(lblKg);
            inputContainer.addView(qtyBox);

            // रुपये फ़ील्ड
            LinearLayout rsBox = new LinearLayout(this);
            rsBox.setOrientation(LinearLayout.VERTICAL);
            rsBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            final EditText etRs = new EditText(this);
            etRs.setHint("₹ रुपये");
            etRs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rsBox.addView(etRs);

            TextView lblRs = new TextView(this);
            lblRs.setText("💰 कुल कैश राशि");
            lblRs.setTextSize(11);
            lblRs.setTextColor(0xFF757575);
            rsBox.addView(lblRs);
            inputContainer.addView(rsBox);

            card.addView(inputContainer);

            // लाइव कनवर्टर कैलकुलेशन लिस्नर्स
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

            // दो बटन्स के लिए होरिज़ॉन्टल रो (रेगुलर कार्ट और एडवांस ऑर्डर)
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            actionRow.setLayoutParams(actionParams);

            // बटन 1: काउंटर कार्ट में जोड़ें
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
                    
                    if (p.stock <= 0) {
                        Toast.makeText(MainActivity.this, "स्टॉक नहीं है! कृपया 'एडवांस ऑर्डर' बटन का उपयोग करें।", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (qtyInKg <= p.stock) {
                        double current = regularCart.containsKey(p.id) ? regularCart.get(p.id) : 0;
                        regularCart.put(p.id, current + qtyInKg);
                        Toast.makeText(MainActivity.this, p.name + " काउंटर कार्ट में जमा!", Toast.LENGTH_SHORT).show();
                        etKg.setText(""); etRs.setText("");
                        updateCartButton();
                    } else {
                        Toast.makeText(MainActivity.this, "स्टॉक कम है!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            actionRow.addView(btnAddRegular);

            // बटन 2: 📋 एडवांस ऑर्डर में जोड़ें (यह स्टॉक 0 होने पर भी काम करेगा)
            Button btnAddAdvance = new Button(this);
            btnAddAdvance.setText("📋 एडवांस बुक");
            btnAddAdvance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800)); // ऑरेंज कलर
            btnAddAdvance.setTextColor(Color.WHITE);
            btnAddAdvance.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            
            btnAddAdvance.setOnClickListener(v -> {
                String kgStr = etKg.getText().toString();
                if (!kgStr.isEmpty()) {
                    double inputVal = Double.parseDouble(kgStr);
                    double qtyInKg = spUnit.getSelectedItem().toString().equals("gm") ? (inputVal / 1000.0) : inputVal;
                    
                    double current = advanceCart.containsKey(p.id) ? advanceCart.get(p.id) : 0;
                    advanceCart.put(p.id, current + qtyInKg);
                    Toast.makeText(MainActivity.this, p.name + " एडवांस ऑर्डर बुक!", Toast.LENGTH_SHORT).show();
                    etKg.setText(""); etRs.setText("");
                    updateCartButton();
                } else {
                    Toast.makeText(MainActivity.this, "कृपया मात्रा दर्ज करें!", Toast.LENGTH_SHORT).show();
                }
            });
            actionRow.addView(btnAddAdvance);

            card.addView(actionRow);
            shopProductList.addView(card);
        }
    }

    private void updateCartButton() {
        int totalItems = regularCart.size() + advanceCart.size();
        btnFloatingCart.setText("🛒 कार्ट (" + totalItems + ")");
    }

    // --- फाइनल बिल रसीद (काउंटर + एडवांस का वर्गीकरण) ---
    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("बिल रसीद समरी");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        double totalAmount = 0;
        final ArrayList<OrderItem> tempItems = new ArrayList<>();

        // 1. काउंटर आइटम्स डिस्प्ले
        if (!regularCart.isEmpty()) {
            TextView tvTitleReg = new TextView(this);
            tvTitleReg.setText("📦 तुरंत काउंटर डिलीवरी:");
            tvTitleReg.setTypeface(null, Typeface.BOLD);
            layout.addView(tvTitleReg);

            for (Map.Entry<String, Double> entry : regularCart.entrySet()) {
                for (Product p : products) {
                    if (p.id.equals(entry.getKey())) {
                        double amt = entry.getValue() * p.price;
                        totalAmount += amt;
                        tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, false));

                        TextView itemTv = new TextView(this);
                        itemTv.setText("  • " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                        layout.addView(itemTv);
                    }
                }
            }
        }

        // 2. एडवांस ऑर्डर्स डिस्प्ले
        if (!advanceCart.isEmpty()) {
            TextView tvTitleAdv = new TextView(this);
            tvTitleAdv.setText("\n📋 अग्रिम बुकिंग (Advance Orders):");
            tvTitleAdv.setTypeface(null, Typeface.BOLD);
            tvTitleAdv.setTextColor(0xFFFF9800);
            layout.addView(tvTitleAdv);

            for (Map.Entry<String, Double> entry : advanceCart.entrySet()) {
                for (Product p : products) {
                    if (p.id.equals(entry.getKey())) {
                        double amt = entry.getValue() * p.price;
                        totalAmount += amt;
                        tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, true));

                        TextView itemTv = new TextView(this);
                        itemTv.setText("  • [Advance] " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt));
                        layout.addView(itemTv);
                    }
                }
            }
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("\nकुल फाइनल बिल: ₹" + String.format("%.2f", totalAmount));
        totalTv.setTextSize(18);
        totalTv.setTypeface(null, Typeface.BOLD);
        totalTv.setTextColor(Color.BLACK);
        layout.addView(totalTv);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("ग्राहक का नाम दर्ज करें");
        layout.addView(nameInput);
        builder.setView(layout);

        final double finalTotal = totalAmount;
        builder.setPositiveButton("ऑर्डर डन करें", (dialog, which) -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty() && (!regularCart.isEmpty() || !advanceCart.isEmpty())) {
                // सिर्फ लाइव बिक्री वाले का स्टॉक घटाएं, एडवांस वाले का नहीं
                for (OrderItem item : tempItems) {
                    if (!item.isAdvance) {
                        for (Product p : products) {
                            if (p.id.equals(item.id)) p.stock -= item.qty;
                        }
                    }
                }
                orders.add(new Order("ORD" + System.currentTimeMillis(), name, tempItems, finalTotal));
                regularCart.clear();
                advanceCart.clear();
                updateCartButton();
                Toast.makeText(MainActivity.this, "सफलतापूर्वक बुक हो गया! 👍", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("बंद करें", null);
        builder.show();
    }

    // --- इन्वेंट्री स्टॉक मैनेजमेंट टैब ---
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
            tvInfo.setText(p.emoji + " " + p.name + "\nस्टॉक काउंटर: " + String.format("%.2f", p.stock) + " kg");
            tvInfo.setTextSize(16);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvInfo);

            Button btnUpdateStock = new Button(this);
            btnUpdateStock.setText("✏️ स्टॉक भरें");
            btnUpdateStock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
            btnUpdateStock.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle(p.name + " - स्टॉक रीलोड");
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                b.setView(input);
                b.setPositiveButton("भरें", (d, w) -> {
                    if (!input.getText().toString().isEmpty()) {
                        p.stock = Double.parseDouble(input.getText().toString());
                        renderInventoryTab();
                    }
                });
                b.show();
            });
            row.addView(btnUpdateStock);
            dashDynamicContent.addView(row);
        }
    }

    // --- ऑर्डर्स रिपोर्ट टैब (लाइव और एडवांस अलग-अलग वर्गीकरण) ---
    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        if (orders.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("कोई बिक्री रिकॉर्ड नहीं मिला।");
            dashDynamicContent.addView(empty);
            return;
        }

        for (Order o : orders) {
            LinearLayout orderCard = new LinearLayout(this);
            orderCard.setOrientation(LinearLayout.VERTICAL);
            orderCard.setPadding(16, 16, 16, 16);
            orderCard.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 12);
            orderCard.setLayoutParams(lp);

            TextView tvHeader = new TextView(this);
            tvHeader.setText("🧾 ग्राहक: " + o.customerName + " | कुल बिल: ₹" + String.format("%.2f", o.total));
            tvHeader.setTypeface(null, Typeface.BOLD);
            orderCard.addView(tvHeader);

            // ऑर्डर के अंदर का माल दिखाएं
            for (OrderItem item : o.items) {
                TextView tvItem = new TextView(this);
                if (item.isAdvance) {
                    tvItem.setText("  • [⚠️ एडवांस बुकिंग] " + item.name + " - " + String.format("%.3f", item.qty) + " kg");
                    tvItem.setTextColor(0xFFFF9800);
                } else {
                    tvItem.setText("  • [✅ काउंटर सेल्स] " + item.name + " - " + String.format("%.3f", item.qty) + " kg");
                    tvItem.setTextColor(0xFF4CAF50);
                }
                orderCard.addView(tvItem);
            }
            dashDynamicContent.addView(orderCard);
        }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews();
        double totalSales = 0;
        for (Order o : orders) totalSales += o.total;
        TextView tv = new TextView(this);
        tv.setText("📊 काउंटर क्लोजिंग रिपोर्ट\n\n💰 कुल गल्ला कैश कलेक्शन: ₹" + String.format("%.2f", totalSales) + "\n🧾 कुल पर्चियां: " + orders.size());
        tv.setTextSize(18);
        tv.setPadding(20, 20, 20, 20);
        dashDynamicContent.addView(tv);
    }
}
