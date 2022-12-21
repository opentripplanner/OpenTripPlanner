package org.opentripplanner.framework.i18n;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class LocalizedStringFormat implements I18NString, Serializable {

  private final String format;
  private final I18NString[] values;

  public LocalizedStringFormat(String format, I18NString... values) {
    this.format = format;
    this.values = values;
  }

  @Override
  public String toString(Locale locale) {
    return String.format(
      format,
      Arrays.stream(values).map(i -> i.toString(locale)).toArray(Object[]::new)
    );
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(format);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final LocalizedStringFormat that = (LocalizedStringFormat) o;
    return format.equals(that.format) && Arrays.equals(values, that.values);
  }

  @Override
  public String toString() {
    return this.toString(null);
  }
}
