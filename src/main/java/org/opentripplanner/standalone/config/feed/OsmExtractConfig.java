package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public class OsmExtractConfig implements DataSourceConfig {

  private final URI source;

  private final WayPropertySetSource osmWayPropertySet;

  private final ZoneId timeZone;

  OsmExtractConfig(OsmExtractConfigBuilder osmExtractConfigBuilder) {
    this.source = Objects.requireNonNull(osmExtractConfigBuilder.getSource());
    this.osmWayPropertySet = osmExtractConfigBuilder.getOsmWayPropertySet();
    this.timeZone = osmExtractConfigBuilder.getTimeZone();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the custom OSM way properties for this OSM extract. Overrides {@link OsmDefaultsConfig#osmWayPropertySetSource}.
   */
  public Optional<WayPropertySetSource> osmWayPropertySet() {
    return Optional.ofNullable(osmWayPropertySet);
  }

  /**
   *
   * @return the timezone to use to resolve opening hours in this extract. Overrides {@link OsmDefaultsConfig#timeZone}
   */
  public Optional<ZoneId> timeZone() {
    return Optional.ofNullable(timeZone);
  }
}
