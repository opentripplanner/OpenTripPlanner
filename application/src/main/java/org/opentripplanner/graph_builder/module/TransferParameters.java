package org.opentripplanner.graph_builder.module;

import java.time.Duration;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Mode-specific parameters for transfers.
 */
public record TransferParameters(
  @Nullable Duration maxTransferDuration,
  @Nullable Duration carsAllowedStopMaxTransferDuration,
  @Nullable Duration bikesAllowedStopMaxTransferDuration,
  boolean disableDefaultTransfers
) {
  public static final Duration DEFAULT_MAX_TRANSFER_DURATION = null;
  public static final Duration DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION = null;
  public static final Duration DEFAULT_BIKES_ALLOWED_STOP_MAX_TRANSFER_DURATION = null;
  public static final boolean DEFAULT_DISABLE_DEFAULT_TRANSFERS = false;

  TransferParameters(Builder builder) {
    this(
      builder.maxTransferDuration,
      builder.carsAllowedStopMaxTransferDuration,
      builder.bikesAllowedStopMaxTransferDuration,
      builder.disableDefaultTransfers
    );
  }

  public String toString() {
    return ToStringBuilder.of(getClass())
      .addDuration("maxTransferDuration", maxTransferDuration)
      .addDuration("carsAllowedStopMaxTransferDuration", carsAllowedStopMaxTransferDuration)
      .addDuration("bikesAllowedStopMaxTransferDuration", bikesAllowedStopMaxTransferDuration)
      .addBool("disableDefaultTransfers", disableDefaultTransfers)
      .toString();
  }

  public static class Builder {

    private Duration maxTransferDuration;
    private Duration carsAllowedStopMaxTransferDuration;
    private Duration bikesAllowedStopMaxTransferDuration;
    private boolean disableDefaultTransfers;

    public Builder() {
      this.maxTransferDuration = DEFAULT_MAX_TRANSFER_DURATION;
      this.carsAllowedStopMaxTransferDuration = DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION;
      this.disableDefaultTransfers = DEFAULT_DISABLE_DEFAULT_TRANSFERS;
    }

    public Builder withMaxTransferDuration(Duration maxTransferDuration) {
      this.maxTransferDuration = maxTransferDuration;
      return this;
    }

    public Builder withCarsAllowedStopMaxTransferDuration(
      Duration carsAllowedStopMaxTransferDuration
    ) {
      this.carsAllowedStopMaxTransferDuration = carsAllowedStopMaxTransferDuration;
      return this;
    }

    public Builder withBikesAllowedStopMaxTransferDuration(
      Duration bikesAllowedStopMaxTransferDuration
    ) {
      this.bikesAllowedStopMaxTransferDuration = bikesAllowedStopMaxTransferDuration;
      return this;
    }

    public Builder withDisableDefaultTransfers(boolean disableDefaultTransfers) {
      this.disableDefaultTransfers = disableDefaultTransfers;
      return this;
    }

    public TransferParameters build() {
      return new TransferParameters(this);
    }
  }
}
