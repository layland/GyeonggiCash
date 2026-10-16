import kr.local.timedclick.MonthlyTime;
import java.time.Instant;

public class MonthlyTimeTest {
    private static long t(String value) { return Instant.parse(value).toEpochMilli(); }
    private static void next(String now, String expected) {
        if (MonthlyTime.next(t(now)) != t(expected)) throw new AssertionError(now);
    }
    public static void main(String[] args) {
        next("2026-10-31T23:59:59Z", "2026-11-01T00:00:00Z");
        next("2026-11-01T00:00:00Z", "2026-12-01T00:00:00Z");
        next("2026-12-31T23:59:59Z", "2027-01-01T00:00:00Z");
        next("2028-02-29T12:00:00Z", "2028-03-01T00:00:00Z");
        next("2026-10-01T00:00:01Z", "2026-11-01T00:00:00Z");
        if (!MonthlyTime.timely(1000, 16000)) throw new AssertionError("deadline inclusive");
        if (MonthlyTime.timely(1000, 16001)) throw new AssertionError("late alarm");
        if (MonthlyTime.timely(1000, 999)) throw new AssertionError("early alarm");
        if (MonthlyTime.timely(0, 1000)) throw new AssertionError("consumed alarm");
        System.out.println("9 schedule checks passed");
    }
}
