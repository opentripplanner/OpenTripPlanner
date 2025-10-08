package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A simple data structure that is used by the {@link TransitTestEnvironment} to create
 * trips, trips on date and patterns.
 */
public record TripInput(
  String id,
  List<StopCall> stops,
  //
  @Nullable Route route,
  // Null means that a default service date will be used
  @Nullable List<LocalDate> serviceDates,
  @Nullable I18NString headsign,
  @Nullable String tripOnServiceDateId
) {
  public static TripInputBuilder of(String id) {
    return new TripInputBuilder(id);
  }

  public static class TripInputBuilder {

    private final String id;
    private final List<StopCall> stops = new ArrayList<>();

    @Nullable
    private Route route;

    @Nullable
    private List<LocalDate> serviceDates = null;

    @Nullable
    private I18NString headsign;

    @Nullable
    private String tripOnServiceDateId;

    TripInputBuilder(String id) {
      this.id = id;
    }

    public TripInputBuilder addStop(RegularStop stopId, String arrivalTime, String departureTime) {
      this.stops.add(
          new StopCall(stopId, TimeUtils.time(arrivalTime), TimeUtils.time(departureTime))
        );
      return this;
    }

    public TripInputBuilder addStop(RegularStop stopId, String arrivalAndDeparture) {
      return addStop(stopId, arrivalAndDeparture, arrivalAndDeparture);
    }

    public TripInput build() {
      return new TripInput(id, stops, route, serviceDates, headsign, tripOnServiceDateId);
    }

    public TripInputBuilder withRoute(Route route) {
      this.route = route;
      return this;
    }

    public TripInputBuilder withHeadsign(I18NString headsign) {
      this.headsign = headsign;
      return this;
    }

    public TripInputBuilder withServiceDates(LocalDate... serviceDates) {
      var list = Arrays.stream(serviceDates).toList();
      ListUtils.requireAtLeastNElements(list, 1);
      this.serviceDates = list;
      return this;
    }

    public TripInputBuilder withWithTripOnServiceDate(String tripOnServiceDateId) {
      this.tripOnServiceDateId = tripOnServiceDateId;
      return this;
    }
  }

  record StopCall(StopLocation stop, int arrivalTime, int departureTime) {}
}
