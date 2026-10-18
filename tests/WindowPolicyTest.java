import kr.local.timedclick.WindowPolicy;
import java.util.List;

public class WindowPolicyTest {
    private static final String TARGET = "selected.app";
    private static WindowPolicy.Window app(String owner, int layer) {
        return new WindowPolicy.Window(true, false, false, layer, owner);
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        WindowPolicy.Window target = app(TARGET, 1);
        WindowPolicy.Window overlay = new WindowPolicy.Window(false, true, true, 10, "our.app");
        check(WindowPolicy.select(List.of(target, overlay), TARGET) == 0);
        check(WindowPolicy.select(List.of(overlay, target), TARGET) == 1);
        check(WindowPolicy.select(List.of(target, app("another.app", 2)), TARGET) == WindowPolicy.OTHER_APPLICATION);
        check(WindowPolicy.select(List.of(app("background.app", 0), target), TARGET) == 1);
        check(WindowPolicy.select(List.of(target, new WindowPolicy.Window(false, false, true, 20, "android")), TARGET) == WindowPolicy.BLOCKED);
        check(WindowPolicy.select(List.of(target, new WindowPolicy.Window(false, false, false, 20, "android")), TARGET) == 0);
        check(WindowPolicy.select(List.of(app(null, 1)), TARGET) == WindowPolicy.UNREADABLE);
        check(WindowPolicy.select(List.of(), TARGET) == WindowPolicy.NO_APPLICATION);
        System.out.println("8 foreground-window checks passed");
    }
}
