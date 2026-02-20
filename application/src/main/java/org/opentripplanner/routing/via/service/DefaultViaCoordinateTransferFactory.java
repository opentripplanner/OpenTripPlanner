package org.opentripplanner.routing.via.service;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.graph_builder.module.nearbystops.NearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StraightLineNearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.TransitServiceResolver;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.service.TransitService;

public class DefaultViaCoordinateTransferFactory implements ViaCoordinateTransferFactory {

  private final Graph graph;
  private final TransitService transitService;
  private final Duration radiusAsDuration;

  @Inject
  public DefaultViaCoordinateTransferFactory(
    Graph graph,
    TransitService transitService,
    Duration radiusAsDuration
  ) {
    this.graph = graph;
    this.transitService = transitService;

    // We divide the regular transfer radius by two, since we have two "transfers" after each
    // other here. This reduces the number of possible transfers to one fourth. The radius should
    // probably have its own configuration setting. There are no algorithmic reasons for using
    // the transfer radius here, any value can be used.
    this.radiusAsDuration = radiusAsDuration.dividedBy(2);
  }

  @Override
  public List<ViaCoordinateTransfer> createViaTransfers(
    RouteRequest request,
    Vertex location,
    WgsCoordinate coordinate
  ) {
    var nearbyStopFinder = createNearbyStopFinder(radiusAsDuration);

    var toStops = findNearbyStops(nearbyStopFinder, location, request, false);
    var fromStops = findNearbyStops(nearbyStopFinder, location, request, true);

    var transfers = new ArrayList<ViaCoordinateTransfer>();

    for (NearbyStop from : fromStops) {
      for (NearbyStop to : toStops) {
        transfers.add(
          new ViaCoordinateTransfer(
            coordinate,
            from.stop.getIndex(),
            to.stop.getIndex(),
            from.edges,
            to.edges,
            (int) (from.state.getElapsedTimeSeconds() + to.state.getElapsedTimeSeconds()),
            from.state.getWeight() + to.state.getWeight()
          )
        );
      }
    }
    return transfers;
  }

  /**
   * Factory method for creating a NearbyStopFinder. The linker will use streets if they are
   * available, or straight-line distance otherwise.
   */
  private NearbyStopFinder createNearbyStopFinder(Duration radiusAsDuration) {
    if (!graph.hasStreets) {
      return new StraightLineNearbyStopFinder(transitService, radiusAsDuration);
    } else {
      return StreetNearbyStopFinder.of(
        new TransitServiceResolver(transitService),
        radiusAsDuration,
        0
      ).build();
    }
  }

  /**
   * Find the nearest stops allowed for transfers.
   */
  private List<NearbyStop> findNearbyStops(
    NearbyStopFinder finder,
    Vertex viaVertex,
    RouteRequest request,
    boolean reverseDirection
  ) {
    var transferRequest = request.journey().transfer();
    var r = finder.findNearbyStops(viaVertex, request, transferRequest, reverseDirection);
    return r
      .stream()
      .filter(it -> !it.stop.transfersNotAllowed())
      .toList();
  }
}
