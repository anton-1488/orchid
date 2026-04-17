package com.subgraph.orchid.bridge;

import java.util.concurrent.TimeUnit;

public enum RetestInterval {
    ONE_HOUR(hoursToMs(1)),
    FOUR_HOURS(hoursToMs(4)),
    SIX_HOURS(hoursToMs(6)),
    EIGHTEEN_HOURS(hoursToMs(18)),
    THIRTY_SIX_HOURS(hoursToMs(36)),
    THREE_DAYS(daysToMs(3)),
    SEVEN_DAYS(daysToMs(7)),
    THIRTY_DAYS(daysToMs(30)),
    SIXTY_DAYS(daysToMs(60));

    private static long hoursToMs(long n) {
        return TimeUnit.MILLISECONDS.convert(n, TimeUnit.HOURS);
    }

    private static long daysToMs(long n) {
        return TimeUnit.MILLISECONDS.convert(n, TimeUnit.DAYS);
    }

    private final long time;

    RetestInterval(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    /**
     * path-spec 5.
     * <p>
     * If Tor fails to connect to an otherwise usable guard, it retries
     * periodically: every hour for six hours, every 4 hours for 3 days, every
     * 18 hours for a week, and every 36 hours thereafter.
     */
    public static RetestInterval getRetestInterval(long timeDown) {
        if (timeDown < SIX_HOURS.getTime()) {
            return ONE_HOUR;
        } else if (timeDown < THREE_DAYS.getTime()) {
            return FOUR_HOURS;
        } else if (timeDown < SEVEN_DAYS.getTime()) {
            return EIGHTEEN_HOURS;
        } else {
            return THIRTY_SIX_HOURS;
        }
    }
}