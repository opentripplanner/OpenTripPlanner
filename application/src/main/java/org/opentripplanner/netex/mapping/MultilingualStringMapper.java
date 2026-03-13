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
   */
  @Nullable
  public static String getStringValue(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }
    List<Serializable> content = multilingualString.getContent();
    if (content == null || content.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (Serializable item : content) {
      if (item instanceof String s) {
        sb.append(s);
      }
    }
    return sb.length() > 0 ? sb.toString() : null;
  }
}
