package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.street.model.edge.StreetEdge;

public class OsmIdNamer implements EdgeNamer {
  @Override
  public I18NString name(OSMWithTags way) { return new NonLocalizedString(Long.toString(way.getId())); }
  @Override
  public void recordEdge(OSMWithTags way, StreetEdge edge) { return;}
  @Override
  public void postprocess() { return; }
}
