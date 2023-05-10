package org.opentripplanner.graph_builder.module.osm.naming;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.edge.StreetEdge;

public class DefaultNamer implements EdgeNamer {

  @Override
  public I18NString name(OSMWithTags way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdge(OSMWithTags way, StreetEdge edge) {}

  @Override
  public void postprocess() {}
}
