package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.model.RequiredFilterValues;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest.TimeAtStop;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripTimeOnDateRequestBuilder {

  private final List<TimeAtStop> timesAtStop;
  private FilterValues<FeedScopedId> agencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> routes = FilterValues.ofEmptyIsEverything("routes", List.of());
  private FilterValues<TransitMode> modes = FilterValues.ofEmptyIsEverything("modes", List.of());
  private Duration timeWindow = Duration.ofHours(2);
  private ArrivalDeparture arrivalDeparture = ArrivalDeparture.BOTH;
  private int numberOfDepartures = 10;

  TripTimeOnDateRequestBuilder(List<TimeAtStop> timesAtStops) {
    this.timesAtStop = timesAtStops;
  }

  public TripTimeOnDateRequestBuilder withAgencies(FilterValues<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripTimeOnDateRequestBuilder withRoutes(FilterValues<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripTimeOnDateRequestBuilder withModes(FilterValues<TransitMode> modes) {
    this.modes = modes;
    return this;
  }

  public TripTimeOnDateRequestBuilder withArrivalDeparture(ArrivalDeparture arrivalDeparture) {
    this.arrivalDeparture = arrivalDeparture;
    return this;
  }

  public TripTimeOnDateRequestBuilder withTimeWindow(Duration timeWindow) {
    this.timeWindow = timeWindow;
    return this;
  }

  public TripTimeOnDateRequestBuilder withNumberOfDepartures(int numberOfDepartures) {
    this.numberOfDepartures = numberOfDepartures;
    return this;
  }

  public TripTimeOnDateRequest build() {
    return new TripTimeOnDateRequest(
      timesAtStop,
      timeWindow,
      arrivalDeparture,
      numberOfDepartures,
      agencies,
      routes,
      modes
    );
  }
}
