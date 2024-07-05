package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.time.Duration;
import org.opentripplanner.raptor.api.model.RaptorValueFormatter;

/**
 * Convert Raptor internal cost to OTP domain model cost, and back.
 * <p>
 * Inside Raptor the a cost unit is 1/100 of a "transit second" using {@code int} as type. In the
 * OTP internal domain the unit used for cost is one "transit second" with type {@code double}. Cost
 * in raptor is calculated using <b>int</b>s to spped up the calculations and to save memory.
 * <p>
 * The reason for using 1/100 of a second resolution is that we want a cost factor of {@code 0.99}
 * to win over a cost factor of {@code 1.00}.
 */
public final class RaptorCostConverter {

  private static final int NOT_SET = -1;
  private static final int PRECISION = 100;
  private static final long FACTOR_TO_MILLISECONDS = 1000 / PRECISION;
  private static final int HALF = PRECISION / 2;

  /* private constructor to prevent instantiation of utility class. */
  private RaptorCostConverter() {}

  /**
   * Convert Raptor internal cost to OTP domain model cost.
   */
  public static int toOtpDomainCost(int raptorCost) {
    if (raptorCost == NOT_SET) {
      return NOT_SET;
    }
    return (raptorCost + HALF) / PRECISION;
  }

  /**
   * Convert Raptor cost-factor to OTP domain model cost-factor.
   */
  public static double toOtpDomainFactor(int raptorFactor) {
    if (raptorFactor == NOT_SET) {
      return NOT_SET;
    }
    return raptorFactor / (double) PRECISION;
  }

  /**
   * Convert Raptor internal cost to a string with format $###.## (in seconds)
   */
  public static String toString(int raptorCost) {
    return RaptorValueFormatter.formatC1(raptorCost);
  }

  /**
   * Convert OTP domain model cost(seconds) to Raptor internal cost(centi-seconds).
   */
  public static int toRaptorCost(double domainCost) {
    return (int) Math.round(domainCost * PRECISION);
  }

  /**
   * Convert Raptor internal cost to a cost represented as a duration.
   */
  public static Duration raptorCostToDuration(int raptorCost) {
    return Duration.ofMillis(raptorCost * FACTOR_TO_MILLISECONDS);
  }

  /**
   * Convert OTP domain model cost to Raptor internal cost.
   */
  public static int toRaptorCost(int domainCost) {
    return domainCost * PRECISION;
  }

  /**
   * Convert an array of OTP domain values(doubles) into Raptor internal values {@code int}s.
   */
  public static int[] toRaptorCosts(double[] domainValues) {
    int[] raptorCost = new int[domainValues.length];
    for (int i = 0; i < domainValues.length; i++) {
      raptorCost[i] = toRaptorCost(domainValues[i]);
    }
    return raptorCost;
  }
}
