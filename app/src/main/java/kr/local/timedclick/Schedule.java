package kr.local.timedclick;

import android.app.*;
import android.content.*;
import android.os.Build;

final class Schedule {
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("schedule", 0); }
    static AlarmManager alarms(Context c) { return c.getSystemService(AlarmManager.class); }
    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 31 || alarms(c).canScheduleExactAlarms(); }
    static PendingIntent intent(Context c, boolean test, long due) {
        return PendingIntent.getBroadcast(c, test ? 2 : 1,
            new Intent(c, ScheduleReceiver.class).setAction(test ? "TEST" : "MONTHLY").putExtra("due", due),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static PendingIntent prepareIntent(Context c, long due) {
        return PendingIntent.getBroadcast(c, 3,
            new Intent(c, ScheduleReceiver.class).setAction("PREPARE").putExtra("due", due),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void log(Context c, String text) {
        prefs(c).edit().putString("result", java.time.Instant.now().atZone(MonthlyTime.SEOUL).toLocalDateTime() + "\n" + text).apply();
    }
    static void notifyService() { if (ClickService.instance != null) ClickService.instance.watchSchedule(); }
    static void arm(Context c, boolean test, long at) {
        alarms(c).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(c, test, at));
        if (!test) {
            alarms(c).cancel(prepareIntent(c, at));
            // Do not spend the allow-while-idle quota on the preparation reminder.
            long prepareAt = at - RunTiming.PREPARE_MS;
            if (prepareAt > System.currentTimeMillis())
                alarms(c).setExact(AlarmManager.RTC_WAKEUP, prepareAt, prepareIntent(c, at));
        }
        prefs(c).edit().putLong(test ? "testAt" : "next", at).apply();
        notifyService();
    }
    static void restore(Context c) {
        if (!prefs(c).getBoolean("enabled", false)) return;
        if (!allowed(c)) { log(c, "정확한 알람 권한이 없어 예약할 수 없습니다. 앱에서 다시 설정하세요."); return; }
        try { arm(c, false, MonthlyTime.next(System.currentTimeMillis())); }
        catch (SecurityException e) { log(c, "알람 권한을 확인하세요."); }
    }
    static void dispatch(Context c, boolean test, long due) {
        String key = test ? "testAt" : "next";
        long now = System.currentTimeMillis();
        if (!RunTiming.canClaim(prefs(c).getLong(key, 0), due, now,
                test || prefs(c).getBoolean("enabled", false))) return;
        // Both the precise service timer and AlarmManager use this single consumer.
        prefs(c).edit().putLong(key, 0).putBoolean("sessionFirstRecorded", false).commit();
        if (MonthlyTime.timely(due, now)) {
            if (ClickService.instance != null) ClickService.instance.executeDue(test, due);
            else failure(c, "접근성 서비스가 연결되지 않아 실행하지 못했습니다.");
        } else {
            if (ClickService.instance != null) ClickService.instance.stopRun("실행 시간이 지나 준비를 종료했습니다.");
            failure(c, "알람이 지연되어 이번 실행을 건너뛰었습니다.");
        }
        if (!test) restore(c);
        notifyService();
    }
    private static void failure(Context c, String text) {
        prefs(c).edit().putString("state", "실패").putString("detail", text).putBoolean("active", false).apply();
        log(c, text);
    }
    static void cancel(Context c) {
        prefs(c).edit().putBoolean("enabled", false).putLong("next", 0).putLong("testAt", 0).apply();
        alarms(c).cancel(intent(c, false, 0));
        alarms(c).cancel(intent(c, true, 0));
        alarms(c).cancel(prepareIntent(c, 0));
        if (ClickService.instance != null) ClickService.instance.stopRun("사용자가 실행을 중지했습니다.");
        prefs(c).edit().putString("state", "중지").putString("detail", "모든 예약과 실행을 중지했습니다.").putBoolean("active", false).apply();
    }
}

