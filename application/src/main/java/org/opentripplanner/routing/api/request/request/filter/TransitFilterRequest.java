package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class TransitFilterRequest implements Serializable, TransitFilter {

  /**
   * This is stored as an array, as they are iterated over for each trip when filtering transit
   * data. Iterator creation is relatively expensive compared to iterating over a short array.
   */
  private final SelectRequest[] select;

  /**
   * {@link TransitFilterRequest#select}
   */
  private final SelectRequest[] not;

  public TransitFilterRequest(Builder builder) {
    this.select = builder.select.toArray(SelectRequest[]::new);
    this.not = builder.not.toArray(SelectRequest[]::new);
  }

  public List<SelectRequest> select() {
    return Collections.unmodifiableList(Arrays.asList(select));
  }

  public List<SelectRequest> not() {
    return Collections.unmodifiableList(Arrays.asList(not));
  }

  public static Builder of() {
    return new Builder();
  }

  @Override
  public boolean isSubModePredicate() {
    for (var selectRequest : select) {
      if (
        selectRequest.transportModeFilter() != null &&
        selectRequest.transportModeFilter().isSubMode()
      ) {
        return true;
      }
    }

    for (var selectRequest : not) {
      if (
        selectRequest.transportModeFilter() != null &&
        selectRequest.transportModeFilter().isSubMode()
      ) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matchTripPattern(TripPattern tripPattern) {
    if (select.length != 0) {
      var anyMatch = false;
      for (SelectRequest s : select) {
        if (s.matches(tripPattern)) {
          anyMatch = true;
          break;
        }
      }
      if (!anyMatch) {
        return false;
      }
    }

    for (SelectRequest s : not) {
      if (s.matches(tripPattern)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean matchTripTimes(TripTimes tripTimes) {
    if (select.length != 0) {
      var anyMatch = false;
      for (var selectRequest : select) {
        if (selectRequest.matchesSelect(tripTimes)) {
          anyMatch = true;
          break;
        }
      }
      if (!anyMatch) {
        return false;
      }
    }

    for (SelectRequest s : not) {
      if (s.matchesNot(tripTimes)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    if (select.length == 0 && not.length == 0) {
      return "ALL";
    }
    return ToStringBuilder.ofEmbeddedType()
      .addCol("select", Arrays.asList(select))
      .addCol("not", Arrays.asList(not))
      .toString();
  }

  public static class Builder {

    private final List<SelectRequest> select = new ArrayList<>();
    private final List<SelectRequest> not = new ArrayList<>();

    public Builder addSelect(SelectRequest selectRequest) {
      this.select.add(selectRequest);

      return this;
    }

    public Builder addNot(SelectRequest selectRequest) {
      this.not.add(selectRequest);

      return this;
    }

    public TransitFilterRequest build() {
      return new TransitFilterRequest(this);
    }
  }
}
