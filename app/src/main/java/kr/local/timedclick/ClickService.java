package kr.local.timedclick;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.os.*;
import android.view.accessibility.*;

public class ClickService extends AccessibilityService {
    static ClickService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private int step;
    private long deadline;
    private String target;
    private final Runnable poll = this::tick;

    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* Poll only during a short authorized run. */ }
    @Override public void onInterrupt() { stopRun("접근성 서비스가 중단되었습니다."); }
    @Override public void onDestroy() { stopRun("접근성 서비스 연결이 해제되었습니다."); instance = null; super.onDestroy(); }

    void begin() {
        if (running) return;
        target = Schedule.prefs(this).getString("package", "");
        if (target.isEmpty()) { Schedule.log(this, "대상 앱을 먼저 선택하세요."); return; }
        step = 0;
        deadline = SystemClock.elapsedRealtime() + 15000;
        running = true;
        tick();
    }
    void stopRun(String reason) {
        handler.removeCallbacks(poll);
        if (running) Schedule.log(this, reason);
        running = false;
    }
    private void tick() {
        if (!running) return;
        if (SystemClock.elapsedRealtime() > deadline) { stopRun("버튼을 찾지 못해 종료했습니다. 직접 화면을 확인하세요."); return; }
        if (getSystemService(KeyguardManager.class).isKeyguardLocked()
                || !getSystemService(PowerManager.class).isInteractive()) {
            stopRun("화면이 꺼져 있거나 잠겨 있어 실행을 중지했습니다."); return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { handler.postDelayed(poll, 150); return; }
        if (!target.equals(String.valueOf(root.getPackageName()))) {
            stopRun("선택한 앱이 전면에 없어 실행을 중지했습니다."); return;
        }
        // If the region sheet was already opened by the user, only select the region.
        if (step == 0 && has(root, "수원시")) step = 1;
        String label = step == 0 ? "빠른 충전 바로가기" : "수원시";
        AccessibilityNodeInfo node = find(root, label);
        boolean clicked = false;
        for (int depth = 0; node != null && depth < 5; depth++, node = node.getParent()) {
            if (node.isVisibleToUser() && node.isEnabled() && node.isClickable()) {
                clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
        if (clicked) {
            if (step == 1) { stopRun("수원시 선택을 요청했습니다. 이후 충전·인증·결제는 직접 진행하세요."); return; }
            step = 1;
        }
        handler.postDelayed(poll, clicked ? 300 : 150);
    }
    private static boolean has(AccessibilityNodeInfo root, String text) { return find(root, text) != null; }
    private static String clean(CharSequence text) {
        return text == null ? "" : text.toString().replaceAll("\\s+", "").replace("›", "").replace("〉", "").replace(">", "");
    }
    private static AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label) {
        if (node == null || !node.isVisibleToUser()) return null;
        String wanted = clean(label);
        if (clean(node.getText()).equals(wanted) || clean(node.getContentDescription()).equals(wanted)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo match = find(node.getChild(i), label);
            if (match != null) return match;
        }
        return null;
    }
}
