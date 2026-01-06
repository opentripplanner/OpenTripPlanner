package org.opentripplanner.service.streetdetails.model;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a level with a comparable 0-based number and name.
 */
public class Level implements Serializable {

  private final double level;
  private final String name;

  public Level(double level, String name) {
    this.level = level;
    this.name = Objects.requireNonNull(name);
  }

  public double level() {
    return this.level;
  }

  public String name() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != this.getClass()) {
      return false;
    }
    Level that = (Level) o;
    return this.level == that.level && Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addNum("level", level).addStr("name", name).toString();
  }
}
