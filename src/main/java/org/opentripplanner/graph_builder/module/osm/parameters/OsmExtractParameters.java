package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.standalone.config.feed.DataSourceConfig;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public class OsmExtractParameters implements DataSourceConfig {

  private final URI source;

  private final WayPropertySetSource osmWayPropertySet;

  private final ZoneId timeZone;

  OsmExtractParameters(OsmExtractParametersBuilder builder) {
    this.source = Objects.requireNonNull(builder.getSource());
    this.osmWayPropertySet = builder.getOsmWayPropertySet();
    this.timeZone = builder.getTimeZone();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the custom OSM way properties for this OSM extract. Overrides {@link OsmDefaultParameters#osmWayPropertySetSource}.
   */
  public Optional<WayPropertySetSource> osmWayPropertySet() {
    return Optional.ofNullable(osmWayPropertySet);
  }

  /**
   *
   * @return the timezone to use to resolve opening hours in this extract. Overrides {@link OsmDefaultParameters#timeZone}
   */
  public Optional<ZoneId> timeZone() {
    return Optional.ofNullable(timeZone);
  }
}
