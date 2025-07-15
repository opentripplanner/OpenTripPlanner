package org.opentripplanner.gtfs.config;

import java.net.URI;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Configure a GTFS feed.
 * Example: {@code [ {type="gtfs", source: "file:///path/to/otp/norway-gtfs.zip"} ] }
 * <p>
 * For object creation, use the {@link GtfsDefaultParameters#withFeedInfo()} method which returns a builder.
 * For example: {@code GtfsDefaultParameters.DEFAULT.copyOfFeed()}.
 */
public final class GtfsFeedParameters extends GtfsDefaultParameters implements DataSourceConfig {

  private final @Nullable String feedId;
  private final URI source;

  public GtfsFeedParameters(
    @Nullable String feedId,
    URI source,
    boolean removeRepeatedStops,
    StopTransferPriority stationTransferPreference,
    boolean discardMinTransferTimes,
    boolean blockBasedInterlining,
    int maxInterlineDistance
  ) {
    super(
      removeRepeatedStops,
      stationTransferPreference,
      discardMinTransferTimes,
      blockBasedInterlining,
      maxInterlineDistance
    );
    this.feedId = feedId;
    this.source = Objects.requireNonNull(source);
  }

  /**
   *  See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}.
   */
  @Nullable
  public String feedId() {
    return feedId;
  }

  /**
   * See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}.
   */
  @Override
  public URI source() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException(
      "Equals and hashCode is not implemented for this class."
    );
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException(
      "Equals and hashCode is not implemented for this class."
    );
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.of(GtfsFeedParameters.class)
      .addStr("feedId", feedId)
      .addObj("source", source);
    return super.toStringAppend(builder).toString();
  }
}
