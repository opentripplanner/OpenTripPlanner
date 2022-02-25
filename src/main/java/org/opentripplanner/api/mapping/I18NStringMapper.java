package org.opentripplanner.api.mapping;

import org.opentripplanner.util.I18NString;

import java.util.Locale;

public class I18NStringMapper {
    static String mapToApi(I18NString string, Locale locale) {
        return string == null ? null : string.toString(locale);
    }
}
