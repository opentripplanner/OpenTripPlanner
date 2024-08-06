package org.opentripplanner.framework.i18n;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public static String mapToApi(I18NString string, Locale locale) {
    return string == null ? null : string.toString(locale);
  }
}
