package org.opentripplanner.netex.mapping;

import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.StringUtils;
import org.rutebanken.netex.model.MultilingualString;

public class MultilingualStringMapper {

  @Nullable
  public static String nullableValueOf(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }

    String value = getStringValue(multilingualString);
    if (StringUtils.hasNoValue(value)) {
      return null;
    }

    return value;
  }

  /**
   * Extract the string value from a MultilingualString.
   * In NeTEx 2.0, MultilingualString uses a mixed content model where
   * the text is stored in getContent() as a list of serializable objects.
   * <p>
   * Returns {@code null} only if the element is absent. An empty element (e.g.
   * {@code <Description/>}) returns an empty string, like {@code getValue()} did before NeTEx 2.0.
   * Use {@link #nullableValueOf(MultilingualString)} to map empty values to {@code null}.
   * <p>
   * Tabs, carriage returns and line feeds are each replaced by a space ({@code xs:normalizedString}
   * semantics), like the {@code NormalizedStringAdapter} applied to {@code getValue()} before
   * NeTEx 2.0. Text is neither trimmed nor collapsed.
   */
  @Nullable
  public static String getStringValue(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }
    List<Serializable> content = multilingualString.getContent();
    if (content == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Serializable item : content) {
      if (item instanceof String s) {
        sb.append(s);
      }
    }
    return normalize(sb);
  }

  private static String normalize(StringBuilder text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\t' || c == '\n' || c == '\r') {
        text.setCharAt(i, ' ');
      }
    }
    return text.toString();
  }
}
