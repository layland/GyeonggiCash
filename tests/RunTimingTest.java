import kr.local.timedclick.RunTiming;

public class RunTimingTest {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        long due = RunTiming.dueElapsed(1000000, 880000, 5000);
        check(due == 125000);
        check(RunTiming.remaining(due, 5000) == 120000);
        check(RunTiming.remaining(due, 125001) == 0);
        check(!RunTiming.expired(due, 140000));
        check(RunTiming.expired(due, 140001));
        check(RunTiming.dueElapsed(1000, 2000, 5000) == 4000);
        // Once mapped, subsequent changes of wall time are not part of this calculation.
        check(RunTiming.remaining(due, 124999) == 1);
        check(RunTiming.canClaim(1000, 1000, 1000, true));
        check(!RunTiming.canClaim(0, 1000, 1001, true)); // consumed by the other trigger
        check(!RunTiming.canClaim(2000, 1000, 2000, true)); // stale broadcast
        check(!RunTiming.canClaim(1000, 1000, 999, true));
        check(!RunTiming.canClaim(1000, 1000, 1000, false));
        System.out.println("12 timing and duplicate-trigger checks passed");
    }
}
