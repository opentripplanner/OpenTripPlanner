package org.opentripplanner.inspector.vector.geofencing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.model.RentalRestrictionExtension;

/**
 * A vector tile layer containing all {@link GeofencingZone}s inside the vector tile bounds.
 */
public class GeofencingZonesLayerBuilder extends LayerBuilder<GeofencingZone> {

  private final Graph graph;

  public GeofencingZonesLayerBuilder(Graph graph, LayerParameters layerParameters) {
    super(
      new GeofencingZonesPropertyMapper(),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    Map<String, GeofencingZone> uniqueZones = new HashMap<>();

    graph
      .findVertices(query)
      .stream()
      .filter(v -> !(v.rentalRestrictions() instanceof NoRestriction))
      .forEach(vertex -> {
        extractGeofencingZones(vertex.rentalRestrictions(), uniqueZones);
      });

    return uniqueZones
      .values()
      .stream()
      .map(zone -> {
        Geometry geometry = zone.geometry();
        geometry.setUserData(zone);
        return geometry;
      })
      .toList();
  }

  private void extractGeofencingZones(
    RentalRestrictionExtension extension,
    Map<String, GeofencingZone> uniqueZones
  ) {
    for (RentalRestrictionExtension ext : extension.toList()) {
      if (ext instanceof GeofencingZoneExtension zoneExt) {
        GeofencingZone zone = zoneExt.zone();
        uniqueZones.put(zone.id().toString(), zone);
      }
    }
  }
}
