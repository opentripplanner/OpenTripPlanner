package org.opentripplanner.standalone.config;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

/**
 * Configure an OpenStreetMap extract.
 */
public class OsmExtractConfig {

  /**
   * URI to the source file.
   */
  public final URI source;

  /**
   * Custom OSM way properties for this extract.
   */
  public final WayPropertySetSource osmWayPropertySet;

  /**
   * The timezone to use to resolve open hours in this extract.
   */
  public final ZoneId timeZone;

  OsmExtractConfig(NodeAdapter config) {
    source = config.asUri("source");
    osmWayPropertySet =
      WayPropertySetSource.fromConfig(config.asText("osmWayPropertySet", "default"));
    timeZone = config.asZoneId("timeZone", null);
  }
}
