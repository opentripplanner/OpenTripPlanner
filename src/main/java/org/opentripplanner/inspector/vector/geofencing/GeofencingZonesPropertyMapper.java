package org.opentripplanner.inspector.vector.geofencing;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.AreaStopsLayerBuilder;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * A {@link PropertyMapper} for the {@link AreaStopsLayerBuilder} for the OTP debug client.
 */
public class GeofencingZonesPropertyMapper extends PropertyMapper<StreetEdge> {

  private final Graph graph;

  public GeofencingZonesPropertyMapper(Graph graph) {
    this.graph = graph;
  }

  public static PropertyMapper<StreetEdge> create(Graph graph) {
    return new GeofencingZonesPropertyMapper(graph);
  }

  @Override
  protected Collection<KeyValue> map(StreetEdge input) {
    return List.of();
  }
}
