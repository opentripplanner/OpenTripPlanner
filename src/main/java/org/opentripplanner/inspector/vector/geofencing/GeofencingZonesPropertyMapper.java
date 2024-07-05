package org.opentripplanner.inspector.vector.geofencing;

import static org.opentripplanner.street.model.RentalRestrictionExtension.RestrictionType.BUSINESS_AREA_BORDER;
import static org.opentripplanner.street.model.RentalRestrictionExtension.RestrictionType.NO_DROP_OFF;
import static org.opentripplanner.street.model.RentalRestrictionExtension.RestrictionType.NO_TRAVERSAL;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A {@link PropertyMapper} for the {@link GeofencingZonesLayerBuilder} for the OTP debug client.
 */
public class GeofencingZonesPropertyMapper extends PropertyMapper<Vertex> {

  @Override
  protected Collection<KeyValue> map(Vertex input) {
    var ext = input.rentalRestrictions();

    // this logic does a best effort attempt at a simple mapping
    // once you have several networks on the same vertex it breaks down.
    // for that you would really need several layers.
    // still, for a quick visualization it is useful
    var debug = ext.debugTypes();
    var networks = new KeyValue("networks", String.join(",", ext.networks()));
    if (debug.contains(BUSINESS_AREA_BORDER)) {
      return List.of(new KeyValue("type", "business-area-border"), networks);
    } else if (debug.contains(NO_TRAVERSAL)) {
      return List.of(new KeyValue("type", "traversal-banned"), networks);
    } else if (debug.contains(NO_DROP_OFF)) {
      return List.of(new KeyValue("type", "drop-off-banned"), networks);
    } else {
      return List.of();
    }
  }
}
