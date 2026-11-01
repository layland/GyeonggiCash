import kr.local.timedclick.PreparationPolicy;

public class PreparationPolicyTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(!PreparationPolicy.previewOnly(1000, 11000));
        check(!PreparationPolicy.previewOnly(1000, 1801000));
        check(PreparationPolicy.previewOnly(1000, 1801001));
        check(PreparationPolicy.previewOnly(1000, 30L * 24 * 60 * 60 * 1000));
        check(PreparationPolicy.keepAwakeTimeout(1000, 10000) == 29000);
        check(PreparationPolicy.keepAwakeTimeout(1000, 10000000) == 1820000);
        check(PreparationPolicy.keepAwakeTimeout(1000, 500) == 20000);
        System.out.println("7 one-tap preparation checks passed");
    }
}
