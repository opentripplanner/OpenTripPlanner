package org.opentripplanner.ext.carpooling;

/**
 * The tuning knobs of the carpooling feature, collected in one place.
 * <p>
 * These are deployment-wide values, not part of the routing request. They are hard-coded to
 * {@link #DEFAULT} for now; the intention is to expose them in the {@code carpooling} section of
 * {@code router-config.json}, so this is the type that mapping would produce.
 *
 * @param boardCost cost added once to every carpool leg — direct, access and egress alike — for
 *        being picked up. It is the cost counterpart of {@code car.pickupTime}, which the carpool
 *        search uses as the stop duration at the pickup, and works like {@code flex.boardCost}.
 *        {@code car.pickupCost} is not used: its default of 120 is too small to keep a short
 *        carpool ride from beating walking, and it is shared with {@code CAR_PICKUP} routing. A
 *        cost of 1 is one second at reluctance 1.0, while walking costs {@code walkReluctance} per
 *        second, so the default of 2400 is ten minutes of walking at a {@code walkReluctance} of
 *        4.0: a carpool ride only wins when it saves the passenger roughly ten minutes of walking.
 */
public record CarpoolingParameters(int boardCost) {
  public static final CarpoolingParameters DEFAULT = new CarpoolingParameters(2400);

  public CarpoolingParameters {
    if (boardCost < 0) {
      throw new IllegalArgumentException("boardCost must not be negative");
    }
  }
}
