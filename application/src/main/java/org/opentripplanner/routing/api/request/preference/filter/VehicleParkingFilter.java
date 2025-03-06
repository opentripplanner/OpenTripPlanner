package org.opentripplanner.routing.api.request.preference.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A filter class that checks if parking faclities match certain conditions for
 * inclusion/exclusion or preference/unpreference.
 */
public class VehicleParkingFilter implements Serializable {

  private final VehicleParkingSelect[] not;
  private final VehicleParkingSelect[] select;

  public VehicleParkingFilter(
    Collection<VehicleParkingSelect> not,
    Collection<VehicleParkingSelect> select
  ) {
    this.not = makeFilter(not);
    this.select = makeFilter(select);
  }

  public VehicleParkingFilter(VehicleParkingSelect not, VehicleParkingSelect select) {
    this(List.of(not), List.of(select));
  }

  public List<VehicleParkingSelect> not() {
    return Arrays.asList(not);
  }

  public List<VehicleParkingSelect> select() {
    return Arrays.asList(select);
  }

  /**
   * Create a request with no conditions.
   */
  public static VehicleParkingFilter empty() {
    return new VehicleParkingFilter(List.of(), List.of());
  }

  /**
   * Checks if a parking facility matches the conditions defined in this filter.
   */
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
    return ToStringBuilder.of(this.getClass())
      .addCol("not", Arrays.asList(not))
      .addCol("select", Arrays.asList(select))
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleParkingFilter that = (VehicleParkingFilter) o;
    return (Arrays.equals(not, that.not) && Arrays.equals(select, that.select));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(not) + Arrays.hashCode(select);
  }

  private static VehicleParkingSelect[] makeFilter(Collection<VehicleParkingSelect> select) {
    return select.stream().filter(f -> !f.isEmpty()).toArray(VehicleParkingSelect[]::new);
  }
}
