package org.opentripplanner.updater.trip;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A simple data structure that is used by the {@link RealtimeTestEnvironment} to create
 * trips, trips on date and patterns.
 */
public record TripInput(
  String id,
  Route route,
  List<StopCall> stops,
  @Nullable I18NString headsign
) {
  public static TripInputBuilder of(String id) {
    return new TripInputBuilder(id);
  }

  /**
   * The ID of the route without the feed ID prefix.
   */
  public String routeId() {
    return route.getId().getId();
  }

  /**
   * The ID of the operator without the feed ID prefix.
   */
  public String operatorId() {
    return route.getOperator().getId().getId();
  }

  public static class TripInputBuilder {

    private final String id;
    private final List<StopCall> stops = new ArrayList<>();
    // can be made configurable if needed
    private Route route = TimetableRepositoryForTest.route("route-1")
      .withOperator(
        Operator.of(TimetableRepositoryForTest.id("operator-1")).withName("Operator 1").build()
      )
      .build();

    @Nullable
    private I18NString headsign;

    TripInputBuilder(String id) {
      this.id = id;
    }

    public TripInputBuilder addStop(RegularStop stopId, String arrivalTime, String departureTime) {
      this.stops.add(
          new StopCall(stopId, TimeUtils.time(arrivalTime), TimeUtils.time(departureTime))
        );
      return this;
    }

    public TripInput build() {
      return new TripInput(id, route, stops, headsign);
    }

    public TripInputBuilder withRoute(Route route) {
      this.route = route;
      return this;
    }

    public TripInputBuilder withHeadsign(I18NString headsign) {
      this.headsign = headsign;
      return this;
    }
  }

  record StopCall(StopLocation stop, int arrivalTime, int departureTime) {}
}
