package org.opentripplanner.ext.emissions.model;

import java.util.function.Consumer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public record EmissionParameters(EmissionViechleParameters car) {
  public static final EmissionParameters DEFAULT = new EmissionParameters(
    EmissionViechleParameters.CAR_DEFAULTS
  );

  public static EmissionParameters.Builder of() {
    return DEFAULT.copyOf();
  }

  private EmissionParameters.Builder copyOf() {
    return new Builder(DEFAULT);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(EmissionParameters.class)
      .addObj("car", car, EmissionViechleParameters.CAR_DEFAULTS)
      .toString();
  }

  public static class Builder {

    private EmissionViechleParameters car;
    private EmissionParameters origin;

    public Builder(EmissionParameters origin) {
      this.origin = origin;
      this.car = origin.car;
    }

    public Builder withCar(Consumer<EmissionViechleParameters.Builder> body) {
      var b = car.copyOf();
      body.accept(b);
      this.car = b.build();
      return this;
    }

    public EmissionParameters build() {
      var candidate = new EmissionParameters(car);
      return origin.equals(candidate) ? origin : candidate;
    }
  }
}
