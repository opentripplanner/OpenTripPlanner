package org.opentripplanner.ext.vectortiles;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.I18NString;

public class I18NStringMapper {

  private Locale locale;

  public I18NStringMapper(Locale locale) {
    this.locale = locale;
  }

  @Nullable
  public String mapToApi(I18NString string) {
    return string == null ? null : string.toString(locale);
  }

  @Nonnull
  public String mapNonnullToApi(I18NString string) {
    return string.toString(locale);
  }
}
