package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.model.vertex.Vertex;

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
      .findVertices(query)
      .stream()
      .filter(v -> !(v.rentalRestrictions() instanceof NoRestriction))
      .flatMap(this::extractZones)
      .distinct()
      .map(this::createGeometryWithUserData)
      .toList();
  }

  private Stream<GeofencingZone> extractZones(Vertex vertex) {
    return vertex
      .rentalRestrictions()
      .toList()
      .stream()
      .filter(ext -> ext instanceof GeofencingZoneExtension)
      .map(ext -> ((GeofencingZoneExtension) ext).zone());
  }

  private Geometry createGeometryWithUserData(GeofencingZone zone) {
    Geometry geometry = zone.geometry();
    geometry.setUserData(zone);
    return geometry;
  }
}
