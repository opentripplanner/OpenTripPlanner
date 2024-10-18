package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nullable;
import org.opentripplanner.model.PickDrop;

public final class PickDropMapper {

  @Nullable
  public static String map(PickDrop pickDrop) {
    return switch (pickDrop) {
      case SCHEDULED -> "SCHEDULED";
      case NONE -> "NONE";
      case CALL_AGENCY -> "CALL_AGENCY";
      case COORDINATE_WITH_DRIVER -> "COORDINATE_WITH_DRIVER";
      case CANCELLED -> null;
    };
  }
}
