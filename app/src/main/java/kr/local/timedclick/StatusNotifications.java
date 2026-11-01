package kr.local.timedclick;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;

final class StatusNotifications {
    private static final String CHANNEL = "preparation";
    static boolean allowed(Context c) {
        return (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) && c.getSystemService(NotificationManager.class).areNotificationsEnabled();
    }
    static void show(Context c, String title, String text, boolean ongoing, boolean alert) {
        if (!allowed(c)) return;
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "준비 완료·실행 결과", NotificationManager.IMPORTANCE_HIGH));
        manager.createNotificationChannel(new NotificationChannel("preparation_status", "준비 진행 상태", NotificationManager.IMPORTANCE_LOW));
        Intent open = ongoing ? c.getPackageManager().getLaunchIntentForPackage(Schedule.prefs(c).getString("package", "")) : null;
        if (open == null) open = new Intent(c, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(c, 70, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getBroadcast(c, 71, new Intent(c, ScheduleReceiver.class).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(c, alert ? CHANNEL : "preparation_status")
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(content)
            .setOngoing(ongoing).setAutoCancel(!ongoing).setOnlyAlertOnce(!alert);
        if (ongoing) builder.addAction(new Notification.Action.Builder(null, "중지", stop).build());
        try { manager.notify(70, builder.build()); }
        catch (SecurityException ignored) { /* User may revoke notification permission at any time. */ }
    }
    static void cancel(Context c) { c.getSystemService(NotificationManager.class).cancel(70); }
    private StatusNotifications() {}
}
