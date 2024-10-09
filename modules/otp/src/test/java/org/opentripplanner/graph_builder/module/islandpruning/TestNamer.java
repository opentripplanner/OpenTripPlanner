package org.opentripplanner.graph_builder.module.islandpruning;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class TestNamer implements EdgeNamer {

  @Override
  public I18NString name(OSMWithTags way) {
    return new NonLocalizedString(String.valueOf(way.getId()));
  }

  @Override
  public void recordEdges(OSMWithTags way, StreetEdgePair edge) {}

  @Override
  public void postprocess() {}
}
