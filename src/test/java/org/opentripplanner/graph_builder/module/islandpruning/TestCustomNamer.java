package org.opentripplanner.graph_builder.module.islandpruning;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetEdge;

class TestCustomNamer implements CustomNamer {

  @Override
  public I18NString name(OSMWithTags way) {
    return new NonLocalizedString(String.valueOf(way.getId()));
  }

  @Override
  public void nameWithEdge(OSMWithTags way, StreetEdge edge) {}

  @Override
  public void postprocess(Graph graph) {}
}
