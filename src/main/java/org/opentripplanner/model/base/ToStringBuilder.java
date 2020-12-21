package org.opentripplanner.model.base;

import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This toString builder witch add elements to a compact string of the form:
 * <p>
 * {@code ClassName{field1:value, field2:value, ...}}
 * <p>
 * Fields equals to 'ignoreValue' is NOT added to the result string. This produces a short and easy
 * to read result. You should use {@code null} as 'ignoreValue' if the field is nullable.
 * <p>
 * The naming of the 'add' methods should give a hint to witch type the value have, this make it
 * easier to choose the right method and less error prune as compared with relaying on pure
 * override, witch often result in a wrong method call.
 * <p>
 * The builder should be independent of locale, the value should always be formatted the same way,
 * this allows us to use the toString in unit tests.
 */
public class ToStringBuilder {
    private static final String FIELD_SEPARATOR = ", ";
    private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private final StringBuilder sb = new StringBuilder();

    private DecimalFormat integerFormat;
    private DecimalFormat decimalFormat;
    private DecimalFormat coordinateFormat;
    private SimpleDateFormat calendarTimeFormat;
    boolean first = true;

    private ToStringBuilder(String name) {
        sb.append(name).append("{");
    }

    /**
     * Create a ToStringBuilder for a regular POJO type. This builder
     * will include metadata(class and field names) when building the to sting.
     */
    public static ToStringBuilder of(Class<?> clazz) {
        return new ToStringBuilder(clazz.getSimpleName());
    }


    /* General purpose formatters */

    public ToStringBuilder addNum(String name, Number num) {
        return addIfNotNull(name, num, this::formatNumber);
    }

    public ToStringBuilder addNum(String name, Number value, Number ignoreValue) {
        return addIfNotIgnored(name, value, ignoreValue, this::formatNumber);
    }

    public ToStringBuilder addNum(String name, Number num, String unit) {
        return addIfNotNull(name, num, n -> formatNumber(n) + unit);
    }

    public ToStringBuilder addBool(String name, Boolean value) {
        return addIfNotNull(name, value);
    }

    public ToStringBuilder addStr(String name, String value) {
        return addIfNotNull(name, value, v -> "'" + v + "'");
    }

    public ToStringBuilder addEnum(String name, Enum<?> value) {
        return addIfNotNull(name, value);
    }

    public ToStringBuilder addObj(String name, Object obj) {
        return addIfNotNull(name, obj);
    }

    public ToStringBuilder addEntityId(String name, TransitEntity entity) {
        return addIfNotNull(name, entity, e -> e.getId().toString());
    }

    public <T> ToStringBuilder addInts(String name, int[] intArray) {
        return addIfNotNull(name, intArray, Arrays::toString);
    }

    public ToStringBuilder addCol(String name, Collection<?> c) {
        return addIfNotNull(name, c);
    }

    public ToStringBuilder addColLimited(String name, Collection<?> c, int limit) {
        if(c == null) { return this; }
        if(c.size() > limit+1) {
            String value = c.stream()
                    .limit(limit)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return addIt(
                    name + "(" + limit + "/" + c.size() + ")",
                    "[" + value + ", ..]"

            );
        }
        return addIfNotNull(name, c);
    }

    /* Special purpose formatters */

    /** Add a Coordinate location, longitude or latitude */
    public ToStringBuilder addCoordinate(String name, Number num) {
        return addIfNotNull(name, num, this::formatCoordinate);
    }

    /**
     * Add the TIME part in the local system timezone using 24 hours. Format:  HH:mm:ss.
     * Note! The DATE is not printed. {@code null} value is ignored.
     */
    public ToStringBuilder addCalTime(String name, Calendar time) {
        return addIfNotNull(name, time, t -> formatTime(t.getTime()));
    }

    /**
     * Add time in seconds since midnight. Format:  hh:mm:ss. Ignore default values.
     */
    public ToStringBuilder addServiceTime(String name, int timeSecondsPastMidnight, int ignoreValue) {
        return addIfNotIgnored(name, timeSecondsPastMidnight, ignoreValue, TimeUtils::timeToStrCompact);
    }

    /**
     * Add times in seconds since midnight. Format:  hh:mm. {@code null} value is ignored.
     */
    public <T> ToStringBuilder addServiceTimeSchedule(String name, int[] value) {
        return addIfNotNull(
                name,
                value,
                a -> Arrays.stream(a)
                        .mapToObj(TimeUtils::timeToStrShort)
                        .collect(Collectors.joining(", ", "[", "]"))
        );
    }

    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the 'PT' prefix. {@code null} value is ignored.
     */
    public ToStringBuilder addDurationSec(String name, Integer durationSeconds) {
        return addIfNotIgnored(name, durationSeconds, null, TimeUtils::durationToStr);
    }

    public ToStringBuilder addDuration(String name, Duration duration) {
        return addIfNotIgnored(
            name,
            duration,
            null,
            d -> TimeUtils.durationToStr((int)d.toSeconds())
        );
    }

    @Override
    public String toString() {
        return sb.append("}").toString();
    }


    /** private methods */

    private <T> ToStringBuilder addIfNotNull(String name, T value) {
        return addIfNotIgnored(name, value, null, Object::toString);
    }

    private <T> ToStringBuilder addIfNotNull(String name, T value, Function<T, String> vToString) {
        return addIfNotIgnored(name, value, null, vToString);
    }

    private <T> ToStringBuilder addIfNotIgnored(String name, T value, T ignoreValue, Function<T, String> mapToString) {
        if(value == ignoreValue) { return this; }
        if(ignoreValue != null && ignoreValue.equals(value)) { return this; }
        if(value == null) { return addIt(name, "null"); }
        return addIt(name, mapToString.apply(value));
    }

    private ToStringBuilder addIt(String name, @NotNull String value) {
        if (first) { first = false; }
        else { sb.append(FIELD_SEPARATOR); }

        sb.append(name).append(": ");
        sb.append(value);
        return this;
    }

    private String formatTime(Date time) {
        if(calendarTimeFormat == null) {
            calendarTimeFormat = new SimpleDateFormat("HH:mm:ss");
        }
        return calendarTimeFormat.format(time.getTime());
    }

    String formatCoordinate(Number value) {
        if(coordinateFormat == null) {
            coordinateFormat = new DecimalFormat("#0.0####", DECIMAL_SYMBOLS);
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
