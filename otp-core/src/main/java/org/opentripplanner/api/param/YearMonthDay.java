package org.opentripplanner.api.param;

import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class YearMonthDay extends QueryParameter {

    static int[] daysInMonth = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public int year;
    public int month;
    public int day;

    public YearMonthDay (String value) {
        super(value);
    }

    @Override
    protected void parse(String value) throws Throwable {
        if (value == null || value.equalsIgnoreCase("today") || value.isEmpty()) {
            value = new LocalDate().toString(); // eeew
        }
        String[] fields = value.split("-");
        year  = Integer.parseInt(fields[0]);
        month = Integer.parseInt(fields[1]);
        day   = Integer.parseInt(fields[2]);
        checkRangeInclusive(year, 2000, 2100);
        checkRangeInclusive(month, 1, 12);
        checkRangeInclusive(day, 1, daysInMonth[month - 1]);
    }

    @Override
    public String toString() {
        return String.format("%02d-%02d-%02d", year, month, day);
    }

    public LocalDate toJoda() {
        return new LocalDate(year, month, day);
    }

    public ServiceDate toOBA() {
        return new ServiceDate(year, month, day);
    }

}