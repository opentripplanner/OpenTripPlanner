package org.opentripplanner.ext.carpooling;

/**
 * The tuning knobs of the carpooling feature, collected in one place.
 * <p>
 * These are deployment-wide values, not part of the routing request. They are hard-coded to
 * {@link #DEFAULT} for now; the intention is to expose them in the {@code carpooling} section of
 * {@code router-config.json}, so this is the type that mapping would produce.
 *
 * @param boardCost cost added once to every carpool leg — direct, access and egress alike — for
 *        getting into the car. It is what keeps very short carpool rides from beating walking:
 *        walking costs {@code duration x walkReluctance}, so {@code 10 x 60 x walkReluctance}
 *        means a carpool ride only wins when it saves the passenger roughly ten minutes of
 *        walking. The default assumes a {@code walkReluctance} of 4.0.
 */
public record CarpoolingParameters(int boardCost) {
  public static final CarpoolingParameters DEFAULT = new CarpoolingParameters(2400);

  public CarpoolingParameters {
    if (boardCost < 0) {
      throw new IllegalArgumentException("boardCost must not be negative");
    }
  }
}
