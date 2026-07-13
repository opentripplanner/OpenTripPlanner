package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A vector tile layer containing all {@link GeofencingZone}s inside the vector tile bounds.
 */
public class GeofencingZonesLayerBuilder extends LayerBuilder<GeofencingZone> {

  private final GeofencingZoneService geofencingZoneService;

  public GeofencingZonesLayerBuilder(
    GeofencingZoneService geofencingZoneService,
    LayerParameters layerParameters
  ) {
    super(
      new GeofencingZonesPropertyMapper(),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.geofencingZoneService = geofencingZoneService;
  }

  @Override
  protected List<Geometry> findGeometries(Envelope query) {
    return geofencingZoneService
      .listZones()
      .stream()
      .map(this::createGeometryWithUserData)
      .toList();
  }

  private Geometry createGeometryWithUserData(GeofencingZone zone) {
    Geometry geometry = zone.geometry();
    geometry.setUserData(zone);
    return geometry;
  }
}
