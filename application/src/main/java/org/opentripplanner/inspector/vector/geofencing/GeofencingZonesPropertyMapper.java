package org.opentripplanner.inspector.vector.geofencing;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

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

    properties.add(kv("id", zone.id()));
    properties.add(kv("name", zone.name()));
    properties.add(kv("network", zone.id().getFeedId()));

    if (zone.isBusinessArea()) {
      properties.add(kv("type", "business-area"));
    } else if (zone.traversalBanned()) {
      properties.add(kv("type", "no-traversal"));
    } else if (zone.dropOffBanned()) {
      properties.add(kv("type", "no-drop-off"));
    }

    return properties;
  }
}
