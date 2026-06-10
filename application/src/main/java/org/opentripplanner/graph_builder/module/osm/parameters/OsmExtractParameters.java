package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.osm.tagmapping.OsmTagMapperSource;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public record OsmExtractParameters(
  URI source,
  OsmTagMapperSource osmTagMapper,
  ZoneId timeZone,
  boolean includeOsmStationEntrances
) implements DataSourceConfig {
  public static final OsmTagMapperSource DEFAULT_OSM_TAG_MAPPER = OsmTagMapperSource.DEFAULT;

  public static final ZoneId DEFAULT_TIME_ZONE = null;

  public static final boolean DEFAULT_INCLUDE_OSM_STATION_ENTRANCES = false;

  public static final OsmExtractParameters DEFAULT = new OsmExtractParametersBuilder().build();

  OsmExtractParameters(OsmExtractParametersBuilder builder) {
    this(
      builder.getSource(),
      builder.getOsmTagMapper(),
      builder.getTimeZone(),
      builder.includeOsmStationEntrances()
    );
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   *
   * @return the timezone to use to resolve opening hours in this extract or the default.
   */
  @Nullable
  public ZoneId timeZone() {
    return timeZone;
  }

  public boolean includeOsmStationEntrances() {
    return includeOsmStationEntrances;
  }

  public OsmExtractParametersBuilder copyOf() {
    return new OsmExtractParametersBuilder(this);
  }
}
