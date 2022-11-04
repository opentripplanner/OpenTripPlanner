package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public class OsmExtractParameters implements DataSourceConfig {

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final URI source;

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final OsmTagMapper osmTagMapper;

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final ZoneId timeZone;

  OsmExtractParameters(OsmExtractParametersBuilder builder) {
    this.source = Objects.requireNonNull(builder.getSource());
    this.osmTagMapper = builder.getOsmTagMapper();
    this.timeZone = builder.getTimeZone();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the custom OSM way properties for this OSM extract. Overrides {@link OsmDefaultParameters#osmOsmTagMapper}.
   */
  public Optional<OsmTagMapper> osmTagMapper() {
    return Optional.ofNullable(osmTagMapper);
  }

  /**
   *
   * @return the timezone to use to resolve opening hours in this extract. Overrides {@link OsmDefaultParameters#timeZone}
   */
  public Optional<ZoneId> timeZone() {
    return Optional.ofNullable(timeZone);
  }
}
