package org.opentripplanner.transit.raptor.api.transit;

/**
 * Convert Raptor internal cost to OTP domain model cost, and back.
 * <p>
 * Inside Raptor the a cost unit is 1/100 of a "transit second" using {@code int} as type. In the
 * OTP internal domain the unit used for cost is one "transit second" with type {@code double}.
 * Cost in raptor is calculated using <b>int</b>s to spped up the calculations and to save memory.
 * <p>
 * The reason for using 1/100 of a second resolution is that we want a cost factor of {@code 0.99}
 * to win over a cost factor of {@code 1.00}.
 */
public final class RaptorCostConverter {

  private static final int PRECISION = 100;

  /* private constructor to prevent instantiation of utility class. */
  private RaptorCostConverter() {}

  /**
   * Convert Raptor internal cost to OTP domain model cost.
   */
  public static int toOtpDomainCost(int raptorCost) {
    return (int) Math.round((double) raptorCost / PRECISION);
  }

  /**
   * Convert OTP domain model cost to Raptor internal cost.
   */
  public static int toRaptorCost(double domainCost) {
    return (int) (domainCost * PRECISION);
  }
}
