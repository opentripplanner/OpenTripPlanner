package org.opentripplanner.netex.mapping;

import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.StringUtils;
import org.rutebanken.netex.model.MultilingualString;

public class MultilingualStringMapper {

  @Nullable
  public static String nullableValueOf(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }

    String value = multilingualString.getValue();
    if (StringUtils.hasNoValue(value)) {
      return null;
    }

    return value;
  }
}
