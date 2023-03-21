package org.opentripplanner.routing.api.request.preference;

/**
 * Relax a value by the given ratio and slack. The relaxed value
 * can be calculated using this function:
 * <pre>
 *   f(x) = ratio * x + slack
 * </pre>
 *
 * @param ratio the factor to multiply into the value, must be minimum 1.0  and max 4.0
 * @param slack the amount of slack to add to the value.
 */
public record Relax(double ratio, int slack) {
  public static final Relax NORMAL = new Relax(1.0, 0);

  public boolean isNormal() {
    return this.equals(NORMAL);
  }
}
