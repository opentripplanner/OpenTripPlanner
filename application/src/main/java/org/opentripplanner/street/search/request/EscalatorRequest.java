package org.opentripplanner.street.search.request;

import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class EscalatorRequest {

  public static final EscalatorRequest DEFAULT = new EscalatorRequest();

  private final double reluctance;
  private final double speed;

  /* Using the angle of 30 degrees and a speed of 0.5 m/s gives a horizontal component
   * of approx. 0.43 m/s. This is typical of short escalators like those in shopping
   * malls. */
  private static final double HORIZONTAL_SPEED = 0.45;

  private EscalatorRequest() {
    this.reluctance = 1.5;
    this.speed = HORIZONTAL_SPEED;
  }

  private EscalatorRequest(Builder builder) {
    reluctance = builder.reluctance;
    speed = builder.speed;
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public double reluctance() {
    return reluctance;
  }

  public double speed() {
    return speed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EscalatorRequest that = (EscalatorRequest) o;
    return (doubleEquals(that.reluctance, reluctance) && doubleEquals(that.speed, speed));
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(EscalatorRequest.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .toString();
  }

  public static class Builder {

    private final EscalatorRequest original;
    private double reluctance;
    private double speed;

    public Builder(EscalatorRequest original) {
      this.original = original;
      this.reluctance = original.reluctance;
      this.speed = original.speed;
    }

    public Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public EscalatorRequest build() {
      var newObj = new EscalatorRequest(this);
      return original.equals(newObj) ? original : newObj;
    }
  }
}
