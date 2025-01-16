package org.opentripplanner.graph_builder.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.nearbystops.NearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.PatternConsideringNearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StraightLineNearbyStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.logging.ProgressTracker;
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
  private final TimetableRepository timetableRepository;
  private final DataImportIssueStore issueStore;

  public DirectTransferGenerator(
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    Duration radiusByDuration,
    List<RouteRequest> transferRequests
  ) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
    this.radiusByDuration = radiusByDuration;
    this.transferRequests = transferRequests;
  }

  @Override
  public void buildGraph() {
    /* Initialize transit model index which is needed by the nearby stop finder. */
    timetableRepository.index();

    /* The linker will use streets if they are available, or straight-line distance otherwise. */
    NearbyStopFinder nearbyStopFinder = createNearbyStopFinder();

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

    List<RouteRequest> flexTransferRequests = new ArrayList<>();
    // Flex transfer requests only use the WALK mode.
    if (OTPFeature.FlexRouting.isOn()) {
      flexTransferRequests.addAll(
        transferRequests
          .stream()
          .filter(transferProfile -> transferProfile.journey().transfer().mode() == StreetMode.WALK)
          .toList()
      );
    }

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

        // Calculate default transfers.
        for (RouteRequest transferProfile : transferRequests) {
          StreetMode mode = transferProfile.journey().transfer().mode();
          for (NearbyStop sd : nearbyStopFinder.findNearbyStops(
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
            TransferKey transferKey = new TransferKey(stop, sd.stop, sd.edges);
            PathTransfer pathTransfer = distinctTransfers.get(transferKey);
            if (pathTransfer == null) {
              // If the PathTransfer can't be found, it is created.
              distinctTransfers.put(
                transferKey,
                new PathTransfer(stop, sd.stop, sd.distance, sd.edges, EnumSet.of(mode))
              );
            } else {
              // If the PathTransfer is found, a new PathTransfer with the added mode is created.
              distinctTransfers.put(transferKey, pathTransfer.withAddedMode(mode));
            }
          }
        }
        // Calculate flex transfers if flex routing is enabled.
        for (RouteRequest transferProfile : flexTransferRequests) {
          // Flex transfer requests only use the WALK mode.
          StreetMode mode = StreetMode.WALK;
          // This code is for finding transfers from AreaStops to Stops, transfers
          // from Stops to AreaStops and between Stops are already covered above.
          for (NearbyStop sd : nearbyStopFinder.findNearbyStops(
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
            // The TransferKey and PathTransfer are created differently for flex routing.
            TransferKey transferKey = new TransferKey(sd.stop, stop, sd.edges);
            PathTransfer pathTransfer = distinctTransfers.get(transferKey);
            if (pathTransfer == null) {
              // If the PathTransfer can't be found, it is created.
              distinctTransfers.put(
                transferKey,
                new PathTransfer(sd.stop, stop, sd.distance, sd.edges, EnumSet.of(mode))
              );
            } else {
              // If the PathTransfer is found, a new PathTransfer with the added mode is created.
              distinctTransfers.put(transferKey, pathTransfer.withAddedMode(mode));
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

    timetableRepository.addAllTransfersByStops(transfersByStop);

    LOG.info(progress.completeMessage());
    LOG.info(
      "Done connecting stops to one another. Created a total of {} transfers from {} stops.",
      nTransfersTotal,
      nLinkedStops
    );
    transferRequests
      .stream()
      .map(transferProfile -> transferProfile.journey().transfer().mode())
      .distinct()
      .forEach(mode ->
        LOG.info(
          "Created {} transfers for mode {}.",
          timetableRepository.findTransfers(mode).size(),
          mode
        )
      );
  }

  /**
   * Factory method for creating a NearbyStopFinder. Will create different finders depending on
   * whether the graph has a street network and if ConsiderPatternsForDirectTransfers feature is
   * enabled.
   */
  private NearbyStopFinder createNearbyStopFinder() {
    var transitService = new DefaultTransitService(timetableRepository);
    NearbyStopFinder finder;
    if (!graph.hasStreets) {
      LOG.info(
        "Creating direct transfer edges between stops using straight line distance (not streets)..."
      );
      finder = new StraightLineNearbyStopFinder(transitService, radiusByDuration);
    } else {
      LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
      finder = new StreetNearbyStopFinder(radiusByDuration, 0, null);
    }

    if (OTPFeature.ConsiderPatternsForDirectTransfers.isOn()) {
      return new PatternConsideringNearbyStopFinder(transitService, finder);
    } else {
      return finder;
    }
  }

  private record TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {}
}
