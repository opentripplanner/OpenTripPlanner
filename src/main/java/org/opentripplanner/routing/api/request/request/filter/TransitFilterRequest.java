package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TransitFilterRequest implements Serializable, TransitFilter {

  public static class Builder {

    private List<SelectRequest> select = new ArrayList<>();
    private List<SelectRequest> not = new ArrayList<>();

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

  public TransitFilterRequest(Builder builder) {
    this.select = Collections.unmodifiableList(builder.select);
    this.not = Collections.unmodifiableList(builder.not);
  }

  private final List<SelectRequest> select;
  private final List<SelectRequest> not;

  public List<SelectRequest> select() {
    return select;
  }

  public List<SelectRequest> not() {
    return not;
  }

  public static Builder of() {
    return new Builder();
  }

  @Override
  public boolean isSubModePredicate() {
    for (var selectRequest : select) {
      if (selectRequest.transportModes() != null && selectRequest.transportModes().isSubMode()) {
        return true;
      }
    }

    for (var selectRequest : not) {
      if (selectRequest.transportModes() != null && selectRequest.transportModes().isSubMode()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matchRoute(Route route) {
    if (!select.isEmpty()) {
      if (select.stream().noneMatch(s -> s.matches(route))) {
        return false;
      }
    }

    if (!not.isEmpty()) {
      if (not.stream().anyMatch(s -> s.matches(route))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean matchTripTimes(TripTimes tripTimes) {
    if (!select.isEmpty()) {
      var anyMatch = false;
      for (var selectRequest : select) {
        if (selectRequest.matches(tripTimes)) {
          anyMatch = true;
          break;
        }
      }
      if (!anyMatch) {
        return false;
      }
    }

    if (!not.isEmpty()) {
      var anyMatch = false;
      for (var selectRequest : select) {
        if (selectRequest.matches(tripTimes)) {
          anyMatch = true;
          break;
        }
      }
      if (anyMatch) {
        return false;
      }
    }

    return true;
  }
}
