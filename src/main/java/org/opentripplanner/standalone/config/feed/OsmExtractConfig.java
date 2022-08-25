package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

/**
 * Configure an OpenStreetMap extract.
 */
public class OsmExtractConfig implements DataSourceConfig {

  private final URI source;

  private final WayPropertySetSource osmWayPropertySet;

  private final ZoneId timeZone;

  OsmExtractConfig(OsmExtractConfigBuilder osmExtractConfigBuilder) {
    this.source = Objects.requireNonNull(osmExtractConfigBuilder.getSource());
    this.osmWayPropertySet =
      osmExtractConfigBuilder.getOsmWayPropertySet() != null
        ? osmExtractConfigBuilder.getOsmWayPropertySet()
        : WayPropertySetSource.fromConfig("default");
    this.timeZone = osmExtractConfigBuilder.getTimeZone();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the custom OSM way properties for this OSM extract.
   */
  public WayPropertySetSource getOsmWayPropertySet() {
    return osmWayPropertySet;
  }

  /**
   *
   * @return the timezone to use to resolve open hours in this extract.
   */
  public ZoneId timeZone() {
    return timeZone;
  }
}
