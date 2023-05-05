package org.opentripplanner.graph_builder.module.islandpruning;

import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;

class TestCustomNamer implements CustomNamer {

  @Override
  public String name(OSMWithTags way, String defaultName) {
    return String.valueOf(way.getId());
  }

  @Override
  public void nameWithEdge(OSMWithTags way, StreetEdge edge) {}

  @Override
  public void postprocess(Graph graph) {}

  @Override
  public void configure() {}
}
