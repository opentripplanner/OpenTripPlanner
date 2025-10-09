package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A simple data structure that is used by the {@link TransitTestEnvironment} to create
 * trips, trips on date and patterns.
 */
public record TripInput(
  String id,
  List<StopCall> stops,
  // Null means that the default route will be used
  @Nullable Route route,
  // Null means that the default service date will be used
  @Nullable List<LocalDate> serviceDates,
  @Nullable I18NString headsign,
  @Nullable String tripOnServiceDateId
) {
  public static TripInputBuilder of(String id) {
    return new TripInputBuilder(id);
  }

  public List<StopLocation> stopLocations() {
    return stops().stream().map(TripInput.StopCall::stop).toList();
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
          new StopCall(
            stopId,
            stops.size(),
            TimeUtils.time(arrivalTime),
            TimeUtils.time(departureTime)
          )
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

  record StopCall(StopLocation stop, int stopSequence, int arrivalTime, int departureTime) {
    public StopTime toStopTime(Trip trip) {
      var st = new StopTime();
      st.setTrip(trip);
      st.setStopSequence(stopSequence);
      st.setStop(stop);
      st.setArrivalTime(arrivalTime);
      st.setDepartureTime(departureTime);
      return st;
    }
  }
}
