package kr.local.timedclick;

/** Opening hours shown by the region-selection screen, in Korea time. */
public enum Region {
    SUWON("suwon", "수원시", 9),
    YONGIN("yongin", "용인시", 13),
    HWASEONG("hwaseong", "화성시", 15);

    public final String id, label;
    public final int hour;
    Region(String id, String label, int hour) { this.id = id; this.label = label; this.hour = hour; }
    public String timeLabel() { return String.format(java.util.Locale.KOREA, "%02d:00", hour); }
    public long next(long now) { return MonthlyTime.next(now, hour); }
    public static Region selected(String id) {
        for (Region region : values()) if (region.id.equals(id)) return region;
        return HWASEONG;
    }
}
