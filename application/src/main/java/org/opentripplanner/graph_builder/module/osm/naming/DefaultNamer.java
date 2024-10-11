package org.opentripplanner.graph_builder.module.osm.naming;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmWithTags;

public class DefaultNamer implements EdgeNamer {

  @Override
  public I18NString name(OsmWithTags way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmWithTags way, StreetEdgePair edge) {}

  @Override
  public void postprocess() {}
}
