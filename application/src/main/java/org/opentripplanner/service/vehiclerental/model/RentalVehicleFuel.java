package org.opentripplanner.service.vehiclerental.model;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;

/**
 * Contains information about the current battery or fuel status.
 * See the <a href="https://github.com/MobilityData/gbfs/blob/v3.0/gbfs.md#vehicle_statusjson">GBFS
 * vehicle_status specification</a> for more details.
 */
public class RentalVehicleFuel {

  /**
   * Current fuel percentage, expressed from 0 to 1.
   * <p>
   * May be {@code null}.
   */
  @Nullable
  public final Ratio percent;

  /**
   * Distance that the vehicle can travel with the current charge or fuel.
   * <p>
   * May be {@code null}.
   */
  @Nullable
  public final Distance range;

  public RentalVehicleFuel(@Nullable Ratio fuelPercent, @Nullable Distance range) {
    this.percent = fuelPercent;
    this.range = range;
  }

  @Nullable
  public Double getPercent() {
    return this.percent != null ? this.percent.ratio : null;
  }

  @Nullable
  public Integer getRange() {
    return this.range != null ? this.range.toMeters() : null;
  }
}
