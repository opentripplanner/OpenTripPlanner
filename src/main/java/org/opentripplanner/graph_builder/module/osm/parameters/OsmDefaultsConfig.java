package org.opentripplanner.graph_builder.module.osm.parameters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Default configuration for OpenStreetMap feeds.
 */
public class OsmDefaultsConfig {

  /**
   * The default set of rules for mapping OSM tags.
   */
  public final WayPropertySetSource osmWayPropertySetSource;

  /**
   * The default time zone for parsing opening hours.
   */
  public final ZoneId timeZone;

  public OsmDefaultsConfig() {
    this(null, null);
  }

  public OsmDefaultsConfig(NodeAdapter config) {
    this(
      config
        .of("osmTagMapping")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asEnum(WayPropertySetSource.Source.DEFAULT)
        .getInstance(),
      config.of("timeZone").withDoc(NA, /*TODO DOC*/"TODO").asZoneId(null)
    );
  }

  public OsmDefaultsConfig(WayPropertySetSource osmWayPropertySetSource, ZoneId timeZone) {
    this.osmWayPropertySetSource =
      osmWayPropertySetSource != null
        ? osmWayPropertySetSource
        : WayPropertySetSource.defaultWayPropertySetSource();
    this.timeZone = timeZone;
  }
}
