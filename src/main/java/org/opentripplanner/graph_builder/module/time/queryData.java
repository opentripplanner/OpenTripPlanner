package org.opentripplanner.graph_builder.module.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

public class queryData {
    private int day;
    private long time;

    public queryData(int day, long time) {
        this.day = day;
        this.time = time;
    }

    public static queryData QueryNaw() {
        ZonedDateTime nowZoned = ZonedDateTime.now();
        Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
        Duration duration = Duration.between(midnight, Instant.now());
        long seconds = duration.getSeconds();
        return new queryData(nowZoned.getDayOfWeek().getValue(), seconds);

    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
