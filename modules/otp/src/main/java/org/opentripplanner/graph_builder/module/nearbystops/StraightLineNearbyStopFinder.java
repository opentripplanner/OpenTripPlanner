package org.opentripplanner.graph_builder.module.nearbystops;

import java.time.Duration;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.DirectGraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.service.TransitService;

public class StraightLineNearbyStopFinder implements NearbyStopFinder {

  private final Duration durationLimit;
  private final DirectGraphFinder directGraphFinder;

  public StraightLineNearbyStopFinder(TransitService transitService, Duration durationLimit) {
    this.durationLimit = durationLimit;

    // We need to accommodate straight line distance (in meters) but when streets are present we
    // use an earliest arrival search, which optimizes on time. Ideally we'd specify in meters,
    // but we don't have much of a choice here. Use the default walking speed to convert.
    this.directGraphFinder = new DirectGraphFinder(transitService::findRegularStops);
  }

  /**
   * Find nearby stops using straight line distance.
   */
  @Override
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    return findNearbyStopsViaDirectTransfers(vertex);
  }

  private List<NearbyStop> findNearbyStopsViaDirectTransfers(Vertex vertex) {
    // It make sense for the directGraphFinder to use meters as a limit, so we convert first
    double limitMeters = durationLimit.toSeconds() * WalkPreferences.DEFAULT.speed();
    Coordinate c0 = vertex.getCoordinate();
    return directGraphFinder.findClosestStops(c0, limitMeters);
  }
}
