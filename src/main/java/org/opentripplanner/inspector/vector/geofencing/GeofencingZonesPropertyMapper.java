package org.opentripplanner.inspector.vector.geofencing;

import static org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.DebugInfo.BUSINESS_AREA_BORDER;
import static org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.DebugInfo.NO_DROP_OFF;
import static org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.DebugInfo.NO_TRAVERSAL;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.edge.StreetEdge;

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
    var debug = ext.debug();
    if (debug.contains(BUSINESS_AREA_BORDER)) {
      return List.of(new KeyValue("type", "business-area-border"));
    } else if (debug.contains(NO_TRAVERSAL)) {
      return List.of(new KeyValue("type", "traversal-banned"));
    } else if (debug.contains(NO_DROP_OFF)) {
      return List.of(new KeyValue("type", "drop-off-banned"));
    } else {
      return List.of();
    }
  }
}
