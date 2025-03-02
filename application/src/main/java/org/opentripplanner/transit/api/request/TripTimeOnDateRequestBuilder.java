package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TripTimeOnDateRequestBuilder {

  private final List<StopLocation> stopLocations;
  private FilterValues<FeedScopedId> agencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> routes = FilterValues.ofEmptyIsEverything("routes", List.of());
  private FilterValues<TransitMode> modes = FilterValues.ofEmptyIsEverything("modes", List.of());
  private Duration timeWindow = Duration.ofHours(2);
  private ArrivalDeparture arrivalDeparture = ArrivalDeparture.BOTH;
  private int numberOfDepartures = 10;
  private Instant time;

  TripTimeOnDateRequestBuilder(List<StopLocation> timesAtStops) {
    this.stopLocations = timesAtStops;
  }

  public TripTimeOnDateRequestBuilder withTime(Instant time) {
    this.time = time;
    return this;
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
      stopLocations,
      time,
      timeWindow,
      arrivalDeparture,
      numberOfDepartures,
      agencies,
      routes,
      modes
    );
  }
}
