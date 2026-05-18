package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.graph.Graph;

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
    return graph
      .getAllGeofencingZoneIndexes()
      .values()
      .stream()
      .flatMap(index -> index.getAllZones().stream())
      .distinct()
      .map(this::createGeometryWithUserData)
      .toList();
  }

  private Geometry createGeometryWithUserData(GeofencingZone zone) {
    Geometry geometry = zone.geometry();
    geometry.setUserData(zone);
    return geometry;
  }
}
