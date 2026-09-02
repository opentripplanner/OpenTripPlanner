package org.opentripplanner.graph_builder.module.osm.internal.naming;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.model.StreetEdgePair;
import org.opentripplanner.graph_builder.module.osm.storage.OsmDatabase;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;

public class DefaultNamer implements EdgeNamer {

  @Override
  public I18NString name(OsmEntity entity) {
    return entity.getAssumedName();
  }

  @Override
  public void recordEdges(OsmWay way, StreetEdgePair edge, OsmDatabase osmdb) {}

  @Override
  public void finalizeNames() {}
}
