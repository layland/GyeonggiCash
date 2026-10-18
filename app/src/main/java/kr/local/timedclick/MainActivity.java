package kr.local.timedclick;

import android.app.*;
import android.content.*;
import android.content.pm.ResolveInfo;
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
    private LinearLayout column;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() { renderStatus(); handler.postDelayed(this, 1000); }
    };
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243, 247, 252));
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(24 * getResources().getDisplayMetrics().density);
        column.setPadding(padding, padding + 20, padding, padding);
        // Respect status/navigation bars, including Android 15 edge-to-edge enforcement.
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(column);
        setContentView(scroll);
        text("지역화폐 예약 클릭", 28);
        text("매월 1일 · 오전 09:00 · 한국 시간", 19);
        text("빠른 충전 바로가기 → 수원시\n두 버튼을 누르면 종료합니다. 충전·결제는 직접 진행하세요.", 16);
        status = text("", 15);
        button("1. 경기지역화폐 앱 선택", this::chooseApp);
        button("2. 접근성 서비스 설정", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        text("접근성이 차단되면: 아래 앱 정보 → 우측 상단 ⋮ → 제한된 설정 허용을 선택한 뒤 접근성을 켜세요.", 14);
        button("제한된 설정 허용 · 앱 정보 열기", () -> startActivity(new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        button("3. 정확한 알람 권한 설정", () -> {
            if (Build.VERSION.SDK_INT >= 31) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName())));
            else toast("이 버전에서는 별도 알람 권한이 필요하지 않습니다.");
        });
        button("매월 예약 켜기", () -> {
            if (!ready()) return;
            Schedule.prefs(this).edit().putBoolean("enabled", true).putString("state", "예약됨")
                .putString("detail", "실행 2분 전부터 화면과 버튼을 확인합니다.").apply();
            Schedule.restore(this);
            renderStatus();
        });
        button("10초 후 시험 실행", () -> {
            if (!ready()) return;
            new AlertDialog.Builder(this).setTitle("실제 버튼 클릭 시험")
                .setMessage("확인을 누른 뒤 10초 안에 경기지역화폐 앱의 빠른 충전 화면을 열어 두세요. ‘수원시’ 선택까지만 실행합니다.")
                .setNegativeButton("취소", null).setPositiveButton("시험 예약", (d, w) -> {
                    try { Schedule.arm(this, true, System.currentTimeMillis() + 10000); toast("10초 안에 대상 앱을 열어 두세요."); }
                    catch (SecurityException e) { toast("정확한 알람 권한을 확인하세요."); }
                }).show();
        });
        button("대상 앱 열기", () -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(Schedule.prefs(this).getString("package", ""));
            if (launch != null) startActivity(launch); else toast("대상 앱을 선택하세요.");
        });
        button("준비 상태 확인", () -> {
            if (ClickService.instance != null) ClickService.instance.watchSchedule();
            toast("실행 2분 전부터 준비합니다. 화면과 버튼 상태는 위에 표시됩니다.");
            renderStatus();
        });
        button("예약 끄기 · 실행 중지", () -> { Schedule.cancel(this); renderStatus(); });
        text("실행 전 준비\n• 화면 잠금을 풀고 대상 앱의 빠른 충전 화면을 열어 두세요.\n• 기기의 날짜·시간을 자동으로 설정하세요.\n• 배터리 절전·앱 자동 종료 설정을 확인하세요.\n\n정각 실행은 기기 상태와 운영체제 지연에 따라 달라집니다. 버튼 글자를 읽을 수 없거나 앱 화면이 바뀌면 실행하지 못할 수 있습니다. 실행 내역은 이 화면에서 확인하세요.", 15);
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(Color.rgb(25, 45, 70));
        view.setPadding(0, 12, 0, 16); column.addView(view); return view;
    }
    private void button(String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(v -> action.run()); column.addView(button);
    }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    private boolean ready() {
        if (Schedule.prefs(this).getString("package", "").isEmpty()) { toast("대상 앱을 선택하세요."); return false; }
        if (ClickService.instance == null) { toast("접근성 설정에서 ‘지역화폐 예약 클릭’을 켜세요."); return false; }
        if (!Schedule.allowed(this)) { toast("정확한 알람 권한을 허용하세요."); return false; }
        return true;
    }
    private void chooseApp() {
        if (Schedule.prefs(this).getBoolean("enabled", false) || Schedule.prefs(this).getLong("testAt", 0) > 0) {
            toast("예약 끄기 · 실행 중지를 누른 후 대상 앱을 변경하세요."); return;
        }
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        apps.removeIf(a -> a.activityInfo.packageName.equals(getPackageName()));
        apps.sort(Comparator.comparing(a -> a.loadLabel(getPackageManager()).toString()));
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) labels[i] = apps.get(i).loadLabel(getPackageManager()) + "\n" + apps.get(i).activityInfo.packageName;
        new AlertDialog.Builder(this).setTitle("설치된 경기지역화폐 앱 선택")
            .setItems(labels, (d, which) -> {
                ResolveInfo chosen = apps.get(which);
                Schedule.prefs(this).edit().putString("package", chosen.activityInfo.packageName)
                    .putString("appName", chosen.loadLabel(getPackageManager()).toString()).apply();
                renderStatus();
            }).show();
    }
    private void renderStatus() {
        android.content.SharedPreferences prefs = Schedule.prefs(this);
        long next = prefs.getLong("next", 0);
        String at = next > 0 ? Instant.ofEpochMilli(next).atZone(MonthlyTime.SEOUL)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "없음";
        status.setText("대상: " + prefs.getString("appName", "선택 필요")
            + "\n접근성: " + (ClickService.instance != null ? "연결됨" : "설정 필요")
            + " · 알람: " + (Schedule.allowed(this) ? "허용" : "권한 필요")
            + "\n월간 예약: " + (prefs.getBoolean("enabled", false) ? "켜짐" : "꺼짐")
            + "\n다음 실행(한국): " + at
            + "\n실행 상태: " + prefs.getString("state", "대기")
            + "\n" + prefs.getString("detail", "예약을 켜고 실행 2분 전에 대상 화면을 열어 주세요.")
            + "\n화면 유지: " + (prefs.getBoolean("screenKept", false) && ClickService.instance != null ? "사용 중" : "준비 구간에 자동 적용")
            + "\n\n" + clickTiming(prefs)
            + "\n최근 내역\n" + prefs.getString("result", "실행 내역 없음"));
    }
    private String clickTiming(android.content.SharedPreferences prefs) {
        long at = prefs.getLong("clickWall", 0);
        if (at <= 0) return "클릭 기록 없음\n";
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS");
        String first = Instant.ofEpochMilli(prefs.getLong("firstClickWall", at)).atZone(MonthlyTime.SEOUL).format(format);
        String last = Instant.ofEpochMilli(at).atZone(MonthlyTime.SEOUL).format(format);
        String target = Instant.ofEpochMilli(prefs.getLong("clickDue", at)).atZone(MonthlyTime.SEOUL).format(format);
        return "목표 시각(한국): " + target
            + "\n최근 실행 첫 클릭(기기 시각): " + first
            + "\n목표 대비 지연: " + prefs.getLong("firstClickDelay", 0) + "ms"
            + "\n마지막 버튼: " + prefs.getString("clickLabel", "") + " · " + last
            + "\n클릭 요청: " + (prefs.getBoolean("clickAccepted", false) ? "접수됨" : "거부됨")
            + "\n클릭 호출 시각이며 서버 도착·충전 완료 시각은 아닙니다.\n";
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}

