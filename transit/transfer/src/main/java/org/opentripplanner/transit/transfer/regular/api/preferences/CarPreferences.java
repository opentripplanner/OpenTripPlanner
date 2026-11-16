package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences of the car transfers: the reluctance. The speed of a car is the speed of the
 * street, and there are no stairs. The values are normalized(rounded) so the class can used as a
 * cache key.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class CarPreferences extends AbstractUserPreferences<CarPreferences> {

  public static final CarPreferences DEFAULT = new CarPreferences();

  private CarPreferences() {
    super(Reluctance.of(2.0));
  }

  private CarPreferences(Builder builder) {
    super(builder);
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public TransferProfileType type() {
    return TransferProfileType.CAR;
  }

  @Override
  CarPreferences defaultInstance() {
    return DEFAULT;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CarPreferences)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return super.appendToString(ToStringBuilder.of(CarPreferences.class)).toString();
  }

  public static class Builder extends AbstractUserPreferences.Builder<CarPreferences, Builder> {

    public Builder(CarPreferences original) {
      super(original);
    }

    @Override
    CarPreferences doBuild() {
      return new CarPreferences(this);
    }
  }
}
