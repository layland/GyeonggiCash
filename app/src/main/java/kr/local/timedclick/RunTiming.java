package kr.local.timedclick;

public final class RunTiming {
    public static final long PREPARE_MS = 120000;
    public static final long GRACE_MS = 15000;
    public static long dueElapsed(long dueWall, long wallNow, long elapsedNow) {
        return elapsedNow + (dueWall - wallNow);
    }
    public static boolean expired(long dueElapsed, long elapsedNow) {
        return elapsedNow - dueElapsed > GRACE_MS;
    }
    public static long remaining(long dueElapsed, long elapsedNow) {
        return Math.max(0, dueElapsed - elapsedNow);
    }
    public static boolean canClaim(long pending, long incoming, long now, boolean authorized) {
        return authorized && incoming > 0 && pending == incoming && now >= incoming;
    }
    private RunTiming() {}
}
