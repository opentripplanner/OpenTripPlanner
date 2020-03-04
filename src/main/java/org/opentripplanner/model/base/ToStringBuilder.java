package org.opentripplanner.model.base;

import org.opentripplanner.transit.raptor.util.TimeUtils;

import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * This toString builder witch add elements to a compact string of the form:
 * <p>
 * {@code ClassName{field1:value, field2:value, ..., NOT-SET:[fieldX, ...]}}
 * <p>
 * The naming of the 'add' methods should give a hint to witch type the value have, this make it
 * easier to choose the right method and less error prune as compared with relaying on pure
 * override, witch often result in a wrong method call.
 */
public class ToStringBuilder {
    private static final String FIELD_SEPARATOR_VO = " ";
    private static final String FIELD_SEPARATOR_OBJ = ", ";
    private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private final boolean includeMetadata;
    private final StringBuilder sb = new StringBuilder();
    private final List<String> unsetFields = new ArrayList<>();

    private DecimalFormat coordinateFormat;
    private SimpleDateFormat calTimeFormater;
    boolean first = true;

    private ToStringBuilder(String name, boolean includeMetadata) {
        this.includeMetadata = includeMetadata;

        if(this.includeMetadata) {
            sb.append(name).append("{");
        }
    }

    /**
     * Create a ToStringBuilder for a regular POJO type. This builder
     * will include metadata(class and field names) when building the to sting.
     */
    public static ToStringBuilder of(Class<?> clazz) {
        return new ToStringBuilder(clazz.getSimpleName(), true);
    }

    /**
     * Create a ToStringBuilder for a ValueObject type.
     * This is used by the {@link ValueObjectToStringBuilder} only; Hence the
     * package local access modifier.
     */
    static ToStringBuilder valueObject() {
        return new ToStringBuilder(null, false);
    }

    /* General purpose formatters */

    public ToStringBuilder addNum(String name, Number num) {
        return addIfNotNull(name, num);
    }
    public ToStringBuilder addNum(String name, Number value, Number defaultValue) {
        if(value == null || value.equals(defaultValue)) { return unset(name); }
        return addIt(name, value.toString());
    }
    public ToStringBuilder addNum(String name, Number num, String unit) {
        return addIfNotNull(name, num, n -> n.toString() + unit);
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
    public ToStringBuilder addCol(String name, Collection<?> c) {
        return addIfNotNull(name, c);
    }

    /* Special purpose formatters */

    /** Add a Coordinate location, longitude or latitude */
    public ToStringBuilder addCoordinate(String name, Number num) {
        return addIfNotNull(name, num, this::formatCoordinate);
    }

    /** Add a Coordinate location: (longitude, latitude) */
    public ToStringBuilder addCoordinate(String name, Number lat, Number lon) {
        if(lat == null && lon == null) { return unset(name); }
        addObj(name, "(" + formatCoordinate(lat) + ", " + formatCoordinate(lon) + ")");
        return this;
    }

    /**
     * Add the TIME part in the local system timezone using 24 hours. Format:  HH:mm:ss.
     * Note! The DATE is not printed.
     */
    public ToStringBuilder addCalTime(String name, Calendar time) {
        return addIfNotNull(name, time, t -> formatTime(t.getTime()));
    }
    /**
     * Add a duration to the string in format like '3h4m35s'. Each component (hours, minutes, and or
     * seconds) is only added if they are not zero {@code 0}. This is the same format as the
     * {@link Duration#toString()}, but without the prefix.
     */
    public ToStringBuilder addDuration(String name, Integer durationSeconds) {
        return addIfNotNull(name, durationSeconds, TimeUtils::durationToStr);
    }

    @Override
    public String toString() {
        if(includeMetadata) {
            if(!unsetFields.isEmpty()) { addIt("NOT_SET", unsetFields.toString()); }
            sb.append("}");
        }
        return sb.toString();
    }

    private <S> ToStringBuilder addIfNotNull(String name, S value) {
        if(value == null) { return unset(name); }
        return addIt(name, value.toString());
    }

    private <S> ToStringBuilder addIfNotNull(String name, S value, Function<S, String> vToString) {
        if(value == null) { return unset(name); }
        return addIt(name, vToString.apply(value));
    }

    private ToStringBuilder addIt(String name, @NotNull String value) {
        try {
            if (first) { first = false; }
            else { sb.append(includeMetadata ? FIELD_SEPARATOR_OBJ : FIELD_SEPARATOR_VO); }

            if(includeMetadata) {
                sb.append(name).append(":");
            }
            sb.append(value);
            return this;
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
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
}
