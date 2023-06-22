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
  public static final boolean DEFAULT_REMOVE_REPEATED_STOPS = true;

  public static final StopTransferPriority DEFAULT_STATION_TRANSFER_PREFERENCE =
    StopTransferPriority.ALLOWED;

  public static final boolean DEFAULT_DISCARD_MIN_TRANSFER_TIMES = false;

  public static final boolean DEFAULT_BLOCK_BASED_INTERLINING = true;

  public static final int DEFAULT_MAX_INTERLINE_DISTANCE = 200;

  public static final GtfsFeedParameters DEFAULT = new GtfsFeedParametersBuilder().build();

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

  public GtfsFeedParametersBuilder copyOf() {
    return new GtfsFeedParametersBuilder(this);
  }
}
