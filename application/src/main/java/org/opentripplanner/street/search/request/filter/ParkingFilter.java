package org.opentripplanner.street.search.request.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A filter class that checks if parking facilities match certain conditions for
 * inclusion/exclusion or preference/unpreference.
 */
public class ParkingFilter {

  private final ParkingSelect[] not;
  private final ParkingSelect[] select;

  public ParkingFilter(Collection<ParkingSelect> not, Collection<ParkingSelect> select) {
    this.not = makeFilter(not);
    this.select = makeFilter(select);
  }

  public ParkingFilter(ParkingSelect not, ParkingSelect select) {
    this(List.of(not), List.of(select));
  }

  public List<ParkingSelect> not() {
    return Arrays.asList(not);
  }

  public List<ParkingSelect> select() {
    return Arrays.asList(select);
  }

  /**
   * Create a request with no conditions.
   */
  public static ParkingFilter empty() {
    return new ParkingFilter(List.of(), List.of());
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
    ParkingFilter that = (ParkingFilter) o;
    return (Arrays.equals(not, that.not) && Arrays.equals(select, that.select));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(not) + Arrays.hashCode(select);
  }

  private static ParkingSelect[] makeFilter(Collection<ParkingSelect> select) {
    return select.stream().filter(f -> !f.isEmpty()).toArray(ParkingSelect[]::new);
  }
}
