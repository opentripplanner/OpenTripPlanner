package org.opentripplanner.transit.model.site;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object for stop level. This is currently only supported by the GTFS import.
 */
public class StopLevel implements Serializable {

  private final String name;
  private final double index;

  /** No builder for 2 args value object - simple to construct */
  public StopLevel(String name, double index) {
    this.name = name;
    this.index = index;
  }

  public String getName() {
    return name;
  }

  public double getIndex() {
    return index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, index);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StopLevel other = (StopLevel) o;
    return Math.abs(other.index - index) < 0.001 && name.equals(other.name);
  }

  @Override
  public String toString() {
    return name + "(" + index + ")";
  }
}
