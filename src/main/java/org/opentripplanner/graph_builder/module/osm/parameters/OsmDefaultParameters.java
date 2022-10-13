package org.opentripplanner.graph_builder.module.osm.parameters;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

/**
 * Default configuration for OpenStreetMap feeds.
 */
public class OsmDefaultParameters {

  /**
   * The default set of rules for mapping OSM tags.
   */
  public final WayPropertySetSource osmWayPropertySetSource;

  /**
   * The default time zone for parsing opening hours.
   */
  public final ZoneId timeZone;

  public OsmDefaultParameters() {
    this(null, null);
  }

  public OsmDefaultParameters(WayPropertySetSource osmWayPropertySetSource, ZoneId timeZone) {
    this.osmWayPropertySetSource =
      osmWayPropertySetSource != null
        ? osmWayPropertySetSource
        : WayPropertySetSource.defaultWayPropertySetSource();
    this.timeZone = timeZone;
  }
}
