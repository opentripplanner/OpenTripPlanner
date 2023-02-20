package org.opentripplanner.routing.api.request.request.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class VehicleParkingFilterRequest {

  private final VehicleParkingFilter[] not;
  private final VehicleParkingFilter[] select;

  public VehicleParkingFilterRequest(
    Collection<VehicleParkingFilter> not,
    Collection<VehicleParkingFilter> select
  ) {
    this.not = not.toArray(new VehicleParkingFilter[0]);
    this.select = select.toArray(new VehicleParkingFilter[0]);
  }

  public boolean matches(VehicleParking p) {
    for (var n : not) {
      if (n.matches(p)) {
        return false;
      }
    }
    // not doesn't match and no selects means it matches
    if (select.length == 0) {
      return true;
    }
    for (var s : select) {
      if (s.matches(p)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addCol("not", Arrays.asList(not))
      .addCol("select", Arrays.asList(select))
      .toString();
  }

  public static VehicleParkingFilterRequest empty() {
    return new VehicleParkingFilterRequest(List.of(), List.of());
  }

  public static VehicleParkingFilterRequest select(Collection<VehicleParkingFilter> select) {
    return new VehicleParkingFilterRequest(List.of(), select);
  }
}
