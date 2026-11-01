package kr.local.timedclick;

public final class PreparationPolicy {
    public static final long MANUAL_LEAD_MS = 30 * 60 * 1000L;
    public static boolean previewOnly(long now, long due) { return due - now > MANUAL_LEAD_MS; }
    public static long keepAwakeTimeout(long nowElapsed, long dueElapsed) {
        return Math.min(MANUAL_LEAD_MS, Math.max(0, dueElapsed - nowElapsed)) + RunTiming.GRACE_MS + 5000;
    }
    private PreparationPolicy() {}
}
