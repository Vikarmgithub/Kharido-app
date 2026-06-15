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
        boolean isAdvance;
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
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", 360, 45, "🟤"));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", 820, 20, "🔷"));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", 320, 35, "⚪"));
            products.add(new Product("p4", "बेसन लड्डू", "Besan Ladoo", 400, 15, "🟡"));
        }
    }

    // --- नया ऑर्डर काउंटर रेंडर ---
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
            if (p.stock <= 0) {
                tvPrice.setText("भाव: ₹" + p.price + "/kg | ⚠️ स्टॉक खत्म (सिर्फ एडवांस)");
                tvPrice.setTextColor(Color.RED);
            } else {
                tvPrice.setText("भाव: ₹" + p.price + "/kg | स्टॉक: " + String.format("%.2f", p.stock) + " kg");
                tvPrice.setTextColor(0xFF757575);
            }
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
            lblKg.setText("⚖️ वजन की मात्रा");
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
                    
                    if (p.stock <= 0) {
                        Toast.makeText(MainActivity.this, "स्टॉक नहीं है! एडवांस बुक करें।", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (qtyInKg <= p.stock) {
                        double current = regularCart.containsKey(p.id) ? regularCart.get(p.id) : 0;
                        regularCart.put(p.id, current + qtyInKg);
                        Toast.makeText(MainActivity.this, p.name + " काउंटर कार्ट में!", Toast.LENGTH_SHORT).show();
                        etKg.setText(""); etRs.setText("");
                        updateCartButton();
                    }
                }
            });
            actionRow.addView(btnAddRegular);

            Button btnAddAdvance = new Button(this);
            btnAddAdvance.setText("📋 एडवांस बुक");
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

    private void updateCartButton() {
        int totalItems = regularCart.size() + advanceCart.size();
        btnFloatingCart.setText("🛒 कार्ट (" + totalItems + ")");
    }

    // --- इन्वेंट्री टैब (➕ नया प्रोडक्ट और 💰 भाव बदलें फ़ीचर्स के साथ) ---
    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();

        // ➕ शीर्ष पर नया उत्पाद जोड़ें बटन
        Button btnAddNewProduct = new Button(this);
        btnAddNewProduct.setText("➕ नई मिठाई दुकान में जोड़ें");
        btnAddNewProduct.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6200EE));
        btnAddNewProduct.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 0, 0, 16);
        btnAddNewProduct.setLayoutParams(btnLp);
        
        btnAddNewProduct.setOnClickListener(v -> showAddProductDialog());
        dashDynamicContent.addView(btnAddNewProduct);

        // मिठाइयों की सूची
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
            tvInfo.setText(p.emoji + " " + p.name + "\nभाव: ₹" + p.price + "/kg | स्टॉक: " + String.format("%.2f", p.stock) + " kg");
            tvInfo.setTextSize(15);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvInfo);

            // 💰 नया भाव बदलें (Rate Option) बटन
            Button btnUpdateRate = new Button(this);
            btnUpdateRate.setText("💰 भाव बदलें");
            btnUpdateRate.setTextSize(12);
            btnUpdateRate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF03DAC5));
            btnUpdateRate.setTextColor(Color.BLACK);
            
            btnUpdateRate.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setTitle(p.name + " - नया रेट तय करें");
                
                final EditText input = new EditText(MainActivity.this);
                input.setHint("नया भाव (रुपये/kg) लिखें");
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                b.setView(input);

                b.setPositiveButton("रेट बदलें", (dialog, which) -> {
                    String str = input.getText().toString();
                    if (!str.isEmpty()) {
                        p.price = Double.parseDouble(str); // भाव अपडेट
                        renderInventoryTab(); // रिफ्रेश इन्वेंट्री
                        Toast.makeText(MainActivity.this, "नया भाव लागू हो गया!", Toast.LENGTH_SHORT).show();
                    }
                });
                b.setNegativeButton("कैंसिल", null);
                b.show();
            });

            row.addView(btnUpdateRate);
            dashDynamicContent.addView(row);
        }
    }

    // --- ➕ नई मिठाई जोड़ने का फॉर्म डायलॉग ---
    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ नई मिठाई का विवरण भरें");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        final EditText etName = new EditText(this); etName.setHint("मिठाई का नाम (उदा: पेड़ा)"); layout.addView(etName);
        final EditText etNameEn = new EditText(this); etNameEn.setHint("English Name (Key)"); layout.addView(etNameEn);
        final EditText etPrice = new EditText(this); etPrice.setHint("भाव प्रति किलो (₹ Rate)"); etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etPrice);
        final EditText etStock = new EditText(this); etStock.setHint("शुरुआती स्टॉक मात्रा (kg)"); etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); layout.addView(etStock);
        final EditText etEmoji = new EditText(this); etEmoji.setHint("फोटो इमोजी (उदा: 🥮, 🟥)"); layout.addView(etEmoji);

        builder.setView(layout);

        builder.setPositiveButton("दुकान में जोड़ें", (dialog, which) -> {
            String name = etName.getText().toString();
            String nameEn = etNameEn.getText().toString();
            String priceStr = etPrice.getText().toString();
            String stockStr = etStock.getText().toString();
            String emoji = etEmoji.getText().toString().isEmpty() ? "🍡" : etEmoji.getText().toString();

            if (!name.isEmpty() && !priceStr.isEmpty() && !stockStr.isEmpty()) {
                double price = Double.parseDouble(priceStr);
                double stock = Double.parseDouble(stockStr);
                String id = "p" + (products.size() + 1);

                // लिस्ट में ऐड होते ही यह दोनों टैब में दिखने लगेगा
                products.add(new Product(id, name, nameEn, price, stock, emoji));
                
                renderInventoryTab(); // इन्वेंट्री अपडेट
                Toast.makeText(MainActivity.this, name + " काउंटर और स्टॉक दोनों में जुड़ गई! 🎉", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "कृपया पूरा विवरण सही भरें!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("कैंसिल", null);
        builder.show();
    }

    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("फाइनल बिल");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        double total = 0;
        final ArrayList<OrderItem> tempItems = new ArrayList<>();

        if (!regularCart.isEmpty()) {
            TextView tvTitleReg = new TextView(this); tvTitleReg.setText("📦 तुरंत काउंटर डिलीवरी:"); tvTitleReg.setTypeface(null, Typeface.BOLD); layout.addView(tvTitleReg);
            for (Map.Entry<String, Double> entry : regularCart.entrySet()) {
                for (Product p : products) {
                    if (p.id.equals(entry.getKey())) {
                        double amt = entry.getValue() * p.price; total += amt;
                        tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, false));
                        TextView itemTv = new TextView(this); itemTv.setText("  • " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt)); layout.addView(itemTv);
                    }
                }
            }
        }

        if (!advanceCart.isEmpty()) {
            TextView tvTitleAdv = new TextView(this); tvTitleAdv.setText("\n📋 अग्रिम बुकिंग (Advance):"); tvTitleAdv.setTypeface(null, Typeface.BOLD); tvTitleAdv.setTextColor(0xFFFF9800); layout.addView(tvTitleAdv);
            for (Map.Entry<String, Double> entry : advanceCart.entrySet()) {
                for (Product p : products) {
                    if (p.id.equals(entry.getKey())) {
                        double amt = entry.getValue() * p.price; total += amt;
                        tempItems.add(new OrderItem(p.id, p.name, entry.getValue(), p.price, true));
                        TextView itemTv = new TextView(this); itemTv.setText("  • [Advance] " + p.name + ": " + String.format("%.3f", entry.getValue()) + " kg = ₹" + String.format("%.2f", amt)); layout.addView(itemTv);
                    }
                }
            }
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("\nकुल राशि: ₹" + String.format("%.2f", total));
        totalTv.setTextSize(18); totalTv.setTypeface(null, Typeface.BOLD);
        layout.addView(totalTv);

        final EditText nameInput = new EditText(this); nameInput.setHint("ग्राहक का नाम"); layout.addView(nameInput);
        builder.setView(layout);

        final double finalTotal = total;
        builder.setPositiveButton("ऑर्डर पक्का करें", (dialog, which) -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty() && (!regularCart.isEmpty() || !advanceCart.isEmpty())) {
                for (OrderItem item : tempItems) {
                    if (!item.isAdvance) {
                        for (Product p : products) { if (p.id.equals(item.id)) p.stock -= item.qty; }
                    }
                }
                orders.add(new Order("ORD" + System.currentTimeMillis(), name, tempItems, finalTotal));
                regularCart.clear(); advanceCart.clear();
                updateCartButton();
                Toast.makeText(MainActivity.this, "ऑर्डर सुरक्षित! 🙏", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        for (Order o : orders) {
            LinearLayout orderCard = new LinearLayout(this); orderCard.setOrientation(LinearLayout.VERTICAL); orderCard.setPadding(16, 16, 16, 16); orderCard.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 4, 0, 12); orderCard.setLayoutParams(lp);
            TextView tvHeader = new TextView(this); tvHeader.setText("🧾 ग्राहक: " + o.customerName + " | कुल बिल: ₹" + String.format("%.2f", o.total)); tvHeader.setTypeface(null, Typeface.BOLD); orderCard.addView(tvHeader);
            for (OrderItem item : o.items) {
                TextView tvItem = new TextView(this);
                if (item.isAdvance) { tvItem.setText("  • [⚠️ एडवांस] " + item.name + " - " + String.format("%.3f", item.qty) + " kg"); tvItem.setTextColor(0xFFFF9800); }
                else { tvItem.setText("  • [✅ काउंटर] " + item.name + " - " + String.format("%.3f", item.qty) + " kg"); tvItem.setTextColor(0xFF4CAF50); }
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
        tv.setText("📊 काउंटर रिपोर्ट\n\n💰 कुल गल्ला कैश कलेक्शन: ₹" + String.format("%.2f", totalSales) + "\n🧾 कुल पर्चियां: " + orders.size());
        tv.setTextSize(18); tv.setPadding(20, 20, 20, 20);
        dashDynamicContent.addView(tv);
    }
}
