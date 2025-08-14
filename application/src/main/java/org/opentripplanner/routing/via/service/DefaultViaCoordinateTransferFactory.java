package org.opentripplanner.routing.via.service;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.nearbystops.NearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StraightLineNearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.StringUtils;

public class DefaultViaCoordinateTransferFactory implements ViaCoordinateTransferFactory {

  private final Graph graph;
  private final TransitService transitService;
  private final Duration radiusAsDuration;
  private final VertexLinker linker;

  @Inject
  public DefaultViaCoordinateTransferFactory(
    Graph graph,
    VertexLinker linker,
    TransitService transitService,
    Duration radiusAsDuration
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.linker = linker;

    // We divide the regular transfer radius by two, since we have two "transfers" after each
    // other here. This reduces the number of possible transfers to one fourth. The radius should
    // probably have its own configuration setting. There are no algorithmic reasons for using
    // the transfer radius here, any value can be used.
    this.radiusAsDuration = radiusAsDuration.dividedBy(2);
  }

  @Override
  public List<ViaCoordinateTransfer> createViaTransfers(
    RouteRequest request,
    String viaLabel,
    WgsCoordinate coordinate
  ) {
    DisposableEdgeCollection tempEdges = null;
    try {
      var nearbyStopFinder = createNearbyStopFinder(radiusAsDuration);
      var name = I18NString.of(
        (StringUtils.hasValue(viaLabel) ? viaLabel : "Via") + " " + coordinate
      );

      var viaVertex = new TemporaryStreetLocation(coordinate.asJtsCoordinate(), name);

      var m = mapTransferMode(request.journey().modes().transferMode);

      tempEdges = linker.linkVertexForRequest(
        viaVertex,
        new TraverseModeSet(m),
        LinkingDirection.BIDIRECTIONAL,
        (via, street) -> {
          var v = (TemporaryStreetLocation) via;
          return List.of(
            TemporaryFreeEdge.createTemporaryFreeEdge(street, v),
            TemporaryFreeEdge.createTemporaryFreeEdge(v, street)
          );
        }
      );

      var toStops = findNearbyStops(nearbyStopFinder, viaVertex, request, false);
      var fromStops = findNearbyStops(nearbyStopFinder, viaVertex, request, true);

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
    } catch (RuntimeException ex) {
      // This make sure we dispose the edges in case something goes wrong.
      if (tempEdges != null) {
        tempEdges.disposeEdges();
      }
      throw ex;
    }
  }

  private static TraverseMode mapTransferMode(StreetMode streetMode) {
    if (streetMode.transferAllowed()) {
      if (streetMode.includesBiking()) {
        return TraverseMode.BICYCLE;
      }
      if (streetMode.includesDriving()) {
        return TraverseMode.CAR;
      }
      if (streetMode.includesWalking()) {
        return TraverseMode.WALK;
      }
    }
    throw new IllegalStateException("Mode not allowed for transfer: " + streetMode);
  }

  /**
   * Factory method for creating a NearbyStopFinder. The linker will use streets if they are
   * available, or straight-line distance otherwise.
   */
  private NearbyStopFinder createNearbyStopFinder(Duration radiusAsDuration) {
    if (!graph.hasStreets) {
      return new StraightLineNearbyStopFinder(transitService, radiusAsDuration);
    } else {
      return new StreetNearbyStopFinder(radiusAsDuration, 0, null);
    }
  }

  /**
   * Find the nearest stops allowed for transfers.
   */
  private List<NearbyStop> findNearbyStops(
    NearbyStopFinder finder,
    TemporaryStreetLocation viaVertex,
    RouteRequest request,
    boolean reverseDirection
  ) {
    var transferRequest = request.journey().transfer();
    var r = finder.findNearbyStops(viaVertex, request, transferRequest, reverseDirection);
    return r.stream().filter(it -> !it.stop.transfersNotAllowed()).toList();
  }
}
