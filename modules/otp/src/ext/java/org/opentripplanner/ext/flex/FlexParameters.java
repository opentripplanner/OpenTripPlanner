package org.opentripplanner.ext.flex;

import java.time.Duration;

/**
 * Define parameters used to configure flex. For further documentation on these parameters, look
 * at the {@link org.opentripplanner.standalone.config.sandbox.FlexConfig} class which implements
 * this interface. The flex package does not use all parameters defined here. Some parameters are
 * passed into the street search(AStar) as part of a flex use-case. We keep them here for
 * completeness and simplicity (just one interface).
 */
public interface FlexParameters {
  /**
   * See {@link org.opentripplanner.standalone.config.sandbox.FlexConfig}
   */
  Duration maxTransferDuration();
  /**
   * See {@link org.opentripplanner.standalone.config.sandbox.FlexConfig}
   */
  Duration maxFlexTripDuration();
  /**
   * See {@link org.opentripplanner.standalone.config.sandbox.FlexConfig}
   */
  Duration maxAccessWalkDuration();
  /**
   * See {@link org.opentripplanner.standalone.config.sandbox.FlexConfig}
   */
  Duration maxEgressWalkDuration();

  /**
   * This defines the default values. This will be used by the OTP configuration and by tests,
   * avoid using this directly.
   */
  static FlexParameters defaultValues() {
    return new FlexParameters() {
      @Override
      public Duration maxTransferDuration() {
        return Duration.ofMinutes(5);
      }

      @Override
      public Duration maxFlexTripDuration() {
        return Duration.ofMinutes(45);
      }

      @Override
      public Duration maxAccessWalkDuration() {
        return Duration.ofMinutes(45);
      }

      @Override
      public Duration maxEgressWalkDuration() {
        return Duration.ofMinutes(45);
      }
    };
  }
}
