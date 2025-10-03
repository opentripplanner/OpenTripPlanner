package org.opentripplanner.graph_builder.module.osm.naming;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;

/**
 * Combines the sidewalk and crosswalk namer.
 */
public class SidewalkCrosswalkNamer implements EdgeNamer {

  private final SidewalkNamer sidewalkNamer = new SidewalkNamer();
  private final CrosswalkNamer crosswalkNamer = new CrosswalkNamer();

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmEntity way, StreetEdgePair pair) {
    sidewalkNamer.recordEdges(way, pair);
    crosswalkNamer.recordEdges(way, pair);
  }

  @Override
  public void postprocess() {
    sidewalkNamer.postprocess();
    crosswalkNamer.postprocess();
  }
}
