package org.opentripplanner.api.param;

public class HourMinuteSecond extends QueryParameter {
    
    public int hour;
    public int minute;
    public int second;

    public HourMinuteSecond (String value) {
        super(value);
    }

    @Override
    protected void parse(String value) throws Throwable {
        String[] fields = value.split(":");
        hour = Integer.parseInt(fields[0]);
        if (fields.length > 1) {
            minute = Integer.parseInt(fields[1]);
        }
        if (fields.length > 2) {
            second = Integer.parseInt(fields[2]);
        }
        checkRangeInclusive(hour,   0, 23);
        checkRangeInclusive(minute, 0, 59);            
        checkRangeInclusive(second, 0, 59);            
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public int toSeconds() {
        return (hour * 60 + minute) * 60 + second;
    }

}