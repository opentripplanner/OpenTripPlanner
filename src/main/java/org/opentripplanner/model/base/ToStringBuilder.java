package org.opentripplanner.model.base;

import org.opentripplanner.transit.raptor.util.TimeUtils;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This toString builder witch add elements to a compact string of the form:
 * <p>
 * {@code ClassName{field1:value, field2:value, ..., NOT_SET:[fieldX, ...]}}
 * <p>
 * The {@code NOT_SET} list of fields are all field witch is null or have the default value.
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
    private final List<String> unsetFields = new ArrayList<>();

    private DecimalFormat integerFormat;
    private DecimalFormat decimalFormat;
    private DecimalFormat coordinateFormat;
    private SimpleDateFormat calTimeFormater;
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

    public ToStringBuilder addNum(String name, Number value, Number defaultValue) {
        return addIfNotDefault(name, value, defaultValue, this::formatNumber);
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

    public ToStringBuilder addObj(String name, Object obj, Object defaultValue) {
        return addIfNotDefault(name, obj, defaultValue);
    }

    public <T> ToStringBuilder addInts(String name, int[] intArray) {
        return addIfNotNull(name, intArray, Arrays::toString);
    }

    public ToStringBuilder addCol(String name, Collection<?> c) {
        return addIfNotNull(name, c);
    }

    public ToStringBuilder addColLimited(String name, Collection<?> c, int limit) {
        if(c == null) { return unset(name); }
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
     * Note! The DATE is not printed.
     */
    public ToStringBuilder addCalTime(String name, Calendar time) {
        return addIfNotNull(name, time, t -> formatTime(t.getTime()));
    }

    /**
     * Add time in seconds since midnight. Format:  hh:mm:ss. Ignore default values.
     */
    public ToStringBuilder addServiceTime(String name, int timeSecondsPastMidnight, int defaultValue) {
        return addIfNotDefault(name, timeSecondsPastMidnight, defaultValue, TimeUtils::timeToStrLong);
    }

    /**
     * Add times in seconds since midnight. Format:  hh:mm. Ignore default values.
     */
    public <T> ToStringBuilder addAsHhMm(String name, int[] intArray) {
        return addIfNotNull(
                name,
                intArray,
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
    public ToStringBuilder addDuration(String name, Integer durationSeconds) {
        return addIfNotDefault(name, durationSeconds, null, TimeUtils::durationToStr);
    }

    public ToStringBuilder addDuration(String name, Duration duration) {
        return addIfNotDefault(
            name,
            duration,
            null,
            d -> TimeUtils.durationToStr((int)d.toSeconds())
        );
    }

    @Override
    public String toString() {
        if(!unsetFields.isEmpty()) { addIt("NOT_SET", unsetFields.toString()); }

        return sb.append("}").toString();
    }


    /** private methods */

    private <T> ToStringBuilder addIfNotNull(String name, T value) {
        return addIfNotDefault(name, value, null);
    }

    private <T> ToStringBuilder addIfNotNull(String name, T value, Function<T, String> vToString) {
        return addIfNotDefault(name, value, null, vToString);
    }

    private <T> ToStringBuilder addIfNotDefault(String name, T value, T defaultValue) {
        return addIfNotDefault(name, value, defaultValue, Object::toString);
    }

    private <T> ToStringBuilder addIfNotDefault(String name, T value, T defaultValue, Function<T, String> mapToString) {
        if(value == defaultValue) { return unset(name); }
        if(defaultValue != null && defaultValue.equals(value)) { return unset(name); }
        if(value == null) { return addIt(name, "null"); }
        return addIt(name, mapToString.apply(value));
    }

    private ToStringBuilder addIt(String name, @NotNull String value) {
        if (first) { first = false; }
        else { sb.append(FIELD_SEPARATOR); }

        sb.append(name).append(":");
        sb.append(value);
        return this;
    }

    private ToStringBuilder unset(String name) {
        unsetFields.add(name);
        return this;
    }

    private String formatTime(Date time) {
        if(calTimeFormater == null) {
            calTimeFormater = new SimpleDateFormat("HH:mm:ss");
        }
        return calTimeFormater.format(time.getTime());
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
