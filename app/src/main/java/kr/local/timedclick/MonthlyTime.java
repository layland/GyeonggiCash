package kr.local.timedclick;

import java.time.*;

public final class MonthlyTime {
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    public static long next(long now) {
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(SEOUL);
        ZonedDateTime target = current.withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (!target.toInstant().isAfter(current.toInstant())) target = target.plusMonths(1);
        return target.toInstant().toEpochMilli();
    }
    public static boolean timely(long due, long now) { return due > 0 && now >= due && now - due <= 15000; }
    private MonthlyTime() {}
}
