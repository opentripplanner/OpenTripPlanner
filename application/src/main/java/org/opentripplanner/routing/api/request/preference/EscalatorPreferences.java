package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.function.Consumer;

public class EscalatorPreferences implements Serializable {

  public static final EscalatorPreferences DEFAULT = new EscalatorPreferences();

  private final double horizontalSpeed;

  /* A quick internet search gives escalator speed range of 0.3-0.6 m/s and angle of 30 degrees.
   * Using the angle of 30 degrees and a speed of 0.5 m/s gives a horizontal component
   * of approx. 0.43 m/s */
  private static final double HORIZONTAL_SPEED = 0.45;

  private EscalatorPreferences() {
    this.horizontalSpeed = HORIZONTAL_SPEED;
  }

  private EscalatorPreferences(Builder builder) {
    this.horizontalSpeed = builder.horizontalSpeed;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public double horizontalSpeed() {
    return horizontalSpeed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EscalatorPreferences that = (EscalatorPreferences) o;
    return horizontalSpeed == that.horizontalSpeed;
  }

  public static class Builder {

    private final EscalatorPreferences original;
    private double horizontalSpeed;

    public Builder(EscalatorPreferences original) {
      this.original = original;
      this.horizontalSpeed = original.horizontalSpeed;
    }

    public EscalatorPreferences original() {
      return original;
    }

    public Builder withHorizontalSpeed(double horizontalSpeed) {
      this.horizontalSpeed = horizontalSpeed;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public EscalatorPreferences build() {
      var value = new EscalatorPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
