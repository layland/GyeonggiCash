package kr.local.timedclick;

import android.app.*;
import android.content.*;
import android.os.Build;

final class Schedule {
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("schedule", 0); }
    static AlarmManager alarms(Context c) { return c.getSystemService(AlarmManager.class); }
    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 31 || alarms(c).canScheduleExactAlarms(); }
    static PendingIntent intent(Context c, boolean test) {
        return PendingIntent.getBroadcast(c, test ? 2 : 1,
            new Intent(c, ScheduleReceiver.class).setAction(test ? "TEST" : "MONTHLY"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void log(Context c, String text) {
        prefs(c).edit().putString("result", java.time.Instant.now().atZone(MonthlyTime.SEOUL).toLocalDateTime() + "\n" + text).apply();
    }
    static void arm(Context c, boolean test, long at) {
        alarms(c).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(c, test));
        prefs(c).edit().putLong(test ? "testAt" : "next", at).apply();
    }
    static void restore(Context c) {
        if (!prefs(c).getBoolean("enabled", false)) return;
        if (!allowed(c)) { log(c, "정확한 알람 권한이 없어 예약할 수 없습니다. 앱에서 다시 설정하세요."); return; }
        try { arm(c, false, MonthlyTime.next(System.currentTimeMillis())); }
        catch (SecurityException e) { log(c, "알람 권한을 확인하세요."); }
    }
    static void cancel(Context c) {
        prefs(c).edit().putBoolean("enabled", false).putLong("next", 0).putLong("testAt", 0).apply();
        alarms(c).cancel(intent(c, false));
        alarms(c).cancel(intent(c, true));
        if (ClickService.instance != null) ClickService.instance.stopRun("사용자가 실행을 중지했습니다.");
    }
}
