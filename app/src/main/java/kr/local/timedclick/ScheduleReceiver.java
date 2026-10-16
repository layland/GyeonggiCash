package kr.local.timedclick;

import android.content.*;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean test = "TEST".equals(action);
        if (!test && !"MONTHLY".equals(action)) { Schedule.restore(context); return; }
        if (!test && !Schedule.prefs(context).getBoolean("enabled", false)) return;
        long due = Schedule.prefs(context).getLong(test ? "testAt" : "next", 0);
        // Consume before dispatch: a duplicate broadcast must not repeat a click sequence.
        Schedule.prefs(context).edit().putLong(test ? "testAt" : "next", 0).commit();
        if (MonthlyTime.timely(due, System.currentTimeMillis())) {
            if (ClickService.instance != null) ClickService.instance.begin();
            else Schedule.log(context, "접근성 서비스가 연결되지 않아 실행하지 못했습니다.");
        } else if (due > 0) Schedule.log(context, "알람이 지연되어 이번 실행을 건너뛰었습니다.");
        if (!test) Schedule.restore(context);
    }
}
