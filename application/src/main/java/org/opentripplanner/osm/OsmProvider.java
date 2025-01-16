package org.opentripplanner.osm;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public interface OsmProvider {
  void readOsm(OsmDatabase osmdb);

  OsmTagMapper getOsmTagMapper();

  void checkInputs();

  WayPropertySet getWayPropertySet();

  ZoneId getZoneId();
}
