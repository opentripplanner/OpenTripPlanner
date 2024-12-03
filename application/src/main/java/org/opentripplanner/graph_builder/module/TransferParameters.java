package org.opentripplanner.graph_builder.module;

import java.time.Duration;

public record TransferParameters(
  Duration maxTransferDuration,
  Duration carsAllowedStopMaxTransferDuration,
  boolean disableDefaultTransfers
) {
  public static final Duration DEFAULT_MAX_TRANSFER_DURATION = Duration.ZERO;
  public static final Duration DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION = Duration.ZERO;
  public static final boolean DEFAULT_DISABLE_DEFAULT_TRANSFERS = false;

  TransferParameters(Builder builder) {
    this(
      builder.maxTransferDuration,
      builder.carsAllowedStopMaxTransferDuration,
      builder.disableDefaultTransfers
    );
  }

  public static class Builder {

    private Duration maxTransferDuration;
    private Duration carsAllowedStopMaxTransferDuration;
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

    public Builder withDisableDefaultTransfers(boolean disableDefaultTransfers) {
      this.disableDefaultTransfers = disableDefaultTransfers;
      return this;
    }

    public TransferParameters build() {
      return new TransferParameters(this);
    }
  }
}
