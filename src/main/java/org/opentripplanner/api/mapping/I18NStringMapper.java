package org.opentripplanner.api.mapping;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

public class I18NStringMapper {

  private final Locale locale;

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

  @Nullable
  static String mapToApi(I18NString string, Locale locale) {
    return string == null ? null : string.toString(locale);
  }
}
