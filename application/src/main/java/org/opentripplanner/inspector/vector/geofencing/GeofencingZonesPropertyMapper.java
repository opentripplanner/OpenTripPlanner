package org.opentripplanner.inspector.vector.geofencing;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A {@link PropertyMapper} for the {@link GeofencingZonesLayerBuilder} for the OTP debug client.
 */
public class GeofencingZonesPropertyMapper extends PropertyMapper<GeofencingZone> {

  @Override
  protected Collection<KeyValue> map(GeofencingZone zone) {
    var properties = new ArrayList<KeyValue>();

    properties.add(new KeyValue("id", zone.id().toString()));
    properties.add(new KeyValue("name", zone.name().toString()));
    properties.add(new KeyValue("network", zone.id().getFeedId()));

    if (zone.isBusinessArea()) {
      properties.add(new KeyValue("type", "business-area"));
    } else if (zone.traversalBanned()) {
      properties.add(new KeyValue("type", "no-traversal"));
    } else if (zone.dropOffBanned()) {
      properties.add(new KeyValue("type", "no-drop-off"));
    }

    properties.add(new KeyValue("traversalBanned", String.valueOf(zone.traversalBanned())));
    properties.add(new KeyValue("dropOffBanned", String.valueOf(zone.dropOffBanned())));

    return properties;
  }
}
