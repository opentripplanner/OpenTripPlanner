package org.opentripplanner.osm;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

/**
 * Provides OSM data to the graph build by filling in {@link OsmDatabase}.
 */
public interface OsmProvider {
  /**
   * Fill the mutable {@link OsmDatabase} with data.
   */
  void readOsm(OsmDatabase osmdb);

  OsmTagMapper getOsmTagMapper();

  void checkInputs();

  WayPropertySet getWayPropertySet();

  ZoneId getZoneId();
}
