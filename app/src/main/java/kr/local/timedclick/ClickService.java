package kr.local.timedclick;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import java.util.ArrayList;
import java.util.List;

public class ClickService extends AccessibilityService {
    static ClickService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean preparing, running, test;
    private long dueWall, dueElapsed;
    private int step;
    private String target = "";
    private String windowReason = "대상 앱 화면을 확인하는 중입니다.";
    private AccessibilityNodeInfo cachedButton;
    private View keepScreenOverlay;
    private PowerManager.WakeLock wakeLock;
    private final Runnable watchTask = this::watchSchedule;
    private final Runnable probeTask = this::probe;
    private final Runnable fireTask = () -> Schedule.dispatch(this, test, dueWall);
    private final Runnable prevalidateTask = () -> { cachedButton = null; probe(); };

    @Override protected void onServiceConnected() {
        instance = this;
        if (Schedule.prefs(this).getBoolean("active", false)) {
            state("실패", "서비스가 재시작되어 이전 준비를 종료했습니다.", false);
        }
        watchSchedule();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (preparing && !running) {
            handler.removeCallbacks(probeTask);
            handler.postDelayed(probeTask, 100);
        }
    }
    @Override public void onInterrupt() { stopRun("접근성 서비스가 중단되었습니다."); }
    @Override public void onDestroy() {
        stopRun("접근성 서비스 연결이 해제되었습니다.");
        instance = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    boolean active() { return preparing || running; }

    void watchSchedule() {
        handler.removeCallbacks(watchTask);
        if (running) return;
        long monthly = Schedule.prefs(this).getBoolean("enabled", false)
            ? Schedule.prefs(this).getLong("next", 0) : 0;
        long trial = Schedule.prefs(this).getLong("testAt", 0);
        boolean chosenTest = trial > 0 && (monthly <= 0 || trial <= monthly);
        long chosen = chosenTest ? trial : monthly;
        if (preparing && dueWall == chosen && test == chosenTest) return;
        clearSession();
        if (chosen <= 0) return;
        long delay = chosen - System.currentTimeMillis();
        if (delay > RunTiming.PREPARE_MS) {
            handler.postDelayed(watchTask, delay - RunTiming.PREPARE_MS);
            return;
        }
        test = chosenTest;
        dueWall = chosen;
        dueElapsed = RunTiming.dueElapsed(chosen, System.currentTimeMillis(), SystemClock.elapsedRealtime());
        target = Schedule.prefs(this).getString("package", "");
        if (target.isEmpty()) { state("실패", "대상 앱을 선택하세요.", false); return; }
        preparing = true;
        step = 0;
        state("준비", "대상 앱의 빠른 충전 화면을 열어 두세요.", true);
        long remaining = RunTiming.remaining(dueElapsed, SystemClock.elapsedRealtime());
        handler.postDelayed(fireTask, remaining);
        handler.postDelayed(prevalidateTask, Math.max(0, remaining - 1000));
        probe();
    }
    void executeDue(boolean isTest, long due) {
        if (running) return;
        if (!preparing || dueWall != due || test != isTest) {
            clearSession();
            test = isTest;
            dueWall = due;
            dueElapsed = RunTiming.dueElapsed(due, System.currentTimeMillis(), SystemClock.elapsedRealtime());
            target = Schedule.prefs(this).getString("package", "");
            step = 0;
        }
        preparing = false;
        running = true;
        handler.removeCallbacks(probeTask);
        handler.removeCallbacks(prevalidateTask);
        handler.removeCallbacks(fireTask);
        if (!screenAvailable()) { finish("실패", "화면이 꺼져 있거나 잠겨 있어 클릭하지 않았습니다."); return; }
        state("클릭 중", "버튼을 다시 확인하고 있습니다.", true);
        probe();
    }
    private boolean screenAvailable() {
        return !getSystemService(KeyguardManager.class).isKeyguardLocked()
            && getSystemService(PowerManager.class).isInteractive();
    }
    private void probe() {
        handler.removeCallbacks(probeTask);
        if (!active()) return;
        if (RunTiming.expired(dueElapsed, SystemClock.elapsedRealtime())) {
            if (running) finish("실패", "실행 시간 안에 클릭할 화면을 확인하지 못했습니다. " + windowReason);
            else Schedule.dispatch(this, test, dueWall);
            return;
        }
        if (!screenAvailable() && running) {
            finish("실패", "화면이 꺼져 있거나 잠겨 있어 실행을 중지했습니다."); return;
        }
        AccessibilityNodeInfo root = screenAvailable() ? targetRoot() : null;
        if (root == null) {
            cachedButton = null;
            releaseScreen();
            state(running ? "화면 확인 중" : "대기", screenAvailable() ? windowReason
                : "잠금을 풀고 대상 앱의 빠른 충전 화면을 열어 주세요.", true);
            // Window roots can be temporarily null or belong to the keep-screen overlay.
            // Never click another app; allow the selected app to become readable in this session.
            handler.postDelayed(probeTask, running ? 150 : 500);
            return;
        }
        keepScreen();
        boolean sheet = AccessibilityMatcher.regionSheet(root);
        if (step == 0 && sheet) step = 1;
        // A city match alone is insufficient: require the region-selection screen.
        String label = step == 0 ? AccessibilityMatcher.QUICK : AccessibilityMatcher.CITY;
        AccessibilityNodeInfo fresh = step == 1 && !sheet ? null : AccessibilityMatcher.uniqueButton(root, label);
        if (cachedButton != null && (!cachedButton.refresh() || !cachedButton.equals(fresh))) cachedButton = null;
        if (cachedButton == null) cachedButton = fresh;
        if (!running) {
            state(cachedButton == null ? "대기" : "준비 완료",
                cachedButton == null ? "대상 버튼을 하나로 식별할 수 없습니다." : label + " 확인됨 · 예약 시각까지 대기", true);
            if (cachedButton == null) handler.postDelayed(probeTask, 500);
            return;
        }
        if (cachedButton == null) {
            windowReason = step == 1 && !sheet ? "지역 선택 창이 아직 확인되지 않았습니다."
                : "대상 앱 창은 확인했지만 ‘" + label + "’ 버튼을 하나로 식별하지 못했습니다.";
            state("화면 확인 중", windowReason, true);
            handler.postDelayed(probeTask, 150); return;
        }
        long clickWall = System.currentTimeMillis();
        long delay = SystemClock.elapsedRealtime() - dueElapsed;
        boolean accepted = cachedButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        android.content.SharedPreferences.Editor timing = Schedule.prefs(this).edit()
            .putLong("clickWall", clickWall).putLong("clickDue", dueWall)
            .putLong("clickDelay", delay).putString("clickLabel", label).putBoolean("clickAccepted", accepted);
        if (step == 0 || !Schedule.prefs(this).getBoolean("sessionFirstRecorded", false)) {
            timing.putLong("firstClickWall", clickWall).putLong("firstClickDelay", delay)
                .putBoolean("sessionFirstRecorded", true);
        }
        timing.apply();
        cachedButton = null;
        if (!accepted) { finish("실패", label + " 클릭 요청이 거부되어 재시도하지 않았습니다."); return; }
        if (step == 1) {
            finish("클릭 요청 완료", "수원시 선택 클릭을 요청했습니다. 충전 완료 여부는 직접 확인하세요.");
        } else {
            step = 1;
            state("클릭 중", "빠른 충전 클릭 요청됨 · 지역 선택 화면을 기다립니다.", true);
            handler.postDelayed(probeTask, 100);
        }
    }
    private AccessibilityNodeInfo targetRoot() {
        List<AccessibilityWindowInfo> windows = getWindows();
        List<WindowPolicy.Window> descriptors = new ArrayList<>();
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        String topOwner = "";
        int topLayer = Integer.MIN_VALUE;
        int topWindowId = -1;
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            String owner = root == null || root.getPackageName() == null ? null : root.getPackageName().toString();
            boolean ownOverlay = window.getType() == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
                && (getPackageName().equals(owner) || "지역화폐 실행 준비 화면 유지".contentEquals(
                    window.getTitle() == null ? "" : window.getTitle()));
            boolean application = window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION;
            descriptors.add(new WindowPolicy.Window(application, ownOverlay,
                window.isFocused() || window.isActive(), window.getLayer(), owner));
            roots.add(root);
            if (application && window.getLayer() > topLayer) {
                topLayer = window.getLayer(); topOwner = owner == null ? "읽을 수 없음" : owner;
                topWindowId = window.getId();
            }
        }
        int selected = WindowPolicy.select(descriptors, target);
        AccessibilityNodeInfo result = selected >= 0 ? roots.get(selected) : null;
        if (selected == WindowPolicy.NO_APPLICATION || selected == WindowPolicy.UNREADABLE) {
            // A fresh active root may become readable before getWindows() catches up.
            AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
            if (activeRoot != null && target.equals(String.valueOf(activeRoot.getPackageName()))
                    && (selected == WindowPolicy.NO_APPLICATION || activeRoot.getWindowId() == topWindowId)) result = activeRoot;
        }
        // Container roots may be marked invisible while their button children are visible.
        // Individual clickable buttons are checked for visibility by AccessibilityMatcher.
        if (result != null) {
            windowReason = "대상 앱 창 확인됨";
        } else {
            result = null;
            if (selected == WindowPolicy.BLOCKED) windowReason = "시스템 창 또는 다른 오버레이가 화면을 가리고 있습니다.";
            else if (selected == WindowPolicy.OTHER_APPLICATION) windowReason = "다른 앱 창이 위에 있습니다. 선택: " + target + " / 감지: " + topOwner;
            else windowReason = "접근성에서 대상 앱 창의 내용을 아직 읽지 못했습니다. 선택: " + target;
        }
        android.content.SharedPreferences p = Schedule.prefs(this);
        if (!windowReason.equals(p.getString("windowDiagnostic", "")))
            p.edit().putString("windowDiagnostic", windowReason).apply();
        return result;
    }
    void stopRun(String reason) {
        handler.removeCallbacks(watchTask);
        if (active()) {
            clearSession();
            state("중지", reason, false);
            Schedule.log(this, reason);
        } else clearSession();
    }
    private void finish(String status, String detail) {
        clearSession();
        state(status, detail, false);
        Schedule.log(this, detail);
        handler.post(watchTask);
    }
    private void state(String status, String detail, boolean active) {
        android.content.SharedPreferences p = Schedule.prefs(this);
        if (status.equals(p.getString("state", "")) && detail.equals(p.getString("detail", ""))
                && active == p.getBoolean("active", false)) return;
        p.edit().putString("state", status).putString("detail", detail).putBoolean("active", active).apply();
    }
    private void clearSession() {
        handler.removeCallbacks(probeTask);
        handler.removeCallbacks(prevalidateTask);
        handler.removeCallbacks(fireTask);
        preparing = false;
        running = false;
        cachedButton = null;
        releaseScreen();
    }
    private void keepScreen() {
        if (keepScreenOverlay != null) return;
        View overlay = new View(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("지역화폐 실행 준비 화면 유지");
        try {
            getSystemService(WindowManager.class).addView(overlay, params);
            keepScreenOverlay = overlay;
            wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getPackageName() + ":click-session");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(RunTiming.PREPARE_MS + RunTiming.GRACE_MS + 5000);
            Schedule.prefs(this).edit().putBoolean("screenKept", true).apply();
        } catch (RuntimeException e) {
            releaseScreen();
        }
    }
    private void releaseScreen() {
        if (keepScreenOverlay != null) {
            try { getSystemService(WindowManager.class).removeView(keepScreenOverlay); }
            catch (RuntimeException ignored) { /* Already removed by the system. */ }
            keepScreenOverlay = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        if (Schedule.prefs(this).getBoolean("screenKept", false))
            Schedule.prefs(this).edit().putBoolean("screenKept", false).apply();
    }
}

