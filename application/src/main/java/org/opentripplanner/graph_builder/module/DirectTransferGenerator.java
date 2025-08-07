package org.opentripplanner.graph_builder.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.opentripplanner.transit.service.TransitService;
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

  /**
   * Constructor used in tests. This initializes transferParametersForMode as an empty map.
   */
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
    this.transferParametersForMode = Map.of();
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
    // Initialize transit model index which is needed by the nearby stop finder.
    timetableRepository.index();

    // The linker will use streets if they are available, or straight-line distance otherwise.
    NearbyStopFinder nearbyStopFinder = createNearbyStopFinder(defaultMaxTransferDuration);

    List<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);
    Set<StopLocation> carsAllowedStops =
      timetableRepository.getStopLocationsUsedForCarsAllowedTrips();
    Set<StopLocation> bikesAllowedStops =
      timetableRepository.getStopLocationsUsedForBikesAllowedTrips();

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

    // Parse the transfer configuration from the parameters given in the build config.
    TransferConfiguration transferConfiguration = parseTransferParameters(nearbyStopFinder);

    stops
      .stream()
      //.parallel()
      .forEach(ts0 -> {
        /* Make transfers to each nearby stop that has lowest weight on some trip pattern.
         * Use map based on the list of edges, so that only distinct transfers are stored. */
        Map<TransferKey, PathTransfer> distinctTransfers = new HashMap<>();
        RegularStop stop = ts0.getStop();

        if (stop.transfersNotAllowed()) {
          return;
        }

        LOG.debug("Linking stop '{}' {}", stop, ts0);

        calculateDefaultTransfers(
          transferConfiguration,
          ts0,
          stop,
          distinctTransfers,
          bikesAllowedStops
        );
        calculateFlexTransfers(transferConfiguration, ts0, stop, distinctTransfers);
        calculateCarsAllowedTransfers(
          transferConfiguration,
          ts0,
          stop,
          distinctTransfers,
          carsAllowedStops
        );

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
  private NearbyStopFinder createNearbyStopFinder(Duration radiusAsDuration) {
    var transitService = new DefaultTransitService(timetableRepository);
    NearbyStopFinder finder;
    if (!graph.hasStreets) {
      LOG.info(
        "Creating direct transfer edges between stops using straight line distance (not streets)..."
      );
      finder = new StraightLineNearbyStopFinder(transitService, radiusAsDuration);
    } else {
      LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
      finder = new StreetNearbyStopFinder(radiusAsDuration, 0, null);
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
   * This method parses the given transfer parameters into a transfer configuration and checks for invalid input.
   */
  private TransferConfiguration parseTransferParameters(NearbyStopFinder nearbyStopFinder) {
    List<RouteRequest> defaultTransferRequests = new ArrayList<>();
    List<RouteRequest> carsAllowedStopTransferRequests = new ArrayList<>();
    List<RouteRequest> flexTransferRequests = new ArrayList<>();
    HashMap<StreetMode, NearbyStopFinder> defaultNearbyStopFinderForMode = new HashMap<>();
    // These are used for calculating transfers only between carsAllowedStops.
    HashMap<StreetMode, NearbyStopFinder> carsAllowedStopNearbyStopFinderForMode = new HashMap<>();

    // Check that the mode specified in transferParametersForMode can also be found in transferRequests.
    for (StreetMode mode : transferParametersForMode.keySet()) {
      if (
        transferRequests
          .stream()
          .noneMatch(transferProfile -> transferProfile.journey().transfer().mode() == mode)
      ) {
        throw new IllegalArgumentException(
          String.format(
            "Mode %s is used in transferParametersForMode but not in transferRequests",
            mode
          )
        );
      }
    }

    for (RouteRequest transferProfile : transferRequests) {
      StreetMode mode = transferProfile.journey().transfer().mode();
      TransferParameters transferParameters = transferParametersForMode.get(mode);
      if (transferParameters != null) {
        // WALK mode transfers can not be disabled. For example, flex transfers need them.
        if (transferParameters.disableDefaultTransfers() && mode == StreetMode.WALK) {
          throw new IllegalArgumentException("WALK mode transfers can not be disabled");
        }
        // Disable normal transfer calculations for the specific mode, if disableDefaultTransfers is set in the build config.
        if (!transferParameters.disableDefaultTransfers()) {
          defaultTransferRequests.add(transferProfile);
          // Set mode-specific maxTransferDuration, if it is set in the build config.
          Duration maxTransferDuration = transferParameters.maxTransferDuration();
          if (maxTransferDuration != null) {
            defaultNearbyStopFinderForMode.put(mode, createNearbyStopFinder(maxTransferDuration));
          } else {
            defaultNearbyStopFinderForMode.put(mode, nearbyStopFinder);
          }
        }
        // Create transfers between carsAllowedStops for the specific mode if carsAllowedStopMaxTransferDuration is set in the build config.
        Duration carsAllowedStopMaxTransferDuration =
          transferParameters.carsAllowedStopMaxTransferDuration();
        if (carsAllowedStopMaxTransferDuration != null) {
          carsAllowedStopTransferRequests.add(transferProfile);
          carsAllowedStopNearbyStopFinderForMode.put(
            mode,
            createNearbyStopFinder(carsAllowedStopMaxTransferDuration)
          );
        }
      } else {
        defaultTransferRequests.add(transferProfile);
        defaultNearbyStopFinderForMode.put(mode, nearbyStopFinder);
      }
    }

    // Flex transfer requests only use the WALK mode.
    if (OTPFeature.FlexRouting.isOn()) {
      flexTransferRequests.addAll(
        transferRequests
          .stream()
          .filter(transferProfile -> transferProfile.journey().transfer().mode() == StreetMode.WALK)
          .toList()
      );
    }

    return new TransferConfiguration(
      defaultTransferRequests,
      carsAllowedStopTransferRequests,
      flexTransferRequests,
      defaultNearbyStopFinderForMode,
      carsAllowedStopNearbyStopFinderForMode
    );
  }

  private boolean doesNotServeBikes(Set<StopLocation> bikesAllowedStops, StopLocation stop) {
    var transitService = new DefaultTransitService(timetableRepository);
    if (OTPFeature.LimitBikeTransfer.isOff()) {
      return false;
    }
    return !transitService.findPatterns(stop).isEmpty() && !bikesAllowedStops.contains(stop);
  }

  /**
   * This method calculates default transfers.
   */
  private void calculateDefaultTransfers(
    TransferConfiguration transferConfiguration,
    TransitStopVertex ts0,
    RegularStop stop,
    Map<TransferKey, PathTransfer> distinctTransfers,
    Set<StopLocation> bikesAllowedStops
  ) {
    for (RouteRequest transferProfile : transferConfiguration.defaultTransferRequests()) {
      StreetMode mode = transferProfile.journey().transfer().mode();

      if (mode.includesBiking() && doesNotServeBikes(bikesAllowedStops, stop)) {
        return;
      }

      var nearbyStops = transferConfiguration
        .defaultNearbyStopFinderForMode()
        .get(mode)
        .findNearbyStops(ts0, transferProfile, transferProfile.journey().transfer(), false);
      for (NearbyStop sd : nearbyStops) {
        // Skip the origin stop, loop transfers are not needed.
        if (sd.stop == stop) {
          continue;
        }
        if (mode == StreetMode.BIKE && doesNotServeBikes(bikesAllowedStops, sd.stop)) {
          continue;
        }

        if (sd.stop.transfersNotAllowed()) {
          continue;
        }
        createPathTransfer(stop, sd.stop, sd, distinctTransfers, mode);
      }
    }
  }

  /**
   * This method calculates flex transfers if flex routing is enabled.
   */
  private void calculateFlexTransfers(
    TransferConfiguration transferConfiguration,
    TransitStopVertex ts0,
    RegularStop stop,
    Map<TransferKey, PathTransfer> distinctTransfers
  ) {
    for (RouteRequest transferProfile : transferConfiguration.flexTransferRequests()) {
      // Flex transfer requests only use the WALK mode.
      StreetMode mode = StreetMode.WALK;
      var nearbyStops = transferConfiguration
        .defaultNearbyStopFinderForMode()
        .get(mode)
        .findNearbyStops(ts0, transferProfile, transferProfile.journey().transfer(), true);
      // This code is for finding transfers from AreaStops to Stops, transfers
      // from Stops to AreaStops and between Stops are already covered above.
      for (NearbyStop sd : nearbyStops) {
        // Skip the origin stop, loop transfers are not needed.
        if (sd.stop == stop) {
          continue;
        }
        if (sd.stop instanceof RegularStop) {
          continue;
        }
        // The TransferKey and PathTransfer are created differently for flex routing.
        createPathTransfer(sd.stop, stop, sd, distinctTransfers, mode);
      }
    }
  }

  /**
   * This method calculates transfers between stops that are visited by trips that allow cars, if configured.
   */
  private void calculateCarsAllowedTransfers(
    TransferConfiguration transferConfiguration,
    TransitStopVertex ts0,
    RegularStop stop,
    Map<TransferKey, PathTransfer> distinctTransfers,
    Set<StopLocation> carsAllowedStops
  ) {
    if (carsAllowedStops.contains(stop)) {
      for (RouteRequest transferProfile : transferConfiguration.carsAllowedStopTransferRequests()) {
        StreetMode mode = transferProfile.journey().transfer().mode();
        var nearbyStops = transferConfiguration
          .carsAllowedStopNearbyStopFinderForMode()
          .get(mode)
          .findNearbyStops(ts0, transferProfile, transferProfile.journey().transfer(), false);
        for (NearbyStop sd : nearbyStops) {
          // Skip the origin stop, loop transfers are not needed.
          if (sd.stop == stop) {
            continue;
          }
          if (sd.stop.transfersNotAllowed()) {
            continue;
          }
          // Only calculate transfers between carsAllowedStops.
          if (!carsAllowedStops.contains(sd.stop)) {
            continue;
          }
          createPathTransfer(stop, sd.stop, sd, distinctTransfers, mode);
        }
      }
    }
  }

  private record TransferConfiguration(
    List<RouteRequest> defaultTransferRequests,
    List<RouteRequest> carsAllowedStopTransferRequests,
    List<RouteRequest> flexTransferRequests,
    HashMap<StreetMode, NearbyStopFinder> defaultNearbyStopFinderForMode,
    HashMap<StreetMode, NearbyStopFinder> carsAllowedStopNearbyStopFinderForMode
  ) {}

  private record TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {}
}
