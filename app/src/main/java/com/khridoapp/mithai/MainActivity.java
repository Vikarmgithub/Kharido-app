package com.khridoapp.mithai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    // डेटा मॉडल क्लासेस
    static class Product {
        String id, name, nameEn, category, restockTime;
        double price, stock;
        Product(String id, String name, String nameEn, String category, double price, double stock) {
            this.id = id; this.name = name; this.nameEn = nameEn;
            this.category = category; this.price = price; this.stock = stock;
            this.restockTime = "";
        }
    }

    static class OrderItem {
        String id, name;
        double qty, price, subtotal;
        boolean isAdvance;
        OrderItem(String id, String name, double qty, double price, boolean isAdvance) {
            this.id = id; this.name = name; this.qty = qty; this.price = price;
            this.isAdvance = isAdvance; this.subtotal = qty * price;
        }
    }

    static class Order {
        String id, customerName, phone, time, status;
        ArrayList<OrderItem> items;
        double total;
        Order(String id, String customerName, String phone, ArrayList<OrderItem> items, double total) {
            this.id = id; this.customerName = customerName; this.phone = phone;
            this.items = items; this.total = total; this.status = "pending";
            this.time = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        }
    }

    // डेटा स्टेट्स
    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private HashMap<String, Double> cart = new HashMap<>();
    private HashMap<String, Double> advanceCart = new HashMap<>();

    // UI कंपोनेंट्स
    private ScrollView shopContainer, dashboardContainer;
    private LinearLayout shopProductList, dashDynamicContent;
    private Button btnShopView, btnDashView, btnFloatingCart;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MithaiAppPrefs", Context.MODE_PRIVATE);

        // UI इनिशियलाइज़ेशन
        shopContainer = findViewById(R.id.shopContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        shopProductList = findViewById(R.id.shopProductList);
        dashDynamicContent = findViewById(R.id.dashDynamicContent);
        btnShopView = findViewById(R.id.btnShopView);
        btnDashView = findViewById(R.id.btnDashView);
        btnFloatingCart = findViewById(R.id.btnFloatingCart);

        // डेटा लोड करना
        initSeedProducts();
        renderShop();
        updateCartButton();

        // नेविगेशन इवेंट्स
        btnShopView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.VISIBLE);
            dashboardContainer.setVisibility(View.GONE);
            btnShopView.setBackgroundColor(0xFFE8871E);
            btnDashView.setBackgroundColor(0xFF5B1A2B);
            renderShop();
        });

        btnDashView.setOnClickListener(v -> {
            shopContainer.setVisibility(View.GONE);
            dashboardContainer.setVisibility(View.VISIBLE);
            btnDashView.setBackgroundColor(0xFFE8871E);
            btnShopView.setBackgroundColor(0xFF5B1A2B);
            renderInventoryTab();
        });

        findViewById(R.id.tabInventory).setOnClickListener(v -> renderInventoryTab());
        findViewById(R.id.tabOrders).setOnClickListener(v -> renderOrdersTab());
        findViewById(R.id.tabAnalytics).setOnClickListener(v -> renderAnalyticsTab());
        
        btnFloatingCart.setOnClickListener(v -> showCartDialog());
    }

    private void initSeedProducts() {
        if (products.isEmpty()) {
            products.add(new Product("p1", "गुलाब जामुन", "Gulab Jamun", "बंगाली", 360, 12));
            products.add(new Product("p2", "काजू कतली", "Kaju Katli", "बर्फी", 820, 6));
            products.add(new Product("p3", "रसगुल्ला", "Rasgulla", "बंगाली", 320, 18));
            products.add(new Product("p4", "बेसन लड्डू", "Besan Ladoo", "लड्डू", 400, 10));
            products.add(new Product("p5", "मोतीचूर लड्डू", "Motichoor Ladoo", "लड्डू", 380, 1.5));
        }
    }

    // --- दुकान रेंडर लॉजिक ---
    private void renderShop() {
        shopProductList.removeAllViews();
        for (final Product p : products) {
            TextView tv = new TextView(this);
            tv.setText(p.name + " (" + p.nameEn + ")\nकीमत: ₹" + p.price + "/किलो  | स्टॉक: " + p.stock + " किलो");
            tv.setPadding(15, 15, 15, 5);
            tv.setTextSize(16); // सुधारा गया: sp हटाया

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            Button btnAdd = new Button(this);
            btnAdd.setText("+ कार्ट में जोड़ें");
            btnAdd.setOnClickListener(v -> {
                double currentQty = cart.containsKey(p.id) ? cart.get(p.id) : 0;
                if (currentQty < p.stock) {
                    cart.put(p.id, currentQty + 0.25);
                    Toast.makeText(MainActivity.this, p.name + " जोड़ा गया", Toast.LENGTH_SHORT).show();
                    updateCartButton();
                } else {
                    Toast.makeText(MainActivity.this, "स्टॉक समाप्त!", Toast.LENGTH_SHORT).show();
                }
            });

            Button btnAdvance = new Button(this);
            btnAdvance.setText("📋 एडवांस ऑर्डर");
            btnAdvance.setOnClickListener(v -> {
                advanceCart.put(p.id, 5.0);
                Toast.makeText(MainActivity.this, p.name + " एडवांस में जोड़ा गया", Toast.LENGTH_SHORT).show();
                updateCartButton();
            });

            row.addView(btnAdd);
            row.addView(btnAdvance);

            shopProductList.addView(tv);
            shopProductList.addView(row);
        }
    }

    private void updateCartButton() {
        int count = cart.size() + advanceCart.size();
        btnFloatingCart.setText("🛒 कार्ट (" + count + ")");
    }

    // --- कार्ट डायलॉग पॉपअप ---
    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("आपका कार्ट भंडार");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        double total = 0;
        for (Map.Entry<String, Double> entry : cart.entrySet()) {
            for (Product p : products) {
                if (p.id.equals(entry.getKey())) {
                    double amt = entry.getValue() * p.price;
                    total += amt;
                    TextView itemTv = new TextView(this);
                    itemTv.setText(p.name + " -> " + entry.getValue() + " किलो = ₹" + amt);
                    layout.addView(itemTv);
                }
            }
        }

        TextView totalTv = new TextView(this);
        totalTv.setText("\nकुल राशि: ₹" + total);
        totalTv.setTextSize(18); // सुधारा गया
        layout.addView(totalTv);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("ग्राहक का नाम दर्ज करें *");
        layout.addView(nameInput);

        builder.setView(layout);
        builder.setPositiveButton("ऑर्डर पक्का करें", (dialog, which) -> {
            String name = nameInput.getText().toString();
            if (!name.isEmpty() && !cart.isEmpty()) {
                ArrayList<OrderItem> items = new ArrayList<>();
                for (Map.Entry<String, Double> entry : cart.entrySet()) {
                    items.add(new OrderItem(entry.getKey(), "मिठाई", entry.getValue(), 400, false));
                }
                orders.add(new Order("ORD" + System.currentTimeMillis(), name, "", items, 1000));
                cart.clear();
                updateCartButton();
                Toast.makeText(MainActivity.this, "ऑर्डर सफल! 🙏", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "विवरण सही भरें", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("बंद करें", null);
        builder.show();
    }

    // --- डैशबोर्ड टैब मैनेजमेंट ---
    private void renderInventoryTab() {
        dashDynamicContent.removeAllViews();
        TextView title = new TextView(this);
        title.setText("📦 इन्वेंटरी स्टॉक मैनेजमेंट\n");
        title.setTextSize(18); // सुधारा गया
        dashDynamicContent.addView(title);

        for (Product p : products) {
            TextView pInfo = new TextView(this);
            pInfo.setText(p.name + " - स्टॉक: " + p.stock + " किलो");
            dashDynamicContent.addView(pInfo);
        }
    }

    private void renderOrdersTab() {
        dashDynamicContent.removeAllViews();
        TextView title = new TextView(this);
        title.setText("🧾 लाइव ऑर्डर्स सूची\n");
        title.setTextSize(18); // सुधारा गया
        dashDynamicContent.addView(title);

        if (orders.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("कोई नया ऑर्डर नहीं मिला है।");
            dashDynamicContent.addView(empty);
        } else {
            for (Order o : orders) {
                TextView oInfo = new TextView(this);
                oInfo.setText("आईडी: " + o.id + "\nग्राहक: " + o.customerName + "\nसमय: " + o.time + "\nस्थिति: " + o.status + "\n");
                dashDynamicContent.addView(oInfo);
            }
        }
    }

    private void renderAnalyticsTab() {
        dashDynamicContent.removeAllViews();
        TextView title = new TextView(this);
        title.setText("📊 बिज़नेस सेल्स रिपोर्ट (Analytics)\n");
        title.setTextSize(18); // सुधारा गया
        dashDynamicContent.addView(title);

        double totalSales = 0;
        for (Order o : orders) {
            totalSales += o.total;
        }

        TextView salesTv = new TextView(this);
        salesTv.setText("कुल बिक्री: ₹" + totalSales + "\nकुल ऑर्डर्स: " + orders.size());
        salesTv.setTextSize(16); // सुधारा गया: 16sp को 16 किया
        dashDynamicContent.addView(salesTv);
    }
}
