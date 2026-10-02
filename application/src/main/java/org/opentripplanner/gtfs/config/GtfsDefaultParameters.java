package org.opentripplanner.gtfs.config;

import java.util.Objects;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Default GTFS feed configuration - can be set for all feeds in the build-config. The
 * {@link GtfsFeedParameters} contain the feed specific extensions - the extra info you must specify
 * for each feed.
 */
public sealed class GtfsDefaultParameters permits GtfsFeedParameters {

  private static final boolean DEFAULT_DISCARD_MIN_TRANSFER_TIMES = false;
  private static final boolean DEFAULT_BLOCK_BASED_INTERLINING = true;
  private static final int DEFAULT_MAX_INTERLINE_DISTANCE = 200;
  private static final boolean DEFAULT_IS_TAXI_DATA = false;

  public static final GtfsDefaultParameters DEFAULT = new GtfsDefaultParameters(
    StopTransferPriority.defaultValue(),
    DEFAULT_DISCARD_MIN_TRANSFER_TIMES,
    DEFAULT_BLOCK_BASED_INTERLINING,
    DEFAULT_MAX_INTERLINE_DISTANCE,
    DEFAULT_IS_TAXI_DATA
  );

  private final StopTransferPriority stationTransferPreference;
  private final boolean discardMinTransferTimes;
  private final boolean blockBasedInterlining;
  private final int maxInterlineDistance;
  private final boolean isTaxiData;

  protected GtfsDefaultParameters(
    StopTransferPriority stationTransferPreference,
    boolean discardMinTransferTimes,
    boolean blockBasedInterlining,
    int maxInterlineDistance,
    boolean isTaxiData
  ) {
    this.stationTransferPreference = Objects.requireNonNull(stationTransferPreference);
    this.discardMinTransferTimes = discardMinTransferTimes;
    this.blockBasedInterlining = blockBasedInterlining;
    this.maxInterlineDistance = maxInterlineDistance;
    this.isTaxiData = isTaxiData;
  }

  GtfsDefaultParameters(GtfsDefaultParametersBuilder builder) {
    this(
      builder.stationTransferPreference(),
      builder.discardMinTransferTimes(),
      builder.blockBasedInterlining(),
      builder.maxInterlineDistance(),
      builder.isTaxiData()
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

  public boolean isTaxiData() {
    return isTaxiData;
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
      .addNum("maxInterlineDistance", maxInterlineDistance, DEFAULT_MAX_INTERLINE_DISTANCE)
      .addBool("isTaxiData", isTaxiData, DEFAULT_IS_TAXI_DATA);
  }
}
