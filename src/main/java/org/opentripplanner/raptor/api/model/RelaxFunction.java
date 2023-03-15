package org.opentripplanner.raptor.api.model;

import java.util.Objects;

/**
 * The relax funtion is used to relax a value by:
 * <pre>
 *   v' := v * ratio + slack
 * </pre>
 * The {@code ratio} is rounded of to the closest 1/16. This is done for
 * performance reasons since we then can use shift-right 4 bit to divide by 16.
 */
public final class RelaxFunction {

  private final int ratioOf16s;
  private final int slack;

  public RelaxFunction(double ratio, int slack) {
    this.ratioOf16s = (int) Math.round(ratio * 16.0);
    this.slack = slack;
  }

  public int relax(int value) {
    return ((value * ratioOf16s) >> 4) + slack;
  }

  @Override
  public String toString() {
    return "f()=" + ratioOf16s + "/16 * v + " + slack;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RelaxFunction that = (RelaxFunction) o;
    return ratioOf16s == that.ratioOf16s && slack == that.slack;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ratioOf16s, slack);
  }
}
