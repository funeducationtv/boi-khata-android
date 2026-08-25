#!/bin/bash
set -e
mkdir -p app/src/main/res/values-bn
cat << 'INNER_EOF' > app/src/main/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Boi Khata</string>
    <string name="dashboard">Dash</string>
    <string name="bill">Bill</string>
    <string name="khata">Khata</string>
    <string name="stock">Stock</string>
    <string name="accounts">Accounts</string>
    <string name="more">More</string>
    
    <string name="today_sales">Today Sales</string>
    <string name="today_collection">Today Collection</string>
    <string name="today_profit">Today Profit(Est)</string>
    
    <string name="new_bill">[+ New Bill]</string>
    <string name="collect_dues">[Collect Dues]</string>
    <string name="add_expense">[+ Expense]</string>
    <string name="reports">[Reports]</string>
    
    <string name="collect_today_title">Who to collect from today?</string>
    <string name="see_all">[See All]</string>
    <string name="collect_button">[Collect]</string>
    
    <string name="low_stock">Low Stock: %1$d</string>
    <string name="order_now">[Order Now →]</string>
    
    <string name="subscription_alert">⚠ Subscription %1$d days left —</string>
    <string name="pay_now">[Pay Now]</string>
    
    <string name="sync_status">🔄 Sync ✓</string>
    <string name="backup_status">☁️ Backup ✓</string>
    <string name="coming_next_phase">Coming Next Phase</string>
    
    <string name="days_format">(%1$d days)</string>
    <string name="currency_symbol">৳</string>
</resources>
INNER_EOF

cat << 'INNER_EOF' > app/src/main/res/values-bn/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">বই খাতা</string>
    <string name="dashboard">ড্যাশ</string>
    <string name="bill">বিল</string>
    <string name="khata">খাতা</string>
    <string name="stock">স্টক</string>
    <string name="accounts">হিসাব</string>
    <string name="more">আরও</string>
    
    <string name="today_sales">আজ বিক্রি</string>
    <string name="today_collection">আজ আদায়</string>
    <string name="today_profit">আজ লাভ(আনুমানিক)</string>
    
    <string name="new_bill">[+ নতুন বিল]</string>
    <string name="collect_dues">[বাকি আদায়]</string>
    <string name="add_expense">[+ খরচ]</string>
    <string name="reports">[রিপোর্ট]</string>
    
    <string name="collect_today_title">আজ কার কাছে আদায় করবেন?</string>
    <string name="see_all">[সব দেখুন]</string>
    <string name="collect_button">[আদায়]</string>
    
    <string name="low_stock">কম স্টক: %1$dটি</string>
    <string name="order_now">[অর্ডার করুন →]</string>
    
    <string name="subscription_alert">⚠ সাবস্ক্রিপশন %1$d দিন বাকি —</string>
    <string name="pay_now">[এখনই পরিশোধ]</string>
    
    <string name="sync_status">🔄 সিঙ্ক ✓</string>
    <string name="backup_status">☁️ ব্যাকআপ ✓</string>
    
    <string name="coming_next_phase">পরবর্তী ফেজে আসছে</string>
    <string name="days_format">(%1$d দিন)</string>
    <string name="currency_symbol">৳</string>
</resources>
INNER_EOF

