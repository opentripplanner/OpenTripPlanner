package org.opentripplanner.api.mapping;

import java.util.Locale;
import org.opentripplanner.transit.model.basic.I18NString;

public class I18NStringMapper {

  private Locale locale;

  I18NStringMapper(Locale locale) {
    this.locale = locale;
  }

  public String mapToApi(I18NString string) {
    return string == null ? null : string.toString(locale);
  }

  static String mapToApi(I18NString string, Locale locale) {
    return string == null ? null : string.toString(locale);
  }
}
