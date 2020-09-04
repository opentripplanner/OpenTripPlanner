package org.opentripplanner.graph_builder.module.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public class QueryData {
    private int day;
    private long time;

    public QueryData(long timeMilis) {
        Instant i = Instant.ofEpochMilli(timeMilis);
        ZonedDateTime z = ZonedDateTime.ofInstant(i, ZoneId.of("Europe/Warsaw"));
        this.setDay(z.getDayOfWeek().getValue());
        this.setTime(z.getHour() * 3600 + z.getMinute() * 60 + z.getSecond());

    }

    public QueryData(int day, long time) {
        this.day = day;
        this.time = time;
    }

    public static QueryData QueryNaw() {
        ZonedDateTime nowZoned = ZonedDateTime.now();
        Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
        Duration duration = Duration.between(midnight, Instant.now());
        long seconds = duration.getSeconds();
        return new QueryData(nowZoned.getDayOfWeek().getValue(), seconds);

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
