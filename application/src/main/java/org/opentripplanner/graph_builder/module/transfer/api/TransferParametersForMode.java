package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Mode-specific parameters for transfers.
 */
public record TransferParametersForMode(
  @Nullable Duration maxDuration,
  @Nullable Duration carsAllowedStopMaxDuration,
  @Nullable Duration bikesAllowedStopMaxDuration,
  boolean disableDefaultTransfers
) {
  public static final Duration DEFAULT_MAX_DURATION = null;
  public static final Duration DEFAULT_CARS_ALLOWED_STOP_MAX_DURATION = null;
  public static final Duration DEFAULT_BIKES_ALLOWED_STOP_MAX_DURATION = null;
  public static final boolean DEFAULT_DISABLE_DEFAULT_TRANSFERS = false;

  TransferParametersForMode(Builder builder) {
    this(
      builder.maxDuration,
      builder.carsAllowedStopMaxDuration,
      builder.bikesAllowedStopMaxDuration,
      builder.disableDefaultTransfers
    );
  }

  public String toString() {
    return ToStringBuilder.of(getClass())
      .addDuration("maxDuration", maxDuration)
      .addDuration("carsAllowedStopMaxDuration", carsAllowedStopMaxDuration)
      .addDuration("bikesAllowedStopMaxDuration", bikesAllowedStopMaxDuration)
      .addBool("disableDefaultTransfers", disableDefaultTransfers)
      .toString();
  }

  public static class Builder {

    private Duration maxDuration;
    private Duration carsAllowedStopMaxDuration;
    private Duration bikesAllowedStopMaxDuration;
    private boolean disableDefaultTransfers;

    public Builder() {
      this.maxDuration = DEFAULT_MAX_DURATION;
      this.carsAllowedStopMaxDuration = DEFAULT_CARS_ALLOWED_STOP_MAX_DURATION;
      this.disableDefaultTransfers = DEFAULT_DISABLE_DEFAULT_TRANSFERS;
    }

    public Builder withMaxDuration(Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
    }

    public Builder withCarsAllowedStopMaxDuration(Duration carsAllowedStopMaxDuration) {
      this.carsAllowedStopMaxDuration = carsAllowedStopMaxDuration;
      return this;
    }

    public Builder withBikesAllowedStopMaxDuration(Duration bikesAllowedStopMaxDuration) {
      this.bikesAllowedStopMaxDuration = bikesAllowedStopMaxDuration;
      return this;
    }

    public Builder withDisableDefaultTransfers(boolean disableDefaultTransfers) {
      this.disableDefaultTransfers = disableDefaultTransfers;
      return this;
    }

    public TransferParametersForMode build() {
      return new TransferParametersForMode(this);
    }
  }
}
