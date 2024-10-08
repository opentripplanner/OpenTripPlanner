package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;

/**
 * Configure an OpenStreetMap extract.
 * Example: {@code "osm" : [ {source: "file:///path/to/otp/norway.pbf"} ] }
 *
 */
public record OsmExtractParameters(URI source, OsmTagMapperSource osmTagMapper, ZoneId timeZone)
  implements DataSourceConfig {
  public static final OsmTagMapperSource DEFAULT_OSM_TAG_MAPPER = OsmTagMapperSource.DEFAULT;

  public static final ZoneId DEFAULT_TIME_ZONE = null;

  public static final OsmExtractParameters DEFAULT = new OsmExtractParametersBuilder().build();

  OsmExtractParameters(OsmExtractParametersBuilder builder) {
    this(builder.getSource(), builder.getOsmTagMapper(), builder.getTimeZone());
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

  public OsmExtractParametersBuilder copyOf() {
    return new OsmExtractParametersBuilder(this);
  }
}
