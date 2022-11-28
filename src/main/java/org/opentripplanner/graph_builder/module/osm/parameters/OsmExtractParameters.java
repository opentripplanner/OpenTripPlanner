package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import java.util.Optional;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public class OsmExtractParameters implements DataSourceConfig {

  public static final OsmTagMapper.Source DEFAULT_OSM_TAG_MAPPER = OsmTagMapper.Source.DEFAULT;

  public static final OsmExtractParameters DEFAULT = new OsmExtractParametersBuilder().build();

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final URI source;

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final OsmTagMapper.Source osmTagMapper;

  /** See {@link org.opentripplanner.standalone.config.buildconfig.OsmConfig}. */
  private final ZoneId timeZone;

  OsmExtractParameters(OsmExtractParametersBuilder builder) {
    this.source = builder.getSource();
    this.osmTagMapper = builder.getOsmTagMapper();
    this.timeZone = builder.getTimeZone();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the custom OSM way properties for this OSM extract or the default.
   */
  public OsmTagMapper.Source osmTagMapper() {
    return osmTagMapper;
  }

  /**
   *
   * @return the timezone to use to resolve opening hours in this extract or the default.
   */
  public Optional<ZoneId> timeZone() {
    return Optional.ofNullable(timeZone);
  }

  public OsmExtractParametersBuilder copyOf() {
    return new OsmExtractParametersBuilder(this);
  }
}
