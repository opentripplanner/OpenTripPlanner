package org.opentripplanner.ext.edgenaming;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;

/**
 * Combines the sidewalk and crosswalk namer.
 */
class SidewalkCrosswalkNamer implements EdgeNamer {

  private final SidewalkNamer sidewalkNamer = new SidewalkNamer();
  private final CrosswalkNamer crosswalkNamer = new CrosswalkNamer();

  @Override
  public I18NString name(OsmEntity entity) {
    return entity.getAssumedName();
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair pair, OsmDatabase osmdb) {
    sidewalkNamer.recordEdges(way, pair, osmdb);
    crosswalkNamer.recordEdges(way, pair, osmdb);
  }

  @Override
  public void finalizeNames() {
    sidewalkNamer.finalizeNames();
    crosswalkNamer.finalizeNames();
  }
}
