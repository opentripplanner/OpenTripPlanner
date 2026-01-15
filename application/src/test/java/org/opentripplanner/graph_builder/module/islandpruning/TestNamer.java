package org.opentripplanner.graph_builder.module.islandpruning;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;

class TestNamer implements EdgeNamer {

  @Override
  public I18NString name(OsmEntity entity) {
    return new NonLocalizedString(String.valueOf(entity.getId()));
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair edge, OsmDatabase osmdb) {}

  @Override
  public void finalizeNames() {}
}
