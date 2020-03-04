package org.opentripplanner.model.base;

import java.time.Duration;
import java.util.Calendar;

/**
 * Use this to-string-builder to build value objects. A
 * [ValueObject](http://wiki.c2.com/?ValueObject) is usually a small object/class with
 * a few fields. We want the {@code toString()} to be small and easy to read. The
 * text should be short and without class and field name prefixes.
 * <p>
 * Examples:
 * <pre>
 * - Money:  "5 kr", "USD 100"
 * - Time:   "2020-01-15", "3h3m5s", "14:23:59"
 * - Coordinate:  "(60.23451, 10.345678)"
 * </pre>
 * <p>
 * {@code ClassName{field1:value, field2:value, ..., NOT-SET:[fieldX, ...]}}
 * <p>
 * Use the {@link #of()}  factory method to create a instance of this class.
 */
public class ValueObjectToStringBuilder {
    private static final String FNAME = "field-name-skipped";

    private final ToStringBuilder buf = ToStringBuilder.valueObject();

    /** Use factory method: {@link #of()}. */
    private ValueObjectToStringBuilder() { }

    public static ValueObjectToStringBuilder of() {
        return new ValueObjectToStringBuilder();
    }

    /* General purpose formatters */

    public ValueObjectToStringBuilder addNum(Number num) {
        buf.addNum(FNAME, num);
        return this;
    }

    public ValueObjectToStringBuilder addNum(Number value, Number defaultValue) {
        buf.addNum(FNAME, value, defaultValue);
        return this;
    }

    public ValueObjectToStringBuilder addNum(Number num, String unit) {
        buf.addNum(FNAME, num, unit);
        return this;
    }

    public ValueObjectToStringBuilder addBool(Boolean value, String ifTrue, String ifFalse) {
        if(value == null) { return this; }
        buf.addObj(FNAME, value ? ifTrue : ifFalse);
        return this;
    }

    public ValueObjectToStringBuilder addStr(String value) {
        buf.addStr(FNAME, value);
        return this;
    }

    public ValueObjectToStringBuilder addEnum(Enum<?> value) {
        buf.addEnum(FNAME, value);
        return this;
    }

    public ValueObjectToStringBuilder addObj(Object obj) {
        buf.addObj(FNAME, obj);
        return this;
    }

    /* Special purpose formatters */

    /** Add a Coordinate location: (longitude, latitude) */
    public ValueObjectToStringBuilder addCoordinate(Number lat, Number lon) {
        if(lat == null && lon == null) { return this; }
        buf.addObj(FNAME, "(" + buf.formatCoordinate(lat) + ", " + buf.formatCoordinate(lon) + ")");
        return this;
    }

    /**
     * Add the TIME part in the local system timezone using 24 hours. Format:  HH:mm:ss.
     * Note! The DATE is not printed.
     */
    public ValueObjectToStringBuilder addCalTime(Calendar time) {
        buf.addCalTime(FNAME, time);
        return this;
    }
    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the prefix.
     */
    public ValueObjectToStringBuilder addDuration(Integer durationSeconds) {
        buf.addDuration(FNAME, durationSeconds);
        return this;
    }

    @Override
    public String toString() {
        String value = buf.toString();
        return value.isBlank() ? "<empty>" : value;
    }
}
