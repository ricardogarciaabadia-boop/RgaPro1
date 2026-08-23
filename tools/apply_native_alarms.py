from pathlib import Path

# PrototypeActivity: expose native scheduling to the WebView and request notification permission.
p = Path('app/src/main/java/com/rgapro1/ocaso/PrototypeActivity.java')
s = p.read_text(encoding='utf-8')
if 'import android.os.Build;' not in s:
    s = s.replace('import android.graphics.BitmapFactory;\n', 'import android.graphics.BitmapFactory;\nimport android.os.Build;\n', 1)
old = '  super.onCreate(b);\n  web=new WebView(this);'
new = '  super.onCreate(b);\n  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7103);\n  web=new WebView(this);'
if old in s and 'POST_NOTIFICATIONS' not in s.split('web=new WebView',1)[0]:
    s = s.replace(old,new,1)
old_bridge = '  @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}\n }'
new_bridge = '  @JavascriptInterface public void capture(String side){runOnUiThread(()->startCamera(side));}\n  @JavascriptInterface public void scheduleExpiry(String payload){try{JSONObject p=new JSONObject(payload);ExpiryAlarmScheduler.scheduleDirect(PrototypeActivity.this,p);}catch(Exception e){}}\n }'
if old_bridge in s and 'scheduleExpiry(String payload)' not in s:
    s = s.replace(old_bridge,new_bridge,1)
p.write_text(s,encoding='utf-8')

# ExpiryAlarmScheduler: use the requested five warning thresholds and support direct WebView records.
p = Path('app/src/main/java/com/rgapro1/ocaso/ExpiryAlarmScheduler.java')
s = p.read_text(encoding='utf-8')
s = s.replace('private static final int[] DAYS = {60,45,30,15};','private static final int[] DAYS = {60,40,30,7,1};')
anchor = '    private static long parseDate(String value) {'
method = '''    public static void scheduleDirect(Context context, JSONObject policy) {
        try {
            String expiry = policy.optString("expiry", "");
            long expiryMillis = parseDate(expiry);
            if (expiryMillis <= 0) return;
            String user = "local";
            String id = policy.optString("id", "web_" + policy.optString("identityNumber", policy.optString("number", String.valueOf(System.currentTimeMillis()))));
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarms == null) return;
            for (int days : DAYS) {
                long trigger = expiryMillis - days * 24L * 60L * 60L * 1000L;
                if (trigger <= System.currentTimeMillis()) continue;
                Intent intent = new Intent(context, ExpiryNotificationReceiver.class)
                        .setAction("com.rgapro1.ocaso.EXPIRY")
                        .putExtra("user", user)
                        .putExtra("policy_id", id)
                        .putExtra("days", days)
                        .putExtra("holder", policy.optString("holder", "Sin titular"))
                        .putExtra("number", policy.optString("number", ""))
                        .putExtra("type", policy.optString("type", "Documento / cliente"));
                int requestCode = Math.abs((user + "|" + id + "|" + days).hashCode());
                PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= 23) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                else alarms.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (Exception ignored) {}
    }

'''
if 'scheduleDirect(Context context' not in s:
    s = s.replace(anchor, method + anchor,1)
p.write_text(s,encoding='utf-8')

# Receiver: accept direct records from the WebView even though they are not in the legacy users_json store.
p = Path('app/src/main/java/com/rgapro1/ocaso/ExpiryNotificationReceiver.java')
s = p.read_text(encoding='utf-8')
s = s.replace('Intent open = new Intent(context, MainActivity.class);','Intent open = new Intent(context, PrototypeActivity.class);')
old = '        JSONObject policy = findPolicy(context, user, policyId);\n        if (policy == null) return;\n        showNotification(context, user, policy, days);\n        ExpiryAlarmScheduler.schedulePolicy(context, user, policy);'
new = '''        JSONObject policy = findPolicy(context, user, policyId);
        boolean direct = policy == null && "local".equals(user);
        if (direct) {
            policy = new JSONObject();
            policy.put("holder", intent.getStringExtra("holder"));
            policy.put("number", intent.getStringExtra("number"));
            policy.put("type", intent.getStringExtra("type"));
            policy.put("id", policyId);
        }
        if (policy == null) return;
        showNotification(context, user, policy, days);
        if (!direct) ExpiryAlarmScheduler.schedulePolicy(context, user, policy);'''
if old in s:
    s = s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
