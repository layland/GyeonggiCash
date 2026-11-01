import kr.local.timedclick.Region;
import java.time.Instant;

public class RegionTest {
    private static long t(String value) { return Instant.parse(value).toEpochMilli(); }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        long before = t("2026-11-01T05:10:00Z"); // November 1, 14:10 KST
        check(Region.HWASEONG.next(before) == t("2026-11-01T06:00:00Z"));
        check(Region.SUWON.next(before) == t("2026-12-01T00:00:00Z"));
        check(Region.YONGIN.next(before) == t("2026-12-01T04:00:00Z"));
        check(Region.HWASEONG.next(t("2026-11-01T06:00:00Z")) == t("2026-12-01T06:00:00Z"));
        check(Region.YONGIN.next(t("2026-11-01T03:59:59Z")) == t("2026-11-01T04:00:00Z"));
        check(Region.HWASEONG.next(t("2026-12-31T23:00:00Z")) == t("2027-01-01T06:00:00Z"));
        check(Region.selected(null) == Region.HWASEONG);
        check(Region.selected("suwon") == Region.SUWON);
        check(Region.selected("yongin") == Region.YONGIN);
        check(Region.selected("hwaseong") == Region.HWASEONG);
        check(Region.selected("unknown") == Region.HWASEONG);
        check(Region.HWASEONG.timeLabel().equals("15:00"));
        System.out.println("12 region and opening-time checks passed");
    }
}
