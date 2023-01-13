package org.opentripplanner.inspector.vector.geofencing;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension;

/**
 * A {@link PropertyMapper} for the {@link GeofencingZonesLayerBuilder} for the OTP debug client.
 */
public class GeofencingZonesPropertyMapper extends PropertyMapper<StreetEdge> {

  @Override
  protected Collection<KeyValue> map(StreetEdge input) {
    var ext = input.getTraversalExtension();

    // this logic does a best effort attempt at a simple mapping
    // once you have several networks on the same edge it breaks down for that you would
    // really need several layers
    // still, for a quick visualization it is useful
    var isBusinessAreaBorder = ext instanceof StreetEdgeRentalExtension.BusinessAreaBorder;
    if (isBusinessAreaBorder) {
      return List.of(new KeyValue("type", "business-area-border"));
    } else if (ext instanceof StreetEdgeRentalExtension.GeofencingZoneExtension gfz) {
      if (gfz.zone().passingThroughBanned()) {
        return List.of(new KeyValue("type", "traversal-banned"));
      } else {
        return List.of(new KeyValue("type", "drop-off-banned"));
      }
    } else {
      return List.of();
    }
  }
}
