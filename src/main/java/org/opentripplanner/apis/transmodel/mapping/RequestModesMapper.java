package org.opentripplanner.apis.transmodel.mapping;

import java.util.Map;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.StreetMode;

class RequestModesMapper {

  private static final String accessModeKey = "accessMode";
  private static final String egressModeKey = "egressMode";
  private static final String directModeKey = "directMode";

  /**
   * Maps GraphQL Modes input type to RequestModes.
   * <p>
   * This only maps access, egress, direct & transfer modes. Transport modes are set using filters.
   */
  static RequestModes mapRequestModes(Map<String, ?> modesInput) {
    RequestModesBuilder mBuilder = RequestModes.of();

    if (modesInput.containsKey(accessModeKey)) {
      StreetMode accessMode = (StreetMode) modesInput.get(accessModeKey);
      mBuilder.withAccessMode(accessMode);
      mBuilder.withTransferMode(accessMode == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK);
    } else {
      mBuilder.withAccessMode(StreetMode.NOT_SET);
    }
    // Non-existing values and null are later translated into NOT_SET.
    mBuilder.withEgressMode((StreetMode) modesInput.get(egressModeKey));
    mBuilder.withDirectMode((StreetMode) modesInput.get(directModeKey));

    return mBuilder.build();
  }
}
