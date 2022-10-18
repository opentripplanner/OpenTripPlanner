package org.opentripplanner.ext.vectortiles;

import java.util.Locale;
import org.opentripplanner.transit.model.basic.I18NString;

public class I18NStringMapper {

  private Locale locale;

  public I18NStringMapper(Locale locale) {
    this.locale = locale;
  }

  public String mapToApi(I18NString string) {
    return string == null ? null : string.toString(locale);
  }
}
