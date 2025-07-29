package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Contains information about the current battery or fuel status.
 * See the <a href="https://github.com/MobilityData/gbfs/blob/v3.0/gbfs.md#vehicle_statusjson">GBFS
 * vehicle_status specification</a> for more details.
 * <p>
 */
public final class RentalVehicleFuel {

  public static final RentalVehicleFuel DEFAULT = new RentalVehicleFuel();

  /**
   * Current fuel percentage, expressed from 0 to 1.
   */
  @Nullable
  private final Ratio percent;

  /**
   * Distance that the vehicle can travel with the current fuel.
   */
  @Nullable
  private final Distance range;

  private RentalVehicleFuel() {
    this.percent = null;
    this.range = null;
  }

  private RentalVehicleFuel(Builder builder) {
    this.percent = builder.percent;
    this.range = builder.range;
  }

  public RentalVehicleFuel(@Nullable Ratio fuelPercent, @Nullable Distance range) {
    this.percent = fuelPercent;
    this.range = range;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Nullable
  public Ratio percent() {
    return this.percent;
  }

  @Nullable
  public Distance range() {
    return range;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RentalVehicleFuel that = (RentalVehicleFuel) o;
    return Objects.equals(percent, that.percent) && Objects.equals(range, that.range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(percent, range);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RentalVehicleFuel.class)
      .addObj("percent", percent, DEFAULT.percent)
      .addObj("range", range, DEFAULT.range)
      .toString();
  }

  public static class Builder {

    private final RentalVehicleFuel original;
    private Ratio percent;
    private Distance range;

    private Builder(RentalVehicleFuel original) {
      this.original = original;
      this.percent = original.percent;
      this.range = original.range;
    }

    public Builder withPercent(@Nullable Ratio percent) {
      this.percent = percent;
      return this;
    }

    public Builder withRange(@Nullable Distance range) {
      this.range = range;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public RentalVehicleFuel build() {
      var value = new RentalVehicleFuel(this);
      return original.equals(value) ? original : value;
    }
  }
}
