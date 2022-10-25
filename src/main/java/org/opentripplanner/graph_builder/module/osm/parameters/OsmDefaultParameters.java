package org.opentripplanner.graph_builder.module.osm.parameters;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

/**
 * Default configuration for OpenStreetMap feeds.
 */
public class OsmDefaultParameters {

  /**
   * The default set of rules for mapping OSM tags.
   */
  public final OsmTagMapper osmOsmTagMapper;

  /**
   * The default time zone for parsing opening hours.
   */
  public final ZoneId timeZone;

  public OsmDefaultParameters() {
    this(null, null);
  }

  public OsmDefaultParameters(OsmTagMapper osmOsmTagMapper, ZoneId timeZone) {
    this.osmOsmTagMapper = osmOsmTagMapper != null ? osmOsmTagMapper : OsmTagMapper.defaultMapper();
    this.timeZone = timeZone;
  }
}
