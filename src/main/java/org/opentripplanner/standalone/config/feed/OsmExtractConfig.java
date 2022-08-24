package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
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

  OsmExtractConfig(OsmExtractConfigBuilder osmExtractConfigBuilder) {
    this.source = Objects.requireNonNull(osmExtractConfigBuilder.getSource());
    this.osmWayPropertySet =
      osmExtractConfigBuilder.getOsmWayPropertySet() != null
        ? osmExtractConfigBuilder.getOsmWayPropertySet()
        : WayPropertySetSource.fromConfig("default");
    this.timeZone = osmExtractConfigBuilder.getTimeZone();
  }
}
