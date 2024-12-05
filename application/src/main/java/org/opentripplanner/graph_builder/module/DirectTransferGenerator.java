package org.opentripplanner.graph_builder.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.opentripplanner.street.model.vertex.Vertex;
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

  private final Duration defaultMaxTransferDuration;

  private final List<RouteRequest> transferRequests;
  private final Map<StreetMode, TransferParameters> transferParametersForMode;
  private final Graph graph;
  private final TimetableRepository timetableRepository;
  private final DataImportIssueStore issueStore;

  public DirectTransferGenerator(
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    Duration defaultMaxTransferDuration,
    List<RouteRequest> transferRequests
  ) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
    this.defaultMaxTransferDuration = defaultMaxTransferDuration;
    this.transferRequests = transferRequests;
    this.transferParametersForMode = Collections.emptyMap();
  }

  public DirectTransferGenerator(
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    Duration defaultMaxTransferDuration,
    List<RouteRequest> transferRequests,
    Map<StreetMode, TransferParameters> transferParametersForMode
  ) {
    this.graph = graph;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
    this.defaultMaxTransferDuration = defaultMaxTransferDuration;
    this.transferRequests = transferRequests;
    this.transferParametersForMode = transferParametersForMode;
  }

  @Override
  public void buildGraph() {
    /* Initialize transit model index which is needed by the nearby stop finder. */
    timetableRepository.index();

    List<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);
    Set<TransitStopVertex> carsAllowedStops = timetableRepository
      .getStopLocationsUsedForCarsAllowedTrips()
      .stream()
      .map(StopLocation::getId)
      .map(graph::getStopVertexForStopId)
      // filter out null values if no TransitStopVertex is found for ID
      .filter(TransitStopVertex.class::isInstance)
      .collect(Collectors.toSet());

    LOG.info("Creating transfers based on requests:");
    transferRequests.forEach(transferProfile -> LOG.info(transferProfile.toString()));
    if (transferParametersForMode.isEmpty()) {
      LOG.info("No mode-specific transfer configurations provided.");
    } else {
      LOG.info("Using transfer configurations for modes:");
      transferParametersForMode.forEach((mode, transferParameters) ->
        LOG.info(mode + ": " + transferParameters)
      );
    }

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

    List<RouteRequest> defaultTransferRequests = new ArrayList<>();
    List<RouteRequest> carsAllowedStopTransferRequests = new ArrayList<>();
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

    /* The linker will use streets if they are available, or straight-line distance otherwise. */
    HashMap<StreetMode, NearbyStopFinder> defaultNearbyStopFinders = new HashMap<>();
    /* These are used for calculating transfers only between carsAllowedStops. */
    HashMap<StreetMode, NearbyStopFinder> carsAllowedStopNearbyStopFinders = new HashMap<>();

    // Parse transfer parameters.
    for (RouteRequest transferProfile : transferRequests) {
      StreetMode mode = transferProfile.journey().transfer().mode();
      TransferParameters transferParameters = transferParametersForMode.get(mode);
      if (transferParameters != null) {
        // Disable normal transfer calculations if disableDefaultTransfers is set in the build config.
        if (!transferParameters.disableDefaultTransfers()) {
          defaultTransferRequests.add(transferProfile);
          // Set mode-specific maxTransferDuration, if it is set in the build config.
          Duration maxTransferDuration = transferParameters.maxTransferDuration();
          if (maxTransferDuration == Duration.ZERO) {
            maxTransferDuration = defaultMaxTransferDuration;
          }
          defaultNearbyStopFinders.put(mode, createNearbyStopFinder(maxTransferDuration, Set.of()));
        }
        // Create transfers between carsAllowedStops for the specific mode if carsAllowedStopMaxTransferDuration is set in the build config.
        Duration carsAllowedStopMaxTransferDuration = transferParameters.carsAllowedStopMaxTransferDuration();
        if (carsAllowedStopMaxTransferDuration != Duration.ZERO) {
          carsAllowedStopTransferRequests.add(transferProfile);
          carsAllowedStopNearbyStopFinders.put(
            mode,
            createNearbyStopFinder(
              carsAllowedStopMaxTransferDuration,
              Collections.<Vertex>unmodifiableSet(carsAllowedStops)
            )
          );
        }
      } else {
        defaultTransferRequests.add(transferProfile);
        defaultNearbyStopFinders.put(
          mode,
          createNearbyStopFinder(defaultMaxTransferDuration, Set.of())
        );
      }
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

       /*  for (RouteRequest transferProfile : defaultTransferRequests) {
          StreetMode mode = transferProfile.journey().transfer().mode();
          findNearbyStops(
            defaultNearbyStopFinders.get(mode),
            ts0,
            transferProfile,
            stop,
            distinctTransfers
          );
        }
        if (OTPFeature.FlexRouting.isOn()) {
          for (RouteRequest transferProfile : defaultTransferRequests) {
            StreetMode mode = transferProfile.journey().transfer().mode();
            // This code is for finding transfers from AreaStops to Stops, transfers
            // from Stops to AreaStops and between Stops are already covered above.
            for (NearbyStop sd : defaultNearbyStopFinders
              .get(mode)
              .findNearbyStops(ts0, transferProfile, transferProfile.journey().transfer(), true)) {
              // Skip the origin stop, loop transfers are not needed.
              if (sd.stop == stop) {
                continue;
              }
              if (sd.stop instanceof RegularStop) {
                continue;
              } */
        
        // Calculate default transfers.
        for (RouteRequest transferProfile : transferRequests) {
          StreetMode mode = transferProfile.journey().transfer().mode();
          findNearbyStops(
            defaultNearbyStopFinders.get(mode),
            ts0,
            transferProfile,
            stop,
            distinctTransfers,
            mode
          );
        }
        // Calculate flex transfers if flex routing is enabled.
        for (RouteRequest transferProfile : flexTransferRequests) {
          StreetMode mode = transferProfile.journey().transfer().mode();
          // This code is for finding transfers from AreaStops to Stops, transfers
          // from Stops to AreaStops and between Stops are already covered above.
          for (NearbyStop sd : defaultNearbyStopFinders.get(mode).findNearbyStops(
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
              EnumSet<StreetMode> modes = EnumSet.of(mode);
              distinctTransfers.put(
                transferKey,
                new PathTransfer(sd.stop, stop, sd.distance, sd.edges, modes)
              );
            } else {
              pathTransfer.addMode(mode);
            }
          }
        }

        // This calculates transfers between stops that can use trips with cars.
        if (carsAllowedStops.contains(ts0)) {
          for (RouteRequest transferProfile : carsAllowedStopTransferRequests) {
            StreetMode mode = transferProfile.journey().transfer().mode();
            findNearbyStops(
              carsAllowedStopNearbyStopFinders.get(mode),
              ts0,
              transferProfile,
              stop,
              distinctTransfers,
              mode
            );
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
    for (StreetMode mode : transferRequests
      .stream()
      .map(transferProfile -> transferProfile.journey().transfer().mode())
      .collect(Collectors.toSet())) {
      LOG.info(
        "Created {} transfers for mode {}.",
        transfersByStop
          .values()
          .stream()
          .filter(pathTransfer -> pathTransfer.getModes().contains(mode))
          .count(),
        mode
      );
    }
  }

  /**
   * Factory method for creating a NearbyStopFinder. Will create different finders depending on
   * whether the graph has a street network and if ConsiderPatternsForDirectTransfers feature is
   * enabled.
   */
  private NearbyStopFinder createNearbyStopFinder(
    Duration radiusByDuration,
    Set<Vertex> findOnlyVertices
  ) {
    var transitService = new DefaultTransitService(timetableRepository);
    NearbyStopFinder finder;
    if (!graph.hasStreets) {
      LOG.info(
        "Creating direct transfer edges between stops using straight line distance (not streets)..."
      );
      finder = new StraightLineNearbyStopFinder(transitService, radiusByDuration);
    } else {
      LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
      finder = new StreetNearbyStopFinder(radiusByDuration, 0, null, Set.of(), findOnlyVertices);
    }

    if (OTPFeature.ConsiderPatternsForDirectTransfers.isOn()) {
      return new PatternConsideringNearbyStopFinder(transitService, finder);
    } else {
      return finder;
    }
  }

  private void findNearbyStops(
    NearbyStopFinder nearbyStopFinder,
    TransitStopVertex ts0,
    RouteRequest transferProfile,
    RegularStop stop,
    Map<TransferKey, PathTransfer> distinctTransfers,
    StreetMode mode
  ) {
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
        EnumSet<StreetMode> modes = EnumSet.of(mode);
        distinctTransfers.put(
          transferKey,
          new PathTransfer(stop, sd.stop, sd.distance, sd.edges, modes)
        );
      } else {
        pathTransfer.addMode(mode);
      }
    }
  }

  private record TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {}
}
