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
 * override, witch often will case the wrong method to get chosen.
 */
public class ToStringBuilder {
    private final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);

    private DecimalFormat coordinateFormat;
    private SimpleDateFormat calTimeFormater;
    private final StringBuilder sb = new StringBuilder();
    private final List<String> unsetFields = new ArrayList<>();
    boolean first = true;

    public ToStringBuilder(Class<?> clazz) {
        this(clazz.getSimpleName());
    }
    public ToStringBuilder(String name) {
        sb.append(name).append("{");
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
        if(!unsetFields.isEmpty()) { addIt("NOT_SET", unsetFields.toString()); }
        sb.append("}");
        return sb.toString();
    }

    private <T> ToStringBuilder addIfNotNull(String name, T value) {
        if(value == null) { return unset(name); }
        return addIt(name, value.toString());
    }

    private <T> ToStringBuilder addIfNotNull(String name, T value, Function<T, String> vToString) {
        if(value == null) { return unset(name); }
        return addIt(name, vToString.apply(value));
    }

    private ToStringBuilder addIt(String name, @NotNull String value) {
        try {
            if (first) { first = false; }
            else { sb.append(", "); }

            sb.append(name).append(":").append(value);
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


    private String formatCoordinate(Number value) {
        if(coordinateFormat == null) {
            coordinateFormat = new DecimalFormat("#0.0####", DECIMAL_SYMBOLS);
        }
        return coordinateFormat.format(value);
    }
}
