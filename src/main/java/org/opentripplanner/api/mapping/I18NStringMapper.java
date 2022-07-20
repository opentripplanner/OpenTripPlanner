package org.opentripplanner.api.mapping;

import java.util.Locale;
import org.opentripplanner.transit.model.basic.I18NString;

public class I18NStringMapper {

  static String mapToApi(I18NString string, Locale locale) {
    return string == null ? null : string.toString(locale);
  }
}
