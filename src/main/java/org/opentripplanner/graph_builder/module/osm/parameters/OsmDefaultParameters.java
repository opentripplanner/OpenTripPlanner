package org.opentripplanner.graph_builder.module.osm.parameters;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

/**
 * Default configuration for OpenStreetMap feeds.
 */
public class OsmDefaultParameters {

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  public final OsmTagMapper osmOsmTagMapper;

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  public final ZoneId timeZone;

  public OsmDefaultParameters() {
    this(null, null);
  }

  public OsmDefaultParameters(OsmTagMapper osmOsmTagMapper, ZoneId timeZone) {
    this.osmOsmTagMapper = osmOsmTagMapper != null ? osmOsmTagMapper : OsmTagMapper.defaultMapper();
    this.timeZone = timeZone;
  }
}
