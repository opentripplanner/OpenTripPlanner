package org.opentripplanner.graph_builder.module.transfer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.transfer.api.RegularTransferParameters;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.PatternConsideringNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilderModule} module that generates the transfers used by FLEX routing: walking
 * connectors between {@link RegularStop}s and the flex {@code AreaStop}/{@code GroupStop}s, in
 * both directions - stop-to-flex-stop (used for flex egress) and flex-stop-to-stop (used for flex
 * access).
 * <p>
 * It will use the street network if OSM data has already been loaded into the graph. Otherwise it
 * will use straight-line distance between stops.
 */
public class DirectTransferGenerator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DirectTransferGenerator.class);

  private static final int NO_STOP_COUNT_LIMIT = 0;

  private final Duration maxTransferDuration;
  private final List<RouteRequest> transferRequests;
  private final Graph graph;
  private final TransitRepository transitRepository;
  private final TransferRepository transferRepository;

  /**
   * Constructor used in tests.
   */
  public DirectTransferGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    Duration maxTransferDuration,
    List<RouteRequest> transferRequests
  ) {
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.maxTransferDuration = maxTransferDuration;
    this.transferRequests = transferRequests;
    this.transferRepository = transferRepository;
  }

  public DirectTransferGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    RegularTransferParameters parameters
  ) {
    this(
      graph,
      transitRepository,
      transferRepository,
      parameters.maxDuration(),
      parameters.requests()
    );
  }

  @Override
  public void buildGraph() {
    // Initialize transit model index which is needed by the nearby stop finder.
    transitRepository.index();

    // The linker will use streets if they are available, or straight-line distance otherwise.
    NearbyStopFinder nearbyStopFinder = createNearbyStopFinder();

    List<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);

    // Flex transfers only use the WALK mode, and are only needed when flex routing is enabled.
    List<RouteRequest> flexTransferRequests = OTPFeature.FlexRouting.isOn()
      ? transferRequests
          .stream()
          .filter(transferProfile -> transferProfile.journey().transfer().mode() == StreetMode.WALK)
          .toList()
      : List.of();

    LOG.info("Creating flex transfers based on requests:");
    flexTransferRequests.forEach(transferProfile -> LOG.info(transferProfile.toString()));

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
        /* Use a map based on the list of edges, so that only distinct transfers are stored. */
        Map<TransferKey, PathTransfer> distinctTransfers = new HashMap<>();
        RegularStop stop = Objects.requireNonNull(
          transitRepository.getSiteRepository().getRegularStop(ts0.getId())
        );

        if (stop.transfersNotAllowed()) {
          return;
        }

        LOG.debug("Linking stop '{}' {}", stop, ts0);

        calculateFlexTransfers(
          nearbyStopFinder,
          flexTransferRequests,
          ts0,
          stop,
          distinctTransfers
        );

        LOG.debug("Linked stop {} with {} flex transfers.", stop, distinctTransfers.size());
        if (!distinctTransfers.isEmpty()) {
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

    transferRepository.addAllTransfersByStops(transfersByStop);

    LOG.info(progress.completeMessage());
    LOG.info(
      "Done connecting stops to one another. Created a total of {} flex transfers from {} stops.",
      nTransfersTotal,
      nLinkedStops
    );
  }

  /**
   * Factory method for creating a NearbyStopFinder. Will create different finders depending on
   * whether the graph has a street network and if ConsiderPatternsForDirectTransfers feature is
   * enabled.
   */
  private NearbyStopFinder createNearbyStopFinder() {
    var transitService = new DefaultTransitService(transitRepository);
    NearbyStopFinder finder;
    if (!graph.hasStreets) {
      LOG.info(
        "Creating direct transfer edges between stops using straight line distance (not streets)..."
      );
      finder = new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);
    } else {
      LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
      finder = StreetNearbyStopFinder.of(null).build();
    }

    if (OTPFeature.ConsiderPatternsForDirectTransfers.isOn()) {
      return new PatternConsideringNearbyStopFinder(transitService, finder);
    } else {
      return finder;
    }
  }

  private void createPathTransfer(
    StopLocation from,
    StopLocation to,
    NearbyStop sd,
    Map<TransferKey, PathTransfer> distinctTransfers,
    StreetMode mode
  ) {
    TransferKey transferKey = new TransferKey(from, to, sd.edges);
    PathTransfer pathTransfer = distinctTransfers.get(transferKey);
    if (pathTransfer == null) {
      // If the PathTransfer can't be found, it is created.
      distinctTransfers.put(
        transferKey,
        new PathTransfer(from, to, sd.distance, sd.edges, EnumSet.of(mode))
      );
    } else {
      // If the PathTransfer is found, a new PathTransfer with the added mode is created.
      distinctTransfers.put(transferKey, pathTransfer.withAddedMode(mode));
    }
  }

  /**
   * Generates the flex transfers for a stop, in both directions: {@code Stop -> AreaStop/GroupStop}
   * (used for flex egress) and {@code AreaStop/GroupStop -> Stop} (used for flex access).
   */
  private void calculateFlexTransfers(
    NearbyStopFinder nearbyStopFinder,
    List<RouteRequest> flexTransferRequests,
    TransitStopVertex ts0,
    RegularStop stop,
    Map<TransferKey, PathTransfer> distinctTransfers
  ) {
    var repository = transitRepository.getSiteRepository();
    for (RouteRequest transferProfile : flexTransferRequests) {
      // Flex transfer requests only use the WALK mode.
      StreetMode mode = StreetMode.WALK;

      for (NearbyStop sd : nearbyStopFinder.findNearbyStops(
        ts0,
        transferProfile,
        mode,
        false,
        maxTransferDuration,
        NO_STOP_COUNT_LIMIT
      )) {
        var nearbyStop = repository.getStopLocation(sd.stopId);
        if (nearbyStop.equals(stop) || nearbyStop instanceof RegularStop) {
          continue;
        }
        createPathTransfer(stop, nearbyStop, sd, distinctTransfers, mode);
      }

      for (NearbyStop sd : nearbyStopFinder.findNearbyStops(
        ts0,
        transferProfile,
        mode,
        true,
        maxTransferDuration,
        NO_STOP_COUNT_LIMIT
      )) {
        var nearbyStop = repository.getStopLocation(sd.stopId);
        if (nearbyStop.equals(stop) || nearbyStop instanceof RegularStop) {
          continue;
        }
        createPathTransfer(nearbyStop, stop, sd, distinctTransfers, mode);
      }
    }
  }

  private record TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {}
}
