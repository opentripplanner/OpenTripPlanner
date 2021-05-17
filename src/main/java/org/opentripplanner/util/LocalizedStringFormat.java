package org.opentripplanner.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LocalizedStringFormat implements I18NString, Serializable {

    private final String format;
    private final I18NString[] values;

    public LocalizedStringFormat(String format, I18NString ... values) {
        this.format = format;
        this.values = values;
    }

    @Override
    public String toString() {
        return this.toString(null);
    }

    @Override
    public String toString(Locale locale) {
        return String.format(format, Arrays.stream(values).map(i -> i.toString(locale)).toArray(Object[]::new));
    }
}
