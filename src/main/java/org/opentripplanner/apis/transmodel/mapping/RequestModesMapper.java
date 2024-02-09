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
   * Default modes are WALK for access, egress, direct & transfer.
   */
  static RequestModes mapRequestModes(Map<String, ?> modesInput) {
    RequestModesBuilder mBuilder = RequestModes.of();

    if (modesInput.containsKey(accessModeKey)) {
      StreetMode accessMode = (StreetMode) modesInput.get(accessModeKey);
      mBuilder.withAccessMode(accessMode);
      mBuilder.withTransferMode(accessMode == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK);
    }
    if (modesInput.containsKey(egressModeKey)) {
      mBuilder.withEgressMode((StreetMode) modesInput.get(egressModeKey));
    }
    if (modesInput.containsKey(directModeKey)) {
      mBuilder.withDirectMode((StreetMode) modesInput.get(directModeKey));
    }

    return mBuilder.build();
  }
}
