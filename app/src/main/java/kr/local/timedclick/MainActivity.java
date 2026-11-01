package kr.local.timedclick;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PREPARE = 1, TEST = 2;
    private LinearLayout column;
    private TextView status, diagnostics;
    private Button prepareButton;
    private Button regionButton;
    private int pendingAction;
    private boolean returningFromSettings;
    private String settingsAction = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { renderStatus(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            pendingAction = savedInstanceState.getInt("pendingAction");
            returningFromSettings = savedInstanceState.getBoolean("returning");
            settingsAction = savedInstanceState.getString("settingsAction", "");
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243, 247, 252));
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(24 * getResources().getDisplayMetrics().density);
        column.setPadding(padding, padding + 20, padding, padding);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(column);
        setContentView(scroll);
        text("지역화폐 예약 클릭", 28);
        regionButton = button("", this::chooseRegion);
        status = text("", 17);
        prepareButton = button("준비", () -> begin(PREPARE, false));
        prepareButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(5, 91, 166)));
        prepareButton.setTextColor(Color.WHITE);
        text("준비를 누르면 예약을 켜고 경기지역화폐 앱을 엽니다.\n버튼이 확인되면 알림으로 알려드립니다.", 15);
        button("시험 · 10초 후", () -> new AlertDialog.Builder(this).setTitle("클릭 시험")
            .setMessage("경기지역화폐 앱을 자동으로 열고 10초 후 빠른 충전 → " + Schedule.region(this).label + "를 누릅니다. 충전·결제는 직접 진행합니다.")
            .setNegativeButton("취소", null).setPositiveButton("시험 시작", (d, w) -> begin(TEST, false)).show());
        button("중지", () -> { pendingAction = 0; Schedule.cancel(this); renderStatus(); });
        button("처음 설정 · 권한", this::showSettings);
        button("상세 기록 보기 / 접기", () -> {
            diagnostics.setVisibility(diagnostics.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            renderStatus();
        });
        diagnostics = text("", 14);
        diagnostics.setVisibility(View.GONE);
        text("잠금 해제와 로그인은 직접 해 주세요.\n지역 선택 후 금액 입력·인증·결제는 직접 진행합니다.", 14);
        autoSelectTarget();
        if (!Schedule.prefs(this).contains("region")) {
            Schedule.prefs(this).edit().putString("region", Region.HWASEONG.id).apply();
            Schedule.restore(this);
        }
    }

    private void chooseRegion() {
        if (ClickService.instance != null && ClickService.instance.isRunning()) {
            toast("클릭 중에는 지역을 변경할 수 없습니다. 중지 후 변경해 주세요."); return;
        }
        Region[] regions = Region.values();
        String[] labels = new String[regions.length];
        for (int i = 0; i < regions.length; i++) labels[i] = regions[i].label + " · 매월 1일 " + regions[i].timeLabel();
        new AlertDialog.Builder(this).setTitle("충전 지역 선택")
            .setItems(labels, (d, which) -> {
                if (Schedule.region(this) != regions[which]) {
                    // A region change invalidates earlier reservations and prepared button nodes.
                    pendingAction = 0;
                    Schedule.cancel(this);
                    Schedule.prefs(this).edit().putString("region", regions[which].id).apply();
                    toast(regions[which].label + " " + regions[which].timeLabel() + " 선택됨. 준비를 눌러 주세요.");
                }
                renderStatus();
            }).show();
    }

    private void autoSelectTarget() {
        if (!Schedule.prefs(this).getString("package", "").isEmpty()) return;
        Map<String, ResolveInfo> matches = new LinkedHashMap<>();
        for (ResolveInfo app : launcherApps()) {
            if ("경기지역화폐".equals(app.loadLabel(getPackageManager()).toString().replaceAll("\\s+", "")))
                matches.put(app.activityInfo.packageName, app);
        }
        if (matches.size() == 1) saveTarget(matches.values().iterator().next());
    }
    private List<ResolveInfo> launcherApps() {
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        apps.removeIf(a -> a.activityInfo.packageName.equals(getPackageName()));
        apps.sort(Comparator.comparing(a -> a.loadLabel(getPackageManager()).toString()));
        return apps;
    }
    private void saveTarget(ResolveInfo app) {
        Schedule.prefs(this).edit().putString("package", app.activityInfo.packageName)
            .putString("appName", app.loadLabel(getPackageManager()).toString()).apply();
    }
    private void begin(int action, boolean allowWithoutNotifications) {
        if (ClickService.instance != null && ClickService.instance.isRunning()) {
            toast("클릭을 진행 중입니다. 중지 후 다시 준비해 주세요."); return;
        }
        autoSelectTarget();
        if (Schedule.prefs(this).getString("package", "").isEmpty()) {
            chooseApp(action); return;
        }
        pendingAction = action;
        if (ClickService.instance == null) {
            new AlertDialog.Builder(this).setTitle("처음 한 번 · 접근성 허용")
                .setMessage("접근성에서 ‘지역화폐 예약 클릭’을 켜 주세요.\n차단되면 앱 정보 → ⋮ → 제한된 설정 허용을 먼저 선택하세요.")
                .setNegativeButton("닫기", (d, w) -> pendingAction = 0)
                .setNeutralButton("앱 정보", (d, w) -> openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))))
                .setPositiveButton("접근성 열기", (d, w) -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();
            return;
        }
        if (!Schedule.allowed(this)) {
            if (Build.VERSION.SDK_INT >= 31) {
                toast("알람 및 리마인더 권한을 켜 주세요.");
                openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            }
            return;
        }
        if (!StatusNotifications.allowed(this) && !allowWithoutNotifications) {
            if (Build.VERSION.SDK_INT >= 33 && !Schedule.prefs(this).getBoolean("notificationAsked", false)) {
                Schedule.prefs(this).edit().putBoolean("notificationAsked", true).apply();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 30);
            } else {
                new AlertDialog.Builder(this).setTitle("준비 완료 알림")
                    .setMessage("알림이 차단되어 있습니다. 알림을 허용하면 경기지역화폐 화면에서도 준비 완료를 확인할 수 있습니다.")
                    .setNegativeButton("알림 없이 계속", (d, w) -> begin(action, true))
                    .setPositiveButton("알림 설정", (d, w) -> openSettings(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()))).show();
            }
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(Schedule.prefs(this).getString("package", ""));
        if (launch == null) {
            pendingAction = 0;
            toast("대상 앱을 열 수 없습니다. 처음 설정에서 앱을 다시 선택해 주세요."); return;
        }
        pendingAction = 0;
        try {
            if (action == PREPARE) {
                // A normal preparation replaces any earlier test, without scheduling an immediate click.
                Schedule.prefs(this).edit().putLong("testAt", 0).putBoolean("enabled", true).apply();
                Schedule.alarms(this).cancel(Schedule.intent(this, true, 0));
                if (ClickService.instance.active()) ClickService.instance.stopRun("새 실행 준비를 시작합니다.");
                Schedule.restore(this);
            } else {
                Schedule.arm(this, true, System.currentTimeMillis() + 10000);
            }
            ClickService.instance.prepareNow();
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (SecurityException e) {
            Schedule.cancel(this);
            toast("권한이 변경되었습니다. 처음 설정에서 권한을 확인해 주세요.");
        } catch (ActivityNotFoundException e) {
            Schedule.cancel(this);
            toast("대상 앱을 열지 못했습니다. 앱을 다시 선택해 주세요.");
        }
    }

    private void openSettings(Intent intent) {
        returningFromSettings = pendingAction != 0;
        settingsAction = intent.getAction();
        startActivity(intent);
    }
    private void showSettings() {
        String[] items = {"대상 앱 변경", "접근성 설정", "알람 권한", "알림 권한", "제한된 설정 허용 · 앱 정보"};
        new AlertDialog.Builder(this).setTitle("처음 설정").setItems(items, (d, index) -> {
            pendingAction = 0;
            if (index == 0) chooseApp(0);
            else if (index == 1) startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            else if (index == 2 && Build.VERSION.SDK_INT >= 31)
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            else if (index == 3) startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            else if (index == 4) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        }).show();
    }
    private void chooseApp(int afterSelection) {
        if (Schedule.prefs(this).getBoolean("enabled", false) || Schedule.prefs(this).getLong("testAt", 0) > 0) {
            toast("중지한 뒤 대상 앱을 변경해 주세요."); return;
        }
        List<ResolveInfo> apps = launcherApps();
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) labels[i] = apps.get(i).loadLabel(getPackageManager()) + "\n" + apps.get(i).activityInfo.packageName;
        new AlertDialog.Builder(this).setTitle("경기지역화폐 선택").setItems(labels, (d, index) -> {
            saveTarget(apps.get(index));
            renderStatus();
            if (afterSelection != 0) begin(afterSelection, false);
        }).show();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 30 && pendingAction != 0) {
            int action = pendingAction;
            begin(action, false);
        }
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(Color.rgb(25, 45, 70));
        view.setPadding(0, 12, 0, 16); column.addView(view); return view;
    }
    private Button button(String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(v -> action.run()); column.addView(button); return button;
    }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private String at(long value, String format) {
        return value <= 0 ? "없음" : Instant.ofEpochMilli(value).atZone(MonthlyTime.SEOUL)
            .format(DateTimeFormatter.ofPattern(format));
    }
    private void renderStatus() {
        android.content.SharedPreferences p = Schedule.prefs(this);
        Region region = Schedule.region(this);
        regionButton.setText(region.label + " · 매월 1일 " + region.timeLabel() + "  ▾");
        boolean connected = ClickService.instance != null;
        status.setText("다음 실행  " + at(p.getLong("next", 0), "MM-dd HH:mm")
            + "\n" + p.getString("state", "준비를 눌러 시작하세요.")
            + "\n" + p.getString("detail", "앱 열기와 버튼 확인을 자동으로 진행합니다."));
        diagnostics.setText("대상: " + p.getString("appName", "자동 선택 또는 처음 설정")
            + "\n접근성: " + (connected ? "연결됨" : "설정 필요")
            + " · 알람: " + (Schedule.allowed(this) ? "허용" : "설정 필요")
            + " · 알림: " + (StatusNotifications.allowed(this) ? "허용" : "차단됨")
            + "\n월간 예약: " + (p.getBoolean("enabled", false) ? "켜짐" : "꺼짐")
            + "\n시험 실행: " + at(p.getLong("testAt", 0), "MM-dd HH:mm:ss")
            + "\n최근 화면 진단: " + p.getString("windowDiagnostic", "없음")
            + "\n화면 유지: " + (connected && p.getBoolean("screenKept", false) ? "사용 중" : "꺼짐")
            + "\n목표 시각: " + at(p.getLong("clickDue", 0), "MM-dd HH:mm:ss.SSS")
            + "\n첫 클릭: " + at(p.getLong("firstClickWall", 0), "MM-dd HH:mm:ss.SSS")
            + "\n목표 대비 지연: " + p.getLong("firstClickDelay", 0) + "ms"
            + "\n마지막 버튼: " + p.getString("clickLabel", "없음")
            + "\n클릭 시각: " + at(p.getLong("clickWall", 0), "MM-dd HH:mm:ss.SSS")
            + "\n클릭 요청: " + (p.getBoolean("clickAccepted", false) ? "접수됨" : "기록 없음 또는 거부됨")
            + "\n클릭 호출 시각이며 서버 도착·충전 완료 시각은 아닙니다."
            + "\n\n최근 내역\n" + p.getString("result", "없음"));
    }
    @Override protected void onResume() {
        super.onResume();
        handler.post(refresh);
        if (returningFromSettings && pendingAction != 0) {
            returningFromSettings = false;
            handler.postDelayed(() -> {
                if (pendingAction == 0 || isFinishing()) return;
                int action = pendingAction;
                boolean granted = Settings.ACTION_ACCESSIBILITY_SETTINGS.equals(settingsAction) ? ClickService.instance != null
                    : Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM.equals(settingsAction) ? Schedule.allowed(this)
                    : Settings.ACTION_APP_NOTIFICATION_SETTINGS.equals(settingsAction) ? StatusNotifications.allowed(this)
                    : ClickService.instance != null;
                if (granted) begin(action, false);
                else toast("설정을 마친 뒤 준비를 눌러 주세요.");
            }, 700);
        }
    }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("pendingAction", pendingAction); out.putBoolean("returning", returningFromSettings);
        out.putString("settingsAction", settingsAction);
        super.onSaveInstanceState(out);
    }
}
