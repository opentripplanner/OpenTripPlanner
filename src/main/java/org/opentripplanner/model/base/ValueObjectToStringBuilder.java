package org.opentripplanner.model.base;

import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;

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
    private static final String FIELD_SEPARATOR = " ";

    private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(
            Locale.US
    );

    private final StringBuilder sb = new StringBuilder();

    private DecimalFormat coordinateFormat;
    private DecimalFormat integerFormat;
    private DecimalFormat decimalFormat;
    boolean skipSep = true;

    /** Use factory method: {@link #of()}. */
    private ValueObjectToStringBuilder() { }

    /**
     * Create a new toString builder for a [ValueObject](http://wiki.c2.com/?ValueObject) type.
     * The builder will NOT include metadata(class and field names) when building the string.
     */
    public static ValueObjectToStringBuilder of() {
        return new ValueObjectToStringBuilder();
    }

    /* General purpose formatters */

    public ValueObjectToStringBuilder addNum(Number num) {
        return addIt(num, it -> formatNumber(num));
    }

    public ValueObjectToStringBuilder addNum(Number num, String unit) {
        return addIt(num, it -> formatNumber(it) + unit);
    }

    public ValueObjectToStringBuilder addBool(Boolean value, String ifTrue, String ifFalse) {
        return addIt(value, it -> it ? ifTrue : ifFalse);
    }

    public ValueObjectToStringBuilder addStr(String value) {
        return addIt(value, it -> "'" + it + "'");
    }

    public ValueObjectToStringBuilder addEnum(Enum<?> value) {
        return addIt(value, Enum::name);
    }

    public ValueObjectToStringBuilder addObj(Object obj) {
        return addIt(obj, Object::toString);
    }

    /**
     * A text/labels to your string. No separator character is writen to the buffer
     * before or after the label - hence you need to include white space in the label if you
     * want it.
     */
    public ValueObjectToStringBuilder addLbl(String label) {
        sb.append(label);
        skipSep = true;
        return this;
    }


    /* Special purpose formatters */

    /**
     * Add a Coordinate location: (longitude, latitude). The coordinate is
     * printed with a precision of 5 digits after the period. The precision level used
     * in OTP is 7 digits, so 2 coordinates that appear to be equal (by toString) might not be
     * exactly equals.
     */
    public ValueObjectToStringBuilder addCoordinate(Number lat, Number lon) {
        return addIt("(" + formatCoordinate(lat) + ", " + formatCoordinate(lon) + ")");
    }

    /**
     * Add time in seconds since midnight. Format:  HH:mm:ss.
     */
    public  ValueObjectToStringBuilder addServiceTime(int secondsPastMidnight) {
        // Use a NOT_SET value witch is unlikely to be used
        return addServiceTime(secondsPastMidnight, -87_654_321);
    }

    /**
     * Add time in seconds since midnight. Format:  HH:mm:ss. Ignore if not set.
     */
    public  ValueObjectToStringBuilder addServiceTime(int secondsPastMidnight, int notSet) {
        return addIt(TimeUtils.timeToStrLong(secondsPastMidnight, notSet));
    }

    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the 'PT' prefix.
     */
    public ValueObjectToStringBuilder addDuration(Integer durationSeconds) {
        return addIt(durationSeconds, TimeUtils::durationToStr);
    }

    @Override
    public String toString() {
        return sb.toString();
    }


    /* private methods  */

    private  ValueObjectToStringBuilder  addIt(String value) {
        return addIt(value, it -> it);
    }

    private <T> ValueObjectToStringBuilder  addIt(T value, Function<T, String> mapToString) {
        if (skipSep) { skipSep = false; }
        else { sb.append(FIELD_SEPARATOR); }
        sb.append(value == null ? "null" : mapToString.apply(value));
        return this;
    }

    String formatCoordinate(Number value) {
        if(coordinateFormat == null) {
            coordinateFormat = new DecimalFormat("0.0####", DECIMAL_SYMBOLS);
        }
        // This need to be null-safe, because one of the coordinates in
        // #addCoordinate(String name, Number lat, Number lon) could be null.
        return value == null ? "null" : coordinateFormat.format(value);
    }

    String formatNumber(Number value) {
        if (value == null) { return "null"; }

        if(value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            if(integerFormat == null) {
                integerFormat = new DecimalFormat("#,##0", DECIMAL_SYMBOLS);
            }
            return integerFormat.format(value);
        }

        if(decimalFormat == null) {
            decimalFormat = new DecimalFormat("#,##0.0##", DECIMAL_SYMBOLS);
        }
        return decimalFormat.format(value);
    }
}
