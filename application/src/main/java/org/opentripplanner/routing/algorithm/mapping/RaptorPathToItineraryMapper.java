package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toOtpDomainCost;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.FrequencyTransitLegBuilder;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegConstructionSupport;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.UnknownTransitPathLeg;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * This maps the paths found by the Raptor search algorithm to the itinerary structure currently
 * used by OTP. The paths, access/egress transfers and transit layer only contains the minimal
 * information needed for routing. Additional information has to be fetched from the graph index to
 * create complete itineraries that can be shown in a trip planner.
 */
public class RaptorPathToItineraryMapper<T extends TripSchedule> {

  private final RaptorTransitData raptorTransitData;

  private final RouteRequest request;
  private final StreetSearchRequest transferStreetRequest;
  private final StreetMode transferMode;
  private final ZonedDateTime transitSearchTimeZero;

  private final GraphPathToItineraryMapper graphPathToItineraryMapper;
  private final TransitService transitService;

  /**
   * Constructs an itinerary mapper for a request and a set of results
   *
   * @param raptorTransitData          the currently active transit layer (may have real-time data
   *                              applied)
   * @param transitSearchTimeZero the point in time all times in seconds are counted from
   * @param request               the current routing request
   */
  public RaptorPathToItineraryMapper(
    Graph graph,
    TransitService transitService,
    RaptorTransitData raptorTransitData,
    ZonedDateTime transitSearchTimeZero,
    RouteRequest request
  ) {
    this.raptorTransitData = raptorTransitData;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.transferMode = request.journey().transfer().mode();
    this.request = request;
    this.transferStreetRequest = StreetSearchRequestMapper.mapToTransferRequest(request).build();
    this.graphPathToItineraryMapper =
      new GraphPathToItineraryMapper(
        transitService.getTimeZone(),
        graph.streetNotesService,
        graph.ellipsoidToGeoidDifference
      );
    this.transitService = transitService;
  }

  public Itinerary createItinerary(RaptorPath<T> path) {
    if (path.isUnknownPath()) {
      return mapDirectPath(path);
    }

    var optimizedPath = path instanceof OptimizedPath ? (OptimizedPath<TripSchedule>) path : null;

    var accessPathLeg = Objects.requireNonNull(path.accessLeg());
    // Map access leg
    List<Leg> legs = new ArrayList<>(mapAccessLeg(accessPathLeg));

    PathLeg<T> pathLeg = path.accessLeg().nextLeg();

    Leg transitLeg = null;

    PathLeg<T> previousLeg = null;
    while (!pathLeg.isEgressLeg()) {
      // Map transit leg
      if (pathLeg.isTransitLeg()) {
        if (
          OTPFeature.ExtraTransferLegOnSameStop.isOn() &&
          isPathTransferAtSameStop(previousLeg, pathLeg)
        ) {
          legs.add(createTransferLegAtSameStop(previousLeg, pathLeg));
        }
        transitLeg = mapTransitLeg(transitLeg, pathLeg.asTransitLeg());
        legs.add(transitLeg);
      }
      // Map transfer leg
      else if (pathLeg.isTransferLeg()) {
        if (includeTransferInItinerary(transitLeg)) {
          legs.addAll(
            mapTransferLeg(
              pathLeg.asTransferLeg(),
              StreetModeToTransferTraverseModeMapper.map(transferMode)
            )
          );
        }
      }

      previousLeg = pathLeg;
      pathLeg = pathLeg.nextLeg();
    }

    // Map egress leg
    EgressPathLeg<T> egressPathLeg = pathLeg.asEgressLeg();
    Itinerary mapped = mapEgressLeg(egressPathLeg);
    legs.addAll(mapped == null ? List.of() : mapped.getLegs());

    Itinerary itinerary = Itinerary.createScheduledTransitItinerary(legs);

    // Map general itinerary fields
    itinerary.setArrivedAtDestinationWithRentedVehicle(
      mapped != null && mapped.isArrivedAtDestinationWithRentedVehicle()
    );

    if (optimizedPath != null) {
      itinerary.setWaitTimeOptimizedCost(
        toOtpDomainCost(optimizedPath.generalizedCostWaitTimeOptimized())
      );
      itinerary.setTransferPriorityCost(toOtpDomainCost(optimizedPath.transferPriorityCost()));
    }

    var penaltyCost = 0;

    var accessPenalty = mapAccessEgressPenalty(accessPathLeg.access());
    itinerary.setAccessPenalty(accessPenalty);
    penaltyCost += accessPenalty.cost().toSeconds();

    var egressPenalty = mapAccessEgressPenalty(egressPathLeg.egress());
    itinerary.setEgressPenalty(egressPenalty);
    penaltyCost += egressPenalty.cost().toSeconds();

    if (path.isC2Set()) {
      itinerary.setGeneralizedCost2(path.c2());
    }

    itinerary.setGeneralizedCost(toOtpDomainCost(path.c1()) - penaltyCost);

    return itinerary;
  }

  private static <T extends TripSchedule> boolean isPathTransferAtSameStop(
    PathLeg<T> previousLeg,
    PathLeg<T> currentLeg
  ) {
    return (
      previousLeg != null &&
      previousLeg.isTransitLeg() &&
      currentLeg.isTransitLeg() &&
      !previousLeg.asTransitLeg().isStaySeatedOntoNextLeg() &&
      (previousLeg.asTransitLeg().toStop() == currentLeg.asTransitLeg().fromStop())
    );
  }

  private List<Leg> mapAccessLeg(AccessPathLeg<T> accessPathLeg) {
    if (accessPathLeg.access().isFree()) {
      return List.of();
    }

    var subItinerary = mapAccessEgressPathLeg(accessPathLeg.access());

    if (subItinerary.getLegs().isEmpty()) {
      return List.of();
    }

    int fromTime = accessPathLeg.fromTime();

    return subItinerary.withTimeShiftToStartAt(createZonedDateTime(fromTime)).getLegs();
  }

  private Leg mapTransitLeg(Leg prevTransitLeg, TransitPathLeg<T> pathLeg) {
    T tripSchedule = pathLeg.trip();

    // If the next leg is an egress leg without a duration then this transit leg
    // arrives at the destination.
    // The cost of the egress should therefore be included in the last transit
    // leg given that it is the last leg.
    int lastLegCost = 0;
    PathLeg<T> nextLeg = pathLeg.nextLeg();
    if (nextLeg.isEgressLeg() && isFree(nextLeg.asEgressLeg())) {
      lastLegCost = pathLeg.nextLeg().c1();
    }

    // Find stop positions in pattern where this leg boards and alights.
    // We cannot assume every stop appears only once in a pattern, so we
    // have to match stop and time.
    int boardStopIndexInPattern = tripSchedule.findDepartureStopPosition(
      pathLeg.fromTime(),
      pathLeg.fromStop()
    );
    int alightStopIndexInPattern = tripSchedule.findArrivalStopPosition(
      pathLeg.toTime(),
      pathLeg.toStop()
    );

    if (tripSchedule.isFrequencyBasedTrip()) {
      int frequencyHeadwayInSeconds = tripSchedule.frequencyHeadwayInSeconds();
      return new FrequencyTransitLegBuilder()
        .withTripTimes(tripSchedule.getOriginalTripTimes())
        .withTripPattern(tripSchedule.getOriginalTripPattern())
        .withBoardStopIndexInPattern(boardStopIndexInPattern)
        .withAlightStopIndexInPattern(alightStopIndexInPattern)
        .withStartTime(createZonedDateTime(pathLeg.fromTime() + frequencyHeadwayInSeconds))
        .withEndTime(createZonedDateTime(pathLeg.toTime()))
        .withServiceDate(tripSchedule.getServiceDate())
        .withZoneId(transitSearchTimeZero.getZone().normalized())
        .withTransferFromPreviousLeg(
          (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg())
        )
        .withTransferToNextLeg((ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg())
        .withGeneralizedCost(toOtpDomainCost(pathLeg.c1() + lastLegCost))
        .withFrequencyHeadwayInSeconds(frequencyHeadwayInSeconds)
        .build();
    }

    TripOnServiceDate tripOnServiceDate = getTripOnServiceDate(tripSchedule);

    var distanceMeters = LegConstructionSupport.computeDistanceMeters(
      tripSchedule.getOriginalTripPattern(),
      boardStopIndexInPattern,
      alightStopIndexInPattern
    );
    return new ScheduledTransitLegBuilder<>()
      .withTripTimes(tripSchedule.getOriginalTripTimes())
      .withTripPattern(tripSchedule.getOriginalTripPattern())
      .withBoardStopIndexInPattern(boardStopIndexInPattern)
      .withAlightStopIndexInPattern(alightStopIndexInPattern)
      .withStartTime(createZonedDateTime(pathLeg.fromTime()))
      .withEndTime(createZonedDateTime(pathLeg.toTime()))
      .withDistanceMeters(distanceMeters)
      .withServiceDate(tripSchedule.getServiceDate())
      .withZoneId(transitSearchTimeZero.getZone().normalized())
      .withTripOnServiceDate(tripOnServiceDate)
      .withTransferFromPreviousLeg(
        (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg())
      )
      .withTransferToNextLeg((ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg())
      .withGeneralizedCost(toOtpDomainCost(pathLeg.c1() + lastLegCost))
      .build();
  }

  private TripOnServiceDate getTripOnServiceDate(T tripSchedule) {
    if (tripSchedule.getOriginalTripTimes() == null) {
      return null;
    }
    TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(
      tripSchedule.getOriginalTripTimes().getTrip().getId(),
      tripSchedule.getServiceDate()
    );
    return transitService.getTripOnServiceDate(tripIdAndServiceDate);
  }

  private boolean isFree(EgressPathLeg<T> egressPathLeg) {
    return egressPathLeg.egress().isFree();
  }

  /**
   * If a routing result transfers at the very same stop, RAPTOR doesn't add a path leg. However,
   * sometimes we want to create a zero distance leg so a UI can show a transfer. Since it would
   * be considered backwards-incompatible, this is an opt-in feature.
   */
  private Leg createTransferLegAtSameStop(PathLeg<T> previousLeg, PathLeg<T> nextLeg) {
    var transferStop = Place.forStop(raptorTransitData.getStopByIndex(previousLeg.toStop()));
    return StreetLeg
      .create()
      .withMode(TraverseMode.WALK)
      .withStartTime(createZonedDateTime(previousLeg.toTime()))
      .withEndTime(createZonedDateTime(nextLeg.fromTime()))
      .withFrom(transferStop)
      .withTo(transferStop)
      .withDistanceMeters(0)
      .withGeneralizedCost(0)
      .withGeometry(GeometryUtils.makeLineString(transferStop.coordinate, transferStop.coordinate))
      .withWalkSteps(List.of())
      .build();
  }

  private List<Leg> mapTransferLeg(TransferPathLeg<T> pathLeg, TraverseMode transferMode) {
    var transferFromStop = raptorTransitData.getStopByIndex(pathLeg.fromStop());
    var transferToStop = raptorTransitData.getStopByIndex(pathLeg.toStop());

    var raptorTransfer = pathLeg.transfer();

    Place from = Place.forStop(transferFromStop);
    Place to = Place.forStop(transferToStop);

    if (raptorTransfer instanceof DefaultRaptorTransfer dftTx) {
      return mapTransferLeg(pathLeg, dftTx.transfer(), transferMode, from, to);
    }
    if (raptorTransfer instanceof ViaCoordinateTransfer viaTx) {
      return mapViaCoordinateTransferLeg(pathLeg, viaTx, transferMode, from, to);
    }
    throw new ClassCastException("Unknown transfer type: " + raptorTransfer.getClass());
  }

  private Itinerary mapEgressLeg(EgressPathLeg<T> egressPathLeg) {
    if (isFree(egressPathLeg)) {
      return null;
    }

    var subItinerary = mapAccessEgressPathLeg(egressPathLeg.egress());

    if (subItinerary.getLegs().isEmpty()) {
      return null;
    }

    // No need to remove penalty here, since we use the fromTime only
    return subItinerary.withTimeShiftToStartAt(createZonedDateTime(egressPathLeg.fromTime()));
  }

  private List<Leg> mapTransferLeg(
    PathLeg<T> pathLeg,
    Transfer transfer,
    TraverseMode transferMode,
    Place from,
    Place to
  ) {
    List<Edge> edges = transfer.getEdges();
    if (edges == null || edges.isEmpty()) {
      return List.of(
        StreetLeg
          .create()
          .withMode(transferMode)
          .withStartTime(createZonedDateTime(pathLeg.fromTime()))
          .withEndTime(createZonedDateTime(pathLeg.toTime()))
          .withFrom(from)
          .withTo(to)
          .withDistanceMeters(transfer.getDistanceMeters())
          .withGeneralizedCost(toOtpDomainCost(pathLeg.c1()))
          .withGeometry(GeometryUtils.makeLineString(transfer.getCoordinates()))
          .withWalkSteps(List.of())
          .build()
      );
    } else {
      return mapTransferLegWithEdges(pathLeg.fromTime(), edges);
    }
  }

  private List<Leg> mapViaCoordinateTransferLeg(
    PathLeg<T> pathLeg,
    ViaCoordinateTransfer transfer,
    TraverseMode transferMode,
    Place from,
    Place to
  ) {
    var fromLegs = mapTransferLegWithEdges(pathLeg.fromTime(), transfer.fromEdges());
    var toLegs = mapTransferLegWithEdges(pathLeg.toTime(), transfer.toEdges());

    if (fromLegs.isEmpty() || toLegs.isEmpty()) {
      throw new IllegalStateException(
        "There need to be at least one edges to get from a stop to the via coordinate and back"
      );
    }
    // We need to timeshift the toLegs
    long toDuration = toLegs.stream().mapToLong(l -> l.getDuration().toSeconds()).sum();

    toLegs = toLegs.stream().map(l -> l.withTimeShift(Duration.ofSeconds(-toDuration))).toList();

    return ListUtils.combine(fromLegs, toLegs);
  }

  private List<Leg> mapTransferLegWithEdges(int fromTime, List<Edge> edges) {
    StateEditor se = new StateEditor(edges.getFirst().getFromVertex(), transferStreetRequest);
    se.setTimeSeconds(createZonedDateTime(fromTime).toEpochSecond());

    State s = se.makeState();
    ArrayList<State> transferStates = new ArrayList<>();
    transferStates.add(s);
    for (Edge e : edges) {
      var states = e.traverse(s);
      if (State.isEmpty(states)) {
        s = null;
      } else {
        transferStates.add(states[0]);
        s = states[0];
      }
    }
    State[] states = transferStates.toArray(State[]::new);
    var graphPath = new GraphPath<>(states[states.length - 1]);
    var subItinerary = graphPathToItineraryMapper.generateItinerary(graphPath);
    return subItinerary.getLegs();
  }

  private Itinerary mapDirectPath(RaptorPath<T> path) {
    return Itinerary.createScheduledTransitItinerary(
      List.of(
        new UnknownTransitPathLeg(
          mapPlace(request.from()),
          mapPlace(request.to()),
          createZonedDateTime(path.startTime()),
          createZonedDateTime(path.endTime()),
          path.numberOfTransfers()
        )
      )
    );
  }

  private Place mapPlace(GenericLocation location) {
    return Place.normal(location.lat, location.lng, new NonLocalizedString(location.label));
  }

  private ZonedDateTime createZonedDateTime(int timeInSeconds) {
    return transitSearchTimeZero.plusSeconds(timeInSeconds);
  }

  /**
   * Include transfer leg in itinerary if the path is a "physical" path-leg between two stops, like
   * walk or bicycle. Do NOT include it if it represents a stay-seated transfer. See more details in
   * https://github.com/opentripplanner/OpenTripPlanner/issues/5086.
   * TODO: the logic should be revisited when adding support for transfer between on-board flex
   *       access and transit.
   */
  private boolean includeTransferInItinerary(Leg transitLegBeforeTransfer) {
    return (
      transitLegBeforeTransfer == null ||
      transitLegBeforeTransfer.getTransferToNextLeg() == null ||
      !transitLegBeforeTransfer.getTransferToNextLeg().getTransferConstraint().isStaySeated()
    );
  }

  private Itinerary mapAccessEgressPathLeg(RaptorAccessEgress accessEgress) {
    return accessEgress
      .findOriginal(RoutingAccessEgress.class)
      .map(RoutingAccessEgress::getLastState)
      .map(GraphPath::new)
      .map(graphPathToItineraryMapper::generateItinerary)
      .orElseThrow();
  }

  private TimeAndCost mapAccessEgressPenalty(RaptorAccessEgress accessEgress) {
    return accessEgress
      .findOriginal(RoutingAccessEgress.class)
      .map(RoutingAccessEgress::penalty)
      .orElseThrow();
  }
}
