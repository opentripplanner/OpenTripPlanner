package org.opentripplanner.gtfs.config;

import java.util.Objects;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Default GTFS feed configuration - can be set for all feeds in the
 * build-config. The {@link GtfsFeedParameters} contain the feed specific
 * extensions - the extra info you must specify for each feed.
 */
public sealed class GtfsDefaultParameters permits GtfsFeedParameters {

  private static final boolean DEFAULT_REMOVE_REPEATED_STOPS = true;
  private static final boolean DEFAULT_DISCARD_MIN_TRANSFER_TIMES = false;
  private static final boolean DEFAULT_BLOCK_BASED_INTERLINING = true;
  private static final int DEFAULT_MAX_INTERLINE_DISTANCE = 200;

  public static final GtfsDefaultParameters DEFAULT = new GtfsDefaultParameters(
    DEFAULT_REMOVE_REPEATED_STOPS,
    StopTransferPriority.defaultValue(),
    DEFAULT_DISCARD_MIN_TRANSFER_TIMES,
    DEFAULT_BLOCK_BASED_INTERLINING,
    DEFAULT_MAX_INTERLINE_DISTANCE
  );

  private final boolean removeRepeatedStops;
  private final StopTransferPriority stationTransferPreference;
  private final boolean discardMinTransferTimes;
  private final boolean blockBasedInterlining;
  private final int maxInterlineDistance;

  protected GtfsDefaultParameters(
    boolean removeRepeatedStops,
    StopTransferPriority stationTransferPreference,
    boolean discardMinTransferTimes,
    boolean blockBasedInterlining,
    int maxInterlineDistance
  ) {
    this.removeRepeatedStops = removeRepeatedStops;
    this.stationTransferPreference = Objects.requireNonNull(stationTransferPreference);
    this.discardMinTransferTimes = discardMinTransferTimes;
    this.blockBasedInterlining = blockBasedInterlining;
    this.maxInterlineDistance = maxInterlineDistance;
  }

  GtfsDefaultParameters(GtfsDefaultParametersBuilder builder) {
    this(
      builder.removeRepeatedStops(),
      builder.stationTransferPreference(),
      builder.discardMinTransferTimes(),
      builder.blockBasedInterlining(),
      builder.maxInterlineDistance()
    );
  }

  /**
   * Note! To get a copy of the default use {@code DEFAULT.copyOf()}
   */
  public GtfsDefaultParametersBuilder copyOf() {
    return new GtfsDefaultParametersBuilder(this);
  }

  public GtfsFeedParametersBuilder withFeedInfo() {
    return new GtfsFeedParametersBuilder(this);
  }

  public boolean removeRepeatedStops() {
    return removeRepeatedStops;
  }

  public StopTransferPriority stationTransferPreference() {
    return stationTransferPreference;
  }

  public boolean discardMinTransferTimes() {
    return discardMinTransferTimes;
  }

  public boolean blockBasedInterlining() {
    return blockBasedInterlining;
  }

  public int maxInterlineDistance() {
    return maxInterlineDistance;
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
    return toStringAppend(ToStringBuilder.of(GtfsDefaultParameters.class)).toString();
  }

  protected ToStringBuilder toStringAppend(ToStringBuilder builder) {
    return builder
      .addBool("removeRepeatedStops", removeRepeatedStops, DEFAULT_REMOVE_REPEATED_STOPS)
      .addEnum(
        "stationTransferPreference",
        stationTransferPreference,
        StopTransferPriority.defaultValue()
      )
      .addBool(
        "discardMinTransferTimes",
        discardMinTransferTimes,
        DEFAULT_DISCARD_MIN_TRANSFER_TIMES
      )
      .addBool("blockBasedInterlining", blockBasedInterlining, DEFAULT_BLOCK_BASED_INTERLINING)
      .addNum("maxInterlineDistance", maxInterlineDistance, DEFAULT_MAX_INTERLINE_DISTANCE);
  }
}
