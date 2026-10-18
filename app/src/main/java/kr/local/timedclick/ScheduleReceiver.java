package kr.local.timedclick;

import android.content.*;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("PREPARE".equals(action)) { Schedule.notifyService(); return; }
        if ("TEST".equals(action) || "MONTHLY".equals(action)) {
            boolean test = "TEST".equals(action);
            // Legacy alarms from the previous app version may have no due extra.
            long due = intent.getLongExtra("due", Schedule.prefs(context).getLong(test ? "testAt" : "next", 0));
            Schedule.dispatch(context, test, due);
            return;
        }
        if (Intent.ACTION_TIME_CHANGED.equals(action) && ClickService.instance != null)
            ClickService.instance.stopRun("기기 시각 변경으로 준비를 다시 설정합니다.");
        Schedule.restore(context);
        Schedule.notifyService();
    }
}

