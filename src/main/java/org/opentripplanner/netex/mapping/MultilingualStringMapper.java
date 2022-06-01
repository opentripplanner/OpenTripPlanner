package org.opentripplanner.netex.mapping;

import javax.annotation.Nullable;
import org.rutebanken.netex.model.MultilingualString;

public class MultilingualStringMapper {

  @Nullable
  public static String nullableValueOf(@Nullable MultilingualString string) {
    return string == null ? null : string.getValue();
  }
}
