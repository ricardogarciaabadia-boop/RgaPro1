package com.rgapro1.ocaso;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpiryNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL="rgapro_expiry";
    @Override public void onReceive(Context c, Intent i){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Vencimientos de pólizas",NotificationManager.IMPORTANCE_DEFAULT));
        Set<String> set=c.getSharedPreferences("rgapro_local",Context.MODE_PRIVATE).getStringSet("policies",new HashSet<>());
        int id=1000;
        for(String raw:set){try{JSONObject p=new JSONObject(raw);long days=days(p.optString("expiry"));if(days>=0&&days<=30){String key="notice_"+p.optString("id")+"_"+new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());if(c.getSharedPreferences("rgapro_local",Context.MODE_PRIVATE).getBoolean(key,false))continue;Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);String when=days==0?"vence hoy":days==1?"vence mañana":"vence en "+days+" días";Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("RgaPro · Próximo vencimiento").setContentText(p.optString("holder")+" · "+p.optString("number")+" · "+when).setStyle(new Notification.BigTextStyle().bigText("La póliza de "+p.optString("holder")+" ("+p.optString("type")+") "+when+".")).setAutoCancel(true).setContentIntent(pi);nm.notify(id++,b.build());c.getSharedPreferences("rgapro_local",Context.MODE_PRIVATE).edit().putBoolean(key,true).apply();}}catch(Exception ignored){}}
    }
    private long days(String s){try{return (new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(s).getTime()-System.currentTimeMillis())/86400000L;}catch(Exception e){return Long.MAX_VALUE;}}
}
