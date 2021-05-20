package org.opentripplanner.model.base;

import static java.lang.Boolean.TRUE;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

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
    /** A random in value, not expected to exist in data */
    private static final int RANDOM_IGNORE_VALUE = -9_371_207;
    private static final String FIELD_SEPARATOR = ", ";
    private static final String FIELD_VALUE_SEP = ": ";
    private static final String NULL_VALUE = "null";

    private final StringBuilder sb = new StringBuilder();
    private final NumberFormat numFormat = new NumberFormat();

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
        return addIfNotNull(name, num, numFormat::formatNumber);
    }

    public ToStringBuilder addNum(String name, Number value, Number ignoreValue) {
        return addIfNotIgnored(name, value, ignoreValue, numFormat::formatNumber);
    }

    public ToStringBuilder addNum(String name, Number num, String unit) {
        return addIfNotNull(name, num, n -> numFormat.formatNumber(n, unit));
    }

    public ToStringBuilder addBool(String name, Boolean value) {
        return addIfNotNull(name, value);
    }

    public ToStringBuilder addBoolIfTrue(String name, Boolean value) {
        if(TRUE.equals(value)) { addLabel(name); }
        return this;
    }

    public ToStringBuilder addStr(String name, String value) {
        return addIfNotNull(name, value, v -> "'" + v + "'");
    }

    public ToStringBuilder addEnum(String name, Enum<?> value) {
        return addEnum(name, value, null);
    }

    public ToStringBuilder addEnum(String name, Enum<?> value, Enum<?> ignoreValue) {
        return addIfNotIgnored(name, value, ignoreValue, Enum::name);
    }

    public ToStringBuilder addObj(String name, Object obj) {
        return addIfNotNull(name, obj);
    }

    public ToStringBuilder addEntityId(String name, TransitEntity entity) {
        return addIfNotNull(name, entity, e -> e.getId().toString());
    }

    public ToStringBuilder addInts(String name, int[] intArray) {
        return addIfNotNull(name, intArray, Arrays::toString);
    }

    public ToStringBuilder addCol(String name, Collection<?> c) {
        return addIfNotNull(name, c);
    }

    /** Add the collection, truncate the number of elements at given maxLimit. */
    public ToStringBuilder addCollection(String name, Collection<?> c, int maxLimit) {
        if(c == null) { return this; }
        if(c.size() > maxLimit+1) {
            String value = c.stream()
                    .limit(maxLimit)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return addIt(
                    name + "(" + maxLimit + "/" + c.size() + ")",
                    "[" + value + ", ..]"

            );
        }
        return addIfNotNull(name, c);
    }

    /** Add the collection, truncate the number of elements at given maxLimit. */
    public ToStringBuilder addIntArraySize(String name, int[] array, int notSet) {
        if(array == null) { return this; }
        return addIt(
            name,
            Arrays.stream(array).filter(t -> t != notSet).count() + "/" + array.length
        );
    }

    /** Add the BitSet: name : {cardinality}/{logical size}/{size} */
    public ToStringBuilder addBitSetSize(String name, BitSet bitSet) {
        if(bitSet == null) { return this; }
        return addIt(name, bitSet.cardinality() + "/" + bitSet.length());
    }

    /* Special purpose formatters */

    /** Add a Coordinate location, longitude or latitude */
    public ToStringBuilder addCoordinate(String name, Number num) {
        return addIfNotNull(name, num, numFormat::formatCoordinate);
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
        return addIfNotIgnored(
                name, timeSecondsPastMidnight, ignoreValue, TimeUtils::timeToStrCompact
        );
    }

    /**
     * Add time in seconds since midnight. Format:  hh:mm:ss.
     */
    public ToStringBuilder addServiceTime(String name, int timeSecondsPastMidnight) {
        return addIfNotIgnored(
                name, timeSecondsPastMidnight, RANDOM_IGNORE_VALUE, TimeUtils::timeToStrCompact
        );
    }

    /**
     * Add times in seconds since midnight. Format:  hh:mm. {@code null} value is ignored.
     */
    public ToStringBuilder addServiceTimeSchedule(String name, int[] value) {
        return addIfNotNull(
            name,
            value,
            a -> Arrays.stream(a)
                .mapToObj(TimeUtils::timeToStrCompact)
                .collect(Collectors.joining(" ", "[", "]"))
        );
    }

    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the 'PT' prefix. {@code null} value is ignored.
     */
    public ToStringBuilder addDurationSec(String name, Integer durationSeconds) {
        return addDurationSec(name, durationSeconds, null);
    }

    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the 'PT' prefix. {@code null} value is ignored.
     */
    public ToStringBuilder addDurationSec(String name, Integer durationSeconds, Integer ignoreValue) {
        return addIfNotIgnored(name, durationSeconds, ignoreValue, DurationUtils::durationToStr);
    }

    public ToStringBuilder addDuration(String name, Duration duration) {
        return addIfNotIgnored(
            name,
            duration,
            null, d -> DurationUtils.durationToStr((int)d.toSeconds())
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
        if(value == null) { return addIt(name, NULL_VALUE); }
        return addIt(name, mapToString.apply(value));
    }

    private ToStringBuilder addIt(String name, @NotNull String value) {
        addLabel(name);
        addValue(value);
        return this;
    }

    private void addLabel(String name) {
        if (first) { first = false; }
        else { sb.append(FIELD_SEPARATOR); }
        sb.append(name);
    }

    private void addValue(@NotNull String value) {
        sb.append(FIELD_VALUE_SEP);
        sb.append(value);
    }

    private String formatTime(Date time) {
        if(calendarTimeFormat == null) {
            calendarTimeFormat = new SimpleDateFormat("HH:mm:ss");
        }
        return calendarTimeFormat.format(time.getTime());
    }
}
