package org.opentripplanner.graph_builder.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilderModule} module that links up the
 * stops of a transit network among themselves. This is necessary for routing in long-distance
 * mode.
 * <p>
 * It will use the street network if OSM data has already been loaded into the graph. Otherwise it
 * will use straight-line distance between stops.
 */
public class DirectTransferGenerator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DirectTransferGenerator.class);

  private final Duration radiusByDuration;

  private final List<RouteRequest> transferRequests;
  private final Graph graph;
  private final TransitModel transitModel;
  private final DataImportIssueStore issueStore;

  public DirectTransferGenerator(
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore,
    Duration radiusByDuration,
    List<RouteRequest> transferRequests
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.issueStore = issueStore;
    this.radiusByDuration = radiusByDuration;
    this.transferRequests = transferRequests;
  }

  @Override
  public void buildGraph() {
    /* Initialize transit model index which is needed by the nearby stop finder. */
    if (transitModel.getTransitModelIndex() == null) {
      transitModel.index();
    }

    /* The linker will use streets if they are available, or straight-line distance otherwise. */
    NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
      new DefaultTransitService(transitModel),
      radiusByDuration,
      0,
      null,
      graph.hasStreets
    );
    if (nearbyStopFinder.useStreets) {
      LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
    } else {
      LOG.info(
        "Creating direct transfer edges between stops using straight line distance (not streets)..."
      );
    }

    List<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);

    ProgressTracker progress = ProgressTracker.track(
      "Create transfer edges for stops",
      1000,
      stops.size()
    );

    AtomicInteger nTransfersTotal = new AtomicInteger();
    AtomicInteger nLinkedStops = new AtomicInteger();

    // This is a synchronizedMultimap so that a parallel stream may be used to insert elements.
    var transfersByStop = Multimaps.<StopLocation, PathTransfer>synchronizedMultimap(
      HashMultimap.create()
    );

    stops
      .stream()
      .parallel()
      .forEach(ts0 -> {
        /* Make transfers to each nearby stop that has lowest weight on some trip pattern.
         * Use map based on the list of edges, so that only distinct transfers are stored. */
        Map<TransferKey, PathTransfer> distinctTransfers = new HashMap<>();
        RegularStop stop = ts0.getStop();

        if (stop.transfersNotAllowed()) {
          return;
        }

        LOG.debug("Linking stop '{}' {}", stop, ts0);

        for (RouteRequest transferProfile : transferRequests) {
          for (NearbyStop sd : findNearbyStops(
            nearbyStopFinder,
            ts0,
            transferProfile,
            transferProfile.journey().transfer(),
            false
          )) {
            // Skip the origin stop, loop transfers are not needed.
            if (sd.stop == stop) {
              continue;
            }
            if (sd.stop.transfersNotAllowed()) {
              continue;
            }
            distinctTransfers.put(
              new TransferKey(stop, sd.stop, sd.edges),
              new PathTransfer(stop, sd.stop, sd.distance, sd.edges)
            );
          }
          if (OTPFeature.FlexRouting.isOn()) {
            // This code is for finding transfers from AreaStops to Stops, transfers
            // from Stops to AreaStops and between Stops are already covered above.
            for (NearbyStop sd : findNearbyStops(
              nearbyStopFinder,
              ts0,
              transferProfile,
              transferProfile.journey().transfer(),
              true
            )) {
              // Skip the origin stop, loop transfers are not needed.
              if (sd.stop == stop) {
                continue;
              }
              if (sd.stop instanceof RegularStop) {
                continue;
              }
              distinctTransfers.put(
                new TransferKey(sd.stop, stop, sd.edges),
                new PathTransfer(sd.stop, stop, sd.distance, sd.edges)
              );
            }
          }
        }

        LOG.debug(
          "Linked stop {} with {} transfers to stops with different patterns.",
          stop,
          distinctTransfers.size()
        );
        if (distinctTransfers.isEmpty()) {
          issueStore.add(new StopNotLinkedForTransfers(ts0));
        } else {
          distinctTransfers
            .values()
            .forEach(transfer -> transfersByStop.put(transfer.from, transfer));
          nLinkedStops.incrementAndGet();
          nTransfersTotal.addAndGet(distinctTransfers.size());
        }

        //Keep lambda! A method-ref would causes incorrect class and line number to be logged
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      });

    transitModel.addAllTransfersByStops(transfersByStop);

    LOG.info(progress.completeMessage());
    LOG.info(
      "Done connecting stops to one another. Created a total of {} transfers from {} stops.",
      nTransfersTotal,
      nLinkedStops
    );
  }

  private static Iterable<NearbyStop> findNearbyStops(
    NearbyStopFinder nearbyStopFinder,
    Vertex vertex,
    RouteRequest request,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    return OTPFeature.ConsiderPatternsForDirectTransfers.isOn()
      ? nearbyStopFinder.findNearbyStopsConsideringPatterns(
        vertex,
        request,
        streetRequest,
        reverseDirection
      )
      : nearbyStopFinder.findNearbyStops(vertex, request, streetRequest, reverseDirection);
  }

  private record TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {}
}
