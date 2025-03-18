package org.opentripplanner.gtfs.graphbuilder;

import java.net.URI;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.transit.model.site.StopTransferPriority;

/**
 * Configure a GTFS feed.
 * Example: {@code [ {type="gtfs", source: "file:///path/to/otp/norway-gtfs.zip"} ] }
 *
 * @param source See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}.
 * @param feedId See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}.
 */
public record GtfsFeedParameters(
  URI source,
  @Nullable String feedId,
  boolean removeRepeatedStops,
  StopTransferPriority stationTransferPreference,
  boolean discardMinTransferTimes,
  boolean blockBasedInterlining,
  int maxInterlineDistance
)
  implements DataSourceConfig {
  private static final URI URI_EXAMPLE = URI.create("file:///path/to/otp/norway-gtfs.zip");
  private static final boolean DEFAULT_REMOVE_REPEATED_STOPS = true;
  private static final boolean DEFAULT_DISCARD_MIN_TRANSFER_TIMES = false;
  private static final boolean DEFAULT_BLOCK_BASED_INTERLINING = true;
  private static final int DEFAULT_MAX_INTERLINE_DISTANCE = 200;

  public static final GtfsFeedParameters DEFAULT = new GtfsFeedParameters(
    URI_EXAMPLE,
    null,
    DEFAULT_REMOVE_REPEATED_STOPS,
    StopTransferPriority.defaultValue(),
    DEFAULT_DISCARD_MIN_TRANSFER_TIMES,
    DEFAULT_BLOCK_BASED_INTERLINING,
    DEFAULT_MAX_INTERLINE_DISTANCE
  );

  GtfsFeedParameters(GtfsFeedParametersBuilder builder) {
    this(
      builder.source(),
      builder.feedId(),
      builder.removeRepeatedStops(),
      builder.stationTransferPreference(),
      builder.discardMinTransferTimes(),
      builder.blockBasedInterlining(),
      builder.maxInterlineDistance()
    );
  }

  public static GtfsFeedParametersBuilder of() {
    return new GtfsFeedParametersBuilder(DEFAULT);
  }

  public GtfsFeedParametersBuilder copyOf() {
    return new GtfsFeedParametersBuilder(this);
  }
}
