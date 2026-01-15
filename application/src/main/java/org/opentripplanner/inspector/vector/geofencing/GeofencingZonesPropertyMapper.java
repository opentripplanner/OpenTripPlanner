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

  public static final String GEOFENCING_ZONE_TYPE_BUSINESS_AREA = "business-area";
  public static final String GEOFENCING_ZONE_TYPE_NO_TRAVERSAL = "no-traversal";
  public static final String GEOFENCING_ZONE_TYPE_NO_DROP_OFF = "no-drop-off";
  public static final String GEOFENCING_ZONE_TYPE = "type";
  public static final String GEOFENCING_ZONE_ID = "id";
  public static final String GEOFENCING_ZONE_NAME = "name";
  public static final String GEOFENCING_ZONE_NETWORK = "network";

  @Override
  protected Collection<KeyValue> map(GeofencingZone zone) {
    var properties = new ArrayList<KeyValue>();

    properties.add(kv(GEOFENCING_ZONE_ID, zone.id()));
    properties.add(kv(GEOFENCING_ZONE_NAME, zone.name()));
    properties.add(kv(GEOFENCING_ZONE_NETWORK, zone.id().getFeedId()));

    if (zone.isBusinessArea()) {
      properties.add(kv(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_BUSINESS_AREA));
    } else if (zone.traversalBanned()) {
      properties.add(kv(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_NO_TRAVERSAL));
    } else if (zone.dropOffBanned()) {
      properties.add(kv(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_NO_DROP_OFF));
    }

    return properties;
  }
}
