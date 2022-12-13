package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TransitFilterRequest implements Cloneable, Serializable, TransitFilter {

  private List<SelectRequest> select = new ArrayList<>();
  private List<SelectRequest> not = new ArrayList<>();

  public List<SelectRequest> select() {
    return select;
  }

  public void setSelect(List<SelectRequest> select) {
    this.select = select;
  }

  public List<SelectRequest> not() {
    return not;
  }

  public void setNot(List<SelectRequest> not) {
    this.not = not;
  }

  @Override
  public String toString() {
    return "FilterRequest{" + "include=" + select + ", exclude=" + not + '}';
  }

  @Override
  public TransitFilterRequest clone() {
    try {
      var clone = (TransitFilterRequest) super.clone();

      clone.select =
        this.select.stream()
          .map(f -> {
            try {
              return f.clone();
            } catch (CloneNotSupportedException e) {
              throw new RuntimeException(e);
            }
          })
          .collect(Collectors.toList());

      clone.not =
        this.not.stream()
          .map(f -> {
            try {
              return f.clone();
            } catch (CloneNotSupportedException e) {
              throw new RuntimeException(e);
            }
          })
          .collect(Collectors.toList());

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
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
